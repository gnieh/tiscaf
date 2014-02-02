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

import scala.concurrent.Future

private object WebSocketLet {
  val key = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
}

private class WebSocketLet(handshake: WHandshake, wlet: WLet) extends HLet {

  import WebSocketLet._

  def aact(talk: HTalk): Future[Any] = {
    // send the header to notifiy that handshake was accepted
    talk
      .setStatus(HStatus.SwitchingProtocol)
      .setHeader("Upgrade", "websocket")
      .setHeader("Connection", "upgrade")
      .setHeader("Sec-WebSocket-Accept", Base64.encode(Sha1.hash(handshake.key + key)).mkString)
      .write("")
      // TODO protocols? extensions?
    ???
  }

}

