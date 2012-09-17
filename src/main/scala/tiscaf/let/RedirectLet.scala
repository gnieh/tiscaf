package tiscaf
package let

/** Redirect (send a moved permanently code) to a fix URI. */
class RedirectLet(toUri: String) extends HLet[Nothing] {

  def act(tk: HTalk) = {
    tk.setContentLength(0)
      .setContentType("text/html")
      .setHeader("Location", toUri)
      .setStatus(HStatus.MovedPermanently)
  }

}
