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

private[websocket] object WFrame {

  def fromStream(stream: Stream[Byte]): Option[WFrame] =
    for {
      (fin, rsv1, rsv2, rsv3, opcode, stream) <- flagsOpcode(stream)
      (masked, payloadLength, stream) <- maskedPaylodLength(stream)
      (maskingKey, stream) <- key(masked, stream)
      (extensionData, remaining, stream) <- extension(rsv1, rsv2, rsv3, maskingKey, payloadLength, stream)
      (applicationData, stream) <- application(maskingKey, remaining, stream)
    } yield ???

  private def flagsOpcode(stream: Stream[Byte]): Option[(Boolean, Boolean, Boolean, Boolean, WOpcode.Value, Stream[Byte])] =
    stream match {
      case byte #:: rest =>
        val fin = (byte & 0x80) == 0x80
        val rsv1 = (byte & 0x40) == 0x40
        val rsv2 = (byte & 0x20) == 0x20
        val rsv3 = (byte & 0x10) == 0x10
        for(opcode <- WOpcode.ofByte((byte & 0x0f).toByte))
          yield (fin, rsv1, rsv2, rsv3, opcode, rest)
      case _ =>
        None
    }

  private def maskedPaylodLength(stream: Stream[Byte]): Option[(Boolean, Long, Stream[Byte])] =
    stream match {
      case byte #::rest =>
        val masked = (byte & 0x80) == 0x80
        length(masked, (byte & 0x7f).toByte, rest)
      case _ =>
        None
    }

  private def length(masked: Boolean, first: Byte, stream: Stream[Byte]): Option[(Boolean, Long, Stream[Byte])] =
    (first, stream) match {
      case (126, byte1 #:: byte2 #:: rest) =>
        Some((masked, (byte1 << 4) | byte2, rest))
      case (127, byte1 #:: byte2 #:: byte3 #:: byte4 #:: byte5 #:: byte6 #:: byte7 #:: byte8 #:: rest) =>
        Some((masked, toLong(byte1, byte2, byte3, byte4, byte5, byte6, byte7, byte8), rest))
      case (length, rest) =>
        Some((masked, length, rest))
      case _ =>
        None
    }

  private def key(masked: Boolean, stream: Stream[Byte]): Option[(Option[Int], Stream[Byte])] =
    (masked, stream) match {
      case (true, key1 #:: key2 #:: key3 #:: key4 #:: rest) =>
        Some((Some(toInt(key1, key2, key3, key4)), rest))
      case (false, _) =>
        Some((None, stream))
      case (true, _) =>
        None
    }

  private def extension(
    rsv1: Boolean,
    rsv2: Boolean,
    rsv3: Boolean,
    maskingKey: Option[Int],
    size: Long,
    stream: Stream[Byte]): Option[(Array[Byte], Long, Stream[Byte])] =
    // TODO support extensions
    Some((Array(), size, stream))

  private def application(maskingKey: Option[Int], size: Long, stream: Stream[Byte]): Option[(Array[Byte], Stream[Byte])] =
    ???

  @inline
  private def unmask(key: Int, position: Int, byte: Byte): Byte =
    (byte ^ nthbyte(position % 4, key)).toByte

  @inline
  private def nthbyte(n: Int, key: Int): Byte =
    ((key >> (8 * (3 - n))) & 0xff).toByte


  private def toLong(bytes: Byte*): Long =
    bytes.foldLeft(0l) { (acc, byte) =>
      (acc << 8) | byte
    }

  private def toInt(bytes: Byte*): Int=
    bytes.foldLeft(0) { (acc, byte) =>
      (acc << 8) | byte
    }

}

private[websocket] object WOpcode extends Enumeration {
  val ContinuationFrame, TextFrame, BinaryFrame, ConnectionClose, Ping, Pong = Value

  def ofByte(byte: Byte): Option[Value] =
    if(byte == 0x00)
      Some(ContinuationFrame)
    else if(byte == 0x01)
      Some(TextFrame)
    else if(byte == 0x02)
      Some(BinaryFrame)
    else if(byte == 0x08)
      Some(ConnectionClose)
    else if(byte == 0x09)
      Some(Ping)
    else if(byte == 0x0a)
      Some(Pong)
    else
      None
}

