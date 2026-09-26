package atlas

import scala.scalajs.js

import Describe.*
import Model.ClassEntry

class DescribeSuite extends munit.FunSuite:

  test("a species label reads in words, singular and plural, with its variant"):
    assertEquals(Species.describe("{cube:4 p3:6}#1"), "4 cubes and 6 triangular prisms (variant 1)")
    assertEquals(
      Species.describe("{tet:1 truncTet:3 p3:2 p6:2}#1"),
      "1 tetrahedron, 3 truncated tetrahedra, 2 triangular prisms and 2 hexagonal prisms (variant 1)"
    )
    assertEquals(Species.describe("{tet:8 oct:6}"), "8 tetrahedra and 6 octahedra")
    assertEquals(Species.describe("{P3:12}"), "12 triangular prisms")
    assertEquals(Species.describe("not a label"), "not a label")
    assertEquals(Species.cellCount("{cube:4 p3:6}#1"), Some(10))

  test("a stacking word reads layer by layer"):
    val ls = layers("C Tu Tu C Tw{0/2} Tw Tu{0/2} Tu").get
    assertEquals(ls.size, 8)
    assertEquals(ls(4), Layer.Row('w', Seq(0), Some(2)))
    assertEquals(composition(ls), "8 layers: 2 cube layers, 6 prism rows (4 along u, 2 along w)")
    assertEquals(layers("C Tu{0,1/3}").get(1), Layer.Row('u', Seq(0, 1), Some(3)))
    assertEquals(layers("C X"), None)
    assertEquals(composition(layers("C").get), "1 layer: 1 cube layer")

  private def entry(fields: (String, js.Any)*): ClassEntry =
    js.Dynamic.literal(fields*).asInstanceOf[ClassEntry]

  test("a class in one line, by what it is"):
    val stacking =
      entry("k" -> 5, "cat" -> "stacking", "word" -> "C Tu Tu C Tw{0/2} Tw Tu{0/2} Tu", "chambers" -> 192)
    assertEquals(
      summary(stacking),
      "a 5-uniform two-direction stacking of 8 layers: 2 cube layers, 6 prism rows (4 along u, 2 along w); 192 chambers in its minimal Delaney–Dress symbol"
    )
    val lift     = entry(
      "k"        -> 3,
      "cat"      -> "lift",
      "word"     -> "",
      "lift"     -> true,
      "tiling"   -> "3.4.6.4; 4.6.12",
      "chambers" -> 36
    )
    assert(summary(lift).startsWith("a 3-uniform prismatic lift of the planar tiling 3.4.6.4; 4.6.12"))
    val cubic    = entry(
      "k"        -> 1,
      "cat"      -> "uniform",
      "word"     -> "",
      "lift"     -> true,
      "tiling"   -> "4.4.4.4",
      "chambers" -> null
    )
    assertEquals(
      summary(cubic),
      "one of the 28 convex uniform honeycombs, the prismatic lift of the Archimedean tiling 4.4.4.4"
    )

  test("a dossier reads as its checks"):
    val d = js.Dynamic.literal(
      valid = true,
      minimal = true,
      distinct = true,
      tuple = "sigma0 512 capped false minimal 512 keys 4 found 128"
    )
    val c = entry("k" -> 5, "dossier" -> d)
    assertEquals(
      checks(c),
      Some(
        "Its symbol is a valid Delaney–Dress symbol, minimal; its 5 vertex stars are pairwise distinct; the assembly at its folding tuple finds it again (128 times)."
      )
    )
    assertEquals(checks(entry("k" -> 2)), None)
