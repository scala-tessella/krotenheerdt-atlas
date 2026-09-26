package atlas

import scala.scalajs.js

import Catalog.*
import Model.ClassEntry

class CatalogSuite extends munit.FunSuite:

  private def entry(
      id: String,
      k: Int,
      name: String,
      cat: String,
      world: String,
      pair: String,
      chambers: js.Any,
      word: String = "",
      species: Seq[Int] = Nil,
      lift: Boolean = false,
      tiling: String = ""
  ): ClassEntry =
    js.Dynamic
      .literal(
        id = id,
        k = k,
        name = name,
        cat = cat,
        world = world,
        pair = pair,
        chambers = chambers,
        word = word,
        key = "",
        species = js.Array(species*),
        cells = js.Array[String](),
        lift = lift,
        tiling = tiling
      )
      .asInstanceOf[ClassEntry]

  private val cubic = entry("k1-004", 1, "cubic", "uniform", "prism", "", null)
  private val a     =
    entry(
      "k2-001",
      2,
      "lift A",
      "lift",
      "prism",
      "{x}#1 ~ {y}#2",
      30,
      species = Seq(16, 21),
      lift = true,
      tiling = "3.4.6.4; 4.6.12"
    )
  private val b     = entry("k2-002", 2, "NEW #1", "census", "slab", "{y}#2 ~ {x}#1", 20, "C Tu Tw")
  private val c     = entry("k2-003", 2, "slab C", "slab", "slab", "{z}#1 ~ {x}#1", 44)
  private val d     = entry("k3-001", 3, "NEW #2", "census", "prism", "{x}#1 ~ {y}#2 ~ {z}#1", 36)
  private val all   = Seq(cubic, a, b, c, d)

  private def ids(cs: Seq[ClassEntry]) = cs.map(_.id)

  test("the filters select by row, world and source"):
    assertEquals(ids(rows(all, Filter(k = Some(2)))), Seq("k2-001", "k2-002", "k2-003"))
    assertEquals(ids(rows(all, Filter(world = Some("slab")))), Seq("k2-002", "k2-003"))
    assertEquals(ids(rows(all, Filter(k = Some(2), source = Some("census")))), Seq("k2-002"))

  test("the search matches the name, the species, the word and the species indices"):
    assertEquals(ids(rows(all, Filter(text = "new"))), Seq("k2-002", "k3-001"))
    assertEquals(ids(rows(all, Filter(text = "{z}"))), Seq("k2-003", "k3-001"))
    assertEquals(ids(rows(all, Filter(text = "tu tw"))), Seq("k2-002"))
    assertEquals(ids(rows(all, Filter(text = "16:21"))), Seq("k2-001"))

  test("the sort flips on the same column, and chambers put the unknown last"):
    val byCh = Filter().sortedBy(Column.Chambers)
    assertEquals(ids(rows(all, byCh)), Seq("k2-002", "k2-001", "k3-001", "k2-003", "k1-004"))
    assertEquals(
      ids(rows(all, byCh.sortedBy(Column.Chambers))),
      Seq("k1-004", "k2-003", "k3-001", "k2-001", "k2-002")
    )
    assertEquals(byCh.sortedBy(Column.Name).ascending, true)

  test("the same species set is compared in any order, within the row, and absent without a set"):
    assertEquals(ids(sameSpeciesSet(all, a)), Seq("k2-002"))
    assertEquals(ids(sameSpeciesSet(all, d)), Nil)
    assertEquals(ids(sameSpeciesSet(all, cubic)), Nil)

  test("the neighbours wrap around the atlas"):
    assertEquals(neighbours(all, cubic) match { case (p, n) => (p.id, n.id) }, ("k3-001", "k2-001"))
    assertEquals(neighbours(all, d) match { case (p, n) => (p.id, n.id) }, ("k2-003", "k1-004"))

  test("the active filters are counted, the sort aside"):
    assertEquals(activeFilters(Filter()), 0)
    assertEquals(activeFilters(Filter(k = Some(2), text = "  ")), 1)
    assertEquals(
      activeFilters(Filter(world = Some("slab"), source = Some("lift"), text = "cube").sortedBy(Column.K)),
      3
    )

  test("the prismatic lifts only: the lifts of every k, with the uniform prismatic ones"):
    val prismatic = entry(
      "k1-020",
      1,
      "triangular prismatic",
      "uniform",
      "prism",
      "",
      null,
      lift = true,
      tiling = "3.3.3.3.3.3"
    )
    assertEquals(ids(rows(all :+ prismatic, Filter(liftsOnly = true))), Seq("k1-020", "k2-001"))
    assertEquals(activeFilters(Filter(liftsOnly = true)), 1)
    assertEquals(ids(rows(all, Filter(text = "4.6.12"))), Seq("k2-001"))

  private val species = Seq(18 -> "{cube:8}#1", 25 -> "{cube:4 p3:6}#1")

  test("the search ranks an exact id, then id prefixes, then names, then the rest"):
    def hitIds(t: String) = search(all, species, t).collect { case Hit.ClassHit(c) => c.id }
    assertEquals(hitIds("k2-002"), Seq("k2-002"))
    assertEquals(hitIds("k2"), Seq("k2-001", "k2-002", "k2-003"))
    assertEquals(hitIds("new"), Seq("k2-002", "k3-001"))
    assertEquals(hitIds("   "), Nil)
    assertEquals(search(all, species, "k", limit = 2).size, 2)

  test("the search finds the vertex stars by label or reading, first when the text is a label"):
    assertEquals(search(all, species, "cubes").collect { case h: Hit.StarHit => h.index }, Seq(18, 25))
    assertEquals(search(all, species, "{cube:4").headOption, Some(Hit.StarHit(25, "{cube:4 p3:6}#1")))

  test("the stars come before the weaker class matches"):
    val hits = search(all, species, "cube")
    assertEquals(hits.take(2), Seq(Hit.StarHit(18, "{cube:8}#1"), Hit.StarHit(25, "{cube:4 p3:6}#1")))

  test("a filter travels in the URL: its query string and back"):
    val f = Filter(
      k = Some(5),
      world = Some("cubic family"),
      source = Some("lift"),
      text = "{cube:4 p3:6}#1",
      liftsOnly = true
    ).sortedBy(Column.Chambers).sortedBy(Column.Chambers)
    assertEquals(Filter.fromQuery(f.toQuery), f)
    assertEquals(Filter().toQuery, "")
    assertEquals(Filter.fromQuery(""), Filter())
    assertEquals(Filter(k = Some(3)).toQuery, "k=3")
    assertEquals(Filter.fromQuery("?k=x&sort=nothing&bogus"), Filter())
