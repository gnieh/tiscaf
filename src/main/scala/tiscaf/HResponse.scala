package tiscaf


object HResponse {
  private val stdDateFormat = new java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss")
  private val offset = java.util.TimeZone.getDefault.getRawOffset

  def stdDateString(mills : Long) = stdDateFormat.synchronized { 
    stdDateFormat.format(new java.util.Date(mills - offset)) + " GMT"
  }

  // 1 second caching ('format' is very costly!)
  private var lastMillis : Long  = 0L
  private var lastStdDate = ""
  
  private def headerDateString = stdDateFormat.synchronized {
    val now = System.currentTimeMillis
    if (now > lastMillis + 1000) {
      lastMillis = now
      lastStdDate = stdDateFormat.format(new java.util.Date(now - offset)) + " GMT"
    }
    lastStdDate
  } 

  private val defaultStatus = HStatus.asString(HStatus.OK, "listen to Jazz")
}

// deals with header only
final private class HResponse {
  
  private var status : String = HResponse.defaultStatus
  // To ignore keys' case key -> value pairs are tranforming
  // into key.toLowerCase -> (key, value) pairs
  private val map = new scala.collection.mutable.HashMap[String, Pair[String,String]]
  
  setHeader("Content-Type", "text/html") // default if user forget
  setHeader("Server", "tiscaf httpd server") // default
  
  //---------------
    
  def setStatus(code : HStatus.Value) : Unit               = { status = HStatus.asString(code) }
  def setStatus(code : HStatus.Value, msg : String) : Unit = { status = HStatus.asString(code, msg) }
  
  def setHeader(key : String, value : String)     = { map(key.trim.toLowerCase) = (key,value) }
  def getHeader(key : String) : Option[String]    = for (p <- map.get(key.trim.toLowerCase)) yield p._2 
  def removeHeader(key : String) : Option[String] = for (p <- map.remove(key.trim.toLowerCase)) yield p._2
  
  def  getBytes: Array[Byte] = {
    val buf = new StringBuilder
    buf.append("HTTP/1.1 ").append(status).append("\r\n")
    
    setHeader("Date", HResponse.headerDateString)

    // write known standard fields in given order...
    for { 
      f <- HHeaderKeys.list
      p <- map.remove(f.toLowerCase)
    } buf.append(p._1).append(": ").append(p._2).append("\r\n")

    // ... and append all others in alphabetical order
    map.values
       .toSeq
       .sortBy(_._1)
       .foreach { p => buf.append(p._1).append(": ").append(p._2).append("\r\n") }

    buf.append("\r\n").toString.getBytes("ISO-8859-1")
  }
  
}

