package tiscaf

import java.util.Date

final class HTalk(data : HTalkData) {

  //----------------------------------------------------------------
  //---------------------------- main user API ---------------------
  //----------------------------------------------------------------

  // request
  val req : HReqData = HReqData(data)

  // is used both for request params decoding and generating output bytes from strings
  def encoding : String = data.app.encoding

  // setting status and response header
  def setStatus(code : HStatus.Value) : HTalk = withHead { resp.setStatus(code); this }
  def setStatus(code : HStatus.Value, msg : String) : HTalk = withHead { resp.setStatus(code, msg); this }

  def setHeader(name : String, value : String) : HTalk = withHead { resp.setHeader(name, value); this }
  def removeHeader(name : String) : Option[String] = withHead { resp.removeHeader(name) }
  def getHeader(name : String) : Option[String] = withHead { resp.getHeader(name) }

  def setContentLength(length : Long) : HTalk = setHeader("Content-Length", length.toString)
  def setCharacterEncoding(charset : String) : HTalk = setHeader("Character-Encoding", charset)
  def setContentType(cType : String) : HTalk = setHeader("Content-Type", cType)

  // output
  def write(ar : Array[Byte], offset : Int, length : Int) : HTalk = { out.write(ar, offset, length); this }
  def write(ar : Array[Byte]) : HTalk = write(ar, 0, ar.length)
  def bytes(s : String) : Array[Byte] = s.getBytes(encoding)
  def write(s : String) : HTalk = write(bytes(s))

  // session
  object ses extends scala.collection.mutable.Map[Any, Any] {

    // implementing mutable.Map
    def get(key : Any) : Option[Any] = if (isAllowed) session.get.get(key) else None
    def iterator : Iterator[(Any, Any)] = if (isAllowed) session.get.iterator else Nil.iterator
    def +=(kv : (Any, Any)) : this.type = if (isAllowed) { session.get += kv; this } else this
    def -=(key : Any) : this.type = if (isAllowed) { session.get -= key; this } else this

    override def size : Int = if (isAllowed) session.get.size else 0
    override def clear : Unit = if (isAllowed) session.get.clear

    override def foreach[U](f : Tuple2[Any, Any] => U) : Unit = if (isAllowed) session.get.foreach[U] { t => f(t) }

    // Map-related helpers
    def asString(key : Any) : Option[String] = get(key).collect { case x : String => x }
    def asBoolean(key : Any) : Option[Boolean] = get(key).collect { case x : Boolean => x }
    def asByte(key : Any) : Option[Byte] = get(key).collect { case x : Byte => x }
    def asShort(key : Any) : Option[Short] = get(key).collect { case x : Short => x }
    def asInt(key : Any) : Option[Int] = get(key).collect { case x : Int => x }
    def asLong(key : Any) : Option[Long] = get(key).collect { case x : Long => x }
    def asFloat(key : Any) : Option[Float] = get(key).collect { case x : Float => x }
    def asDouble(key : Any) : Option[Double] = get(key).collect { case x : Double => x }
    def asDate(key : Any) : Option[Date] = get(key).collect { case x : Date => x }

    def clearKeeping(keysToKeep : Any*) : Unit = if (isAllowed) (this.keySet -- keysToKeep.toSet).foreach(remove(_))

    // session-specific
    def tracking : HTracking.Value = data.app.tracking
    def isAllowed : Boolean = tracking != HTracking.NotAllowed
    def isValid : Boolean = isAllowed && session.get.isValid
    def invalidate : Unit = if (isAllowed) session.get.invalidate

    def idKey : String = data.app.sidKey
    def id : String = if (isAllowed) session.get.sid else ""
    def idPhrase : String = if (isAllowed) (";" + idKey + "=" + id) else ""
  }

  //---------------------- internals -----------------------------

  private[tiscaf] def close : PeerWant.Value = out.close

  private val resp = new HResponse
  private val session : Option[HSess] =
    if (ses.tracking == HTracking.NotAllowed) None
    else Some(new HSess(data, resp))

  private def withHead[T](code : => T) : T =
    if (out.isHeaded) { data.writer.dispose; sys.error("header is already sent") }
    else code

  private lazy val out : HOut =
    if (gzipable) new HGzipOut(data, resp, session)
    else if (data.app.buffered) new HBufferedOut(data, resp, session)
    else new HIdentOut(data, resp, session)

  private def gzipable = {
    def accepted = req.header("Accept-Encoding") match {
      case None    => false
      case Some(x) => x.contains("gzip")
    }
    def mimeOk = resp.getHeader("Content-Type") match {
      case None    => false
      case Some(x) => HMime.gzipable.contains(x.split(";")(0).trim)
    }
    data.app.gzip && accepted && mimeOk
  }
}

