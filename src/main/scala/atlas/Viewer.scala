package atlas

import com.raquo.laminar.api.L.*
import org.scalajs.dom

import Model.*

/** The patch viewer: the cells of a class on a canvas (the scene of [[Scene]], painted farthest first),
  * turned by dragging, zoomed by pinching or the wheel, reset by a double tap or click ([[Gesture]]), with
  * the cell legend (full names on hover), the shrink and height sliders, the vertex orbits switch and the
  * orbit highlighted from the orbits card ([[highlight]]). While a gesture is in progress the drawing is
  * lighter (no outlines, no vertex spheres) and it is repainted at most once per animation frame, sharp on
  * dense screens. The angles and the zoom are kept for the visit, so the next class opens seen from the same
  * side; the settings are the remembered [[Prefs]].
  */
object Viewer:

  private val home = (-0.56, 0.37, 1.0)
  private val az   = Var(home._1)
  private val el   = Var(home._2)
  private val zoom = Var(home._3)

  /** The vertex orbit whose spheres the viewer shows alone (the orbit row under the pointer or tapped). */
  val highlight: Var[Option[Int]] = Var(None)

  private def reset(): Unit =
    az.set(home._1); el.set(home._2); zoom.set(home._3)

  def apply(meta: Meta, patch: ClassPatch): HtmlElement =
    highlight.set(None)
    val canvas   = canvasTag(cls := "viewer", aria.label := s"the ${patch.cells.length} cells of ${patch.id}")
    val dragging = Var(false)
    var gesture  = Gesture.State()
    val view     = Prefs.shrink.signal
      .combineWith(
        Prefs.cut.signal,
        Prefs.orbits.signal,
        az.signal,
        el.signal,
        dragging.signal,
        highlight.signal
      )
      .map((s, c, o, a, e, d, h) => (Scene.View(a, e, s / 100.0, c / 1000.0, o && !d, h.filter(_ => !d)), d))
    val frame    = view.map((v, d) => (Scene.items(patch.cells, v), d)).combineWith(zoom.signal)

    // the latest frame, painted on the next animation frame (several changes in one frame paint once)
    var latest           = Option.empty[(Vector[Scene.Item], Boolean, Double)]
    var pending          = false
    def schedule(): Unit =
      if !pending then
        pending = true
        dom.window.requestAnimationFrame { _ =>
          pending = false
          latest.foreach((is, light, z) => paint(canvas.ref, is, z, light))
        }

    def act(a: Gesture.Action): Unit = a match
      case Gesture.Action.Rotate(dx, dy) =>
        az.update(_ - dx * 0.008)
        el.update(v => math.max(-1.5, math.min(1.5, v + dy * 0.008)))
      case Gesture.Action.Zoom(f)        => zoom.update(z => math.max(0.2, math.min(12, z * f)))
      case Gesture.Action.Reset          => reset()
      case Gesture.Action.Nothing        => ()

    div(
      cls := "card viewer-card",
      div(cls := "legend", cellLegend(meta, patch)),
      canvas.amend(
        onPointerDown --> { e =>
          canvas.ref.setPointerCapture(e.pointerId)
          val (s, a) = Gesture.down(gesture, e.pointerId.toInt, e.clientX, e.clientY, e.timeStamp)
          gesture = s; dragging.set(Gesture.active(s)); act(a)
        },
        onPointerMove --> { e =>
          val (s, a) = Gesture.move(gesture, e.pointerId.toInt, e.clientX, e.clientY)
          gesture = s; act(a)
        },
        List(onPointerUp, onPointerCancel).map(_ --> { (e: dom.PointerEvent) =>
          gesture = Gesture.up(gesture, e.pointerId.toInt); dragging.set(Gesture.active(gesture))
        }),
        onWheel.preventDefault --> { e => act(Gesture.Action.Zoom(if e.deltaY < 0 then 1.1 else 1 / 1.1)) },
        frame --> { (is, light, z) => latest = Some((is, light, z)); schedule() },
        windowEvents(_.onResize) --> { _ => schedule() }
      ),
      div(
        cls   := "controls",
        label(
          "shrink",
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
          "height",
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
          cls      := "switch",
          input(
            typ := "checkbox",
            controlled(checked <-- Prefs.orbits.signal, onClick.mapToChecked --> Prefs.orbits)
          ),
          "vertex orbits"
        ),
        button(cls := "quiet", onClick --> { _ => reset() }, "reset view")
      ),
      p(
        cls   := "hint",
        s"${patch.cells.length} cells · drag to turn · pinch or wheel to zoom · double-tap to reset"
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

  /** Paints the items on the canvas, fitted to it and scaled by the zoom, at the screen's pixel density;
    * `light` leaves out the outlines (while a gesture is in progress).
    */
  def paint(cv: dom.HTMLCanvasElement, items: Vector[Scene.Item], zoom: Double, light: Boolean): Unit =
    val (w, h) = (cv.clientWidth.toDouble, cv.clientHeight.toDouble)
    val dpr    = math.max(1.0, dom.window.devicePixelRatio)
    cv.width = math.round(w * dpr).toInt
    cv.height = math.round(h * dpr).toInt
    val cx     = cv.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
    cx.setTransform(dpr, 0, 0, dpr, 0, 0)
    val bg     = dom.window.getComputedStyle(cv).getPropertyValue("--canvas").trim
    cx.fillStyle = if bg.nonEmpty then bg else "#fff"
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
            if !light then
              cx.strokeStyle = "rgba(20,20,20,0.4)"
              cx.stroke()
          case Scene.Dot(_, at, color)   =>
            val (x, y) = px(at)
            cx.beginPath()
            cx.arc(x, y, math.max(1.5, s * 0.12), 0, 2 * math.Pi)
            cx.fillStyle = color
            cx.fill()
            cx.strokeStyle = "rgba(0,0,0,0.5)"
            cx.stroke()
