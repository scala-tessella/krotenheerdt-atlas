package atlas

/** The vertex stars (species) as the atlas labels them, `{cube:4 p3:6}#1`: the cells around a vertex by type
  * and number, and the variant (#n) among the stars with those cells. [[describe]] reads a label in words.
  */
object Species:

  /** A cell type in the singular and the plural, by its name in the labels (the case of p3… varies). */
  private val names: Map[String, (String, String)] = Map(
    "tet"       -> ("tetrahedron", "tetrahedra"),
    "cube"      -> ("cube", "cubes"),
    "oct"       -> ("octahedron", "octahedra"),
    "trunctet"  -> ("truncated tetrahedron", "truncated tetrahedra"),
    "co"        -> ("cuboctahedron", "cuboctahedra"),
    "truncoct"  -> ("truncated octahedron", "truncated octahedra"),
    "trunccube" -> ("truncated cube", "truncated cubes"),
    "rco"       -> ("rhombicuboctahedron", "rhombicuboctahedra"),
    "tco"       -> ("truncated cuboctahedron", "truncated cuboctahedra"),
    "p3"        -> ("triangular prism", "triangular prisms"),
    "p6"        -> ("hexagonal prism", "hexagonal prisms"),
    "p8"        -> ("octagonal prism", "octagonal prisms"),
    "p12"       -> ("dodecagonal prism", "dodecagonal prisms")
  )

  private val Label = """\{([^}]*)\}(?:#(\d+))?""".r

  /** The cells of a label (type as written, number) and its variant, if the label parses. */
  def parse(label: String): Option[(Seq[(String, Int)], Option[Int])] = label.trim match
    case Label(cells, variant) =>
      val parts = cells.trim.split("\\s+").toSeq.filter(_.nonEmpty).map(_.split(':'))
      Option.when(parts.nonEmpty && parts.forall(p => p.length == 2 && p(1).toIntOption.isDefined))(
        (parts.map(p => (p(0), p(1).toInt)), Option(variant).map(_.toInt))
      )
    case _                     => None

  private def andList(xs: Seq[String]): String = xs match
    case Seq()     => ""
    case Seq(a)    => a
    case init :+ z => init.mkString(", ") + " and " + z

  /** The cells of a label as a sorted multiset, the type in lower case: equal for two variants of one star.
    */
  private def cellsOf(label: String): Option[Seq[(String, Int)]] =
    parse(label).map(_._1.map((t, n) => (t.toLowerCase, n)).sorted)

  /** Whether `labels` holds another star with the same cells as `label`, so that its variant tells them
    * apart.
    */
  def hasVariants(label: String, labels: Iterable[String]): Boolean =
    cellsOf(label).exists(cells => labels.exists(o => o != label && cellsOf(o).contains(cells)))

  /** A label in words: "4 cubes and 6 triangular prisms (variant 1)", the variant named only when `labels`
    * (the atlas's stars) holds another star with the same cells; the label itself when it does not parse.
    */
  def describe(label: String, labels: Iterable[String]): String =
    parse(label).fold(label) { (cells, variant) =>
      val words = cells.map { (t, n) =>
        names.get(t.toLowerCase).fold(s"$n $t")((one, many) => s"$n ${if n == 1 then one else many}")
      }
      andList(words) + variant.filter(_ => hasVariants(label, labels)).fold("")(v => s" (variant $v)")
    }

  /** The number of cells around the vertex. */
  def cellCount(label: String): Option[Int] = parse(label).map(_._1.map(_._2).sum)
