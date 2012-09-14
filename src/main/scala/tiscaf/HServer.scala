package tiscaf

/** A server provides:
 *   - few common settings,
 *   - list of [[tiscaf.HApp]]lications,
 *   - start/stop methods
 */
trait HServer {

  self =>

  //------------------ to implement -----------------------------------

  /** Returns the list of available applications.
   *  @see [[tiscaf.HApp]]
   */
  protected def apps: Seq[HApp]

  /** Returns the list of ports, the server listens to. */
  protected def ports: Set[Int]

  //------------------------- to override ------------------------------

  /** Returns the server name, used in response headers. */
  protected def name = "tiscaf"

  /** Returns the port number listened to for a stop message. */
  protected def stopPort: Int = 8911

  protected def tcpNoDelay: Boolean = false // use 'true' for benchmarking only!

  /** Returns the executor pool size. */
  protected def poolSize: Int = Runtime.getRuntime.availableProcessors * 2

  /** Returns the executor queue size. */
  protected def queueSize: Int = Int.MaxValue

  /** Returns the NIO buffer size. */
  def bufferSize: Int = 4096 // public to be used in, say, FsLet

  /** Returns the connection timeout. It has to purposes:
   *   - a connection without any socket activity during this period will be closed
   *   - if you use (and client wants) 'keep-alive' connection, this period is declared in response header
   */
  def connectionTimeoutSeconds: Int = 20 // is used for Keep-Alive mode (if turned on)
  // *and* in multiplexer to dispose dead selector's keys

  /** Returns the time a shutdown process let the HLets a chance to finish properly. */
  def interruptTimeoutMillis: Int = 1000 // at shutdown process take HLets a chance to finish 

  /** Called when an uncatched error is thrown. You may delegate to the log system of your choice. */
  protected def onError(e: Throwable): Unit = e.printStackTrace

  /** Returns the maximum upload size allowed. */
  protected def maxPostDataLength: Int = 65536 // for POST other than multipart/form-data

  // override if you want more elaborated shutdown procedure (and replace tiscaf.HStop)
  protected def startStopListener: Unit = sync.Sync.spawnNamed("StopListener") {
    val serverSocket = new java.net.ServerSocket(stopPort)
    println(name + " stop listener is listening to port " + stopPort)
    val dataSocket = serverSocket.accept
    val ar = new Array[Byte](256)
    dataSocket.getInputStream.read(ar)
    if (new String(ar, "ISO-8859-1") startsWith ("stop")) { dataSocket.close; serverSocket.close; stop }
    else sys.error(name + ": invalid stop sequence")
  }

  /** Returns the SSL settings if any. */
  def ssl: Option[HSsl] = None

  //-------------------------- user API ---------------------------------

  /** Starts the server. */
  final def start: Unit = synchronized {
    if (isStopped.get) {
      plexer.start
      ports.foreach { port => plexer.addListener(peerFactory, port) }
      // listen to SSL ports if any configured
      ssl match {
        case Some(sslData) =>
          sslData.ports.foreach { port => plexer.addSslListener(peerFactory, port) }
        case None => // just do nothing
      }
      startStopListener
      isStopped.set(false)
      println(name + " server was started on port(s) " + ports.toSeq.sortBy(x => x).mkString(", "))
    } else sys.error("the server is already started")
  }

  /** Stops the server. */
  def stop: Unit = synchronized {
    if (!isStopped.get) {
      isStopped.set(true)
      talksExe.stopAccepting
      Thread.sleep(interruptTimeoutMillis)
      talksExe.shutdown
      plexer.stop
      println(name + " server stopped")
    } else sys.error("the server is already stopped")
  }

  //--------------------------- internals -------------------------------

  // nothing must be started in init, so using few objects and lazy vals

  private lazy val talksExe = new sync.SyncExe(poolSize, queueSize, "ServerExecutorPool", onError)
  private val timeoutMillis = connectionTimeoutSeconds * 1000L
  private val isStopped = new sync.SyncBool(true)

  private object plexer extends HPlexer {
    def timeoutMillis: Long = self.timeoutMillis
    def tcpNoDelay: Boolean = self.tcpNoDelay
    def onError(e: Throwable): Unit = self.onError(e)
    def ssl = self.ssl
  }

  import java.nio.channels.SelectionKey

  // key place
  private def peerFactory(aKey: SelectionKey): HPeer = new HPeer { self =>

    def plexer: HPlexer = HServer.this.plexer
    def key: SelectionKey = aKey

    def bufferSize: Int = HServer.this.bufferSize

    def onError(e: Throwable): Unit = HServer.this.onError(e)

    val acceptor =
      new HAcceptor(new HWriter(self), apps, HServer.this.connectionTimeoutSeconds, HServer.this.onError, maxPostDataLength)

    def submit(toRun: Runnable): Unit = if (!isStopped.get) talksExe.submit(toRun)
  }

}
