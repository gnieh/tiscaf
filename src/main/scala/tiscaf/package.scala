
import scala.util.continuations._

/** The tiscaf server is a lightweight and easy way to integrate an HTTP
 *  server to your application.
 */
package object tiscaf {

  @scala.inline
  def dummy : Unit @suspendable = shift { k : (Unit => Unit) =>
    k()
  }

  @scala.inline
  implicit def toDummy[T](code : => T) : T @suspendable = {
    dummy
    code
  }

}