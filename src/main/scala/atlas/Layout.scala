package atlas

import com.raquo.laminar.api.L.*
import org.scalajs.dom

/** The layout's breakpoint as a signal: narrow screens (phones, narrow windows) get the one-column pages, the
  * class cards instead of the table and the filters behind a button. The same width as the style sheet's
  * breakpoint.
  */
object Layout:

  val breakpoint: String = "(max-width: 760px)"

  private val query = dom.window.matchMedia(breakpoint)

  val narrow: Var[Boolean] = Var(query.matches)

  query.addEventListener("change", (_: dom.Event) => narrow.set(query.matches))

  /** How many rows a list shows at first, and how many more each time its end comes into view. */
  val pageSize: Int = 60
