package atlas

import scala.scalajs.js

import Model.AtlasIndex

class PageHeadSuite extends munit.FunSuite:

  private def entry(
      id: String,
      k: Int,
      name: String,
      cat: String,
      lift: Boolean,
      species: Seq[Int]
  ): js.Dynamic =
    js.Dynamic.literal(
      id = id,
      k = k,
      name = name,
      cat = cat,
      world = "prism",
      pair = species.map(n => if n == 16 then "{cube:4 p3:6}#1" else "{p3:12}#2").mkString(" ~ "),
      chambers = if species.isEmpty then null else 12,
      source = "",
      word = "",
      key = "",
      net = if id == "k2-001" then "btu" else "",
      species = js.Array(species*),
      cells = js.Array("P3", "cube"),
      lift = lift,
      tiling = if lift then "3.3.3.4.4; 3.3.4.3.4" else ""
    )

  private val index: AtlasIndex = js.Dynamic
    .literal(
      version = "test",
      meta = js.Dynamic.literal(
        cells = js.Array("cube", "P3"),
        cellNames = js.Dictionary("cube" -> "cube (square prism)", "P3" -> "triangular prism"),
        species = js.Dictionary("16" -> "{cube:4 p3:6}#1", "30" -> "{p3:12}#2")
      ),
      sequence = js.Array(
        js.Dynamic.literal(k = 1, n = 28, status = "theorem", note = "", planar = 11),
        js.Dynamic.literal(k = 2, n = 57, status = "theorem", note = "", planar = 20),
        js.Dynamic.literal(k = 3, n = 0, status = "theorem", note = "", planar = 0)
      ),
      classes = js.Array(
        entry("k1-001", 1, "elongated triangular prismatic", "uniform", lift = true, Nil),
        entry("k2-001", 2, "lift 3.3.3.4.4; 3.3.4.3.4", "lift", lift = true, Seq(16, 30)),
        entry("k2-002", 2, "NEW #1", "census", lift = false, Seq(16, 30))
      )
    )
    .asInstanceOf[AtlasIndex]

  test("the home page states the number of classes and the sequence"):
    val h = PageHead.of(index, Route.Home)
    assertEquals(h.title, PageHead.siteTitle)
    assertEquals(h.canonical, "https://atlas.tessell.art/")
    assert(h.description.startsWith("All 3 k-uniform Krötenheerdt honeycombs"), h.description)
    assert(h.description.endsWith("The sequence is 28, 57 and vanishes from k = 3 on."), h.description)

  test("a class is titled by its id and name and described by what the index knows of it"):
    val h = PageHead.of(index, Route.Class("k2-001", orbits = true))
    assert(h.title.startsWith("k2-001 lift 3.3.3.4.4; 3.3.4.3.4 — "), h.title)
    assertEquals(h.canonical, "https://atlas.tessell.art/class/k2-001")
    assertEquals(
      h.description,
      "A 2-uniform prismatic lift of the planar tiling 3.3.3.4.4; 3.3.4.3.4; 12 chambers in its minimal " +
        "Delaney–Dress symbol. Cells: triangular prism, cube. Vertex stars: {cube:4 p3:6}#1 ~ {p3:12}#2. RCSR net btu."
    )

  test("a vertex star is read in words"):
    val h = PageHead.of(index, Route.Star(16))
    assert(h.title.startsWith("Vertex star {cube:4 p3:6}#1 — "), h.title)
    assert(h.description.startsWith("4 cubes and 6 triangular prisms around a vertex"), h.description)
    assert(h.description.endsWith("in 2 classes of the atlas, k = 2 to 2."), h.description)

  test("the states of a page share its canonical address"):
    assertEquals(
      PageHead.of(index, Route.Classes(Catalog.Filter(k = Some(2)))).canonical,
      "https://atlas.tessell.art/classes"
    )
    assertEquals(PageHead.of(index, Route.Guide(Some("trust"))).canonical, "https://atlas.tessell.art/guide")

  test("the pages are the fixed ones, the rows of lifts, the stars and the classes, each once"):
    val pages = PageHead.pages(index)
    assertEquals(pages.size, 7 + 2 + 2 + 3)
    assertEquals(pages.distinct, pages)
    assert(pages.forall(PageHead.exists(index, _)))
    assertEquals(pages.map(Route.page), pages)
    assertEquals(pages.map(r => Route.parse(Route.path(r))), pages)

  test("an address may name what is not there"):
    assert(!PageHead.exists(index, Route.Class("k9-001", orbits = false)))
    assert(!PageHead.exists(index, Route.Star(17)))
    assert(!PageHead.exists(index, Route.Lifts(Some(3))))
    assert(!PageHead.exists(index, Route.NotFound))
    assertEquals(
      PageHead.of(index, Route.Class("k9-001", orbits = false)),
      PageHead.of(index, Route.NotFound)
    )
