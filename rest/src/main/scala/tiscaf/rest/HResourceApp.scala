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

/** A tiscaf application with support for matching first resources path
 *  then the HTTP verbs. It makes it possible to group actions by resource
 *
 *  @author Lucas Satabin
 */
trait HResourceApp extends HApp with HRest {

  private var resources =
    PartialFunction.empty[HReqData, PartialFunction[HReqType.Value, HLet]]

  def resource(resolver: PartialFunction[HReqData, PartialFunction[HReqType.Value, HLet]]): Unit =
    resources = resources orElse resolver

  final def resolve(req: HReqData): Option[HLet] =
    for {
      resource <- resources.lift(req)
      action <- resource.lift(req.method)
    } yield action

}

