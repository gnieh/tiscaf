package zgs.httpd
package let

class RedirectLet(toUri : String) extends HLet[Nothing] {

  def act(tk : HTalk) : Unit @scala.util.continuations.suspendable = {
    tk.setContentLength(0)
      .setContentType("text/html")
      .setHeader("Location", toUri)
      .setStatus(HStatus.MovedPermanently)
    ()
  }

}
