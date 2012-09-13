package zgs.httpd
package let

import java.io.File

trait FsLet extends HLet[Nothing] {

  //----------------- to implement -------------------------

  protected def dirRoot : String // will be mounted to uriRoot

  //----------------- to override -------------------------

  protected def uriRoot : String = "" // say, "myKit/theDir"
  protected def indexes : Seq[String] = Nil // say, List("index.html", "index.htm")
  protected def allowLs : Boolean = false
  protected def bufSize : Int = 4096
  protected def plainAsDefault : Boolean = false

  //-------------------- init ------------------------------

  // force trailing slash
  private val theDirRoot : String = {
    val tmp = (new File(dirRoot)).getCanonicalPath.replace("\\\\", "/")
    if (tmp.endsWith("/")) tmp else tmp + "/"
  }
  // remove leading and trailing slashes
  private val theUriRoot = {
    val tmp = if (uriRoot.startsWith("/")) uriRoot.substring(1) else uriRoot
    if (tmp.endsWith("/")) tmp.substring(0, tmp.length - 1) else tmp
  }

  //------------------ HLet implemented --------------------

  def act(tk : HTalk) = if ((tk.req.uriPath).startsWith(theUriRoot)) {
    val uriExt = if (tk.req.uriExt.isDefined) { ";" + tk.req.uriExt.get } else ""

    val pathRest = tk.req.uriPath.substring(theUriRoot.length)
    val path = theDirRoot + { if (pathRest.startsWith("/")) pathRest.substring(1) else pathRest }
    val f = new File(path)

    if ((f.getCanonicalPath.replace("\\\\", "/") + "/").startsWith(theDirRoot) && f.exists) {
      if (f.isDirectory) {
        // try indexes first - before direcory listing
        if (theUriRoot.isEmpty || tk.req.uriPath.endsWith("/")) indexes.find { index =>
          val indexFile = new File(path + index)
          indexFile.exists && indexFile.isFile
        } match {
          case None    => if (allowLs) new DirLet(theDirRoot, theUriRoot, pathRest).act(tk) else notFound(tk)
          case Some(x) => new FiLet(path + x, bufSize, plainAsDefault).act(tk)
        }
        else new RedirectLet("/" + theUriRoot + pathRest + "/" + uriExt) act (tk)
      } // isDirectory
      else new FiLet(path, bufSize, plainAsDefault).act(tk)
    } else notFound(tk)
  } else notFound(tk)

  private def notFound(tk : HTalk) = new ErrLet(HStatus.NotFound) act (tk)
}
