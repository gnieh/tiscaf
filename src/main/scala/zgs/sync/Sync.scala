package zgs.sync

object Sync {
  
  def spawn(code: => Unit): Unit =
    new Thread(new Runnable { def run: Unit = { code } }) start
  
  def spawnNamed(name: String)(code: => Unit) : Unit = {
    val t = new Thread(new Runnable { def run: Unit = { code } }) 
    t setName name
    t start
  }
  
  def daemon(code: => Unit): Unit = {
    val t = new Thread(new Runnable { def run: Unit = { code } })
    t setDaemon true
    t start
  }
  
  def daemonNamed(name: String)(code: => Unit) : Unit = {
    val t = new Thread(new Runnable { def run: Unit = { code } })
    t setName name
    t setDaemon true
    t start
  }
  
  def join(code: => Unit): Unit = {
    val t = new Thread(new Runnable { def run: Unit = { code } })
    t.start
    t.join
  }
  
}
