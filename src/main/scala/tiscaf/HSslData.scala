package tiscaf

/** SSL settings data. */
trait HSslData {

  //---------------------- to implement ------------------------------

  /** SSL ports. */
  protected def ports : Set[Int]

  /** Keystore passphrase. */
  val passphrase : String

  /** Keystore containing server certificate and CA certificate(s). */
  val keystore : String

  //---------------------- to override ------------------------------

  /** Require client authentication. By default `false`. */
  val clientAuth = false // client authentication

  /** Trusted client certificates and CA certificates. By default `None`. */
  val truststore : Option[String] = None // trusted client certificates and CA certificates

  /** Trusted client certificate depth according to CA certificate(s). By default `1`. */
  val trustDepth = 1 // trust depth for client certificate according to CA certificates in the truststore

}