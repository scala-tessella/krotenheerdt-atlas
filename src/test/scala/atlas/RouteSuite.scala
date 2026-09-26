package atlas

class RouteSuite extends munit.FunSuite:

  test("the places parse from their fragments"):
    assertEquals(Route.parse("#sequence"), Route.Sequence)
    assertEquals(Route.parse("#classes"), Route.Classes(None))
    assertEquals(Route.parse("#classes/k=5"), Route.Classes(Some(5)))
    assertEquals(Route.parse("#class/k3-040"), Route.Class("k3-040", orbits = false))
    assertEquals(Route.parse("#class/k3-040?orbits"), Route.Class("k3-040", orbits = true))

  test("the earlier atlas page's fragments lead to the same places"):
    assertEquals(Route.parse("#counts"), Route.Sequence)
    assertEquals(Route.parse("#table"), Route.Classes(None))
    assertEquals(Route.parse("#table/k=4"), Route.Classes(Some(4)))

  test("an empty or unknown fragment is the sequence"):
    assertEquals(Route.parse(""), Route.Sequence)
    assertEquals(Route.parse("#"), Route.Sequence)
    assertEquals(Route.parse("#nowhere"), Route.Sequence)
    assertEquals(Route.parse("#class/"), Route.Sequence)
    assertEquals(Route.parse("#classes/k=x"), Route.Sequence)

  test("fragment is the inverse of parse"):
    for r <- Seq(
               Route.Sequence,
               Route.Classes(None),
               Route.Classes(Some(7)),
               Route.Class("k7-011", false),
               Route.Class("k7-011", true)
             )
    do assertEquals(Route.parse(Route.fragment(r)), r)
