package atlas

import scala.scalajs.js

import com.raquo.laminar.api.L.*
import org.scalajs.dom

import Model.*

/** The patch viewer: the cells of a class on a canvas (the scene of [[Scene]], painted farthest first),
  * dragged to rotate and wheeled to zoom, with the cell legend (full names on hover), the shrink and height
  * sliders, the vertex orbits switch and the orbit legend. The angles and the zoom are kept for the visit, so
  * the next class opens seen from the same side; the settings are the remembered [[Prefs]].
  */
object Viewer:

  private val az   = Var(-0.56)
  private val el   = Var(0.37)
  private val zoom = Var(1.0)

  def apply(meta: Meta, patch: ClassPatch): HtmlElement =
    val canvas = canvasTag(cls := "viewer")
    val view   = Prefs.shrink.signal
      .combineWith(Prefs.cut.signal, Prefs.orbits.signal, az.signal, el.signal)
      .map((s, c, o, a, e) => Scene.View(a, e, s / 100.0, c / 1000.0, o))
    val items  = view.map(Scene.items(patch.cells, _))
    var drag   = Option.empty[(Double, Double)]
    div(
      cls := "card",
      h3("Patch"),
      div(cls := "bar", cellLegend(meta, patch)),
      canvas.amend(
        onPointerDown --> { e =>
          drag = Some((e.clientX, e.clientY)); canvas.ref.setPointerCapture(e.pointerId)
        },
        onPointerMove --> { e =>
          drag.foreach { (x, y) =>
            az.update(_ - (e.clientX - x) * 0.008)
            el.update(v => math.max(-1.5, math.min(1.5, v + (e.clientY - y) * 0.008)))
            drag = Some((e.clientX, e.clientY))
          }
        },
        onPointerUp --> { _ => drag = None },
        onWheel.preventDefault --> { e => zoom.update(_ * (if e.deltaY < 0 then 1.1 else 0.9)) },
        items.combineWith(zoom.signal) --> { (is, z) => paint(canvas.ref, is, z) },
        windowEvents(_.onResize).sample(items, zoom.signal) --> { (is, z) => paint(canvas.ref, is, z) }
      ),
      div(
        cls   := "bar",
        span("drag to rotate · wheel to zoom"),
        label(
          "shrink ",
          input(
            typ     := "range",
            minAttr := "55",
            maxAttr := "100",
            controlled(
              value <-- Prefs.shrink.signal.map(_.toString),
              onInput.mapToValue.map(_.toInt) --> Prefs.shrink
            )
          )
        ),
        label(
          "height ",
          input(
            typ     := "range",
            minAttr := "0",
            maxAttr := "1000",
            controlled(
              value <-- Prefs.cut.signal.map(_.toString),
              onInput.mapToValue.map(_.toInt) --> Prefs.cut
            )
          )
        ),
        label(
          input(
            typ := "checkbox",
            controlled(checked <-- Prefs.orbits.signal, onClick.mapToChecked --> Prefs.orbits)
          ),
          " vertex orbits"
        ),
        span(s"${patch.cells.length} cells")
      ),
      div(
        cls   := "bar",
        children <-- Prefs.orbits.signal.map { on =>
          if !on then Nil
          else if patch.orbits.isEmpty then List(span("no orbit data for this patch"))
          else
            patch.orbits.toList.zipWithIndex.map((label, i) =>
              span(
                span(cls := "sw dot", backgroundColor := Palette.orbitCss(i)),
                s"orbit ${i + 1}: ",
                span(cls := "mono", label)
              )
            )
        }
      )
    )

  /** The cell types of the patch, each with its colour and its full name on hover. */
  def cellLegend(meta: Meta, patch: ClassPatch): Seq[HtmlElement] =
    patch.cells.toSeq.map(_.k).distinct.sorted.map { k =>
      val short = meta.cells(k)
      span(
        title := meta.cellNames.getOrElse(short, short),
        span(cls := "sw", backgroundColor := Palette.cellCss(k)),
        short
      )
    }

  /** Paints the items on the canvas, fitted to it and scaled by the zoom. */
  def paint(cv: dom.HTMLCanvasElement, items: Vector[Scene.Item], zoom: Double): Unit =
    val (w, h) = (cv.clientWidth, cv.clientHeight)
    cv.width = w
    cv.height = h
    val cx     = cv.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
    cx.fillStyle = "#fff"
    cx.fillRect(0, 0, w, h)
    if items.nonEmpty then
      val (x0, x1, y0, y1)        = Scene.bounds(items)
      val s                       = math.min(w / (x1 - x0 + 1), h / (y1 - y0 + 1)) * 0.9 * zoom
      val (ox, oy)                = (w / 2 - s * (x0 + x1) / 2, h / 2 + s * (y0 + y1) / 2)
      def px(p: (Double, Double)) = (ox + s * p._1, oy - s * p._2)
      cx.lineJoin = "round"
      cx.lineWidth = math.max(0.4, s * 0.01)
      for item <- items do
        item match
          case Scene.Face(_, pts, color) =>
            cx.beginPath()
            pts.map(px).zipWithIndex.foreach { case ((x, y), i) =>
              if i == 0 then cx.moveTo(x, y) else cx.lineTo(x, y)
            }
            cx.closePath()
            cx.fillStyle = color
            cx.fill()
            cx.strokeStyle = "rgba(20,20,20,0.4)"
            cx.stroke()
          case Scene.Dot(_, at, color)   =>
            val (x, y) = px(at)
            cx.beginPath()
            cx.arc(x, y, math.max(2.5, s * 0.12), 0, 2 * math.Pi)
            cx.fillStyle = color
            cx.fill()
            cx.strokeStyle = "rgba(0,0,0,0.5)"
            cx.stroke()
