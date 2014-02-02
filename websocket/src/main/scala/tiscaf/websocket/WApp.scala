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

import util._

/** A WebSocket application is a group of request handlers communicating
 *  with the client over the [WebSocket protocol](https://tools.ietf.org/html/rfc6455).
 *
 *  @author Lucas Satabin
 */
trait WApp extends HApp {

  //----------------------- to implement -------------------------

  protected def wresolve(resource: String): Option[WLet]

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
  def version: String = "13"

  //--------------------------- internals -------------------------------

  final override def chunked: Boolean = false

  final def resolve(req: HReqData): Option[HLet] = wresolve(req.uriPath) match {
    case Some(wlet) =>
      // perform handshake
      val handshake = for {
        // client must send a get request
        HReqType.Get <- Some(req.method)
        // with a 'Host' header
        host <- req.header("Host")
        // and "Upgrade" header with "websocket" value (case insensitive)
        "websocket" <- req.header("Upgrade").map(_.toLowerCase)
        // and "Connection" header with "upgrade" value (case insensitive)
        "upgrade" <- req.header("Connection").map(_.toLowerCase)
        // and a "Sec-WebSocket-Key" header that is 16 bytes long once decoded
        key <- req.header("Sec-WebSocket-Key")
        if Base64.decode(key).length == 16
        // and a "Sec-WebSocket-Version" header
        version <- req.header("Sec-WebSocket-Version")
        // and an optional "Origin" header
        origin = req.header("Origin")
        // and an optional list of websocket protocols ordered by preference
        protocols <- req.header("Sec-WebSocket-Protocol").map(_.split(",").map(_.trim).toList).orElse(Some(Nil))
        // and an optional list of websocket extensions
        extensions <- req.header("Sec-WebSocket-Extensions").map(_.split(",").map(_.trim).toList).orElse(Some(Nil))
      } yield WHandshake(host, key, version, origin, protocols, extensions)

      handshake match {
        case Some(handshake @ WHandshake(_, _, v, origin, _, _)) if validate(origin) && v == version =>
          Some(new WebSocketLet(handshake, wlet))
        case Some(WHandshake(_, _, _, origin, _, _)) if !validate(origin) =>
          Some(forbiddenLet)
        case Some(WHandshake(_, _, v, _, _, _)) if v != version =>
          Some(upgradeLet)
        case None =>
          Some(badLet)
      }

    case None =>
      // no websocket handler at the given path
      None

  }

  private val badLet = new let.ErrLet(HStatus.BadRequest)
  private val forbiddenLet = new let.ErrLet(HStatus.Forbidden)
  private val upgradeLet = new HSimpleLet {
    def act(talk: HTalk): Unit = {
      talk.setStatus(HStatus.UpgradeRequired).setHeader("Sec-WebSocket-Version", version)
    }
  }


}

