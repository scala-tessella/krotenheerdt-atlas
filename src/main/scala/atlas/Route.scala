package atlas

import Catalog.Filter

/** The application's places, kept in the URL fragment so the static site needs no server-side routing: `#`
  * (the home page), `#sequence`, `#classes` (with the table's filter as a query,
  * `#classes?k=5&world=prism&sort=chambers`, so a filtered list can be linked), `#lifts` (the prismatic
  * lifts, `/k=<k>` for one row), `#stars` and `#star/<species index>` (the vertex stars), `#guide` (and
  * `#guide/<section>`), `#about`, `#class/<id>` (with `?orbits` to switch the vertex orbits on). The
  * fragments of the earlier atlas page (`#counts`, `#table`, `#table/k=<k>`) and `#classes/k=<k>` still lead
  * to the same places.
  */
enum Route:
  case Home
  case Sequence
  case Classes(filter: Filter)
  case Lifts(k: Option[Int])
  case Stars
  case Star(index: Int)
  case Guide(section: Option[String])
  case About
  case Class(id: String, orbits: Boolean)

object Route:

  private val KFilter = """k=(\d+)""".r

  /** The route of a fragment (with or without its leading '#'); anything unknown is the home page. */
  def parse(fragment: String): Route =
    val body          = fragment.stripPrefix("#")
    val (path, query) = body.span(_ != '?')
    path.split('/').toList match
      case ("sequence" | "counts") :: Nil                => Sequence
      case ("classes" | "table") :: Nil                  => Classes(Filter.fromQuery(query))
      case ("classes" | "table") :: KFilter(k) :: Nil    => Classes(Filter(k = Some(k.toInt)))
      case "lifts" :: Nil                                => Lifts(None)
      case "lifts" :: KFilter(k) :: Nil                  => Lifts(Some(k.toInt))
      case "stars" :: Nil                                => Stars
      case "star" :: n :: Nil if n.toIntOption.isDefined => Star(n.toInt)
      case "guide" :: Nil                                => Guide(None)
      case "guide" :: s :: Nil if s.nonEmpty             => Guide(Some(s))
      case "about" :: Nil                                => About
      case "class" :: id :: Nil if id.nonEmpty           =>
        Class(id, query.stripPrefix("?").split('&').contains("orbits"))
      case _                                             => Home

  /** The fragment of a route, the inverse of [[parse]]. */
  def fragment(r: Route): String = r match
    case Home              => "#"
    case Sequence          => "#sequence"
    case Classes(f)        => "#classes" + Option(f.toQuery).filter(_.nonEmpty).fold("")("?" + _)
    case Lifts(None)       => "#lifts"
    case Lifts(Some(k))    => s"#lifts/k=$k"
    case Stars             => "#stars"
    case Star(n)           => s"#star/$n"
    case Guide(None)       => "#guide"
    case Guide(Some(s))    => s"#guide/$s"
    case About             => "#about"
    case Class(id, orbits) => s"#class/$id" + (if orbits then "?orbits" else "")
