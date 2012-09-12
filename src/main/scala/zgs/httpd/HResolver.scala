package zgs.httpd

private object HResolver {

  private object errApp extends HApp {
    override def tracking = HTracking.NotAllowed
    override def keepAlive = false
    override def chunked = false
    override def buffered = false
    override def gzip = false
    def resolve(req : HReqData) = sys.error("not used")
    val hLet = new let.ErrLet(HStatus.NotFound)
  }

  def resolve(apps : Seq[HApp], req : HReqData) : (HApp, HLet[_]) = {
    @scala.annotation.tailrec
    def doFind(rest : Seq[HApp]) : (HApp, HLet[_]) = rest match {
      case Seq() => (errApp, errApp.hLet)
      case Seq(a, _*) => a.resolve(req) match {
        case Some(let) => (a, let)
        case None      => doFind(rest.tail)
      }
    }
    doFind(apps)
  }
}
