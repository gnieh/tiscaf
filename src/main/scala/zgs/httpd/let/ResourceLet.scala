package zgs.httpd
package let

trait ResourceLet extends HLet[Nothing] {

  //----------------- to implement -------------------------

  protected def dirRoot : String // will be mounted to uriRoot

  //----------------- to override -------------------------

  // Following Java convention: returns null if resource is not found.
  // We must return null for direcories in jars. Official API doesn't provide any
  // way to differentiate files and directories in jars. But stream.available()
  // rises an error for directories.
  protected def getResource(path : String) : java.io.InputStream = {
    val url = this.getClass.getResource(path)
    if (url == null) null else url.getProtocol match {
      case "jar"  => val is = url.openStream; try { is.available; is } catch { case _ => null }
      case "file" => if (new java.io.File(url.toURI) isFile) url.openStream else null
      case _      => null
    }
  }
  protected def uriRoot : String = "" // say, "myKit/theDir"
  protected def indexes : Seq[String] = Nil // say, List("index.html", "index.htm")
  protected def bufSize : Int = 4096
  protected def plainAsDefault : Boolean = false

  //-------------------- init ------------------------------

  // starts with anf ends with "/"
  private val theDirRoot : String = "/" + dirRoot + { if (dirRoot.endsWith("/")) "" else "/" }

  private val theUriRoot = { // remove leading and trailing "/"
    val anUri = uriRoot
    val tmp = if (anUri.startsWith("/")) anUri.substring(1) else uriRoot
    if (tmp.endsWith("/")) tmp.substring(0, tmp.length - 1) else tmp
  }

  private def resolvePath(tk : HTalk) : String = {
    val pathRest = tk.req.uriPath.substring(theUriRoot.length)
    def path = theDirRoot + { if (pathRest.startsWith("/")) pathRest.substring(1) else pathRest }
    new java.io.File(path).getCanonicalPath
  }

  //------------------ HLet implemented --------------------

  def act(tk : HTalk) : Unit @scala.util.continuations.suspendable = if ((tk.req.uriPath).startsWith(theUriRoot)) {
    val path = resolvePath(tk)

    val fullPathAndStream = ("" +: indexes)
      .map(idx => (path + { if (idx.length == 0) "" else "/" + idx }).replace("//", "/"))
      .map { fullPath => (fullPath, getResource(fullPath)) }
      .find(_._2 != null)

    if (fullPathAndStream.isEmpty) notFound(tk) else {
      def cType = HMime.exts.keySet.find(ext => fullPathAndStream.get._1.toLowerCase.endsWith("." + ext)) match {
        case Some(e) => HMime.exts(e)
        case None    => if (plainAsDefault) "text/plain" else "application/octet-stream"
      }
      tk.setContentType(cType)

      val ar = new Array[Byte](bufSize)
      val is = fullPathAndStream.get._2

      @scala.annotation.tailrec
      def step(wasRead : Int) : Unit = if (wasRead > 0) {
        tk.write(ar, 0, wasRead)
        step(is.read(ar))
      }
      step(is.read(ar))
      is.close
    }
  } else notFound(tk)

  private def notFound(tk : HTalk) = new ErrLet(HStatus.NotFound, tk.req.uriPath) act (tk)
}
