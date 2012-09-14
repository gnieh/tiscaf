package tiscaf

import javax.net.ssl._
import java.security._

import scala.collection.{ mutable => mute }

/** tiscaf SSL engine. It manages the session cache as well as SSL settings. */
trait HSsl {

  //---------------------- to implement ------------------------------

  /** SSL ports. */
  private[tiscaf] def ports: Set[Int]

  /** Keystore passphrase. */
  private[tiscaf] def passphrase: String

  /** Keystore containing server certificate and CA certificate(s). */
  private[tiscaf] def keystore: KeyStore

  //---------------------- to override ------------------------------

  /** Require client authentication. By default `false`. */
  def clientAuth: HClientAuth.Value = HClientAuth.None // client authentication

  /** Trusted client certificates and CA certificates. By default `None`. */
  def truststore: Option[KeyStore] = None // trusted client certificates and CA certificates

  /** Trusted client certificate depth according to CA certificate(s). By default `1`. */
  def trustDepth = 1 // trust depth for client certificate according to CA certificates in the truststore

  /** The protocol. By default `SSL` */
  def protocol = "SSL"

  /** Specific JCE provider name if one wants to use it.
   *  By default `None` which means that the default provider is used.
   */
  def provider: Option[String] = None

  /** SSL session timeout in minutes. By default `5`. */
  def sslSessionTimeoutMin: Int = 5

  //---------------------- internals ------------------------------

  private val keyManagers = {
    val factory = provider match {
      case Some(p) =>
        KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm, p)
      case None => KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm)
    }
    factory.init(keystore, passphrase.toCharArray)
    factory.getKeyManagers
  }

  private val trustManagers =
    truststore match {
      case Some(ts) =>
        val factory = provider match {
          case Some(p) =>
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm, p)
          case None => TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)
        }
        factory.init(ts)
        factory.getTrustManagers
      case None => null
    }

  private val sslContext = {
    val context = provider match {
      case Some(p) => SSLContext.getInstance(protocol, p)
      case None    => SSLContext.getInstance(protocol)
    }
    context.init(keyManagers, trustManagers, new SecureRandom)
    context
  }

  private[tiscaf] def engine(host: String, port: Int) = {
    sessions.engine(host, port) match {
      case Some(engine) => engine
      case None         => sessions.create(host, port, sslContext)
    }
  }

  private object sessions {

    def create(host: String, port: Int, context: SSLContext): SSLEngine = {
      val newSess = SslSess(now, context.createSSLEngine(host, port))
      bags((host, port)) = newSess
      newSess.engine
    }

    def engine(host: String, port: Int): Option[SSLEngine] =
      for (sess <- bags.get((host, port))) yield sess.engine

    def restamp(host: String, port: Int): Unit =
      for (b <- bags.get((host, port)))
        yield bags((host, port)) = b.restamp

    def isValid(host: String, port: Int): Boolean = bags.get((host, port)).isDefined

    def invalidate(host: String, port: Int) {
      bags.remove((host, port)) match {
        case Some(sess) =>
          // close the SSL engine
          sess.engine.closeInbound
          sess.engine.closeOutbound
        case None =>
      }
    }

    // internals
    private case class SslSess(val stamp: Long, val engine: SSLEngine) {
      def restamp = SslSess(now, engine)
    }

    private val bags = new mute.HashMap[(String, Int), SslSess] with mute.SynchronizedMap[(String, Int), SslSess]

    private def now = System.currentTimeMillis

    private def cleanExpired: Unit = bags.synchronized {
      for ((host, port) <- bags.keySet) {
        val sess = bags((host, port))
        if (sess.stamp < now - (sslSessionTimeoutMin * 60000L)) invalidate(host, port)
      }
    }

    // starting cleaning daemon
    new java.util.Timer(true).scheduleAtFixedRate(
      new java.util.TimerTask { def run { cleanExpired } },
      60000, 60000) // every minute
  }

}

/** Indicates whether client authentication is accepted, required,
 *  or if none is needed.
 */
object HClientAuth extends Enumeration {
  val None, Accepted, Required = Value
}