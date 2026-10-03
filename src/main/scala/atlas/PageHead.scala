package atlas

import Model.*

/** What the head of a page says of it: its title, its description and its canonical address. */
final case class PageHead(title: String, description: String, canonical: String)

/** The head of every page and the list of the pages, from the index alone, with no page drawn: the
  * application sets the head as one moves through it, and the build writes one HTML file per page with it.
  */
object PageHead:

  /** The site's origin, for the canonical addresses. */
  val site: String = "https://atlas.tessell.art"

  val siteTitle: String = "The atlas of the Krötenheerdt honeycombs"

  private def titled(t: String): String = s"$t — atlas of the Krötenheerdt honeycombs"

  private def lifts(i: AtlasIndex): Seq[ClassEntry] = i.classes.toSeq.filter(isLift)

  /** The nonzero values of the sequence, and the first k where it is zero. */
  private def sequence(i: AtlasIndex): (Seq[Int], Int) =
    val rows = i.sequence.toSeq.sortBy(_.k)
    val lead = rows.takeWhile(_.n > 0)
    (lead.map(_.n), lead.lastOption.fold(1)(_.k + 1))

  /** The cells of a class by their full names, without the parenthesis some names carry. */
  private def cellsOf(i: AtlasIndex, c: ClassEntry): Seq[String] =
    c.cells.toSeq.map(t => i.meta.cellNames.get(t).fold(t)(_.takeWhile(_ != '(').trim))

  private def classHead(i: AtlasIndex, c: ClassEntry): (String, String) =
    val cells = cellsOf(i, c)
    (
      titled(s"${c.id} ${c.name}"),
      Seq(
        Some(Describe.summary(c).capitalize + "."),
        Option.when(cells.nonEmpty)(s"Cells: ${cells.mkString(", ")}."),
        Option.when(c.pair.nonEmpty)(s"Vertex stars: ${c.pair}."),
        netOf(c).map(n => s"RCSR net $n.")
      ).flatten.mkString(" ")
    )

  private def starHead(i: AtlasIndex, n: Int, label: String): (String, String) =
    val labels = Stars.species(i).map(_._2)
    val users  = i.classes.toSeq.filter(_.species.contains(n))
    val rows   = users.map(_.k).distinct.sorted
    val where  = if rows.isEmpty then "" else s", k = ${rows.head} to ${rows.last}"
    (
      titled(s"Vertex star $label"),
      s"${Species.describe(label, labels).capitalize} around a vertex: one of the ${labels.size} vertex stars " +
        s"the Krötenheerdt honeycombs are built from, in ${users.size} classes of the atlas$where."
    )

  private val missing: (String, String) = (titled("No such page"), "The atlas has no page at this address.")

  def of(i: AtlasIndex, r: Route): PageHead =
    val total                = i.classes.length
    val (lead, zero)         = sequence(i)
    val (title, description) = r match
      case Route.Home           =>
        (
          siteTitle,
          s"All $total k-uniform Krötenheerdt honeycombs of Euclidean 3-space, by convex uniform polyhedra, to turn " +
            s"in 3D, compare and read. The sequence is ${lead.mkString(", ")} and vanishes from k = $zero on."
        )
      case Route.Sequence       =>
        (
          titled("The three-dimensional Krötenheerdt sequence"),
          s"The number of k-uniform Krötenheerdt honeycombs of Euclidean 3-space, row by row: " +
            s"${lead.mkString(", ")} for k = 1 to ${zero - 1} and 0 from k = $zero on, with the planar sequence " +
            "inside it and how each count is known."
        )
      case Route.Classes(_)     =>
        (
          titled(s"The $total classes"),
          s"The table of all $total k-uniform Krötenheerdt honeycombs of Euclidean 3-space, k = 1 to ${zero -
              1}: " +
            "each class with its source, its world, its chambers and its vertex stars, to filter, sort and search."
        )
      case Route.Lifts(None)    =>
        (
          titled("Prismatic lifts"),
          s"The ${lifts(i).size} planar Krötenheerdt tilings, each drawn and stacked into prisms: the prismatic " +
            "lifts among the honeycombs of the atlas, row by row."
        )
      case Route.Lifts(Some(k)) =>
        val n = lifts(i).count(_.k == k)
        if n == 0 then missing
        else
          (
            titled(s"Prismatic lifts, k = $k"),
            (if k == 1 then s"The $n Archimedean tilings of the plane"
             else s"The $n planar Krötenheerdt tilings with $k vertex types") +
              s", each drawn, and the $k-uniform honeycombs they become when stacked into prisms."
          )
      case Route.Stars          =>
        (
          titled("Vertex stars"),
          s"The ${i.meta.species.size} vertex stars, the arrangements of cells around a vertex, that the " +
            "Krötenheerdt honeycombs from k = 2 on are built from, each drawn, read in words and linked to its classes."
        )
      case Route.Star(n)        =>
        Stars.species(i).find(_._1 == n).fold(missing)((_, label) => starHead(i, n, label))
      case Route.Guide(_)       =>
        (
          titled("Guide"),
          "The notions the atlas is written in: honeycombs and their cells, vertex orbits and the Krötenheerdt " +
            "condition, vertex stars, prismatic lifts, stacking words, Delaney–Dress symbols, how the counts are " +
            "certified and why they can be trusted."
        )
      case Route.About          =>
        (
          titled("About"),
          s"What the atlas of the $total Krötenheerdt honeycombs is, the papers behind it, how to cite it and its " +
            "data, and how it is made."
        )
      case Route.Class(id, _)   => i.classes.find(_.id == id).fold(missing)(classHead(i, _))
      case Route.NotFound       => missing
    PageHead(title, description, site + Route.path(if exists(i, r) then Route.page(r) else Route.NotFound))

  /** Whether the atlas has a page for a route: an address may name a class, a star or a row that is not
    * there.
    */
  def exists(i: AtlasIndex, r: Route): Boolean = r match
    case Route.NotFound       => false
    case Route.Class(id, _)   => i.classes.exists(_.id == id)
    case Route.Star(n)        => i.meta.species.contains(n.toString)
    case Route.Lifts(Some(k)) => lifts(i).exists(_.k == k)
    case _                    => true

  /** Every page of the atlas, in the order of the site map: the fixed pages, the rows of the prismatic lifts,
    * the vertex stars and the classes.
    */
  def pages(i: AtlasIndex): Seq[Route] =
    Seq(
      Route.Home,
      Route.Sequence,
      Route.Classes(Catalog.Filter()),
      Route.Lifts(None),
      Route.Stars,
      Route.Guide(None),
      Route.About
    ) ++
      lifts(i).map(_.k).distinct.sorted.map(k => Route.Lifts(Some(k))) ++
      Stars.species(i).map((n, _) => Route.Star(n)) ++
      i.classes.toSeq.map(c => Route.Class(c.id, orbits = false))
