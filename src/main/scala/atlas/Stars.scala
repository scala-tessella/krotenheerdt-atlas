package atlas

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js

import com.raquo.laminar.api.L.*
import org.scalajs.dom

import Model.*

/** The vertex stars: the 23 species the classes of k ≥ 2 are built from, each drawn, read in words and linked
  * to the classes that use it; a page per star with the star in the viewer and its classes by k.
  */
object Stars:

  /** The species of the index, by index, in order. */
  def species(i: AtlasIndex): Seq[(Int, String)] =
    i.meta.species.toSeq.map((n, l) => (n.toInt, l)).sortBy(_._1)

  /** The species index of a label, if the index lists it. */
  def indexOf(i: AtlasIndex, label: String): Option[Int] =
    species(i).collectFirst { case (n, l) if l == label => n }

  private def users(i: AtlasIndex, n: Int): Seq[ClassEntry] = i.classes.toSeq.filter(_.species.contains(n))

  /** A star drawn small from a fixed side. */
  private def thumbnail(star: StarDrawing): HtmlElement =
    canvasTag(
      cls        := "star",
      aria.label := s"the vertex star ${star.label}",
      onMountCallback { ctx =>
        val items = Scene.items(star.cells, Scene.View(-0.56, 0.45, 0.94, 1.0, orbits = false))
        dom.window.requestAnimationFrame(_ => Viewer.paint(ctx.thisNode.ref, items, 1.0, light = false))
      }
    )

  private def usage(cs: Seq[ClassEntry]): String =
    cs.groupBy(_.k).toSeq.sortBy(_._1).map((k, g) => s"${g.size} at k = $k").mkString(", ")

  def view(i: AtlasIndex): HtmlElement =
    val stars = Signal.fromFuture(Data.stars)
    div(
      div(
        cls := "card intro",
        h2("Vertex stars"),
        p(
          cls := "prose",
          "A vertex star is the arrangement of cells around a vertex. The honeycombs of the atlas from k = 2 on are ",
          "built from the 23 vertex stars below: a k-uniform Krötenheerdt honeycomb has k kinds of vertex, with k ",
          "pairwise distinct stars. A label counts the cells by type, and its number tells apart the stars with the ",
          "same cells: {cube:4 p3:6}#1 and #2 both have four cubes and six triangular prisms, arranged differently."
        )
      ),
      div(
        cls := "star-grid",
        species(i).map((n, label) =>
          val cs  = users(i, n)
          a(
            cls  := "star-card",
            href := Route.fragment(Route.Star(n)),
            child <-- stars.map(_.flatMap(_.get(n.toString)).fold[Node](div(cls := "star"))(thumbnail)),
            div(
              cls := "star-text",
              div(cls := "mono star-label", label),
              div(Species.describe(label)),
              div(cls := "note", s"${cs.size} classes: ${usage(cs)}")
            )
          )
        )
      )
    )

  def page(i: AtlasIndex, n: Int): HtmlElement =
    species(i).find(_._1 == n).fold(p(cls := "note", s"no vertex star $n")) { (_, label) =>
      val cs    = users(i, n)
      val stars = Signal.fromFuture(Data.stars)
      div(
        div(cls := "nav-bar", a(href := Route.fragment(Route.Stars), "← all vertex stars")),
        h2(cls  := "class-title", span(cls := "mono id", label)),
        p(
          cls   := "summary",
          s"${Species.describe(label).capitalize} around a vertex; used by ${cs.size} classes."
        ),
        div(
          cls   := "classpage",
          div(
            cls := "side",
            div(
              cls := "card",
              h3("Classes with this star"),
              cs.groupBy(_.k).toSeq.sortBy(_._1).map((k, g) =>
                div(
                  cls := "star-users",
                  h3(s"k = $k", span(cls := "note", s" · ${g.size}")),
                  div(
                    cls := "links",
                    g.map(c =>
                      a(href := Route.fragment(Route.Class(c.id, orbits = false)), title := c.name, c.id)
                    )
                  )
                )
              )
            )
          ),
          child <-- stars.map(_.flatMap(_.get(n.toString)) match
            case None       => div(cls := "main-col", div(cls := "card", p(cls := "note", "loading the star…")))
            case Some(star) =>
              val asPatch = js.Dynamic
                .literal(id = label, k = 0, orbits = js.Array(label), cells = star.cells)
                .asInstanceOf[ClassPatch]
              div(
                cls := "main-col",
                Viewer(i.meta, asPatch),
                p(
                  cls := "note",
                  s"Cut from ",
                  a(href := Route.fragment(Route.Class(star.source, orbits = false)), star.source),
                  "."
                )
              ))
        )
      )
    }
