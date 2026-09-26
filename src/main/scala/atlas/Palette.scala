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

  /** The orbit colours: the Okabe–Ito palette, told apart under every common colour-vision deficiency (its
    * closest pair keeps a CIE76 distance of 16 or more under simulated protanopia, deuteranopia and
    * tritanopia; the palette before it fell to 4.3 under protanopia). Seven colours, one per orbit up to k = 7.
    */
  val orbit: Vector[String] = Vector(
    "#E69F00",
    "#56B4E9",
    "#009E73",
    "#F0E442",
    "#0072B2",
    "#D55E00",
    "#CC79A7"
  )

  def cellCss(k: Int): String = { val (r, g, b) = cell(k); s"rgb($r,$g,$b)" }

  /** A cell colour darkened by a luminance in [0, 1]. */
  def shaded(k: Int, lum: Double): String =
    val (r, g, b) = cell(k)
    s"rgb(${math.round(r * lum)},${math.round(g * lum)},${math.round(b * lum)})"

  def orbitCss(o: Int): String = orbit(o % orbit.size)
