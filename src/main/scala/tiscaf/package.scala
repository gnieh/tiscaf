/*******************************************************************************
 * This file is part of tiscaf.
 * 
 * tiscaf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with tiscaf.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
import scala.util.continuations._

/** The tiscaf server is a lightweight and easy way to integrate an HTTP
 *  server to your application.
 */
package object tiscaf {

  @scala.inline
  def dummy: Unit @suspendable = shift { k: (Unit => Unit) =>
    k()
  }

  @scala.inline
  implicit def toDummy[T](code: => T): T @suspendable = {
    dummy
    code
  }

}
