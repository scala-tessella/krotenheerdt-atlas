package atlas

import scala.concurrent.ExecutionContext.Implicits.global

import com.raquo.laminar.api.L.*
import org.scalajs.dom

import Catalog.*
import Model.*

/** The atlas application: the sequence, the table of classes and a page per class, driven by the URL fragment
  * ([[Route]]).
  */
object Main:

  val route: Var[Route] = Var(Route.parse(dom.window.location.hash))

  /** The table's filter and sort, kept for the visit, so the table reopens as it was left. */
  val filter: Var[Filter] = Var(Filter())

  def go(r: Route): Unit = dom.window.location.hash = Route.fragment(r)

  def main(args: Array[String]): Unit =
    renderOnDomContentLoaded(dom.document.getElementById("app"), app)

  def app: HtmlElement =
    val index = Signal.fromFuture(Data.index)
    div(
      windowEvents(_.onHashChange) --> { _ => route.set(Route.parse(dom.window.location.hash)) },
      headerTag(
        h1(a(href := Route.fragment(Route.Home), "Krötenheerdt honeycombs of E³")),
        navTag(
          a(
            href := Route.fragment(Route.Sequence),
            cls("on") <-- route.signal.map(_ == Route.Sequence),
            "Sequence"
          ),
          a(
            href := Route.fragment(Route.Classes(None)),
            cls("on") <-- route.signal.map { case Route.Classes(_) => true; case _ => false },
            "Classes"
          ),
          a(
            href := Route.fragment(Route.Lifts(None)),
            cls("on") <-- route.signal.map { case Route.Lifts(_) => true; case _ => false },
            span(cls := "long", "Planar lifts"),
            span(cls := "short", "Lifts")
          ),
          a(
            href := Route.fragment(Route.Stars),
            cls("on") <-- route.signal.map { case Route.Stars | Route.Star(_) => true; case _ => false },
            span(cls := "long", "Vertex stars"),
            span(cls := "short", "Stars")
          ),
          a(
            href := Route.fragment(Route.Guide(None)),
            cls("on") <-- route.signal.map { case Route.Guide(_) => true; case _ => false },
            "Guide"
          ),
          child.maybe <-- route.signal.map {
            case r: Route.Class => Some(a(href := Route.fragment(r), cls := "on", "Class"))
            case _              => None
          }
        ),
        span(cls := "spacer"),
        child.maybe <-- index.map(_.map(Search.box)),
        span(
          cls    := "note total",
          child.text <-- index.map(_.fold("")(i =>
            s"${i.classes.length} classes, k = 1 to ${i.classes.map(_.k).max}"
          ))
        )
      ),
      mainTag(
        child <-- index.combineWith(route.signal).map {
          case (None, _)                          => p(cls := "note", "loading the atlas…")
          case (Some(i), Route.Home)              => Home.view(i)
          case (Some(i), Route.Sequence)          => sequenceView(i)
          case (Some(_), Route.Guide(section))    => Guide.view(section)
          case (Some(i), Route.About)             => About.view(i.classes.length)
          case (Some(i), Route.Lifts(k))          => Lifts.view(i, k)
          case (Some(i), Route.Stars)             => Stars.view(i)
          case (Some(i), Route.Star(n))           => Stars.page(i, n)
          case (Some(i), Route.Classes(k))        =>
            k.foreach(k => filter.update(_.copy(k = Some(k))))
            classesView(i)
          case (Some(i), Route.Class(id, orbits)) =>
            i.classes.find(_.id == id).fold(p(cls := "note", s"no class $id"))(classView(i, _, orbits))
        }
      ),
      footerTag(
        a(href   := Route.fragment(Route.Guide(None)), "Guide"),
        a(href   := Route.fragment(Route.About), "About"),
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
        "28, 57, 119, 146, 122, 78, 16 and vanishes from k = 8 on, at the same point as the planar one."
      ),
      p(
        cls := "prose",
        "Every planar Krötenheerdt tiling is here too: stacked into prisms it becomes a honeycomb of the atlas, its ",
        "prismatic lift. The planar sequence sits inside the spatial one, row by row — ",
        a(href := Route.fragment(Route.Lifts(None)), "browse the planar lifts"),
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
                      href       := Route.fragment(Route.Classes(Some(r.k))),
                      aria.label := s"the $listed classes of k = ${r.k}",
                      r.n
                    )
                ),
                td(
                  r.planar.toOption.filter(_ > 0).fold[Modifier[HtmlElement]](r.planar.getOrElse(0).toString)(
                    n =>
                      a(
                        href       := Route.fragment(Route.Lifts(Some(r.k))),
                        aria.label := s"the $n planar lifts of k = ${r.k}",
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
        "planar lifts only"
      ),
      button(cls := "quiet", onClick --> { _ => filter.set(Filter()) }, "clear")
    )
    div(
      filter.signal.changes --> { _ => shown.set(Layout.pageSize) },
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
                  th(
                    cls := "sortable",
                    onClick --> { _ => filter.update(_.sortedBy(col)) },
                    child.text <-- filter.signal.map(f =>
                      col.title + (if f.sort == col then if f.ascending then " ▲" else " ▼" else "")
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

  private def classLink(c: ClassEntry): Route = Route.Class(c.id, orbits = false)

  private def tableRow(c: ClassEntry): HtmlElement =
    tr(
      cls := "row",
      onClick --> { _ => go(classLink(c)) },
      td(a(cls := "mono", href := Route.fragment(classLink(c)), c.id)),
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
      href := Route.fragment(classLink(c)),
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
      a(cls := "term", href := Route.fragment(Route.Guide(Some(g))), title)
    )
    Seq(dt(term), dd(value*))

  /** The class's description list: what is known of it, each line only when there is something to show. */
  def details(
      c: ClassEntry,
      same: Seq[ClassEntry],
      link: ClassEntry => HtmlElement,
      starIndex: String => Option[Int]
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
                  a(href := Route.fragment(Route.Star(n)), title := Species.describe(l), l)
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
      c.net.toOption.filter(_.nonEmpty).fold(Nil)(n =>
        row("net", n, span(cls := "note", " (preliminary identification)"))
      ),
      if same.isEmpty then Nil
      else row("same species set", same.flatMap(o => Seq(link(o), span(", "))).dropRight(1)*)
    ).flatten

  def classView(i: AtlasIndex, c: ClassEntry, orbits: Boolean): HtmlElement =
    if orbits then Prefs.orbits.set(true) // #class/<id>?orbits turns the orbits on
    val classes             = i.classes.toSeq
    val (prev, next)        = neighbours(classes, c)
    val same                = sameSpeciesSet(classes, c)
    val patch               = Signal.fromFuture(Data.patch(c.id))
    def link(o: ClassEntry) = a(href := Route.fragment(Route.Class(o.id, orbits = false)), o.id)
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
          div(cls := "card", dl(details(c, same, link, Stars.indexOf(i, _)))),
          Lifts.onClassPage(c),
          Option(c.word).filter(_.nonEmpty).flatMap(ClassParts.wordStrip),
          ClassParts.foundCard(c)
        ),
        child <-- patch.map {
          case None     => div(cls := "main-col", div(cls := "card", p(cls := "note", "loading the patch…")))
          case Some(pt) =>
            div(cls := "main-col", Viewer(i.meta, pt), ClassParts.orbitsCard(pt, Stars.indexOf(i, _)))
        }
      )
    )
