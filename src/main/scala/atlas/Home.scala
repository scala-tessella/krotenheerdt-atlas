package atlas

import com.raquo.laminar.api.L.*

import Model.*

/** The home page: what the atlas is, the sequence at a glance beside the planar one, the ways into the atlas,
  * and a few classes worth a first look.
  */
object Home:

  /** The sequence as paired bars, N_k and the planar count, for k from 1 to 8. */
  private def chart(i: AtlasIndex): SvgElement =
    val rows               = i.sequence.toSeq.filter(_.k <= 8).sortBy(_.k)
    val top                = rows.map(_.n).max.toDouble
    val (w, h, base)       = (640.0, 230.0, 190.0)
    val slot               = (w - 40) / rows.size
    def height(n: Int)     = (base - 24) * n / top
    svg.svg(
      svg.cls     := "chart",
      svg.viewBox := s"0 0 $w $h",
      svg.role    := "img",
      aria.label  := rows.map(r =>
        s"k = ${r.k}: ${r.n} honeycombs, ${r.planar.getOrElse(0)} planar tilings"
      ).mkString("; "),
      svg.line(
        svg.x1  := "30",
        svg.x2  := (w - 10).toString,
        svg.y1  := base.toString,
        svg.y2  := base.toString,
        svg.cls := "axis"
      ),
      rows.zipWithIndex.flatMap { (r, j) =>
        val x      = 40 + j * slot
        val planar = r.planar.getOrElse(0)
        Seq(
          svg.rect(
            svg.x          := x.toString,
            svg.y          := (base - height(r.n)).toString,
            svg.width      := (slot * 0.36).toString,
            svg.height     := height(r.n).toString,
            svg.cls        := "bar-space"
          ),
          svg.rect(
            svg.x          := (x + slot * 0.38).toString,
            svg.y          := (base - height(planar)).toString,
            svg.width      := (slot * 0.36).toString,
            svg.height     := height(planar).toString,
            svg.cls        := "bar-plane"
          ),
          svg.text(
            svg.x          := (x + slot * 0.18).toString,
            svg.y          := (base - height(r.n) - 5).toString,
            svg.textAnchor := "middle",
            svg.cls        := "bar-value",
            r.n.toString
          ),
          // the planar value on its own bar, zero included: from k = 8 the planar sequence shows its 0
          svg.text(
            svg.x          := (x + slot * 0.56).toString,
            svg.y          := (base - height(planar) - 5).toString,
            svg.textAnchor := "middle",
            svg.cls        := "bar-value plane",
            planar.toString
          ),
          svg.text(
            svg.x          := (x + slot * 0.37).toString,
            svg.y          := (base + 18).toString,
            svg.textAnchor := "middle",
            svg.cls        := "bar-k",
            s"k = ${r.k}"
          )
        )
      }
    )

  private def entry(r: Route, title: String, text: String): HtmlElement =
    a(cls := "entry card", href := Route.path(r), h3(title), p(text))

  /** A featured class: why it is worth a look. */
  private def featured(i: AtlasIndex, id: String, why: String): Option[HtmlElement] =
    i.classes.find(_.id == id).map(c =>
      a(
        cls  := "featured card",
        href := Route.path(Route.Class(c.id, orbits = false)),
        div(span(cls := "mono id", c.id), " ", span(cls := s"tag ${c.cat}", c.cat)),
        h3(c.name),
        p(why)
      )
    )

  def view(i: AtlasIndex): HtmlElement =
    val total = i.classes.length
    // the nonzero rows as the data bundle gives them, so the tile never quotes a stale sequence
    val rows  = i.sequence.toSeq.sortBy(_.k)
    val lead  = rows.takeWhile(_.n > 0).map(_.n).mkString(", ")
    div(
      cls := "home",
      div(
        cls  := "card hero",
        h2("The Krötenheerdt honeycombs of space"),
        p(
          cls := "prose",
          "Fill space with cubes, prisms, tetrahedra, octahedra and the other convex uniform polyhedra, face to face, so that the ",
          "vertices fall into exactly k kinds and each kind has its own arrangement of cells around it. For each k ",
          "there are finitely many such honeycombs: two at k = 8, where the planar sequence has already vanished, and none at all from k = 9 on. This atlas shows every one of ",
          s"them — $total honeycombs — to turn in 3D, compare, and read."
        ),
        chart(i),
        p(
          cls := "chart-legend",
          span(cls := "sw space"),
          "N",
          sub("k"),
          ", the honeycombs of space with k kinds of vertex  ",
          span(cls := "sw plane"),
          "the planar tilings with k kinds of vertex, all of them among the honeycombs as ",
          a(href   := Route.path(Route.Lifts(None)), "prismatic lifts")
        )
      ),
      div(
        cls  := "entries",
        entry(
          Route.Sequence,
          "The sequence",
          s"$lead, then zero: row by row, with how each count is known."
        ),
        entry(
          Route.Classes(Catalog.Filter()),
          "Browse the classes",
          s"All $total honeycombs, filtered by k, world and source, or searched."
        ),
        entry(
          Route.Lifts(None),
          "Prismatic lifts",
          "The 135 planar Krötenheerdt tilings, lifted into space, and drawn."
        ),
        entry(
          Route.Stars,
          "Vertex stars",
          "The 23 arrangements of cells around a vertex that the honeycombs are built from."
        ),
        entry(
          Route.Guide(None),
          "Guide",
          "What the atlas counts, how to read it, and why the counts can be trusted: orbits, stars, words, symbols, certificates."
        )
      ),
      h2(cls := "section-title", "A first look"),
      div(
        cls  := "featured-list",
        featured(
          i,
          "k2-057",
          "The first 2-uniform Krötenheerdt honeycomb in print: Andreini's no. 13′ of 1905, filed then as uniform. " +
            "Its tetrahedra are paired face to face between truncated tetrahedra — the mirrored stacking of " +
            "quarter cubic slabs."
        ),
        featured(
          i,
          "k7-006",
          "One of the three largest classes: 336 chambers in its minimal symbol, a 7-uniform stacking of cube layers " +
            "and prism rows in two directions, with hexagon rows of periods 2 and 4."
        ),
        featured(
          i,
          "k7-011",
          "A 7-uniform prismatic lift with dodecagonal prisms: one of the seven planar tilings of the last non-empty row, " +
            "lifted into space."
        )
      )
    )
