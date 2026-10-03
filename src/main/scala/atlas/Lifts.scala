package atlas

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js

import com.raquo.laminar.api.L.*

import Model.*

/** The planar Krötenheerdt tilings inside the atlas: every k-uniform tiling of the plane with k distinct
  * vertex types, stacked into prisms, is a k-uniform Krötenheerdt honeycomb, so the planar sequence 11, 20,
  * 39, 33, 15, 10, 7 sits inside the spatial one. The gallery shows them by k, each drawn and linked to its
  * lift; the same drawing heads the page of every lift.
  */
object Lifts:

  /** The atlas cell ordinal of the prism over a polygon with this many sides, whose colour the polygon takes.
    */
  private val prismOf: Map[Int, Int] = Map(3 -> 9, 4 -> 1, 6 -> 10, 8 -> 11, 12 -> 12)

  /** A planar tiling as SVG: its polygons coloured as the prisms over them; `window` crops it to a square of
    * that side around its centre (the gallery's thumbnails), or shows it whole.
    */
  def drawing(t: TilingDrawing, window: Option[Double], label: String): SvgElement =
    val (x, y, w, h)                 = (t.box(0), t.box(1), t.box(2), t.box(3))
    val (cx, cy)                     = (x + w / 2, y + h / 2)
    // a crop stays inside the patch's ragged edge (polygons up to a dodecagon's radius past the square)
    val (vw, vh)                     = window.fold((w, h)) { s =>
      val side = math.max(3.0, math.min(s, math.min(w, h) - 3.8)); (side, side)
    }
    val pad                          = 0.1
    svg.svg(
      svg.cls                 := "tiling",
      svg.viewBox             := s"${cx - vw / 2 - pad} ${cy - vh / 2 - pad} ${vw + 2 * pad} ${vh + 2 * pad}",
      svg.preserveAspectRatio := "xMidYMid slice",
      aria.label              := label,
      svg.role                := "img",
      t.paths.toSeq.sortBy(_._1.toInt).map((sides, d) =>
        svg.path(
          svg.d              := d,
          svg.fill           := Palette.cellCss(prismOf.getOrElse(sides.toInt, 1)),
          svg.stroke         := "rgba(20,20,20,0.55)",
          svg.strokeWidth    := "0.035",
          svg.strokeLineJoin := "round"
        )
      )
    )

  /** The subtitle of a row of the gallery. */
  private def rowTitle(k: Int, n: Int): String =
    if k == 1 then s"the $n Archimedean tilings" else s"$n tilings with $k vertex types"

  def view(i: AtlasIndex, only: Option[Int]): HtmlElement =
    val lifts    = i.classes.toSeq.filter(isLift)
    val byK      = lifts.groupBy(_.k).toSeq.sortBy(_._1)
    val tilings  = Signal.fromFuture(Data.tilings)
    val sequence = i.sequence.toSeq.filter(r => r.planar.getOrElse(0) > 0).sortBy(_.k)
    div(
      cls := "lifts-page",
      div(
        cls := "card intro",
        h2("The planar sequence inside the spatial one"),
        p(
          cls := "prose",
          "Stack the polygons of a planar tiling into prisms, floor upon floor, and the tiling becomes a honeycomb ",
          "of E³: its prismatic lift. The vertex stars of the lift correspond one to one to the vertex types of the ",
          "tiling, so a k-uniform tiling of the plane with k distinct vertex types lifts to a k-uniform Krötenheerdt ",
          "honeycomb. Every planar Krötenheerdt tiling is therefore in the atlas, and the planar sequence ",
          sequence.map(_.planar.getOrElse(0)).mkString(", "),
          " sits inside the spatial one ",
          sequence.map(_.n).mkString(", "),
          ". Each tiling below links to its lift."
        ),
        div(
          cls := "chips",
          a(
            cls       := "chip",
            cls("on") := only.isEmpty,
            href      := Route.path(Route.Lifts(None)),
            s"all ${lifts.size}"
          ),
          byK.map((k, cs) =>
            a(
              cls       := "chip",
              cls("on") := only.contains(k),
              href      := Route.path(Route.Lifts(Some(k))),
              s"k = $k · ${cs.size}"
            )
          )
        )
      ),
      byK.filter((k, _) => only.forall(_ == k)).map((k, cs) =>
        sectionTag(
          cls := "lift-row",
          h3(s"k = $k", span(cls := "note", s" · ${rowTitle(k, cs.size)}")),
          div(
            cls := "tiles",
            cs.map(c =>
              a(
                cls  := "tile",
                href := Route.path(Route.Class(c.id, orbits = false)),
                child <-- tilings.map(ts =>
                  ts.flatMap(_.get(c.id)) match
                    case Some(t) => drawing(t, Some(8), tilingOf(c).getOrElse(c.name))
                    case None    =>
                      div(
                        cls := "tiling missing",
                        if ts.isEmpty then "…" else "planar tiling not identified by key"
                      )
                ),
                div(
                  cls := "tile-caption",
                  span(cls := "mono id", c.id),
                  span(cls := "mono types", tilingOf(c).getOrElse("—"))
                )
              )
            )
          )
        )
      )
    )

  /** The planar tiling of a lift, whole, for its class page (nothing for a class that is not a named lift).
    */
  def onClassPage(c: ClassEntry): Option[HtmlElement] =
    tilingOf(c).map { types =>
      val tilings = Signal.fromFuture(Data.tilings)
      div(
        cls := "card planar-card",
        h3("Planar tiling"),
        child <-- tilings.map(ts =>
          ts.flatMap(_.get(c.id)).fold[Node](p(cls := "note", "…"))(t => drawing(t, None, types))
        ),
        p(
          cls := "note",
          span(cls := "mono", types),
          s" — this honeycomb is its prismatic lift. ",
          a(href   := Route.path(Route.Lifts(Some(c.k))), s"All planar tilings with k = ${c.k}")
        )
      )
    }
