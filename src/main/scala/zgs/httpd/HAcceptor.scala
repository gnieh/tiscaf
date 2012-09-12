package zgs.httpd

import scala.collection.{ mutable => mute }

import scala.util.continuations._

private object HReqState extends Enumeration {
  val WaitsForHeader, WaitsForData, WaitsForPart, WaitsForOctets, IsInvalid, IsReady = Value
}

private class HConnData {
  import scala.collection.{ mutable => mute }
  var reqState : HReqState.Value = HReqState.WaitsForHeader
  var tail : Array[Byte] = new Array[Byte](0)
  var acceptedTotalLength : Long = 0L
  var header : Option[HReqHeader] = None
  var parMap : mute.Map[String, Seq[String]] = new mute.HashMap[String, Seq[String]]()
  var appLet : Option[(HApp, HLet[_])] = None
  var octetStream : Option[Array[Byte]] = None
  var parts : Option[HPartData] = None

  def toTalkData(aWriter : HWriter) : HTalkData = new HTalkData {
    def header = HConnData.this.header.get
    def parMap = Map[String, Seq[String]]() ++ HConnData.this.parMap
    def app = HConnData.this.appLet.get._1
    def octets = HConnData.this.octetStream
    def writer = aWriter
  }

  def reset : Unit = {
    reqState = HReqState.WaitsForHeader
    tail = new Array[Byte](0)
    acceptedTotalLength = 0L
    header = None
    parMap.clear
    appLet = None
    octetStream = None
    parts = None
  }
}

private trait HTalkData {
  def header : HReqHeader
  def parMap : Map[String, Seq[String]]
  def app : HApp
  def octets : Option[Array[Byte]]
  def writer : HWriter

  def aliveReq = try { header.isPersistent } catch { case _ => false }
}

