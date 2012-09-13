package tiscaf

final private class HWriter(peer : HPeer) {
  
  // It is just a mediator between write client (HOut) and HPeer

  def write(ar : Array[Byte]) : Unit = write(ar, 0, ar.length)

  @scala.annotation.tailrec
  def write(ar : Array[Byte], offset : Int, length : Int) : Unit = if (length > 0) {
    if (offset < 0)                  throwError("invalid arguments: offset < 0")
    if (length + offset > ar.length) throwError("invalid bounds: length + offset > ar.length")

    def writeLength = if (peer.bufferSize > length) length else peer.bufferSize
    peer.writeToChannel(ar, offset, writeLength)
    write(ar, offset + writeLength, length - writeLength)
  }
  
  def writeSeq(seq : Seq[HOutArray]) : Unit = write(concat(seq))
  def write(ar : HOutArray) : Unit = write(ar.ar, ar.offset,ar.length)
  
  def wakeupReading = peer.connRead
  def close : Unit  = peer.connClose
  def dispose       = peer.dispose
  
  def remoteIp      = peer.remoteIp
  
  // internals ------------------------------------------------

  private def concat(ars : Seq[HOutArray]) : Array[Byte] = {
    def length = ars.foldLeft(0) { (current, ar) => current + ar.length }
    val out = new Array[Byte](length)
    
    @scala.annotation.tailrec
    def step(offset : Int, rest : Seq[HOutArray]) : Unit = if (!rest.isEmpty) {
      if (rest.head.length > 0) Array.copy(rest.head.ar, rest.head.offset, out, offset, rest.head.length)
      step(offset + rest.head.length, rest.tail)
    }
    
    step(0, ars)
    out
  }
  
  private def throwError(msg : String) = { dispose; sys.error(msg) }
}

final private class HOutArray(val ar : Array[Byte], val offset : Int, val length : Int) {
  def this(anAr: Array[Byte]) = this(anAr, 0, anAr.length)
}
