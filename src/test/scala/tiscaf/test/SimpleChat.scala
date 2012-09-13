package tiscaf
package test

/** @author Lucas Satabin
 *
 */
object SimpleChat extends App with HServer {

  def ports = Set(8080)
  def apps = List(ChatApp)

  start

}

object ChatApp extends HApp {

  // chunked answer allows streaming
  override val chunked = true

  private[test] val clients = scala.collection.mutable.Map.empty[String, HLet[(String, String)]]

  def resolve(req : HReqData) = req.uriPath match {
    case "subscribe"    => Some(new SubscribeLet)
    case "unscubscribe" => Some(new UnsubscribeLet)
    case "commit"       => Some(new CommitLet)
    case _ => Some(new let.ResourceLet {
      def dirRoot = "zgs/httpd/test/static"
    })
  }

}

class SubscribeLet extends HLet[(String, String)] {
  private[this] var id = ""
  override protected[this] def onSuspend = ChatApp.clients.synchronized {
    ChatApp.clients(id) = this
  }

  def act(talk : HTalk) = {

    // new subscription message, store the let
    id = talk.req.param("nick").get
    val continue = ChatApp.clients.synchronized {
      if (ChatApp.clients.contains(id)) {
        false
      } else {
        true
      }
    }

    if (continue) {
      talk.setContentType(HMime.json)
      talk.write("""{"nick":"!server!","message":"welcome!!!!"}""")
      while (true) {
        val (nick, msg) = suspend
        talk.write("""{"nick":"""" + nick + """","message":"""" + msg + """"}""")
      }
    } else {
      err(HStatus.Forbidden, "client already connected to chat", talk)
    }
  }
}

class UnsubscribeLet extends HSimpleLet {
  def act(talk : HTalk) = {
    ChatApp.clients.synchronized {
      ChatApp.clients.remove(talk.req.param("id").get)
      ()
    }
  }
}

class CommitLet extends HSimpleLet {
  def act(talk : HTalk) = {
    val nick = talk.req.param("nick").get
    val msg = talk.req.param("message").get
    ChatApp.clients.synchronized {
      // broadcast the message to all clients
      ChatApp.clients.foreach {
        case (_, client) =>
          client.resume(nick, msg)
      }
    }
  }
}