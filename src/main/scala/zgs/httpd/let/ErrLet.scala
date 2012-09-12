package zgs.httpd
package let

class ErrLet(status : HStatus.Value, msg : String = "") extends HLet[Nothing] {

  def act(tk : HTalk) : Unit @scala.util.continuations.suspendable = {
    val add = if (msg.length == 0) "" else ", " + msg
    val toWrite = (HStatus.asString(status) + add + "\n").getBytes("ISO-8859-1")

    tk.setStatus(status)
      .setContentType("text/plain")
      .setContentLength(toWrite.length) // if not chunked
      .write(toWrite)

    tk.ses.invalidate
  }
}
