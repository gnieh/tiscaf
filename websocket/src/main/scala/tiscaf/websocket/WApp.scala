/** *****************************************************************************
 *  This file is part of tiscaf.
 *
 *  tiscaf is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Foobar is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with tiscaf.  If not, see <http://www.gnu.org/licenses/>.
 *  ****************************************************************************
 */
package tiscaf
package websocket

/** A WebSocket application is a group of request handlers communicating
 *  with the client over the [WebSocket protocol](https://tools.ietf.org/html/rfc6455).
 *
 *  @author Lucas Satabin
 */
trait WApp extends HApp {

  //----------------------- to override ---------------------------

  /** Override this to perform some check on the origin to determine
   *  whether this handshake should be authorized.
   *  By default the origin is always authorized if defined and rejected
   *  if not defined
   */
  def validate(origin: Option[String]): Boolean = origin.isDefined

  /** The version supported by this server.
   *  By default the supported version is 13
   */
  def version: Int = 13

  final override val chunked = true


}

