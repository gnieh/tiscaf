package tiscaf

import java.nio.ByteBuffer
import java.nio.channels.{ Selector, SelectionKey, SocketChannel }

import scala.util.continuations._

private trait HPeer {

  private val plexerBarrier = new java.util.concurrent.CyclicBarrier(1)
  private val theBuf = ByteBuffer.allocate(bufferSize)

  //------------------- to implement ------------------------------

  def plexer : HPlexer
  def key : SelectionKey

  def bufferSize : Int

  def onError(e : Throwable) : Unit

  val acceptor : HAcceptor
  def submit(toRun : Runnable) : Unit

  //-------------------------------------------------------------------

  final def connClose = plexer.needToClose(key)
  final def connWrite = plexer.needToWrite(key)
  final def connRead = plexer.needToRead(key)

  final def channel : SocketChannel = key.channel.asInstanceOf[SocketChannel]
  final def remoteIp = channel.socket.getInetAddress.getHostAddress

  final def dispose = {
    plexerBarrier.reset
    connClose
  }

  // core place - main iteration
  final def readChannel : Unit = try {

    def doTalkItself : Unit = reset {
      acceptor.talk match {
        case PeerWant.Read  => acceptor.in.reset; connRead // new alive request/response
        case PeerWant.Close => connClose
        case x              => sys.error("unexpected PeerWant value " + x)
      }
    }

    theBuf.clear
    val wasRead = channel.read(theBuf)
    if (wasRead == -1) dispose // counterpart peer wants to write but writes nothing
    else {
      def toRun = new Runnable {
        def run : Unit = {
          acceptor.accept(theBuf.array.take(wasRead))
          acceptor.in.reqState match {
            case HReqState.IsReady   => acceptor.resolveAppLet; doTalkItself
            case HReqState.IsInvalid => dispose
            case _ /* WaitsForXxx */ => connRead
          }
        }
      }
      submit(toRun)
    }
  } catch { case e => dispose; onError(e) }

  final def proceedToWrite : Unit = plexerBarrier.reset // called by plexer

  // ByteBuffer.wrap(ar, offset, length) is slower in my tests rather direct array putting
  final def writeToChannel(ar : Array[Byte], offset : Int, length : Int) = {
    if (length > 0) {
      theBuf.clear
      theBuf.put(ar, offset, length)
      theBuf.flip
      plexerBarrier.await
      channel.write(theBuf)

      if (theBuf.hasRemaining && !writeAside(theBuf, channel)) dispose
    }

    // it is valid to use connWrite here even for case when HOut will switch to
    // read later:
    // - counterpart peer will not be ready to read, and selector will not
    //   select a key,
    // - if by some reason selector still selects a key for writing, it will just
    //   reset not awaited barrier, as far as HOut doesn't write anything.
    connWrite
  }

  // for me it rarely happens with big (say, >= 64KB) buffers only
  final def writeAside(buf : ByteBuffer, sc : SocketChannel) : Boolean = {
    val tmpSelector = Selector.open
    val theKey = sc.register(tmpSelector, SelectionKey.OP_WRITE)
    theKey.attach(System.currentTimeMillis)

    @scala.annotation.tailrec
    def nextSelect : Boolean = if (buf.hasRemaining) {
      if (theKey.attachment.asInstanceOf[Long] + plexer.timeoutMillis < System.currentTimeMillis) {
        tmpSelector.close
        false
      } else {
        tmpSelector.select(200)
        val it = tmpSelector.selectedKeys.iterator
        if (it.hasNext) {
          val aKey = it.next
          it.remove
          sc.write(buf)
          aKey.attach(System.currentTimeMillis)
        }
        nextSelect
      }
    } else { tmpSelector.close; true }

    nextSelect
  }
}

