package atlas

/** The application's places, kept in the URL fragment so the static site needs no server-side routing:
  * `#sequence`, `#classes` (with `/k=<k>` to open the table on one row), `#lifts` (the planar lifts, `/k=<k>`
  * for one row), `#class/<id>` (with `?orbits` to switch the vertex orbits on). The fragments of the earlier
  * atlas page (`#counts`, `#table`, `#table/k=<k>`) still lead to the same places.
  */
enum Route:
  case Sequence
  case Classes(k: Option[Int])
  case Lifts(k: Option[Int])
  case Class(id: String, orbits: Boolean)

object Route:

  private val KFilter = """k=(\d+)""".r

  /** The route of a fragment (with or without its leading '#'); anything unknown is the sequence. */
  def parse(fragment: String): Route =
    val body          = fragment.stripPrefix("#")
    val (path, query) = body.span(_ != '?')
    path.split('/').toList match
      case ("classes" | "table") :: Nil               => Classes(None)
      case ("classes" | "table") :: KFilter(k) :: Nil => Classes(Some(k.toInt))
      case "lifts" :: Nil                             => Lifts(None)
      case "lifts" :: KFilter(k) :: Nil               => Lifts(Some(k.toInt))
      case "class" :: id :: Nil if id.nonEmpty        =>
        Class(id, query.stripPrefix("?").split('&').contains("orbits"))
      case _                                          => Sequence

  /** The fragment of a route, the inverse of [[parse]]. */
  def fragment(r: Route): String = r match
    case Sequence          => "#sequence"
    case Classes(None)     => "#classes"
    case Classes(Some(k))  => s"#classes/k=$k"
    case Lifts(None)       => "#lifts"
    case Lifts(Some(k))    => s"#lifts/k=$k"
    case Class(id, orbits) => s"#class/$id" + (if orbits then "?orbits" else "")
