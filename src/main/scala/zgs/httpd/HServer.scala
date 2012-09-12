package zgs.httpd


trait HServer {

  //------------------ to implement -----------------------------------
  
  protected def apps : Seq[HApp]
  
  protected def ports : Set[Int]
  
  
  //------------------------- to override ------------------------------
  
  protected def name = "tiscaf"
  
  protected def stopPort : Int = 8911
  
  protected def tcpNoDelay : Boolean = false // use 'true' for benchmarking only!
  
  protected def poolSize : Int  = Runtime.getRuntime.availableProcessors * 2
  protected def queueSize : Int = Int.MaxValue
  
  // public to be used in, say, FsLet
  def bufferSize : Int  = 4096

  def connectionTimeoutSeconds : Int  = 20  // is used for Keep-Alive mode (if turned on)
                                            // *and* in multiplexer to dispose dead selector's keys
  
  def interruptTimeoutMillis : Int = 1000 // at shutdown process take HLets a chance to finish 

  protected def onError(e : Throwable) : Unit = e.printStackTrace

  protected def maxPostDataLength : Int = 65536 // for POST other than multipart/form-data

  // override if you want more elaborated shutdown procedure (and replace zgs.httpd.HStop)
  protected def startStopListener : Unit = zgs.sync.Sync.spawnNamed("StopListener") {
    val serverSocket = new java.net.ServerSocket(stopPort)
    println(name + " stop listener is listening to port " + stopPort)
    val dataSocket  = serverSocket.accept
    val ar = new Array[Byte](256)
    dataSocket.getInputStream.read(ar)
    if (new String(ar, "ISO-8859-1") startsWith("stop")) { dataSocket.close; serverSocket.close; stop }
    else sys.error(name + ": invalid stop sequence")
  }

  //-------------------------- user API ---------------------------------
  
  final def start : Unit = synchronized { 
    if (isStopped.get) {
      plexer.start
      ports.foreach { port => plexer.addListener(peerFactory, port) }
      startStopListener
      isStopped.set(false)
      println(name + " server was started on port(s) " + ports.toSeq.sortBy(x => x).mkString(", "))
    }
    else sys.error("the server is already started")
  }
  
  def stop : Unit = synchronized {
    if (!isStopped.get) {
      isStopped.set(true)
      talksExe.stopAccepting
      Thread.sleep(interruptTimeoutMillis)
      talksExe.shutdown
      plexer.stop
      println(name + " server stopped")
    }
    else sys.error("the server is already stopped")
  }
  
  //--------------------------- internals -------------------------------
  
  // nothing must be started in init, so using few objects and lazy vals

  private lazy val talksExe = new zgs.sync.SyncExe(poolSize, queueSize, "ServerExecutorPool", onError)
  private val timeoutMillis = connectionTimeoutSeconds * 1000L
  private val isStopped     = new zgs.sync.SyncBool(true)
  
  private object plexer extends HPlexer {
    def timeoutMillis : Long          = HServer.this.timeoutMillis
    def tcpNoDelay : Boolean          = HServer.this.tcpNoDelay
    def onError(e : Throwable) : Unit = HServer.this.onError(e)
  }

  import java.nio.channels.SelectionKey
  
  // key place
  private def peerFactory(aKey : SelectionKey) : HPeer = new HPeer { self =>

    def plexer: HPlexer   = HServer.this.plexer
    def key: SelectionKey = aKey
    
    def bufferSize : Int  = HServer.this.bufferSize
    
    def onError(e : Throwable) : Unit = HServer.this.onError(e)
    
    val acceptor = 
      new HAcceptor(new HWriter(self), apps, HServer.this.connectionTimeoutSeconds, HServer.this.onError, maxPostDataLength)
    
    def submit(toRun: Runnable) : Unit = if (!isStopped.get) talksExe.submit(toRun)
  }

}
