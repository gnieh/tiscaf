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

import scala.collection.mutable.Map

/** A tiscaf application with support for matching first against the HTTP verb
 *  then the resources.
 *  It makes it possible to group similar actions together.
 *
 *  @author Lucas Satabin
 */
trait HActionApp extends HApp with HRest {

  private val actions =
    Map.empty[HReqVerb.Value, PartialFunction[HReqData, HLet]]

  import HReqVerb._

  /** Register new actions for PUT method */
  def put(handler: PartialFunction[HReqData, HLet]): Unit =
    actions(Put) =
      actions.getOrElse(Put, PartialFunction.empty) orElse handler

  /** Register new actions for POST method (either multipart, url-encoded or octet stream) */
  def post(handler: PartialFunction[HReqData, HLet]): Unit =
    actions(Post) =
      actions.getOrElse(Post, PartialFunction.empty) orElse handler

  /** Register new actions for DELETE method */
  def delete(handler: PartialFunction[HReqData, HLet]): Unit =
    actions(Delete) =
      actions.getOrElse(Delete, PartialFunction.empty) orElse handler

  /** Register new actions for PATCH method */
  def patch(handler: PartialFunction[HReqData, HLet]): Unit =
    actions(Patch) =
      actions.getOrElse(Patch, PartialFunction.empty) orElse handler

  /** Register new actions for GET method */
  def get(handler: PartialFunction[HReqData, HLet]): Unit =
    actions(Get) =
      actions.getOrElse(Get, PartialFunction.empty) orElse handler

  /** Register new actions for OPTIONS method */
  def options(handler: PartialFunction[HReqData, HLet]): Unit =
    actions(Options) =
      actions.getOrElse(Options, PartialFunction.empty) orElse handler

  /** Register new actions for HEAD method */
  def head(handler: PartialFunction[HReqData, HLet]): Unit =
    actions(Head) =
      actions.getOrElse(Head, PartialFunction.empty) orElse handler

  final def resolve(req: HReqData): Option[HLet] =
    for {
      handler <- actions.get(HReqVerb.fromReqType(req.method))
      action <- handler.lift(req)
    } yield action

}

