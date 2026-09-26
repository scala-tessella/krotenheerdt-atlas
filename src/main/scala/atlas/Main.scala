package atlas

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js

import com.raquo.laminar.api.L.*
import org.scalajs.dom

import Model.*

/** The atlas application: the sequence, the table of classes and a page per class, driven by the URL
  * fragment. The shell of the port: the class page lists what the patch holds; the viewer comes next.
  */
object Main:

  val route: Var[Route] = Var(Route.parse(dom.window.location.hash))

  def go(r: Route): Unit = dom.window.location.hash = Route.fragment(r)

  def main(args: Array[String]): Unit =
    renderOnDomContentLoaded(dom.document.getElementById("app"), app)

  def app: HtmlElement =
    val index = Signal.fromFuture(Data.index)
    div(
      windowEvents(_.onHashChange) --> { _ => route.set(Route.parse(dom.window.location.hash)) },
      headerTag(
        h1("Krötenheerdt honeycombs of E³"),
        navTag(
          a(href := Route.fragment(Route.Sequence), "Sequence"),
          a(href := Route.fragment(Route.Classes), "Classes")
        ),
        span(cls := "count", child.text <-- index.map(_.fold("")(i => s"${i.classes.length} classes")))
      ),
      mainTag(
        child <-- index.combineWith(route.signal).map {
          case (None, _)                          => p(cls := "note", "loading the atlas…")
          case (Some(i), Route.Sequence)          => sequenceView(i)
          case (Some(i), Route.Classes)           => classesView(i)
          case (Some(i), Route.Class(id, orbits)) =>
            i.classes.find(_.id == id).fold(p(cls := "note", s"no class $id"))(classView(i, _, orbits))
        }
      )
    )

  def sequenceView(i: AtlasIndex): HtmlElement =
    table(
      thead(tr(th("k"), th("N", sub("k")), th("status"), th("composition"))),
      tbody(
        i.sequence.toSeq.sortBy(_.k).map(r => tr(td(r.k), td(r.n), td(r.status), td(r.note)))
      )
    )

  def classesView(i: AtlasIndex): HtmlElement =
    table(
      thead(tr(th("id"), th("name"), th("source"), th("chambers"), th("species"))),
      tbody(
        i.classes.toSeq.map(c =>
          tr(
            td(a(href := Route.fragment(Route.Class(c.id, orbits = false)), c.id)),
            td(c.name),
            td(c.cat),
            td(chambersOf(c).fold("")(_.toString)),
            td(cls := "mono", c.pair)
          )
        )
      )
    )

  def classView(i: AtlasIndex, c: ClassEntry, orbits: Boolean): HtmlElement =
    val patch = Signal.fromFuture(Data.patch(c.id))
    div(
      h2(s"${c.id} · ${c.name}"),
      dl(
        dt("k"),
        dd(c.k),
        dt("source"),
        dd(c.source),
        dt("chambers"),
        dd(chambersOf(c).fold("—")(_.toString)),
        dt("species"),
        dd(cls := "mono", if c.pair.isEmpty then "—" else c.pair)
      ),
      child <-- patch.map {
        case None    => p(cls := "note", "loading the patch…")
        case Some(p) => patchSummary(i, p, orbits)
      }
    )

  /** What the patch holds: its cells by type (full names on hover) and its vertex orbits. */
  def patchSummary(i: AtlasIndex, pt: ClassPatch, orbits: Boolean): HtmlElement =
    val byType = pt.cells.toSeq.groupMapReduce(_.k)(_ => 1)(_ + _).toSeq.sortBy(_._1)
    div(
      h3(s"patch: ${pt.cells.length} cells"),
      ul(
        byType.map((k, n) =>
          val short = i.meta.cells(k)
          li(span(title := i.meta.cellNames.getOrElse(short, short), short), s": $n")
        )
      ),
      h3(s"${pt.orbits.length} vertex orbit${if pt.orbits.length == 1 then "" else "s"}"),
      ol(pt.orbits.toSeq.map(o => li(cls := "mono", o))),
      p(cls := "note", if orbits then "vertex orbits on" else "vertex orbits off")
    )
