package zgs.httpd

object HTracking extends Enumeration {
  val NotAllowed, Uri, Cookie = Value
}

trait HApp {
  //----------------------- to implement -------------------------
  def resolve(req : HReqData) : Option[HLet[_]]

  //----------------------- to override ---------------------------
  def tracking : HTracking.Value = HTracking.NotAllowed
  def sessionTimeoutMinutes : Int = 15
  def maxSessionsCount : Int = 500
  def keepAlive : Boolean = true
  def chunked : Boolean = false
  def buffered : Boolean = false
  def gzip : Boolean = false
  def encoding : String = "UTF-8"
  def cookieKey : String = "TISCAF_SESSIONID"
  def sidKey : String = "sid"
}
