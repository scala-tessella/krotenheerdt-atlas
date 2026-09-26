package atlas

import com.raquo.laminar.api.L.*
import org.scalajs.dom

import Describe.Layer
import Model.*

/** The parts of a class page beyond the viewer: the vertex orbits (each with its vertex star drawn and read
  * in words, highlighting its vertices in the viewer), how the class was found and what its record checks,
  * and its stacking word drawn layer by layer.
  */
object ClassParts:

  /** The vertex star of an orbit, drawn small from a fixed side. */
  private def starThumbnail(patch: ClassPatch, orbit: Int): HtmlElement =
    val star        = Scene.starOf(patch.cells, orbit)
    canvasTag(
      cls        := "star",
      aria.label := s"the vertex star of orbit ${orbit + 1}",
      onMountCallback { ctx =>
        val items = Scene.items(star, Scene.View(-0.56, 0.45, 0.94, 1.0, orbits = false))
        dom.window.requestAnimationFrame(_ => Viewer.paint(ctx.thisNode.ref, items, 1.0, light = false))
      }
    )

  /** The vertex orbits: one row each, hovered (or tapped) to show its vertices alone in the viewer. */
  def orbitsCard(patch: ClassPatch): Option[HtmlElement] =
    Option.when(patch.orbits.nonEmpty)(
      div(
        cls := "card orbits-card",
        h3(s"${patch.orbits.length} vertex orbit${if patch.orbits.length == 1 then "" else "s"}"),
        p(
          cls := "note",
          "Each orbit is one kind of vertex, with its star of cells. Point at an orbit (or tap it) to see its vertices."
        ),
        div(
          cls := "orbit-rows",
          patch.orbits.toSeq.zipWithIndex.map((label, i) =>
            div(
              cls      := "orbit-row",
              tabIndex := 0,
              cls("on") <-- Viewer.highlight.signal.map(_.contains(i)),
              onMouseEnter --> { _ => Viewer.highlight.set(Some(i)) },
              onMouseLeave --> { _ => Viewer.highlight.set(None) },
              onFocus --> { _ => Viewer.highlight.set(Some(i)) },
              onBlur --> { _ => Viewer.highlight.set(None) },
              onClick --> { _ => Viewer.highlight.update(h => if h.contains(i) then None else Some(i)) },
              starThumbnail(patch, i),
              div(
                cls := "orbit-text",
                div(span(cls := "sw dot", backgroundColor := Palette.orbitCss(i)), s"orbit ${i + 1}"),
                div(cls := "orbit-words", Species.describe(label)),
                div(cls := "mono orbit-label", label)
              )
            )
          )
        )
      )
    )

  /** How the class was found and what its record checks. */
  def foundCard(c: ClassEntry): HtmlElement =
    div(
      cls := "card found-card",
      h3("How it was found"),
      p(Describe.found(c)),
      Describe.checks(c).map(t => p(t)),
      c.dossier.toOption.map(d =>
        htmlTag("details")(
          htmlTag("summary")("the record"),
          p(
            cls := "mono",
            s"valid ${d.valid}, minimal ${d.minimal}, species distinct ${d.distinct}; folding tuple: ${d.tuple}"
          )
        )
      )
    )

  /** The atlas colours of a cube layer and of a prism row. */
  private val cubeColour = Palette.cellCss(1)
  private val rowColour  = Palette.cellCss(9)
  private val hexColour  = Palette.cellCss(10)

  /** A stacking word drawn from the bottom up: a band per layer (grey a cube layer, orange a prism row,
    * marked u or w by its direction), with the hexagonal prisms a row carries marked at their offsets in its
    * period.
    */
  def wordStrip(word: String): Option[HtmlElement] =
    Describe.layers(word).map { ls =>
      val (bandH, labelW, width) = (14, 64, 260)
      val height                 = ls.size * bandH
      div(
        cls := "card word-card",
        h3("Stacking word"),
        p(cls         := "note", Describe.composition(ls), ", from the bottom up:"),
        svg.svg(
          svg.cls     := "word-strip",
          svg.viewBox := s"0 0 $width $height",
          svg.role    := "img",
          aria.label  := s"the stacking word $word",
          ls.reverse.zipWithIndex.flatMap { (l, j) =>
            val y     = j * bandH
            val token = word.trim.split("\\s+")(ls.size - 1 - j)
            val band  = l match
              case Layer.Cube                  => Seq(rect(labelW, y, width - labelW, bandH - 2, cubeColour))
              case Layer.Row(axis, hexes, per) =>
                val base  = rect(labelW, y, width - labelW, bandH - 2, rowColour)
                val marks = per.toSeq.flatMap(p =>
                  val cell = (width - labelW).toDouble / p
                  hexes.map(o =>
                    rect(labelW + o * cell + cell * 0.25, y + 2, cell * 0.5, bandH - 6, hexColour)
                  )
                )
                val dir   = svg.text(
                  svg.x          := (width - 8).toString,
                  svg.y          := (y + bandH - 5).toString,
                  svg.textAnchor := "end",
                  svg.cls        := "word-dir",
                  axis.toString
                )
                (base +: marks) :+ dir
            val label =
              svg.text(svg.x := "0", svg.y := (y + bandH - 5).toString, svg.cls := "word-token", token)
            label +: band
          }
        ),
        p(
          cls         := "note",
          span(cls := "mono", "C"),
          " a cube layer · ",
          span(cls := "mono", "Tu"),
          ", ",
          span(cls := "mono", "Tw"),
          " a row of triangular prisms along u or w · ",
          span(cls := "mono", "{i/p}"),
          " hexagonal prisms at offset i in a period of p"
        )
      )
    }

  private def rect(x: Double, y: Double, w: Double, h: Double, fill: String): SvgElement =
    svg.rect(
      svg.x      := x.toString,
      svg.y      := y.toString,
      svg.width  := w.toString,
      svg.height := h.toString,
      svg.rx     := "2",
      svg.fill   := fill
    )
