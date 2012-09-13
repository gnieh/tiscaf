package zgs.httpd
package let

//import implicits._

class RedirectLet(toUri : String) extends HLet[Nothing] {

  def act(tk : HTalk) = {
    tk.setContentLength(0)
      .setContentType("text/html")
      .setHeader("Location", toUri)
      .setStatus(HStatus.MovedPermanently)
  }

}
