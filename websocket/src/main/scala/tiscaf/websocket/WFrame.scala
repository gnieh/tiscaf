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

/** A websocket frame received from the client or to send to a client.
 *  This frame is unmasked if it was received from the client, or not masked
 *  yet if the server masks its frames.
 *
 *  @author Lucas Satabin
 */
private[websocket] case class WFrame(
  fin: Boolean,
  rsv1: Boolean,
  rsv2: Boolean,
  rsv3: Boolean,
  opcode: WOpcode.Value,
  extensionData: Array[Byte],
  applicationData: Array[Byte])

private[websocket] object WOpcode extends Enumeration {
  val ContinuationFrame, TextFrame, BinaryFrame, ConnectionClose, Ping, Pong = Value
}

