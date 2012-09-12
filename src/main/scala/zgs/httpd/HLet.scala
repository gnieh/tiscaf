package zgs.httpd

import scala.util.continuations._

object HLet {
  val exe = new zgs.sync.SyncExe(10, Int.MaxValue, "HLetResumer")
}

trait HLet[T] {

  //------------------- to implement ------------------------

  def act(tal : HTalk) : Unit @suspendable

  //-------------------- to override ------------------------

  def partsAcceptor(reqInfo : HReqHeaderData) : Option[HPartsAcceptor] = None // for multipart requests

  protected[this] def onSuspend {} // called when the execution of this HLet is suspended

  //------------------------ few helpers --------------------

  private[this] var kont : Option[T => Unit] = None

  final def resume(v : T) = kont match {
    case Some(k) =>
      HLet.exe.submit(new Runnable {
        def run() {
          k(v)
        }
      }) // resume computation
    case None => throw new RuntimeException("Computation already terminated") // computation terminated, nothing to do
  }

  final protected def suspend = shift { k : (T => Unit) =>
    onSuspend
    kont = Some(k)
  }

  protected def err(status : HStatus.Value, msg : String, tk : HTalk) = new let.ErrLet(status, msg) act (tk)
  protected def err(status : HStatus.Value, tk : HTalk) = new let.ErrLet(status) act (tk)
  protected def e404(tk : HTalk) = err(HStatus.NotFound, tk)

  protected def redirect(uriPath : String, tk : HTalk) = new let.RedirectLet(uriPath) act (tk)

  protected def sessRedirect(uriPath : String, tk : HTalk) : Unit @scala.util.continuations.suspendable = {
    val parts = uriPath.split("\\?", 2)
    val url = parts(0) + ";" + tk.ses.idKey + "=" + tk.ses.id + {
      if (parts.size == 2) "?" + parts(1)
      else ""
    }
    new let.RedirectLet(url) act (tk)
  }
}
