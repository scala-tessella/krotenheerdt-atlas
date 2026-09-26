package atlas

import Gesture.*

class GestureSuite extends munit.FunSuite:

  test("one pointer dragging rotates by its movement"):
    val (s1, a1) = down(State(), 1, 100, 100, 0)
    assertEquals(a1, Action.Nothing)
    val (s2, a2) = move(s1, 1, 110, 95)
    assertEquals(a2, Action.Rotate(10, -5))
    val (_, a3)  = move(s2, 1, 110, 100)
    assertEquals(a3, Action.Rotate(0, 5))

  test("a hovering mouse, not down, does nothing"):
    assertEquals(move(State(), 1, 50, 50)._2, Action.Nothing)

  test("two pointers pinching zoom by the ratio of their distances"):
    val (s1, _) = down(State(), 1, 100, 100, 0)
    val (s2, _) = down(s1, 2, 200, 100, 10)
    move(s2, 2, 300, 100)._2 match
      case Action.Zoom(f) => assertEqualsDouble(f, 2.0, 1e-9)
      case other          => fail(s"expected a zoom, got $other")
    move(s2, 1, 150, 100)._2 match
      case Action.Zoom(f) => assertEqualsDouble(f, 0.5, 1e-9)
      case other          => fail(s"expected a zoom, got $other")

  test("a second tap soon and near resets; late or far does not"):
    val (s1, _) = down(State(), 1, 100, 100, 0)
    val s2      = up(s1, 1)
    assertEquals(down(s2, 1, 105, 102, 200)._2, Action.Reset)
    assertEquals(down(s2, 1, 105, 102, 500)._2, Action.Nothing)
    assertEquals(down(s2, 1, 180, 100, 200)._2, Action.Nothing)

  test("a pinch is not a double tap, and lifting a finger leaves one-finger rotation"):
    val (s1, _) = down(State(), 1, 100, 100, 0)
    val (s2, a) = down(s1, 2, 110, 100, 50)
    assertEquals(a, Action.Nothing)
    val s3      = up(s2, 2)
    assert(active(s3))
    assertEquals(move(s3, 1, 120, 100)._2, Action.Rotate(20, 0))
    assert(!active(up(s3, 1)))
