package atlas

import Catalog.{Column, Filter}

class RouteSuite extends munit.FunSuite:

  test("the places parse from their fragments"):
    assertEquals(Route.parse("#sequence"), Route.Sequence)
    assertEquals(Route.parse("#classes"), Route.Classes(Filter()))
    assertEquals(Route.parse("#classes/k=5"), Route.Classes(Filter(k = Some(5))))
    assertEquals(Route.parse("#lifts"), Route.Lifts(None))
    assertEquals(Route.parse("#lifts/k=3"), Route.Lifts(Some(3)))
    assertEquals(Route.parse("#stars"), Route.Stars)
    assertEquals(Route.parse("#star/25"), Route.Star(25))
    assertEquals(Route.parse("#guide"), Route.Guide(None))
    assertEquals(Route.parse("#guide/words"), Route.Guide(Some("words")))
    assertEquals(Route.parse("#about"), Route.About)
    assertEquals(
      Route.parse("#classes?k=5&world=prism&sort=chambers&desc"),
      Route.Classes(Filter(k = Some(5), world = Some("prism"), sort = Column.Chambers, ascending = false))
    )
    assertEquals(Route.parse("#class/k3-040"), Route.Class("k3-040", orbits = false))
    assertEquals(Route.parse("#class/k3-040?orbits"), Route.Class("k3-040", orbits = true))

  test("the earlier atlas page's fragments lead to the same places"):
    assertEquals(Route.parse("#counts"), Route.Sequence)
    assertEquals(Route.parse("#table"), Route.Classes(Filter()))
    assertEquals(Route.parse("#table/k=4"), Route.Classes(Filter(k = Some(4))))

  test("an empty or unknown fragment is the home page"):
    assertEquals(Route.parse(""), Route.Home)
    assertEquals(Route.parse("#"), Route.Home)
    assertEquals(Route.parse("#nowhere"), Route.Home)
    assertEquals(Route.parse("#class/"), Route.Home)
    assertEquals(Route.parse("#classes/k=x"), Route.Home)
    assertEquals(Route.parse("#classes?junk"), Route.Classes(Filter()))
    assertEquals(Route.parse("#star/x"), Route.Home)

  test("fragment is the inverse of parse"):
    for r <- Seq(
               Route.Home,
               Route.Sequence,
               Route.Classes(Filter()),
               Route.Classes(Filter(k = Some(7))),
               Route.Classes(Filter(source = Some("lift"), text = "3.4.6.4", liftsOnly = true)),
               Route.Lifts(None),
               Route.Lifts(Some(2)),
               Route.Stars,
               Route.Star(31),
               Route.Guide(None),
               Route.Guide(Some("stars")),
               Route.About,
               Route.Class("k7-011", false),
               Route.Class("k7-011", true)
             )
    do assertEquals(Route.parse(Route.fragment(r)), r)
