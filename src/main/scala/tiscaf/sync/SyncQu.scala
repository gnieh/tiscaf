package tiscaf.sync

final class SyncQu[T](cap : Int = Int.MaxValue) {

  //----------------------------- API -------------------

  // not blocking! - rises an error on capacity exceeding
  def put(item : T) : Unit = synchronized {
    require(!closed, "putting to closed SyncQu")
    require(theSize < cap, "SyncQu: max capacity is exceeded: " + cap)
    if (theSize == 0) {
      first = Some(new Node(item))
      last = first
      theSize = 1
    }
    else {
      last.get.next = Some(new Node(item))
      last = last.get.next
      theSize += 1
    }
    notifyAll() // ... takers
  }

  // blocking
  def take : T = synchronized {
    require(!closed, "taking from closed SyncQu")
    while (theSize == 0) wait()
    doTake
  }
  
  // not blocking
  def takeOpt: Option[T] = synchronized {
    require(!closed, "takingOpt from closed SyncQu")
    if (theSize == 0) None 
    else {
      val result = doTake
      notifyAll() // ... other takers
      Some(result)
    }
  }
  
  // not blocking
  def drain: Seq[T] = synchronized {
    @scala.annotation.tailrec
    def next(acc: List[T]): List[T] = if (theSize == 0) acc else next(doTake :: acc)
    
    next(List.empty[T]).reverse
  }
  
  def size: Int = synchronized { theSize }

  def close : Unit = synchronized {
    closed = true
    // deref all
    while (first.isDefined) {
      val second = first.get.next
      first.get.next = None
      first = second
    }
    last = None
    theSize = 0
    notifyAll() // ...takers to rise an error: require(!closed) will be violated
  }
  
  //----------------- internals --------------------------

  private var closed  = false
  private var theSize = 0

  private var first : Option[Node] = None
  private var last : Option[Node]  = None
  
  private def doTake: T = {
    val result = first.get
    first = result.next
    result.next = None // deref
    theSize -= 1
    if (first.isEmpty) last = None
    result.value
  }

  private class Node(val value : T)  {
    var next : Option[Node] = None
  }
}
