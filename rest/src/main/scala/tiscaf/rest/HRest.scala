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
package tiscaf
package rest

import java.text.{ SimpleDateFormat, ParseException }

/** A bunch of helpers used to simplify writing Restful APIs.
 *  It mainly consists of nice extractors helping to match agains a resource path,
 *  query string, ...
 *
 *  @author Lucas Satabin
 */
trait HRest {

  object dot {
    def unapply(input: String) = {
      val index = input.lastIndexOf('.')
      if (index > 0) {
        // there is at least tow elements
        Some((input.substring(0, index), input.substring(index + 1)))
      } else {
        None
      }
    }
  }

  object long {
    def unapply(input: String) = try {
      Some(input.toLong)
    } catch {
      case _: Exception => None
    }
  }

  object int {
    def unapply(input: String) = try {
      Some(input.toInt)
    } catch {
      case _: Exception => None
    }
  }

  object date {
    val formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    def unapply(input: String) = try {
      Option(formatter.parse(input))
    } catch {
      case _: ParseException =>
        None
    }
  }

  /** Specific extractor to extract the path and query parts from a request */
  object ? {

    def unapply(req: HReqData): Option[(String, String)] =
      if(req.query != "")
        Some(req.uriPath, req.query)
      else
        None

  }

  /** Enriches `StringContext` with string enterpolators used to pattern match against a request */
  implicit class RestContext(val sc: StringContext) {

    /** Allows people to pattern match against some URL and bind values when needed */
    object p {

      val regex =
        sc.parts.map(scala.util.matching.Regex.quoteReplacement).mkString("/?", "([^/]+)", "").r

      def unapplySeq(s: String): Option[Seq[String]] =
        regex.unapplySeq(s)

      def unapplySeq(req: HReqData): Option[Seq[String]] =
        regex.unapplySeq(req.uriPath)

    }

    /** Allows people to pattern match against some query string */
    object q {

      val regex = sc.parts.map(scala.util.matching.Regex.quoteReplacement).mkString("([^&]+)").r

      def unapplySeq(s: String): Option[Seq[String]] =
        regex.unapplySeq(s)

    }

  }

  implicit class RichOption[T](opt: Option[T]) {

    def is(v: T): Boolean = opt match {
      case Some(v1) => v1 == v
      case None     => false
    }

  }

}

object HReqVerb extends Enumeration {
  val Get, Post, Put, Patch, Delete, Options, Head = Value

  def fromReqType(req: HReqType.Value): HReqVerb.Value = req match {
    case HReqType.Get =>
      Get
    case HReqType.PostData | HReqType.PostOctets | HReqType.PostMulti =>
      Post
    case HReqType.Put =>
      Put
    case HReqType.Patch =>
      Patch
    case HReqType.Delete =>
      Delete
    case HReqType.Options =>
      Options
    case HReqType.Head =>
      Head
  }

}

