package atlas

import Catalog.{Column, Filter}

class RouteSuite extends munit.FunSuite:

  test("the places parse from their addresses"):
    assertEquals(Route.parse("/"), Route.Home)
    assertEquals(Route.parse("/sequence"), Route.Sequence)
    assertEquals(Route.parse("/classes"), Route.Classes(Filter()))
    assertEquals(Route.parse("/lifts"), Route.Lifts(None))
    assertEquals(Route.parse("/lifts/3"), Route.Lifts(Some(3)))
    assertEquals(Route.parse("/stars"), Route.Stars)
    assertEquals(Route.parse("/star/25"), Route.Star(25))
    assertEquals(Route.parse("/guide"), Route.Guide(None))
    assertEquals(Route.parse("/guide#words"), Route.Guide(Some("words")))
    assertEquals(Route.parse("/about"), Route.About)
    assertEquals(
      Route.parse("/classes?k=5&world=prism&sort=chambers&desc"),
      Route.Classes(Filter(k = Some(5), world = Some("prism"), sort = Column.Chambers, ascending = false))
    )
    assertEquals(Route.parse("/class/k3-040"), Route.Class("k3-040", orbits = false))
    assertEquals(Route.parse("/class/k3-040?orbits"), Route.Class("k3-040", orbits = true))

  test("a trailing slash, and a query or a fragment a place does not read, are ignored"):
    assertEquals(Route.parse(""), Route.Home)
    assertEquals(Route.parse("/?v=2"), Route.Home)
    assertEquals(Route.parse("/#nowhere"), Route.Home)
    assertEquals(Route.parse("/sequence/"), Route.Sequence)
    assertEquals(Route.parse("/class/k3-040/"), Route.Class("k3-040", orbits = false))
    assertEquals(Route.parse("/classes?junk"), Route.Classes(Filter()))

  test("an unknown address is no page"):
    assertEquals(Route.parse("/nowhere"), Route.NotFound)
    assertEquals(Route.parse("/class"), Route.NotFound)
    assertEquals(Route.parse("/class/k3-040/more"), Route.NotFound)
    assertEquals(Route.parse("/lifts/k=3"), Route.NotFound)
    assertEquals(Route.parse("/star/x"), Route.NotFound)
    assertEquals(Route.parse("/404"), Route.NotFound)

  test("path is the inverse of parse"):
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
               Route.Class("k7-011", true),
               Route.NotFound
             )
    do assertEquals(Route.parse(Route.path(r)), r)

  test("the page of a route drops its state"):
    assertEquals(Route.page(Route.Classes(Filter(k = Some(7)))), Route.Classes(Filter()))
    assertEquals(Route.page(Route.Guide(Some("stars"))), Route.Guide(None))
    assertEquals(Route.page(Route.Class("k7-011", true)), Route.Class("k7-011", false))
    assertEquals(Route.page(Route.Star(31)), Route.Star(31))

  test("the fragments of the earlier addresses lead to the same places"):
    assertEquals(Route.legacy("#sequence"), Some(Route.Sequence))
    assertEquals(Route.legacy("#classes"), Some(Route.Classes(Filter())))
    assertEquals(Route.legacy("#classes/k=5"), Some(Route.Classes(Filter(k = Some(5)))))
    assertEquals(
      Route.legacy("#classes?k=5&world=prism"),
      Some(Route.Classes(Filter(k = Some(5), world = Some("prism"))))
    )
    assertEquals(Route.legacy("#lifts"), Some(Route.Lifts(None)))
    assertEquals(Route.legacy("#lifts/k=3"), Some(Route.Lifts(Some(3))))
    assertEquals(Route.legacy("#stars"), Some(Route.Stars))
    assertEquals(Route.legacy("#star/25"), Some(Route.Star(25)))
    assertEquals(Route.legacy("#guide"), Some(Route.Guide(None)))
    assertEquals(Route.legacy("#guide/words"), Some(Route.Guide(Some("words"))))
    assertEquals(Route.legacy("#about"), Some(Route.About))
    assertEquals(Route.legacy("#class/k3-040"), Some(Route.Class("k3-040", orbits = false)))
    assertEquals(Route.legacy("#class/k3-040?orbits"), Some(Route.Class("k3-040", orbits = true)))
    assertEquals(Route.legacy("#counts"), Some(Route.Sequence))
    assertEquals(Route.legacy("#table"), Some(Route.Classes(Filter())))
    assertEquals(Route.legacy("#table/k=4"), Some(Route.Classes(Filter(k = Some(4)))))

  test("any other fragment is none of them"):
    assertEquals(Route.legacy(""), None)
    assertEquals(Route.legacy("#"), None)
    assertEquals(Route.legacy("#nowhere"), None)
    assertEquals(Route.legacy("#class/"), None)
    assertEquals(Route.legacy("#classes/k=x"), None)
    assertEquals(Route.legacy("#star/x"), None)
