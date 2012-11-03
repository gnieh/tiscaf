/** *****************************************************************************
 *  This file is part of tiscaf.
 *
 *  tiscaf is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Foobar is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with tiscaf.  If not, see <http://www.gnu.org/licenses/>.
 *  ****************************************************************************
 */
package tiscaf

import scala.util.continuations._

/** Handles an HTTP request and makes some computation.
 *  This computation may be suspended at any moment and several times
 *  by calling the [[tiscaf.HLet]]#suspend methods. The computation
 *  will be resumed at this point when the [[tiscaf.HLet]]#resume method
 *  is called. The data passed to the `resume` method is returned by the
 *  `suspend` call.
 *
 *  @tparam T the type of data passed when the computation is resumed
 *
 */
trait HLet[T] {

  //------------------- to implement ------------------------

  /** This method contains the actual (suspendable) computation.
   *  The given `talk` parameter contains the methods to access the request and
   *  session data, as well as the method to send the response to the client.
   *  @see [[tiscaf.HTalk]]
   */
  def act(talk: HTalk): Any @suspendable

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
  def partsAcceptor(reqInfo: HReqHeaderData): Option[HPartsAcceptor] = None // for multipart requests

  /** Called when the computation is suspended.
   *  It may be used to store this HLet and resume it later.
   *  @note Implementors must take care of synchronization as different `HLet`s
   *   may be called concurrently.
   */
  protected[this] def onSuspend {} // called when the execution of this HLet is suspended

  //------------------------ few helpers --------------------

  private[this] var kont: Option[T => Unit] = None

  /** Resumes the computation of this `HLet` if not already terminated.
   *  Caller pass values that may then be used in the rest of the computation.
   *  Computation is resumed synchronously, a call to this method blocks
   *  the current thread until computation is finished.
   */
  final def resume(v: T) = kont match {
    case Some(k) =>
      k(v) // resume computation in the same thread
    case None =>
      throw new RuntimeException("Computation already terminated") // computation terminated, nothing to do
  }

  /** Suspends the computation of this `HLet`. Computation may then
   *  be resumed later.
   */
  final protected[this] def suspend = shift { k: (T => Unit) =>
    onSuspend
    kont = Some(k)
  }

  /** Answers with an error response with the given code and message. */
  protected def err(status: HStatus.Value, msg: String, tk: HTalk) = new let.ErrLet(status, msg) act (tk)

  /** Answers with an error response with the given code. */
  protected def err(status: HStatus.Value, tk: HTalk) = new let.ErrLet(status) act (tk)

  /** Answers with an 404 error message. */
  protected def e404(tk: HTalk) = err(HStatus.NotFound, tk)

  /** Redirects the client to the given URI. */
  protected def redirect(uriPath: String, tk: HTalk) = new let.RedirectLet(uriPath) act (tk)

  /** Redirects the client to the given URI and adds the sessions ID to
   *  the URI parameters.
   */
  protected def sessRedirect(uriPath: String, tk: HTalk) = {
    val parts = uriPath.split("\\?", 2)
    val url = parts(0) + ";" + tk.ses.idKey + "=" + tk.ses.id + {
      if (parts.size == 2) "?" + parts(1)
      else ""
    }
    new let.RedirectLet(url) act (tk)
  }
}

/** A simple [[tiscaf.HLet]] that will not be suspended or suspended and resumed
 *  without any parameter passed.
 *  Most of the time, one wants to implement this one instead of [[tiscaf.HLet]].
 */
trait HSimpleLet extends HLet[Unit]
