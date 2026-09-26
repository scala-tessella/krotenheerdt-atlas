package atlas

import Model.*

/** The table's and the class page's logic over the classes of the index, with no page in it: the filter and
  * the sort of the table, the classes of the same species set and a class's neighbours in atlas order.
  */
object Catalog:

  /** The columns the table sorts by. */
  enum Column(val title: String):
    case Id       extends Column("id")
    case K        extends Column("k")
    case Name     extends Column("name")
    case Source   extends Column("source")
    case World    extends Column("world")
    case Chambers extends Column("chambers")
    case Species  extends Column("species")

  /** The table's state: a row k, a world, a source (the category), a search text, and the sort. */
  final case class Filter(
      k: Option[Int] = None,
      world: Option[String] = None,
      source: Option[String] = None,
      text: String = "",
      liftsOnly: Boolean = false,
      sort: Column = Column.Id,
      ascending: Boolean = true
  ):
    /** The same filter sorted by a column: its direction flipped when it is already the sort. */
    def sortedBy(c: Column): Filter =
      if c == sort then copy(ascending = !ascending) else copy(sort = c, ascending = true)

  /** What the search text is matched against: the name, the species, the stacking word, the key and the
    * species indices.
    */
  def searchable(c: ClassEntry): String =
    Seq(
      c.name,
      c.pair,
      c.word,
      c.key,
      c.species.mkString(":"),
      tilingOf(c).getOrElse("")
    ).mkString(" ").toLowerCase

  private def ordering(col: Column): Ordering[ClassEntry] = col match
    case Column.Id       => Ordering.by(_.id)
    case Column.K        => Ordering.by(_.k)
    case Column.Name     => Ordering.by(_.name)
    case Column.Source   => Ordering.by(_.cat)
    case Column.World    => Ordering.by(_.world)
    // the classes with no chamber count (the uniform honeycombs) come last either way
    case Column.Chambers => Ordering.by(c => chambersOf(c).getOrElse(Int.MaxValue))
    case Column.Species  => Ordering.by(_.pair)

  /** The classes the filter selects, in its order. */
  def rows(classes: Seq[ClassEntry], f: Filter): Seq[ClassEntry] =
    val text = f.text.trim.toLowerCase
    val kept = classes.filter(c =>
      f.k.forall(_ == c.k) && f.world.forall(_ == c.world) && f.source.forall(_ == c.cat) &&
        (!f.liftsOnly || isLift(c)) && (text.isEmpty || searchable(c).contains(text))
    )
    val ord  = ordering(f.sort)
    kept.sorted(using if f.ascending then ord else ord.reverse)

  /** How many filters are set (the sort is not a filter). */
  def activeFilters(f: Filter): Int =
    Seq(
      f.k.isDefined,
      f.world.isDefined,
      f.source.isDefined,
      f.text.trim.nonEmpty,
      f.liftsOnly
    ).count(identity)

  /** The distinct values of a column over the classes, sorted, for its filter's choices. */
  def choices(classes: Seq[ClassEntry], value: ClassEntry => String): Seq[String] =
    classes.map(value).distinct.sorted

  /** A suggestion of the search box: a class, or a vertex star (by species index and label). */
  enum Hit:
    case ClassHit(c: ClassEntry)
    case StarHit(index: Int, label: String)

  /** The search box's suggestions for a text, best first: the classes whose id is the text, then whose id
    * starts with it, then whose name contains it, then whose species, word, key or tiling contain it (atlas
    * order within each rank); then the vertex stars whose label or reading contains it. At most `limit`; none
    * for a blank text.
    */
  def search(classes: Seq[ClassEntry], species: Seq[(Int, String)], text: String, limit: Int = 8): Seq[Hit] =
    val t = text.trim.toLowerCase
    if t.isEmpty then Nil
    else
      def rank(c: ClassEntry): Option[Int] =
        if c.id.toLowerCase == t then Some(0)
        else if c.id.toLowerCase.startsWith(t) then Some(1)
        else if c.name.toLowerCase.contains(t) then Some(2)
        else if searchable(c).contains(t) then Some(3)
        else None
      val ranked                           = classes.flatMap(c => rank(c).map(r => (r, c))).sortBy(_._1)
      val strong                           = ranked.filter(_._1 <= 2).map(p => Hit.ClassHit(p._2))
      val weak                             = ranked.filter(_._1 == 3).map(p => Hit.ClassHit(p._2))
      val byStar                           = species
        .filter((_, l) => l.toLowerCase.contains(t) || Species.describe(l).toLowerCase.contains(t))
        .map((i, l) => Hit.StarHit(i, l))
      // the stars come after the ids and names and before the weaker matches (species set, word, key, tiling),
      // and first of all when the text looks like a label
      val hits                             = if t.startsWith("{") then byStar ++ strong ++ weak else strong ++ byStar ++ weak
      hits.take(limit)

  /** A species set in a canonical order, so sets listed in different orders compare equal. */
  def speciesSet(pair: String): String = pair.split("~").map(_.trim).sorted.mkString(" ~ ")

  /** The other classes of the same row with the same species set; none for a class with no species set. */
  def sameSpeciesSet(classes: Seq[ClassEntry], c: ClassEntry): Seq[ClassEntry] =
    if c.pair.isEmpty then Nil
    else
      val set = speciesSet(c.pair)
      classes.filter(o => o.id != c.id && o.k == c.k && o.pair.nonEmpty && speciesSet(o.pair) == set)

  /** The previous and the next class in atlas order, wrapping around. */
  def neighbours(classes: Seq[ClassEntry], c: ClassEntry): (ClassEntry, ClassEntry) =
    val i = classes.indexWhere(_.id == c.id)
    val n = classes.size
    (classes((i - 1 + n) % n), classes((i + 1) % n))
