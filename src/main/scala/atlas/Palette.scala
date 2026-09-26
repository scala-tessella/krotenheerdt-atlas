package atlas

/** The viewer's colours: one per cell type ordinal (tet, cube, oct, truncTet, co, truncOct, truncCube, rco,
  * tco, P3, P6, P8, P12), and one per vertex orbit.
  */
object Palette:

  val cell: Vector[(Int, Int, Int)] = Vector(
    (224, 122, 63),
    (150, 150, 158),
    (79, 127, 191),
    (217, 79, 106),
    (90, 176, 160),
    (106, 143, 208),
    (176, 122, 204),
    (224, 165, 63),
    (122, 158, 63),
    (217, 143, 90),
    (90, 158, 217),
    (200, 90, 143),
    (143, 143, 90)
  )

  val orbit: Vector[String] = Vector(
    "#e6194b",
    "#3cb44b",
    "#4363d8",
    "#f58231",
    "#911eb4",
    "#42d4f4",
    "#f032e6",
    "#bfef45",
    "#fabed4",
    "#469990",
    "#9A6324"
  )

  def cellCss(k: Int): String = { val (r, g, b) = cell(k); s"rgb($r,$g,$b)" }

  /** A cell colour darkened by a luminance in [0, 1]. */
  def shaded(k: Int, lum: Double): String =
    val (r, g, b) = cell(k)
    s"rgb(${math.round(r * lum)},${math.round(g * lum)},${math.round(b * lum)})"

  def orbitCss(o: Int): String = orbit(o % orbit.size)
