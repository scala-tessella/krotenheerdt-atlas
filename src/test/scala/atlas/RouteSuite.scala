package atlas

class RouteSuite extends munit.FunSuite:

  test("the places parse from their fragments"):
    assertEquals(Route.parse("#sequence"), Route.Sequence)
    assertEquals(Route.parse("#classes"), Route.Classes)
    assertEquals(Route.parse("#class/k3-040"), Route.Class("k3-040", orbits = false))
    assertEquals(Route.parse("#class/k3-040?orbits"), Route.Class("k3-040", orbits = true))

  test("an empty or unknown fragment is the sequence"):
    assertEquals(Route.parse(""), Route.Sequence)
    assertEquals(Route.parse("#"), Route.Sequence)
    assertEquals(Route.parse("#nowhere"), Route.Sequence)
    assertEquals(Route.parse("#class/"), Route.Sequence)

  test("fragment is the inverse of parse"):
    for r <- Seq(Route.Sequence, Route.Classes, Route.Class("k7-011", false), Route.Class("k7-011", true)) do
      assertEquals(Route.parse(Route.fragment(r)), r)
