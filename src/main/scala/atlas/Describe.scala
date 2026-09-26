package atlas

import Model.*

/** A class in words, from what the index knows of it: a one-line description, how it was found, what its
  * record checks, and its stacking word read layer by layer.
  */
object Describe:

  /** A layer of a stacking word: a cube layer, or a prism row along u or w, with the hexagon offsets (of a
    * period) the row carries.
    */
  enum Layer:
    case Cube
    case Row(axis: Char, hexagons: Seq[Int], period: Option[Int])

  private val Token = """(C|Tu|Tw)(?:\{([\d,]+)/(\d+)\})?""".r

  /** The layers of a stacking word ("C Tu Tu C Tw{0/2} Tw …"); None if a token is not a layer. */
  def layers(word: String): Option[Seq[Layer]] =
    val tokens = word.trim.split("\\s+").toSeq.filter(_.nonEmpty)
    val parsed = tokens.map {
      case Token("C", null, null)    => Some(Layer.Cube)
      case Token(t, null, null)      => Some(Layer.Row(t(1), Nil, None))
      case Token("C", _, _)          => None
      case Token(t, offsets, period) =>
        Some(Layer.Row(t(1), offsets.split(',').toSeq.map(_.toInt), Some(period.toInt)))
      case _                         => None
    }
    Option.when(tokens.nonEmpty && parsed.forall(_.isDefined))(parsed.flatten)

  private def plural(n: Int, one: String, many: String): String = s"$n ${if n == 1 then one else many}"

  /** The composition of a word: "16 layers: 4 cube layers, 12 prism rows (6 along u, 6 along w)". */
  def composition(ls: Seq[Layer]): String =
    val cubes = ls.count(_ == Layer.Cube)
    val u     = ls.count { case Layer.Row('u', _, _) => true; case _ => false }
    val w     = ls.count { case Layer.Row('w', _, _) => true; case _ => false }
    val rows  =
      if u + w == 0 then Nil
      else
        Seq(plural(u + w, "prism row", "prism rows") +
          (if u > 0 && w > 0 then s" ($u along u, $w along w)" else if u > 0 then " along u" else " along w"))
    plural(ls.size, "layer", "layers") + ": " +
      (Option.when(cubes > 0)(plural(cubes, "cube layer", "cube layers")).toSeq ++ rows).mkString(", ")

  /** The one-line description of a class. */
  def summary(c: ClassEntry): String =
    val kUniform = s"${c.k}-uniform"
    val word     = Option(c.word).filter(_.nonEmpty).flatMap(layers)
    val base     =
      if c.cat == "uniform" then
        tilingOf(c).fold("one of the 28 convex uniform honeycombs")(t =>
          s"one of the 28 convex uniform honeycombs, the prismatic lift of the Archimedean tiling $t"
        )
      else if isLift(c) then
        tilingOf(c).fold(s"a $kUniform prismatic lift of a planar Krötenheerdt tiling")(t =>
          s"a $kUniform prismatic lift of the planar tiling $t"
        )
      else if c.cat == "slab" then s"a $kUniform slab necklace of octet and prism slabs"
      else if c.cat == "Barlow" then s"a $kUniform Barlow stacking of octet layers"
      else
        word.fold(s"a $kUniform Krötenheerdt honeycomb")(ls =>
          val directions = ls.collect { case Layer.Row(a, _, _) => a }.distinct.size
          val kind       = if directions == 2 then "two-direction stacking" else "stacking"
          s"a $kUniform $kind of ${composition(ls)}"
        )
    base + chambersOf(c).fold("")(n => s"; $n chambers in its minimal Delaney–Dress symbol")

  /** How the class was found, from its source. */
  def found(c: ClassEntry): String = c.source match
    case s if s.startsWith("the 28")               =>
      "One of the 28 convex uniform honeycombs, the classical list the atlas starts from (k = 1)."
    case s if s.startsWith("census")               =>
      s"Found by the census of row k = ${c.k}: every assembly of ${c.k} vertex stars from their foldings, up to the census's chamber bound, reduced to minimal symbols and realized."
    case s if s.startsWith("stacking enumeration") =>
      "Found by the enumeration of stacking words, beyond the census's chamber bound: layers of cubes and rows of prisms in two directions."
    case s if s.startsWith("prismatic lift")       =>
      "The prismatic lift of a planar Krötenheerdt tiling, identified by the canonical key of its symbol."
    case s if s.startsWith("slab necklace")        =>
      "A slab necklace: a periodic stack of octet slabs and prism slabs."
    case s if s.startsWith("Barlow")               =>
      "A Barlow stacking: octet layers stacked in a periodic sequence of positions."
    case s                                         => s.capitalize + "."

  private val Found = """found (\d+)""".r.unanchored

  /** The checks a dossier records, in words, when there is a dossier: the symbol valid and minimal, the
    * vertex stars distinct, and how often the assembly at the class's folding tuple found it again.
    */
  def checks(c: ClassEntry): Option[String] = c.dossier.toOption.map { d =>
    def yes(b: Boolean, what: String) = if b then what else s"not $what"
    val symbol                        = Seq(yes(d.valid, "a valid Delaney–Dress symbol"), yes(d.minimal, "minimal")).mkString(", ")
    val stars                         = if d.distinct then s"its ${c.k} vertex stars are pairwise distinct"
    else "its vertex stars are not all distinct"
    val again                         = d.tuple match
      case Found(n) if n.toInt > 0 => s"; the assembly at its folding tuple finds it again ($n times)"
      case Found(_)                => "; the assembly at its folding tuple does not find it"
      case _                       => ""
    s"Its symbol is $symbol; $stars$again."
  }
