package atlas

import scala.scalajs.js

import Model.Cell

/** The scene on known answers: a unit cube (atlas ordinal 1) and a stack of two. */
class SceneSuite extends munit.FunSuite:

  private def cube(z: Double, orbits: Seq[Int]): Cell =
    val v =
      for (x, y, zz) <-
          Seq((0, 0, 0), (1, 0, 0), (1, 1, 0), (0, 1, 0), (0, 0, 1), (1, 0, 1), (1, 1, 1), (0, 1, 1))
      yield js.Array[Double](x, y, zz + z)
    js.Dynamic
      .literal(
        k = 1,
        v = js.Array(v*),
        f = js.Array(
          js.Array(0, 1, 2, 3),
          js.Array(4, 5, 6, 7),
          js.Array(0, 1, 5, 4),
          js.Array(1, 2, 6, 5),
          js.Array(2, 3, 7, 6),
          js.Array(3, 0, 4, 7)
        ),
        o = js.Array(orbits*)
      )
      .asInstanceOf[Cell]

  private val one = js.Array(cube(0, Seq.fill(8)(0)))
  private val two = js.Array(cube(0, Seq.fill(8)(0)), cube(1, Seq(0, 0, 0, 0, 1, 1, 1, 1)))

  private def faces(is: Vector[Scene.Item]) = is.collect { case f: Scene.Face => f }
  private def dots(is: Vector[Scene.Item])  = is.collect { case d: Scene.Dot => d }

  test("a cube seen from a generic direction shows three faces"):
    val is = Scene.items(one, Scene.View(-0.56, 0.37, 0.82, 1.0, orbits = false))
    assertEquals(faces(is).size, 3)
    assertEquals(dots(is).size, 0)

  private def area(f: Scene.Face): Double =
    val p = f.pts
    math.abs(p.indices.map(i => p(i)._1 * p((i + 1) % p.size)._2 - p((i + 1) % p.size)._1 * p(i)._2).sum) / 2

  test("a cube seen from straight above shows its top face, the sides only edge-on"):
    val is = Scene.items(one, Scene.View(0.3, math.Pi / 2 - 1e-3, 1.0, 1.0, orbits = false))
    assertEquals(faces(is).count(area(_) > 1e-2), 1)
    assertEqualsDouble(faces(is).map(area).max, 1.0, 1e-3)

  test("the shrink leaves the visible faces unchanged"):
    val a = Scene.items(one, Scene.View(0.7, 0.4, 1.0, 1.0, orbits = false))
    val b = Scene.items(one, Scene.View(0.7, 0.4, 0.55, 1.0, orbits = false))
    assertEquals(faces(a).size, faces(b).size)

  test("the height cut leaves out the cells above it"):
    val all = Scene.items(two, Scene.View(0.7, 0.4, 0.8, 1.0, orbits = false))
    val low = Scene.items(two, Scene.View(0.7, 0.4, 0.8, 0.0, orbits = false))
    assertEquals(faces(all).size, 6)
    assertEquals(faces(low).size, 3)

  test("the orbits put one sphere at every distinct vertex, coloured by its orbit"):
    val is = Scene.items(two, Scene.View(0.7, 0.4, 0.8, 1.0, orbits = true))
    assertEquals(dots(is).size, 12) // the two cubes share their four middle vertices
    assertEquals(dots(is).map(_.color).distinct.sorted, Seq(Palette.orbitCss(0), Palette.orbitCss(1)).sorted)

  test("the items come farthest first"):
    val is = Scene.items(two, Scene.View(0.7, 0.4, 0.8, 1.0, orbits = true))
    assertEquals(is.map(_.depth), is.map(_.depth).sorted)

  test("a highlighted orbit shows its spheres alone, even with the orbits off"):
    val is = Scene.items(two, Scene.View(0.7, 0.4, 0.8, 1.0, orbits = false, highlight = Some(1)))
    assertEquals(dots(is).size, 4) // the top four vertices of the upper cube carry orbit 1
    assertEquals(dots(is).map(_.color).distinct, Seq(Palette.orbitCss(1)))

  test("the star of an orbit: the cells around its most central vertex"):
    assertEquals(Scene.starOf(two, 0).length, 2) // a middle vertex, in both cubes
    assertEquals(Scene.starOf(two, 1).length, 1) // a top vertex, in the upper cube only
    assertEquals(Scene.starOf(two, 5).length, 0)
