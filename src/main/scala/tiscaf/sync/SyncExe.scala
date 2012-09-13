package tiscaf.sync


private object SyncExe {
  private var num : Int = 0 
  def nextNum : Int = synchronized { val v = num; num += 1; v }
}


final class SyncExe(poolSize: Int, queueCap: Int, name: String, onError: Throwable => Unit = {_ =>  }) {
  
  //-------------------- API -----------------------------------

  // rises exception if the queue is full
  def submit(task : Runnable) : Unit = {
    require(working.get, "submitting to shutted down SyncExe")
    qu.put(task)
  }

  def stopAccepting : Unit = working.set(false) // doesn't prevent to drain the queue
  
  def shutdown : Unit = {
    stopAccepting
    qu.close // closes the queue for both 'take' and 'put'
    cancelAll
  }
  
  //-------------------- internals ------------------------------
  
  private val working = new java.util.concurrent.atomic.AtomicBoolean(true) //new SyncBool(true)
  private val qu      = new SyncQu[Runnable](queueCap)
  private val poolNum = SyncExe.nextNum
  
  private val threads = for(i <- 0 until poolSize) yield {
    val t = newThread(i); t.start; t }
  
  private def newThread(i : Int) : Thread = new Thread {
    override def run = while (working.get) { try { qu.take.run } catch { 
      case e : java.lang.InterruptedException => // ignore
      case e                                  => SyncExe.this.onError(e)} 
    }
    setName(SyncExe.this.name + "-" + poolNum + "-" + i)
  }
      
  private def cancelAll : Unit = threads.foreach { t => try { t.interrupt } catch { case _ => } }
}
