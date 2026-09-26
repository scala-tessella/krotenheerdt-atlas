package atlas

/** The application's places, kept in the URL fragment so the static site needs no server-side routing:
  * `#sequence`, `#classes`, `#class/<id>` (with `?orbits` to switch the vertex orbits on).
  */
enum Route:
  case Sequence
  case Classes
  case Class(id: String, orbits: Boolean)

object Route:

  /** The route of a fragment (with or without its leading '#'); anything unknown is the sequence. */
  def parse(fragment: String): Route =
    val body          = fragment.stripPrefix("#")
    val (path, query) = body.span(_ != '?')
    path.split('/').toList match
      case "classes" :: Nil                    => Classes
      case "class" :: id :: Nil if id.nonEmpty =>
        Class(id, query.stripPrefix("?").split('&').contains("orbits"))
      case _                                   => Sequence

  /** The fragment of a route, the inverse of [[parse]]. */
  def fragment(r: Route): String = r match
    case Sequence          => "#sequence"
    case Classes           => "#classes"
    case Class(id, orbits) => s"#class/$id" + (if orbits then "?orbits" else "")
