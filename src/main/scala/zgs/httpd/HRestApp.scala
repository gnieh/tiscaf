package zgs.httpd

import scala.collection.mutable.ListBuffer

/** This HApp gives to the user some utility methods to easily write
 *  RESTful services.
 *
 *  @author Lucas Satabin
 *
 */
class HRestApp extends HApp {

  /** This method takes care of the dispatch based on the
   *  HTTP verb of the request.
   */
  final def resolve(req : HReqData) = {
    val handlers = req.method match {
      case HReqType.Get => getHandler
      case HReqType.PostData | HReqType.PostMulti | HReqType.PostOctets =>
        postHandler
      case HReqType.Delete => deleteHandler
      case _               => throw new RuntimeException("Unknown request type")
    }

    val splitted = splitPath(req.uriPath)

    // find the first handler for this request
    handlers.find(_.isDefinedAt(splitted, req)) match {
      case Some(handler) =>
        Some(handler(splitted, req))
      case _ => None
    }

  }

  private val postHandler =
    ListBuffer.empty[PartialFunction[(List[String], HReqData), HLet[_]]]
  private val getHandler =
    ListBuffer.empty[PartialFunction[(List[String], HReqData), HLet[_]]]
  private val deleteHandler =
    ListBuffer.empty[PartialFunction[(List[String], HReqData), HLet[_]]]

  def post(handler : PartialFunction[(List[String], HReqData), HLet[_]]) {
    postHandler += handler
  }

  def get(handler : PartialFunction[(List[String], HReqData), HLet[_]]) {
    getHandler += handler
  }

  def delete(handler : PartialFunction[(List[String], HReqData), HLet[_]]) {
    deleteHandler += handler
  }

  private def splitPath(path : String) =
    path.split("/").toList

}