package zgs.httpd

import scala.util.continuations._

private object HLet {
  val exe = new zgs.sync.SyncExe(10, Int.MaxValue, "HLetResumer")
}

/** Handles an HTTP request and makes some computation.
 *  This computation may be suspended at any moment and several times
 *  by calling the [[zgs.httpd.HLet#suspend]] methods. The computation
 *  will be resumed at this point when the [[zgs.httpd.HLet#resume]] method
 *  is called. The data passed to the `resume` method is returned by the
 *  `suspend` call.
 *
 *  @tparam T the type of data passed when the computation is resumed
 *
 */
trait HLet[T] {

  //------------------- to implement ------------------------

  /** This method contains the actual (suspendable) computation. */
  def act(tal : HTalk) : Any @suspendable

  //-------------------- to override ------------------------

  /** When this `HLet` accepts multipart requests, this method must return
   *  a parts acceptor that will process the different parts.
   *  An example of part acceptor is:
   *  {{{
   *  class ImageUpload extends HSimpleLet {
   *
   *      class ImagePartsAcceptor(reqInfo: HReqHeaderData)
   *           extends HPartsAcceptor(reqInfo) {
   *
   *        // the parts are stored in a byte array output stream
   *        private var input: ByteArrayOutputStream = _
   *
   *        // accept only uploaded image files
   *        def open(desc: HPartDescriptor) =
   *          reqInfo.header("Content-Type") match {
   *            case Some(mime) if mime.startsWith("image/") =>
   *              input = new ByteArrayOutputStream
   *              true
   *            case _ => false
   *          }
   *
   *        def accept(bytes: Array[Byte]) = {
   *          // write part into the buffer
   *          input.write(bytes)
   *          // accept more parts
   *          true
   *        }
   *
   *        def close {
   *          // save the bytes containing the image
   *          image = Some(input.toByteArray)
   *          // close the output stream
   *          input.close
   *          input = null
   *        }
   *
   *        def declineAll { input = null }
   *     }
   *
   *    private var image: Option[Array[Byte]] = None
   *
   *    def act(talk: HTalk) = {
   *      val response = image match {
   *        case Some(img) => "Image uploaded"
   *        case None => "Not uploaded Image"
   *      }
   *
   *      talk.setContentLength(response.length).write(response)
   *
   *    }
   *
   *  }
   *  }}}
   */
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
      }) // resume computation in another thread
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

  protected def sessRedirect(uriPath : String, tk : HTalk) = {
    val parts = uriPath.split("\\?", 2)
    val url = parts(0) + ";" + tk.ses.idKey + "=" + tk.ses.id + {
      if (parts.size == 2) "?" + parts(1)
      else ""
    }
    new let.RedirectLet(url) act (tk)
  }
}

/** A simple [[zgs.httpd.HLet]] that will not be suspended.
 *  Most of the time, one wants to implement this one instead of [[zgs.httpd.HLet]].
 */
trait HSimpleLet extends HLet[Nothing]