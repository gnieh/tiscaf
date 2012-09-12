package zgs.httpd

trait HReqData {
  // common
  def method: HReqType.Value
  def host: Option[String]
  def port: Option[String]
  def uriPath: String
  def uriExt: Option[String]
  def query: String
  def remoteIp: String
  def contentLength: Option[Long]

  // header
  def header(key: String): Option[String]
  def headerKeys: scala.collection.Set[String]

  // parameters
  def paramsKeys: Set[String]
  def params(key: String): Seq[String]
  def param(key: String): Option[String]
  def softParam(key: String): String

  def asQuery(ignore: Set[String] = Set()): String

  // param(key) helpers
  def asByte(key: String): Option[Byte]
  def asShort(key: String): Option[Short]
  def asInt(key: String): Option[Int]
  def asLong(key: String): Option[Long]
  def asFloat(key: String): Option[Float]
  def asDouble(key: String): Option[Double]

  // POST/application/octet-stream case
  def octets: Option[Array[Byte]]
}

object HReqData {

  def apply(data: HTalkData) = new HReqData {
    // common
    def method: HReqType.Value = data.header.reqType
    def host: Option[String] = data.header.host
    def port: Option[String] = data.header.port
    def uriPath: String = decode(data.header.uriPath)
    def uriExt: Option[String] = data.header.uriExt // no decoding needed
    def query: String = decode(data.header.query)
    def remoteIp: String = data.writer.remoteIp
    def contentLength: Option[Long] = data.header.contentLength

    // header
    def header(key: String): Option[String] = data.header.header(key)
    def headerKeys: scala.collection.Set[String] = data.header.headerKeys

    // parameters
    def paramsKeys: Set[String] = data.parMap.keySet.map(decode)
    def params(key: String): Seq[String] = data.parMap.getOrElse(encode(key), Nil).map(decode)
    def param(key: String): Option[String] = params(key) match {
      case Seq(x, _*) => Some(x)
      case _ => None
    }
    def softParam(key: String): String = param(encode(key)).map(decode).getOrElse("")

    def asQuery(ignore: Set[String] = Set()): String = {
      def paramQuery(key: String) = params(key).map(v => { encode(key) + "=" + encode(v) }).mkString("&")
      paramsKeys.diff(ignore).map(paramQuery).mkString("&")
    }

    // param(key) helpers
    def asByte(key: String): Option[Byte] = try { Some(param(key).get.toByte) } catch { case _ => None }
    def asShort(key: String): Option[Short] = try { Some(param(key).get.toShort) } catch { case _ => None }
    def asInt(key: String): Option[Int] = try { Some(param(key).get.toInt) } catch { case _ => None }
    def asLong(key: String): Option[Long] = try { Some(param(key).get.toLong) } catch { case _ => None }
    def asFloat(key: String): Option[Float] = try { Some(param(key).get.toFloat) } catch { case _ => None }
    def asDouble(key: String): Option[Double] = try { Some(param(key).get.toDouble) } catch { case _ => None }

    // POST/application/octet-stream case
    def octets: Option[Array[Byte]] = data.octets

    private def encode(s: String) = try { java.net.URLEncoder.encode(s, data.app.encoding) } catch { case _ => s }
    private def decode(s: String) = try { java.net.URLDecoder.decode(s, data.app.encoding) } catch { case _ => s }
  }

}
