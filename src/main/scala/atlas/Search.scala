package atlas

import scala.scalajs.js

import com.raquo.laminar.api.L.*
import org.scalajs.dom

import Catalog.Hit
import Model.*

/** The search box of the header, on every page: suggestions as one types ([[Catalog.search]]), walked with
  * the arrow keys, opened with Enter or a click, closed with Escape.
  */
object Search:

  def box(i: AtlasIndex): HtmlElement =
    val classes                   = i.classes.toSeq
    val species                   = Stars.species(i)
    val text                      = Var("")
    val selected                  = Var(0)
    val open                      = Var(false)
    def hits(t: String): Seq[Hit] = Catalog.search(classes, species, t)

    def goTo(h: Hit): Unit =
      h match
        case Hit.ClassHit(c)   => Main.go(Route.Class(c.id, orbits = false))
        case Hit.StarHit(n, _) => Main.go(Route.Star(n))
      text.set("")
      open.set(false)

    def row(h: Hit): Seq[Modifier[HtmlElement]] = h match
      case Hit.ClassHit(c)       => Seq(span(cls := "mono hit-id", c.id), span(cls := "hit-name", c.name))
      case Hit.StarHit(_, label) =>
        Seq(span(cls := "mono hit-id", label), span(cls := "hit-name", "vertex star"))

    div(
      cls := "search-box",
      input(
        typ               := "search",
        placeholder       := "search the atlas",
        role              := "combobox",
        aria.autoComplete := "list",
        aria.controls     := "search-suggestions",
        aria.expanded <-- text.signal.combineWith(open.signal).map((t, o) => o && hits(t).nonEmpty),
        aria.activeDescendant <-- selected.signal.map(s => s"search-hit-$s"),
        aria.label        := "search the atlas: a class id, a name, a species label, a word, a key or a tiling",
        controlled(
          value <-- text.signal,
          onInput.mapToValue --> { t => text.set(t); selected.set(0); open.set(true) }
        ),
        onKeyDown --> { e =>
          val hs = hits(text.now())
          e.key match
            case "ArrowDown" => e.preventDefault(); selected.update(s => math.min(s + 1, hs.size - 1))
            case "ArrowUp"   => e.preventDefault(); selected.update(s => math.max(s - 1, 0))
            case "Enter"     => hs.lift(selected.now()).orElse(hs.headOption).foreach(goTo)
            case "Escape"    => open.set(false)
            case _           => ()
        },
        onFocus --> { _ => open.set(true) },
        // closed a moment after the focus leaves, so a click on a suggestion lands first
        onBlur --> { _ => js.timers.setTimeout(150)(open.set(false)) }
      ),
      child.maybe <-- text.signal.combineWith(open.signal, selected.signal).map { (t, o, s) =>
        val hs = hits(t)
        Option.when(o && hs.nonEmpty)(
          ul(
            cls    := "suggestions",
            role   := "listbox",
            idAttr := "search-suggestions",
            hs.zipWithIndex.map((h, j) =>
              li(
                cls("on")     := j == s,
                role          := "option",
                idAttr        := s"search-hit-$j",
                aria.selected := j == s,
                onMouseDown.preventDefault --> { _ => goTo(h) },
                row(h)
              )
            )
          )
        )
      }
    )
