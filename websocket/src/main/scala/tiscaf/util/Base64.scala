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
package util

import scala.annotation.tailrec

/** Simple utility class that encodes/decodes to base64
 *
 *  @author Lucas Satabin
 */
object Base64 {

  private val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

  @tailrec
  private def encode(bytes: List[Byte], acc: List[Char]): List[Char] = bytes match {
    case b1 :: b2 :: b3 :: rest =>
      // simple case, there are enough bytes
      val c1 = (b1 & 0xfc) >>> 2
      val c2 = ((b1 & 0x03) << 4) | ((b2 & 0xf0) >>> 4)
      val c3 = ((b2 & 0x0f) << 2) | ((b3 & 0xc0) >>> 6)
      val c4 = b3 & 0x3f
      encode(rest, chars(c4) :: chars(c3) :: chars(c2) :: chars(c1) :: acc)
    case b1 :: b2 :: Nil =>
      // missing one byte in this end
      val c1 = (b1 & 0xfc) >>> 2
      val c2 = ((b1 & 0x03) << 4) | ((b2 & 0xf0) >>> 4)
      val c3 = ((b2 & 0x0f) << 2)
      encode(Nil, '=' :: chars(c3) :: chars(c2) :: chars(c1) :: acc)
    case b :: Nil =>
      // missing two bytes in this end
      val c1 = (b & 0xfc) >>> 2
      val c2 = ((b & 0x03) << 4)
      encode(Nil, '=' :: '=' :: chars(c2) :: chars(c1) :: acc)
    case Nil =>
      acc.reverse
  }

  def encode(str: String): String =
    encode(str.getBytes.toList, Nil).mkString

  def encode(bytes: List[Byte]): List[Char] =
    encode(bytes, Nil)

  @tailrec
  private def decode(characters: List[Char], acc: List[Byte]): List[Byte] = characters match {
    case c1 :: c2 :: '=' :: '=' :: _ =>
      val i1 = chars.indexOf(c1)
      val i2 = chars.indexOf(c2)
      val b = ((i1 << 2) | (i2 >>> 4)).toByte
      decode(Nil, b :: acc)
    case c1 :: c2 :: c3 :: '=' :: _ =>
      val i1 = chars.indexOf(c1)
      val i2 = chars.indexOf(c2)
      val i3 = chars.indexOf(c3)
      val b1 = ((i1 << 2) | (i2 >>> 4)).toByte
      val b2 = (((i2 & 0x0f) << 4) | (i3 >>> 2)).toByte
      decode(Nil, b2 :: b1 :: acc)
    case c1 :: c2 :: c3 :: c4 :: rest =>
      val i1 = chars.indexOf(c1)
      val i2 = chars.indexOf(c2)
      val i3 = chars.indexOf(c3)
      val i4 = chars.indexOf(c4)
      val b1 = ((i1 << 2) | (i2 >>> 4)).toByte
      val b2 = (((i2 & 0x0f) << 4) | (i3 >>> 2)).toByte
      val b3 = (((i3 & 0x03) << 6) | i4).toByte
      decode(rest, b3 :: b2 :: b1 :: acc)
    case _ =>
      acc.reverse
  }

  def decode(str: String): String = {
    if(!str.forall(c => chars.contains(c) || c == '=') || str.size % 4 != 0)
      throw new IllegalArgumentException("Not a base64 encoded string")
    new String(decode(str.toList, Nil).toArray)
  }

  def decode(str: List[Char]): List[Byte] = {
    if(!str.forall(c => chars.contains(c) || c == '=') || str.size % 4 != 0)
      throw new IllegalArgumentException("Not a base64 encoded string")
    decode(str, Nil)
  }

}

