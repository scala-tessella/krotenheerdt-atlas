package atlas

import com.raquo.laminar.api.L.*
import org.scalajs.dom

/** The viewer's settings, remembered from class to class and, where storage is available, across reloads: the
  * vertex orbits switch, the shrink of the cells (55–100 %) and the height cut (0–1000 of the patch's
  * height). The storage keys are those of the earlier atlas page, so a setting saved there still applies.
  */
object Prefs:

  private def load(name: String): Option[String] =
    try Option(dom.window.localStorage.getItem("atlas." + name))
    catch case _: Throwable => None

  private def save(name: String, value: String): Unit =
    try dom.window.localStorage.setItem("atlas." + name, value)
    catch case _: Throwable => () // storage unavailable: this visit only

  private def remembered[A](name: String, default: A, read: String => Option[A], write: A => String): Var[A] =
    val v = Var(load(name).flatMap(read).getOrElse(default))
    v.signal.changes.foreach(a => save(name, write(a)))(using unsafeWindowOwner)
    v

  private def intIn(lo: Int, hi: Int)(s: String): Option[Int] = s.toIntOption.filter(i => i >= lo && i <= hi)

  val orbits: Var[Boolean] = remembered("orbits", false, s => Some(s == "1"), b => if b then "1" else "0")
  val shrink: Var[Int]     = remembered("shrink", 82, intIn(55, 100), _.toString)
  val cut: Var[Int]        = remembered("zcut", 1000, intIn(0, 1000), _.toString)
