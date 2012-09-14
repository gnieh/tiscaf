package tiscaf

import java.net.InetSocketAddress
import java.nio.channels.{ SelectionKey, Selector, ServerSocketChannel }

import sync._

private trait HPlexer {

  //---------------------- to implement ------------------------------

  def timeoutMillis: Long
  def tcpNoDelay: Boolean
  def onError(e: Throwable): Unit
  def ssl: Option[HSsl]

  //---------------------- SPI ------------------------------------------

  final def start = synchronized {
    if (!isWorking.get) {
      isWorking.set(true)
      Sync.spawnNamed("Plexer") { try { plex } catch { case e => onError(e) } }
    }
  }

  final def stop: Unit = synchronized { // close once only 
    if (isWorking.get) {
      isWorking.set(false)
      selector.close
      servers.foreach(_.asInstanceOf[ServerSocketChannel].close)
      servers.clear
    }
  }

  final def addListener(peerFactory: SelectionKey => HPeer, port: Int): Unit = Sync.spawnNamed("Acceptor-" + port) {
    try {
      val serverChannel = ServerSocketChannel.open
      servers += serverChannel
      serverChannel.configureBlocking(true)
      serverChannel.socket.bind(new InetSocketAddress(port))

      while (isWorking.get) try {
        val socketCannel = serverChannel.accept
        socketCannel.socket.setTcpNoDelay(tcpNoDelay)
        socketCannel.configureBlocking(false)
        keySetGuard.synchronized {
          selector.wakeup
          val key = socketCannel.register(selector, 0)
          key.attach(new HKeyData(peerFactory(key)))
          needToRead(key)
        }
      } catch {
        case e: java.nio.channels.AsynchronousCloseException =>
        case e => onError(e)
      }
    } catch {
      case e: java.nio.channels.AsynchronousCloseException =>
      case e => throw e
    }
  }

  final def addSslListener(peerFactory: SelectionKey => HPeer, port: Int): Unit = Sync.spawnNamed("Acceptor-" + port) {
    try {
      // TODO implement it (with SSL sessions)
      val serverChannel = ServerSocketChannel.open
      servers += serverChannel
      serverChannel.configureBlocking(true)
      serverChannel.socket.bind(new InetSocketAddress(port))

      while (isWorking.get) try {
        val socketCannel = serverChannel.accept
        val remoteIp = socketCannel.socket.getInetAddress.getHostAddress
        val remotePort = socketCannel.socket.getPort
        socketCannel.socket.setTcpNoDelay(tcpNoDelay)
        socketCannel.configureBlocking(false)
        keySetGuard.synchronized {
          selector.wakeup
          val key = socketCannel.register(selector, 0)
          key.attach(new HKeyData(peerFactory(key)))
          needToRead(key)
        }
      } catch {
        case e: java.nio.channels.AsynchronousCloseException =>
        case e => onError(e)
      }
    } catch {
      case e: java.nio.channels.AsynchronousCloseException =>
      case e => throw e
    }
  }

  final def needToClose(key: SelectionKey): Unit = { wakeQu.put(key, PeerWant.Close); selector.wakeup }
  final def needToWrite(key: SelectionKey): Unit = { wakeQu.put(key, PeerWant.Write); selector.wakeup }
  final def needToRead(key: SelectionKey): Unit = { wakeQu.put(key, PeerWant.Read); selector.wakeup }

  //------------------------- internals --------------------------------------

  private val isWorking = new SyncBool(false)
  private val servers = new scala.collection.mutable.HashSet[ServerSocketChannel] with scala.collection.mutable.SynchronizedSet[ServerSocketChannel]
  private val selector = Selector.open
  //-- in accordance with Ron Hitchens (Ron, thanks for the trick!)
  private val keySetGuard = new AnyRef

  //-----------------------------------------------------------

  type KeyWant = Pair[SelectionKey, PeerWant.Value]
  private val wakeQu = new SyncQu[KeyWant]

  private def processWakeQu: Unit = {
    @scala.annotation.tailrec
    def step: Unit = wakeQu.takeOpt match {
      case Some(kw) =>
        kw._2 match {
          case PeerWant.Write => if (kw._1.isValid) kw._1.interestOps(SelectionKey.OP_WRITE)
          case PeerWant.Close => keySetGuard.synchronized { selector.wakeup; kw._1.channel.close } // changes keySet
          case PeerWant.Read  => if (kw._1.isValid) kw._1.interestOps(SelectionKey.OP_READ)
        }
        step
      case _ =>
    }
    step
  }

  // check expired connections - not too often
  private val expireDelta = if (timeoutMillis > 10000L) 1000L else timeoutMillis / 10L
  private val lastExpire = new SyncField[Long]
  lastExpire.set(System.currentTimeMillis)

  private def processExpiration: Unit = try {
    val now = System.currentTimeMillis
    if (now > lastExpire.get + expireDelta) {
      val timeX = now - timeoutMillis
      val it = selector.keys.iterator
      while (it.hasNext) {
        val key = it.next
        val att = key.attachment
        if (att == null || att.asInstanceOf[HKeyData].stamp < timeX) key.channel.close
      }
      lastExpire.set(now)
    }
  } catch { case e => onError(e) }

  // main multiplexer loop
  private def plex: Unit = while (isWorking.get) try {

    processWakeQu // these operations change keys' states only
    keySetGuard.synchronized { processExpiration } // these operation change keySet content

    if (selector.select > 0) {
      val it = selector.selectedKeys.iterator
      val now = System.currentTimeMillis
      while (it.hasNext) {
        val key = it.next
        it.remove
        if (key.isValid) {
          val data = key.attachment.asInstanceOf[HKeyData]
          data.stamp = now
          if (key.isWritable) try { key.interestOps(0); data.peer.proceedToWrite } catch { case _ => /* can be canceled */ }
          else if (key.isReadable) try { key.interestOps(0); data.peer.readChannel } catch { case _ => /* can be canceled */ }
        }
      }
    }

  } catch { case e => onError(e) }

}

private class HKeyData(val peer: HPeer) {
  var stamp = System.currentTimeMillis
}

private object PeerWant extends Enumeration {
  val Close = Value("Close")
  val Read = Value("Read")
  val Write = Value("Write")
}

