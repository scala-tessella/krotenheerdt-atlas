package atlas

import scala.concurrent.ExecutionContext.Implicits.global

import com.raquo.laminar.api.L.*
import org.scalajs.dom

import Catalog.*
import Model.*

/** The atlas application: the sequence, the table of classes and a page per class, driven by the address
  * ([[Route]]). Every page is also written at build time ([[written]]), and that text stands until the index
  * is fetched and the application takes its place.
  */
object Main:

  /** The address as the location gives it: path, query and fragment. */
  private def address: String =
    val l = dom.window.location
    l.pathname + l.search + l.hash

  /** The route the visit starts at. An address of the earlier kind, a fragment on the home page, is replaced
    * by the address of its place.
    */
  private def start: Route =
    Option.when(dom.window.location.pathname == "/")(dom.window.location.hash).flatMap(Route.legacy) match
      case Some(r) => dom.window.history.replaceState(null, "", Route.path(r)); r
      case None    => Route.parse(address)

  val route: Var[Route] = Var(start)

  /** The table's filter and sort, kept for the visit, so the table reopens as it was left. */
  val filter: Var[Filter] = Var(Filter())

  /** Goes to a place, as a step of the history. The page opens at its top, except from class to class, where
    * the arrows walk the atlas with the viewer in place; the guide scrolls to its section.
    */
  def go(r: Route): Unit =
    if Route.path(r) != address then
      val walking = (route.now(), r) match
        case (_: Route.Class, _: Route.Class) => true
        case _                                => false
      dom.window.history.pushState(null, "", Route.path(r))
      route.set(r)
      if !walking then dom.window.scrollTo(0, 0)
    else
      r match
        case Route.Guide(Some(s)) => Guide.scrollTo(s)
        case _                    => ()

  /** A plain click on a link to a place of the atlas goes there without loading a page; a click with a
    * modifier, a link elsewhere or to a new tab, and a click already handled are left to the browser.
    */
  private def follow(e: dom.MouseEvent): Unit =
    if !e.defaultPrevented && e.button == 0 && !(e.ctrlKey || e.metaKey || e.shiftKey || e.altKey) then
      val here = dom.window.location
      e.target match
        case el: dom.Element =>
          Option(el.closest("a")).collect { case a: dom.HTMLAnchorElement => a }
            .filter(a => a.target.isEmpty && a.protocol == here.protocol && a.host == here.host)
            .map(a => Route.parse(a.pathname + a.search + a.hash))
            .filter(_ != Route.NotFound)
            .foreach { r => e.preventDefault(); go(r) }
        case _               => ()

  /** The head of the document follows the page: its title, its description and its canonical address. */
  private def show(h: PageHead): Unit =
    dom.document.title = h.title
    def set(selector: String, attribute: String, value: String): Unit =
      Option(dom.document.querySelector(selector)).foreach(_.setAttribute(attribute, value))
    set("meta[name=description]", "content", h.description)
    set("link[rel=canonical]", "href", h.canonical)

  def main(args: Array[String]): Unit =
    // the page as it was written at build time stands until the index is here
    Data.index.foreach { i =>
      val container = dom.document.getElementById("app")
      container.textContent = ""
      render(container, app(i))
    }

  def app(i: AtlasIndex): HtmlElement =
    frame(i, child <-- route.signal.map(content(i, _))).amend(
      windowEvents(_.onPopState) --> { _ => route.set(Route.parse(address)) },
      onClick --> { e => follow(e) },
      route.signal --> { r => show(PageHead.of(i, r)) }
    )

  /** A page as it is written at build time: the frame around what the page holds before any of its data is
    * fetched, with the table of classes whole, so that every class is linked from it.
    */
  def written(i: AtlasIndex, r: Route): HtmlElement =
    frame(
      i,
      r match
        case Route.Classes(_) => allClasses(i)
        case _                => content(i, r)
    )

  private def content(i: AtlasIndex, r: Route): HtmlElement = r match
    case Route.Home              => Home.view(i)
    case Route.Sequence          => sequenceView(i)
    case Route.Guide(section)    => Guide.view(section)
    case Route.About             => About.view(i.classes.length)
    case Route.Lifts(k)          => Lifts.view(i, k)
    case Route.Stars             => Stars.view(i)
    case Route.Star(n)           => Stars.page(i, n)
    case Route.Classes(f)        =>
      filter.set(f) // the table opens as its URL says
      classesView(i)
    case Route.Class(id, orbits) =>
      i.classes.find(_.id == id).fold(p(cls := "note", s"no class $id"))(classView(i, _, orbits))
    case Route.NotFound          => p(cls := "note", "no such page")

  /** The header, the content and the footer of every page. */
  private def frame(i: AtlasIndex, content: Modifier[HtmlElement]): HtmlElement =
    div(
      // the first stop of the keyboard: straight to the page's content, past the header
      a(
        cls          := "skip",
        href         := "",
        onClick.preventDefault --> { _ =>
          Option(dom.document.getElementById("content")).foreach(_.asInstanceOf[dom.HTMLElement].focus())
        },
        "skip to content"
      ),
      headerTag(
        h1(a(href := Route.path(Route.Home), "Krötenheerdt honeycombs of E³")),
        navTag(
          a(
            href := Route.path(Route.Sequence),
            cls("on") <-- route.signal.map(_ == Route.Sequence),
            "Sequence"
          ),
          a(
            href := Route.path(Route.Classes(Filter())),
            href <-- filter.signal.map(f => Route.path(Route.Classes(f))),
            cls("on") <-- route.signal.map { case Route.Classes(_) => true; case _ => false },
            "Classes"
          ),
          a(
            href := Route.path(Route.Lifts(None)),
            cls("on") <-- route.signal.map { case Route.Lifts(_) => true; case _ => false },
            span(cls := "long", "Prismatic lifts"),
            span(cls := "short", "Lifts")
          ),
          a(
            href := Route.path(Route.Stars),
            cls("on") <-- route.signal.map { case Route.Stars | Route.Star(_) => true; case _ => false },
            span(cls := "long", "Vertex stars"),
            span(cls := "short", "Stars")
          ),
          a(
            href := Route.path(Route.Guide(None)),
            cls("on") <-- route.signal.map { case Route.Guide(_) => true; case _ => false },
            "Guide"
          ),
          child.maybe <-- route.signal.map {
            case r: Route.Class => Some(a(href := Route.path(r), cls := "on", "Class"))
            case _              => None
          }
        ),
        span(cls := "spacer"),
        Search.box(i),
        span(cls := "note total", s"${i.classes.length} classes, k = 1 to ${i.classes.map(_.k).max}")
      ),
      mainTag(idAttr := "content", tabIndex := -1, content),
      footerTag(
        a(href   := Route.path(Route.Guide(None)), "Guide"),
        a(href   := Route.path(Route.About), "About"),
        span(cls := "note", s"data ${DataVersion.value}"),
        a(href   := "https://www.tessell.art", target := "_blank", rel := "noopener", "tessell.art")
      )
    )

  // ---------- the sequence ----------

  def sequenceView(i: AtlasIndex): HtmlElement =
    val classes = i.classes.toSeq
    div(
      cls := "card",
      h2("The three-dimensional Krötenheerdt sequence"),
      p(
        cls := "prose",
        "N",
        sub("k"),
        " counts the face-to-face honeycombs of E³ by unit-edge convex uniform polyhedra with exactly ",
        "k vertex orbits carrying k pairwise distinct vertex stars. For k ≥ 5 every such honeycomb is a ",
        "two-direction stacking of cube layers and prism rows, or the prismatic lift of a planar Krötenheerdt ",
        "tiling; the planar numbers are 11, 20, 39, 33, 15, 10, 7 and then 0. The three-dimensional sequence reads ",
        "28, 57, 119, 146, 122, 78, 18, 2 and vanishes from k = 9 on, one row after the planar one."
      ),
      p(
        cls := "prose",
        "Every planar Krötenheerdt tiling is here too: stacked into prisms it becomes a honeycomb of the atlas, its ",
        "prismatic lift. The planar sequence sits inside the spatial one, row by row — ",
        a(href := Route.path(Route.Lifts(None)), "browse the prismatic lifts"),
        "."
      ),
      div(
        cls := "table-wrap",
        table(
          cls := "seq",
          thead(
            tr(
              th("k"),
              th("N", sub("k")),
              th(title := "the planar Krötenheerdt tilings, lifted among the N_k", "planar"),
              th("status"),
              th(cls   := "composition", "composition")
            )
          ),
          tbody(
            i.sequence.toSeq.sortBy(_.k).map { r =>
              val listed = classes.count(_.k == r.k)
              tr(
                td(r.k),
                td(
                  if listed == 0 then r.n.toString
                  else
                    a(
                      href       := Route.path(Route.Classes(Filter(k = Some(r.k)))),
                      aria.label := s"the $listed classes of k = ${r.k}",
                      r.n
                    )
                ),
                td(
                  r.planar.toOption.filter(_ > 0).fold[Modifier[HtmlElement]](r.planar.getOrElse(0).toString)(
                    n =>
                      a(
                        href       := Route.path(Route.Lifts(Some(r.k))),
                        aria.label := s"the $n prismatic lifts of k = ${r.k}",
                        n
                      )
                  )
                ),
                td(cls := s"status-${r.status}", r.status),
                td(cls := "composition", r.note)
              )
            }
          )
        )
      ),
      p(
        cls := "note",
        "theorem: complete and certified; exact: complete under the structure theorems, every class certified."
      ),
      p(
        cls := "prose",
        "The first row is the theorem of ",
        em(Papers.honeycombsTitle),
        " (",
        Papers.honeycombs,
        "); the rows from k = 2 on and the vanishing are the theorems of ",
        em(Papers.sequenceTitle),
        " (",
        Papers.sequence,
        "). The structure theorems are proved in the papers; every count and every class is re-derived by a ",
        "public verification artifact that anyone can run. The guide tells ",
        a(href := Route.path(Route.Guide(Some("trust"))), "why the counts can be trusted"),
        "."
      )
    )

  // ---------- the table ----------

  private def tag(c: ClassEntry): HtmlElement = span(cls := s"tag ${c.cat}", c.cat)

  /** A choice filter: "all" or one of the column's values. */
  private def choice(
      title: String,
      values: Seq[String],
      get: Filter => Option[String],
      set: (Filter, Option[String]) => Filter
  ): HtmlElement =
    label(
      title + " ",
      select(
        option(value := "", "all"),
        values.map(v => option(value := v, v)),
        controlled(
          value <-- filter.signal.map(get(_).getOrElse("")),
          onChange.mapToValue --> { v => filter.update(set(_, Option(v).filter(_.nonEmpty))) }
        )
      )
    )

  /** How many rows of the list are shown; reset to a page whenever the filter changes. */
  private val shown: Var[Int] = Var(Layout.pageSize)

  def classesView(i: AtlasIndex): HtmlElement =
    val classes     = i.classes.toSeq
    val rows        = filter.signal.map(Catalog.rows(classes, _))
    val visible     = rows.combineWith(shown.signal).map((r, n) => r.take(n))
    val filtersOpen = Var(false)
    val controls    = Seq(
      choice("k", choices(classes, _.k.toString), _.k.map(_.toString), (f, v) => f.copy(k = v.map(_.toInt))),
      choice("world", choices(classes, _.world), _.world, (f, v) => f.copy(world = v)),
      choice("source", choices(classes, _.cat), _.source, (f, v) => f.copy(source = v)),
      label(
        cls      := "search",
        "search",
        input(
          typ         := "search",
          placeholder := "species label, name, word, key",
          controlled(
            value <-- filter.signal.map(_.text),
            onInput.mapToValue --> { t => filter.update(_.copy(text = t)) }
          )
        )
      ),
      label(
        cls      := "check",
        input(
          typ := "checkbox",
          controlled(
            checked <-- filter.signal.map(_.liftsOnly),
            onClick.mapToChecked --> { b => filter.update(_.copy(liftsOnly = b)) }
          )
        ),
        "prismatic lifts only"
      ),
      button(cls := "quiet", onClick --> { _ => filter.set(Filter()) }, "clear")
    )
    div(
      // every change of the filter shows the first page again and is written into the URL, replacing the entry
      // (no history step, no re-render: the search box keeps its focus while one types)
      filter.signal.changes --> { f =>
        shown.set(Layout.pageSize)
        dom.window.history.replaceState(null, "", Route.path(Route.Classes(f)))
      },
      div(
        cls   := "toolbar",
        button(
          cls    := "filters-toggle",
          aria.expanded <-- filtersOpen.signal,
          onClick --> { _ => filtersOpen.update(!_) },
          child.text <-- filter.signal.map(f =>
            if activeFilters(f) == 0 then "Filters" else s"Filters (${activeFilters(f)})"
          )
        ),
        span(cls := "note", child.text <-- rows.map(r => s"${r.size} of ${classes.size} classes"))
      ),
      div(cls := "filters", cls("open") <-- filtersOpen.signal, controls),
      child <-- Layout.narrow.signal.map { narrow =>
        if narrow then div(cls := "cards", children <-- visible.map(_.map(card)))
        else
          table(
            cls                := "classes",
            thead(
              tr(
                Column.values.toSeq.map(col =>
                  // a header sorts by its column, a button for the keyboard; the sort is announced
                  th(
                    cls := "sortable",
                    aria.sort <-- filter.signal.map(f =>
                      if f.sort != col then "none" else if f.ascending then "ascending" else "descending"
                    ),
                    button(
                      cls := "sort",
                      onClick --> { _ => filter.update(_.sortedBy(col)) },
                      child.text <-- filter.signal.map(f =>
                        col.title + (if f.sort == col then if f.ascending then " ▲" else " ▼" else "")
                      )
                    )
                  )
                )
              )
            ),
            tbody(children <-- visible.map(_.map(tableRow)))
          )
      },
      // the end of the list: coming into view, it shows a page more
      child.maybe <-- rows.combineWith(shown.signal).map((r, n) =>
        Option.when(n < r.size)(
          div(
            cls := "more",
            onMountCallback { ctx =>
              val observer = new dom.IntersectionObserver((entries, _) =>
                if entries.exists(_.isIntersecting) then shown.update(_ + Layout.pageSize)
              )
              observer.observe(ctx.thisNode.ref)
            },
            button(
              cls := "quiet",
              onClick --> { _ => shown.update(_ + Layout.pageSize) },
              s"show more (${r.size - n} left)"
            )
          )
        )
      )
    )

  /** The table with every class and no control, for the page written at build time. */
  private def allClasses(i: AtlasIndex): HtmlElement =
    table(
      cls := "classes",
      thead(tr(Column.values.toSeq.map(col => th(col.title)))),
      tbody(i.classes.toSeq.map(tableRow))
    )

  private def classLink(c: ClassEntry): Route = Route.Class(c.id, orbits = false)

  private def tableRow(c: ClassEntry): HtmlElement =
    tr(
      cls := "row",
      onClick --> { _ => go(classLink(c)) },
      td(a(cls := "mono", href := Route.path(classLink(c)), c.id)),
      td(c.k),
      td(c.name),
      td(tag(c)),
      td(c.world),
      td(chambersOf(c).fold("")(_.toString)),
      td(cls := "mono", if c.pair.nonEmpty then c.pair else c.cells.mkString(" "))
    )

  /** A class as a card, on narrow screens: id, name, tags and chambers, and one line of species. */
  private def card(c: ClassEntry): HtmlElement =
    a(
      cls  := "class-card",
      href := Route.path(classLink(c)),
      div(cls := "card-head", span(cls := "mono id", c.id), tag(c), span(cls := "world", c.world)),
      div(cls := "card-name", c.name),
      div(
        cls   := "card-meta",
        chambersOf(c).fold("")(n => s"$n chambers"),
        span(cls := "mono species", if c.pair.nonEmpty then c.pair else c.cells.mkString(" "))
      )
    )

  // ---------- a class ----------

  /** The guide's section for a term of the class page's list. */
  private val guideOf: Map[String, String] = Map(
    "species"       -> "stars",
    "world"         -> "worlds",
    "chambers"      -> "symbols",
    "key"           -> "symbols",
    "stacking word" -> "words",
    "source"        -> "certificates"
  )

  /** A line of the class page's list, its term linked to the guide where the guide explains it. */
  private def row(title: String, value: Modifier[HtmlElement]*): Seq[HtmlElement] =
    val term = guideOf.get(title).fold[Modifier[HtmlElement]](title)(g =>
      a(cls := "term", href := Route.path(Route.Guide(Some(g))), title)
    )
    Seq(dt(term), dd(value*))

  /** The class's description list: what is known of it, each line only when there is something to show. */
  def details(
      c: ClassEntry,
      same: Seq[ClassEntry],
      link: ClassEntry => HtmlElement,
      starIndex: String => Option[Int],
      allLabels: Iterable[String]
  ): Seq[HtmlElement] =
    // each vertex star of the set links to its page
    val labels                    = c.pair.split("~").toSeq.map(_.trim).filter(_.nonEmpty)
    val species: Seq[HtmlElement] =
      if c.pair.isEmpty then Nil
      else
        row(
          "species",
          span(
            cls := "mono",
            labels.flatMap(l =>
              Seq(
                starIndex(l).fold[Node](span(l))(n =>
                  a(href := Route.path(Route.Star(n)), title := Species.describe(l, allLabels), l)
                ),
                span(" ~ ")
              )
            ).dropRight(1)
          ),
          if c.species.isEmpty then emptyNode else span(cls := "note", s" indices ${c.species.mkString(":")}")
        )
    Seq(
      row("k", c.k.toString),
      row("source", tag(c), " ", c.source),
      row("world", s"${c.world} (${c.cells.mkString(", ")})"),
      species,
      chambersOf(c).fold(Nil)(n => row("chambers", s"$n (minimal Delaney–Dress symbol)")),
      if c.key.isEmpty then Nil else row("key", span(cls := "mono", c.key)),
      if c.word.isEmpty then Nil else row("stacking word", span(cls := "mono", c.word)),
      netOf(c).fold(Nil)(n => row("RCSR net", Papers.ext(s"http://rcsr.net/nets/$n", n))),
      if same.isEmpty then Nil
      else row("same species set", same.flatMap(o => Seq(link(o), span(", "))).dropRight(1)*)
    ).flatten

  def classView(i: AtlasIndex, c: ClassEntry, orbits: Boolean): HtmlElement =
    if orbits then Prefs.orbits.set(true) // /class/<id>?orbits turns the orbits on
    val classes             = i.classes.toSeq
    val (prev, next)        = neighbours(classes, c)
    val same                = sameSpeciesSet(classes, c)
    val patch               = Signal.fromFuture(Data.patch(c.id))
    def link(o: ClassEntry) = a(href := Route.path(Route.Class(o.id, orbits = false)), o.id)
    div(
      // the arrow keys walk the atlas, unless a control (a slider, the search box) has the focus
      documentEvents(_.onKeyDown).filter(e => !e.target.isInstanceOf[dom.HTMLInputElement]) --> { e =>
        if e.key == "ArrowLeft" then go(Route.Class(prev.id, orbits = false))
        if e.key == "ArrowRight" then go(Route.Class(next.id, orbits = false))
      },
      div(
        cls  := "nav-bar",
        button(onClick --> { _ => go(Route.Class(prev.id, orbits = false)) }, s"← ${prev.id}"),
        button(onClick --> { _ => go(Route.Class(next.id, orbits = false)) }, s"${next.id} →"),
        span(s"${classes.size} classes; arrows switch")
      ),
      h2(cls := "class-title", span(cls := "mono id", c.id), " ", c.name),
      p(cls  := "summary", Describe.summary(c).capitalize + "."),
      div(
        cls  := "classpage",
        div(
          cls := "side",
          div(cls := "card", dl(details(c, same, link, Stars.indexOf(i, _), Stars.species(i).map(_._2)))),
          Lifts.onClassPage(c),
          Option(c.word).filter(_.nonEmpty).flatMap(ClassParts.wordStrip),
          ClassParts.foundCard(c)
        ),
        child <-- patch.map {
          case None     => div(cls := "main-col", div(cls := "card", p(cls := "note", "loading the patch…")))
          case Some(pt) =>
            div(
              cls := "main-col",
              Viewer(i.meta, pt),
              ClassParts.orbitsCard(pt, Stars.indexOf(i, _), Stars.species(i).map(_._2))
            )
        }
      )
    )
