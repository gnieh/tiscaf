package tiscaf

object HTree {
  implicit def string2lay(aDir : String) = new HTree { override def dir = aDir }

  def stub(text : String) : HLet[_] = new HSimpleLet {
    def act(tk : HTalk) = {
      val out = tk.bytes(text)
      tk.setContentLength(out.length) // if not buffered
        .setContentType("text/plain; charset=UTF-8")
        .write(out)
    }
  }
}

/** If your application has (at least partly) tree-like structure
 *  (URI nodes correspond to request handlers), you can assign handlers
 *  to tree nodes in eyes-friendly manner:
 *  {{{
 *  object MngApp extends HApp {
 *
 *    def resolve(req: HReqHeaderData): Option[HLet] = admRoot.resolve(req.uriPath)
 *
 *    private lazy val bookkeepers =
 *      "bookkeeper" += (
 *        "new"  ! stub("new bookkeeper - not implemented yet"),
 *        "list" ! adm.bk.ListBkLet
 *      )
 *
 *    private lazy val admRoot =
 *      "adm" += ( // adm root hasn't a handler. If has: "adm" ! adm.RootLet += (
 *        "in" ! adm.InLet, // "domain.com/adm/in"
 *        "menu" ! adm.MenuLet,
 *        "manager" += (
 *          "new"  ! adm.man.NewManLet,
 *          "list" ! adm.man.ListManLet
 *        ),
 *        bookkeepers
 *      )
 *
 *    // ...
 *  }
 *  }}}
 */
trait HTree { self =>

  def dir : String = ""
  def let : Option[HLet[_]] = None
  def lays : Seq[HTree] = Nil

  final def !(addLet : => HLet[_]) = new HTree {
    override def dir = self.dir
    override def let = Some(addLet)
    override def lays = self.lays
  }

  final def +=(addLays : HTree*) = new HTree {
    override def dir = self.dir
    override def let = self.let
    override def lays = addLays.toSeq
  }

  final def resolve(dirs : Seq[String]) : Option[HLet[_]] = dirs.filter(_.length != 0).toSeq match {
    case Seq() => if (self.dir.length == 0) self.let else None // uri == ""
    case seq =>
      // not-tail recursion
      def nextDir(rest : Seq[String], lay : HTree) : Option[HTree] = lay.dir match {
        case s if s == rest.head =>
          if (rest.size > 1) lay.lays.find(_.dir == rest.tail.head).flatMap(nextDir(rest.tail, _))
          else Some(lay) // it's a leaf in dirs - the only place of possible success
        case "" => lay.lays.find(_.dir == rest.head).flatMap(nextDir(rest, _))
        case _  => None
      }
      nextDir(seq, self).flatMap(_.let)
  }

  final def resolve(uriPath : String) : Option[HLet[_]] = resolve(uriPath.split("/"))
}
