
import scala.util.continuations._

/** @author Lucas Satabin
 *
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