private class HAcceptor(
    val writer : HWriter,
    apps : Seq[HApp],
    connectionTimeout : Int,
    onError : Throwable => Unit,
    maxPostDataLength : Int) {

  val in = new HConnData

  def accept(bytes : Array[Byte]) : Unit = {
    in.tail = in.tail ++ bytes

    in.reqState match {
      case HReqState.WaitsForHeader => inHeader
      case HReqState.WaitsForData   => inData
      case HReqState.WaitsForPart   => inParts
      case HReqState.WaitsForOctets => inOctets
      case x                        => // don't change
    }
  }

  private def maxHeaderLength = 8192 // ANLI to config?

  private def inHeader : Unit = {
    val till = in.tail.length - 4
    def findEol(start : Int) : Option[Int] = if (till < start) None else (start to till).find { i =>
      in.tail(i) == 13 &&
        in.tail(i + 1) == 10 &&
        in.tail(i + 2) == 13 &&
        in.tail(i + 3) == 10
    }
    // at least lines with method is expected: 'GET / HTTP/1.1' length is 14
    findEol(14) match {
      case None => in.reqState = HReqState.WaitsForHeader
      case Some(shift) => if (shift >= maxHeaderLength) in.reqState = HReqState.IsInvalid else {
        in.header = Some(new HReqHeader(new String(in.tail.take(shift), "ISO-8859-1") split ("\r\n")))
        in.tail = in.tail.slice(shift + 4, in.tail.length)

        in.header.get.reqType match {
          case HReqType.Get        => parseParams(in.header.get.query); in.reqState = HReqState.IsReady
          case HReqType.Delete     => parseParams(in.header.get.query); in.reqState = HReqState.IsReady
          case HReqType.PostData   => parseParams(in.header.get.query); inData
          case HReqType.PostOctets => parseParams(in.header.get.query); inOctets
          case HReqType.PostMulti  => parseParams(in.header.get.query); inParts
          case HReqType.Invalid    => in.reqState = HReqState.IsInvalid
        }
      }
    }
  }

  //post, form data
  private def inData : Unit = {
    val length = in.header.get.contentLength.get.toInt
    if (length > maxPostDataLength) in.reqState = HReqState.IsInvalid
    else if (length > in.tail.length) in.reqState = HReqState.WaitsForData
    else {
      in.reqState = HReqState.IsReady
      parseParams(new String(in.tail.take(length), "ISO-8859-1"))
    }
  }

  // post, unknown content type, falling back to octet mode
  private def inOctets : Unit = {
    val contentLength = in.header.get.contentLength.get.toInt
    if (contentLength + in.tail.length > maxPostDataLength) in.reqState = HReqState.IsInvalid
    else {
      in.octetStream match {
        case None         => in.octetStream = Some(in.tail.take(in.tail.length))
        case Some(octets) => in.octetStream = Some(octets ++ in.tail.take(in.tail.length))
      }
      in.tail = new Array[Byte](0)
      if (in.octetStream.get.length == contentLength) in.reqState = HReqState.IsReady
      else in.reqState = HReqState.WaitsForOctets
    }
  }

  //---------------- post, multipart
  private def inParts : Unit = {
    resolveAppLet
    in.appLet.get._2.partsAcceptor(in.header.get) match {
      case None => in.reqState = HReqState.IsInvalid
      case Some(acceptor) =>
        if (in.parts.isEmpty) {
          // at this point tail must have at least --boundary\r\n... or wait for bytes further
          val header = in.header.get
          def minSize = HParts.toBytes("--" + in.header.get.boundary.get + "\r\n").length
          if (in.tail.length >= minSize)
            in.parts = Some(new HPartData(acceptor, in.header.get.boundary.get))
        }

        if (in.parts.isEmpty) in.reqState = HReqState.WaitsForPart
        else {
          in.parts.get.tail = in.parts.get.tail ++ in.tail
          in.tail = new Array[Byte](0) // HMulti consumes all the tail
          HMulti.process(in.parts.get) match {
            case HReqState.IsInvalid =>
              in.reqState = HReqState.IsInvalid
              in.parts = None
            case x => // HReqState.IsReady or HReqState.WaitsForPart
              if (in.acceptedTotalLength + in.tail.length > in.header.get.contentLength.get)
                in.reqState = HReqState.IsInvalid
              else in.reqState = x
          }
        }
    }
  }

  // used both for query string and post data
  private def parseParams(s : String) : Unit = {
    def newPairs = s.split("&")
      .filter(_.length >= 2)
      .map(_.split("="))
      .filter(is => is.length > 0 && is.size > 0)
      .map { p => if (p.size == 1) Pair(p(0), "") else Pair(p(0), p(1)) }

    for (newPair <- newPairs) yield in.parMap.get(newPair._1) match {
      case None         => in.parMap(newPair._1) = Seq(newPair._2)
      case Some(oldSeq) => in.parMap(newPair._1) = oldSeq :+ newPair._2
    }
  }

  def talk : PeerWant.Value @suspendable = {
    val (app, let) = in.appLet.get
    val tk = new HTalk(in.toTalkData(writer))

    if ((app.tracking != HTracking.NotAllowed) &&
      (HSessMonitor.count.get(app) >= app.maxSessionsCount)) {
      try {
        new zgs.httpd.let.ErrLet(HStatus.ServiceUnavailable, "too many sessions") act (tk)
      } catch {
        case e =>
          onError(e)
      }
    } else {
      if (app.keepAlive && in.header.get.isPersistent) {
        dummy
        tk.setHeader("Connection", "keep-alive").setHeader("Keep-Alive", "timeout=" + connectionTimeout + ", max=100")
      } else {
        dummy
        tk.setHeader("Connection", "close").removeHeader("Keep-Alive")
      }

      try {
        let.act(tk)
      } catch {
        case e =>
          onError(e) // reporting HLet errors
          try {
            new zgs.httpd.let.ErrLet(HStatus.InternalServerError).act(tk)
          } // connection can be closed here...
          catch {
            case _ => // ...and "header is already sent" will be arised; don't report it.
              dummy
          }
      }
    }

    tk.close
  }

  def resolveAppLet : Unit = in.appLet match {
    case None =>
      val req = HReqData(in.toTalkData(writer))
      in.appLet = Some(HResolver.resolve(apps, req))
    case _ =>
  }
}

