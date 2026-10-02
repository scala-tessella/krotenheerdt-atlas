package atlas

import scala.scalajs.js

import Model.*

/** The facades read the bundle's JSON as the export writes it (a class file and index entries in that shape).
  */
class ModelSuite extends munit.FunSuite:

  private val patchJson =
    """{"id":"k2-029","k":2,"orbits":["{p3:12}#2","{tet:4 oct:3 p3:6}#1"],
      |"cells":[{"k":9,"v":[[0,0,0],[1,0,0],[0.5,0.866,0],[0,0,1],[1,0,1],[0.5,0.866,1]],
      |"f":[[0,1,2],[3,4,5],[0,1,4,3],[1,2,5,4],[2,0,3,5]],"o":[0,1,0,1,0,1]}]}""".stripMargin

  test("a class patch reads through its facade"):
    val p = js.JSON.parse(patchJson).asInstanceOf[ClassPatch]
    assertEquals(p.id, "k2-029")
    assertEquals(p.orbits.toSeq, Seq("{p3:12}#2", "{tet:4 oct:3 p3:6}#1"))
    assertEquals(p.cells.length, 1)
    val c = p.cells(0)
    assertEquals((c.k, c.v.length, c.f.length, c.o.toSeq.distinct.sorted), (9, 6, 5, Seq(0, 1)))
    assertEqualsDouble(c.v(2)(1), 0.866, 1e-9)

  test("a net is optional: empty or absent where none is identified"):
    def entry(json: String) = js.JSON.parse(json).asInstanceOf[ClassEntry]
    assertEquals(netOf(entry("""{"id":"k2-057","net":"znz"}""")), Some("znz"))
    assertEquals(netOf(entry("""{"id":"k2-001","net":""}""")), None)
    assertEquals(netOf(entry("""{"id":"k1-004"}""")), None)

  test("chambers are optional: null for the uniform honeycombs"):
    val known   = js.JSON.parse("""{"id":"k2-029","chambers":20}""").asInstanceOf[ClassEntry]
    val unknown = js.JSON.parse("""{"id":"k1-004","chambers":null}""").asInstanceOf[ClassEntry]
    assertEquals(chambersOf(known), Some(20))
    assertEquals(chambersOf(unknown), None)
