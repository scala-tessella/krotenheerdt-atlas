package atlas

import Catalog.Filter

/** The application's places, each at its own path, so that every page has an address of its own and can be
  * written at build time: `/` (the home page), `/sequence`, `/classes` (with the table's filter as a query,
  * `/classes?k=5&world=prism&sort=chambers`, so a filtered list can be linked), `/lifts` (the prismatic
  * lifts, `/lifts/<k>` for one row), `/stars` and `/star/<species index>` (the vertex stars), `/guide` (and
  * `/guide#<section>`), `/about`, `/class/<id>` (with `?orbits` to switch the vertex orbits on). The places
  * were once kept in the URL fragment (`#class/<id>`, and before that `#counts`, `#table`): [[legacy]] still
  * leads from those to the same places.
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
  case NotFound

object Route:

  private val Number  = """(\d+)""".r
  private val KFilter = """k=(\d+)""".r

  private def orbitsIn(query: String): Boolean = query.stripPrefix("?").split('&').contains("orbits")

  /** The route of an address, its path, query and fragment as the location gives them (`/classes?k=5`,
    * `/guide#trust`); a trailing slash is ignored, and anything unknown is [[NotFound]].
    */
  def parse(address: String): Route =
    val (rest, fragment) = address.span(_ != '#')
    val (path, query)    = rest.span(_ != '?')
    path.split('/').toList.filter(_.nonEmpty) match
      case Nil                         => Home
      case "sequence" :: Nil           => Sequence
      case "classes" :: Nil            => Classes(Filter.fromQuery(query))
      case "lifts" :: Nil              => Lifts(None)
      case "lifts" :: Number(k) :: Nil => Lifts(Some(k.toInt))
      case "stars" :: Nil              => Stars
      case "star" :: Number(n) :: Nil  => Star(n.toInt)
      case "guide" :: Nil              => Guide(Option(fragment.stripPrefix("#")).filter(_.nonEmpty))
      case "about" :: Nil              => About
      case "class" :: id :: Nil        => Class(id, orbitsIn(query))
      case _                           => NotFound

  /** The address of a route, the inverse of [[parse]]. */
  def path(r: Route): String = r match
    case Home              => "/"
    case Sequence          => "/sequence"
    case Classes(f)        => "/classes" + Option(f.toQuery).filter(_.nonEmpty).fold("")("?" + _)
    case Lifts(None)       => "/lifts"
    case Lifts(Some(k))    => s"/lifts/$k"
    case Stars             => "/stars"
    case Star(n)           => s"/star/$n"
    case Guide(None)       => "/guide"
    case Guide(Some(s))    => s"/guide#$s"
    case About             => "/about"
    case Class(id, orbits) => s"/class/$id" + (if orbits then "?orbits" else "")
    case NotFound          => "/404"

  /** The page a route is a state of: the table whatever its filter, the guide whatever its section, a class
    * whatever its orbits. One page is written, and one address is canonical, for all its states.
    */
  def page(r: Route): Route = r match
    case Classes(_)   => Classes(Filter())
    case Guide(_)     => Guide(None)
    case Class(id, _) => Class(id, orbits = false)
    case other        => other

  /** The route of a fragment of the earlier addresses (`#class/k3-040?orbits`, `#guide/trust`, `#lifts/k=3`,
    * and the older `#counts`, `#table`, `#table/k=<k>`), with or without its leading '#'; None for anything
    * else, a fragment of the home page included.
    */
  def legacy(fragment: String): Option[Route] =
    val (path, query) = fragment.stripPrefix("#").span(_ != '?')
    path.split('/').toList match
      case ("sequence" | "counts") :: Nil             => Some(Sequence)
      case ("classes" | "table") :: Nil               => Some(Classes(Filter.fromQuery(query)))
      case ("classes" | "table") :: KFilter(k) :: Nil => Some(Classes(Filter(k = Some(k.toInt))))
      case "lifts" :: Nil                             => Some(Lifts(None))
      case "lifts" :: KFilter(k) :: Nil               => Some(Lifts(Some(k.toInt)))
      case "stars" :: Nil                             => Some(Stars)
      case "star" :: Number(n) :: Nil                 => Some(Star(n.toInt))
      case "guide" :: Nil                             => Some(Guide(None))
      case "guide" :: s :: Nil if s.nonEmpty          => Some(Guide(Some(s)))
      case "about" :: Nil                             => Some(About)
      case "class" :: id :: Nil if id.nonEmpty        => Some(Class(id, orbitsIn(query)))
      case _                                          => None
