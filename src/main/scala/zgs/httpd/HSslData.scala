package zgs.httpd

/** @author Lucas Satabin
 *
 */
trait HSslData {

  //---------------------- to implement ------------------------------

  protected def ports : Set[Int]

  val passphrase : String

  val keystore : String

  //---------------------- to override ------------------------------

  val clientAuth = false // client authentication

  val truststore : Option[String] = None // trusted client certificates and CA certificates

  val trustDepth = 1 // trust depth for client certificate according to CAs certificates in the truststore

}