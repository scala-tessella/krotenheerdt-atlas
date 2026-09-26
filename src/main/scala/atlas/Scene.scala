package atlas

import scala.scalajs.js

import Model.Cell

/** The projection of a patch for the canvas, with no drawing in it: every cell shrunk towards its centre, the
  * cells above the height cut left out, the faces turned away from the eye culled, the rest lit from a fixed
  * direction and projected on the view plane, and, with the orbits on, a sphere at every vertex coloured by
  * its orbit. The items come back in depth order, the farthest first (the painter's algorithm).
  */
object Scene:

  /** The view: azimuth and elevation of the eye (radians), the shrink (0–1), the height cut (0–1 of the
    * patch's height) and whether the vertex orbits are shown.
    */
  final case class View(
      az: Double,
      el: Double,
      shrink: Double,
      cut: Double,
      orbits: Boolean,
      highlight: Option[Int] = None
  )

  sealed trait Item:
    def depth: Double
  final case class Face(depth: Double, pts: Vector[(Double, Double)], color: String) extends Item
  final case class Dot(depth: Double, at: (Double, Double), color: String)           extends Item

  private type V3 = (Double, Double, Double)
  private def sub(a: V3, b: V3): V3          = (a._1 - b._1, a._2 - b._2, a._3 - b._3)
  private def dot(a: V3, b: V3): Double      = a._1 * b._1 + a._2 * b._2 + a._3 * b._3
  private def cross(a: V3, b: V3): V3        =
    (a._2 * b._3 - a._3 * b._2, a._3 * b._1 - a._1 * b._3, a._1 * b._2 - a._2 * b._1)
  private def mean(ps: Seq[V3]): V3          =
    val n = ps.size.toDouble
    (ps.map(_._1).sum / n, ps.map(_._2).sum / n, ps.map(_._3).sum / n)
  private def point(v: js.Array[Double]): V3 = (v(0), v(1), v(2))

  private val light: V3 = {
    val (x, y, z) = (0.3, 0.35, 0.89); val n = math.sqrt(x * x + y * y + z * z); (x / n, y / n, z / n)
  }

  /** The height of each cell's centre, and the cut height a fraction `cut` of the way up the patch. */
  def cutHeight(cells: js.Array[Cell], cut: Double): (Vector[Double], Double) =
    val zs = cells.toVector.map(c => c.v.toSeq.map(_(2)).sum / c.v.length)
    if zs.isEmpty then (zs, 0.0) else (zs, zs.min + (zs.max - zs.min) * cut + 1e-6)

  def items(cells: js.Array[Cell], view: View): Vector[Item] =
    val (ca, sa, ce, se) = (math.cos(view.az), math.sin(view.az), math.cos(view.el), math.sin(view.el))
    val eye: V3          = (ce * ca, ce * sa, se)
    val right: V3        = (-sa, ca, 0.0)
    val up: V3           = (-se * ca, -se * sa, ce)
    def onScreen(p: V3)  = (dot(p, right), dot(p, up))
    val (zs, zcut)       = cutHeight(cells, view.cut)
    val out              = Vector.newBuilder[Item]
    for (c, ci) <- cells.toSeq.zipWithIndex if zs(ci) <= zcut do
      val raw = c.v.toSeq.map(point)
      val cen = mean(raw)
      val vs  = raw.map(p =>
        (
          cen._1 + (p._1 - cen._1) * view.shrink,
          cen._2 + (p._2 - cen._2) * view.shrink,
          cen._3 + (p._3 - cen._3) * view.shrink
        )
      )
      for face <- c.f.toSeq do
        val p  = face.toSeq.map(i => vs(i))
        val fc = mean(p)
        // the outward normal: the one pointing away from the cell's centre
        val n0 = cross(sub(p(1), p(0)), sub(p(2), p(0)))
        val n1 = if dot(n0, sub(fc, cen)) < 0 then (-n0._1, -n0._2, -n0._3) else n0
        val nn = math.sqrt(dot(n1, n1))
        val n  = (n1._1 / nn, n1._2 / nn, n1._3 / nn)
        if dot(n, eye) > 0 then
          val lum = 0.55 + 0.45 * math.max(0, dot(n, light))
          out += Face(dot(fc, eye), p.map(onScreen).toVector, Palette.shaded(c.k, lum))
    // the spheres: every orbit with the orbits on; one orbit alone, orbits on or off, while it is highlighted
    if view.orbits || view.highlight.isDefined then
      val seen = collection.mutable.Set.empty[(Long, Long, Long)]
      for (c, ci) <- cells.toSeq.zipWithIndex if zs(ci) <= zcut && c.o.length == c.v.length do
        for (v, vi) <- c.v.toSeq.zipWithIndex if c.o(vi) >= 0 && view.highlight.forall(_ == c.o(vi)) do
          val p   = point(v)
          val key = (math.round(p._1 * 100), math.round(p._2 * 100), math.round(p._3 * 100))
          if seen.add(key) then out += Dot(dot(p, eye) + 0.01, onScreen(p), Palette.orbitCss(c.o(vi)))
    out.result().sortBy(_.depth)

  /** The vertex star of an orbit: the cells around the vertex of that orbit nearest the patch's centre (the
    * most central vertex has its whole star in the patch); empty if no vertex carries the orbit.
    */
  def starOf(cells: js.Array[Cell], orbit: Int): js.Array[Cell] =
    val all        = cells.toSeq
    val centre     = mean(all.flatMap(_.v.toSeq.map(point)))
    def key(p: V3) = (math.round(p._1 * 100), math.round(p._2 * 100), math.round(p._3 * 100))
    val ofOrbit    =
      for c <- all if c.o.length == c.v.length; (v, vi) <- c.v.toSeq.zipWithIndex if c.o(vi) == orbit
      yield point(v)
    if ofOrbit.isEmpty then js.Array()
    else
      val at = ofOrbit.minBy(p => dot(sub(p, centre), sub(p, centre)))
      js.Array(all.filter(_.v.toSeq.exists(v => key(point(v)) == key(at)))*)

  /** The bounding box of the projected items on the view plane: (x0, x1, y0, y1). */
  def bounds(items: Vector[Item]): (Double, Double, Double, Double) =
    val pts = items.flatMap {
      case f: Face => f.pts
      case d: Dot  => Vector(d.at)
    }
    if pts.isEmpty then (0, 1, 0, 1)
    else (pts.map(_._1).min, pts.map(_._1).max, pts.map(_._2).min, pts.map(_._2).max)
