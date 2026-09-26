package atlas

import com.raquo.laminar.api.L.*
import org.scalajs.dom

/** The guide: the notions the atlas is written in, one section each, linked from wherever a term appears
  * (`#guide/<section>`). The texts follow the definitions and theorems of the papers, in plain words.
  */
object Guide:

  final case class Section(id: String, title: String, body: () => Seq[Modifier[HtmlElement]])

  private def to(r: Route, text: String): HtmlElement     = a(href := Route.fragment(r), text)
  private def term(id: String, text: String): HtmlElement = to(Route.Guide(Some(id)), text)
  private def m(text: String): HtmlElement                = span(cls := "mono", text)

  val sections: Seq[Section] = Seq(
    Section(
      "honeycombs",
      "Honeycombs and their cells",
      () =>
        Seq(
          p(
            "A honeycomb fills space with convex polyhedra that meet face to face: every face of a cell is the ",
            "whole face of exactly one neighbour. The honeycombs of the atlas use convex uniform polyhedra with ",
            "unit edges — regular faces, and every vertex of a cell like every other."
          ),
          p(
            "Only thirteen such polyhedra occur in any honeycomb: the cube, the regular tetrahedron and ",
            "octahedron, the cuboctahedron, the truncated tetrahedron, octahedron and cube, the ",
            "rhombicuboctahedron, the truncated cuboctahedron, and the prisms over the triangle, the hexagon, the ",
            "octagon and the dodecagon. The viewer colours each cell by its type; the legend names them in full."
          ),
          p(
            "With one kind of vertex there are exactly 28 such honeycombs, the convex uniform honeycombs: the ",
            "first row of the atlas."
          )
        )
    ),
    Section(
      "orbits",
      "Vertex orbits and the Krötenheerdt condition",
      () =>
        Seq(
          p(
            "The symmetries of a honeycomb — the isometries of space that carry it onto itself — sort its ",
            "vertices into orbits: two vertices are in one orbit when a symmetry carries one onto the other. A ",
            "honeycomb is k-uniform when its vertices fall into exactly k orbits."
          ),
          p(
            "Without a further condition there is nothing to count: stacking slabs with a free choice at each ",
            "level already gives uncountably many honeycombs, some with infinitely many orbits. The condition ",
            "that makes the count finite goes back to Krötenheerdt's classification of the plane: the k orbits ",
            "must carry k pairwise distinct ",
            term("stars", "vertex stars"),
            ". A k-uniform honeycomb meeting it is a k-uniform Krötenheerdt honeycomb, and N",
            sub("k"),
            " counts them."
          ),
          p(
            "In the plane Krötenheerdt found 11, 20, 39, 33, 15, 10, 7 tilings for k = 1 to 7 and none beyond. ",
            "In space the sequence reads 28, 57, 119, 146, 122, 78, 16 and vanishes from k = 8 on — ",
            to(Route.Sequence, "the sequence"),
            "."
          )
        )
    ),
    Section(
      "stars",
      "Vertex stars and their labels",
      () =>
        Seq(
          p(
            "The star of a vertex is the set of cells around it, up to congruence; the atlas also calls it the ",
            "vertex's species. Around a vertex of a honeycomb only 34 arrangements of the thirteen cells are ",
            "possible, 26 of them occur in some honeycomb, and the classes from k = 2 on use 23."
          ),
          p(
            "A label counts the cells by type and adds a number for the arrangement: ",
            m("{cube:4 p3:2 p6:2}#1"),
            " is the star of a vertex with four cubes below it and, above, a hexagonal and a triangular prism, ",
            "each counted twice because the vertex lies on an edge shared by two prisms of a column. ",
            m("{cube:4 p3:6}#1"),
            " and ",
            m("#2"),
            " have the same cells, arranged differently. The atlas reads every label in words."
          ),
          p(
            "On a class page each vertex orbit is shown with its star; ",
            to(Route.Stars, "the vertex stars"),
            " page lists all 23 with the classes that use them."
          )
        )
    ),
    Section(
      "worlds",
      "The four worlds",
      () =>
        Seq(
          p("The cells of a honeycomb's stars place it in one of four worlds:"),
          ul(
            li(strong("prism"), " — cubes and prisms only; almost every class of the atlas lives here."),
            li(
              strong("slab"),
              " — tetrahedra, octahedra and triangular prisms: layers of the octet honeycomb and of prisms."
            ),
            li(
              strong("hexagon"),
              " — stars with a truncated tetrahedron, as in the quarter cubic honeycomb."
            ),
            li(
              strong("cubic family"),
              " — the uniform honeycombs with cuboctahedra, truncated cubes and the like, which occur only at k = 1."
            )
          ),
          p(
            "Sets of stars mixing the octet with cubes or hexagonal prisms never carry a class. From k = 5 on only ",
            "the prism world has any: every class there is a ",
            term("lifts", "prismatic lift"),
            " or a ",
            term("words", "two-direction stacking"),
            "."
          )
        )
    ),
    Section(
      "lifts",
      "Prismatic lifts",
      () =>
        Seq(
          p(
            "Stack the polygons of a planar tiling into prisms, floor upon floor, and the tiling becomes a ",
            "honeycomb: its prismatic lift. The stars of the lift correspond one to one to the vertex types of the ",
            "tiling, so a planar k-uniform Krötenheerdt tiling lifts to a k-uniform Krötenheerdt honeycomb."
          ),
          p(
            "The whole planar sequence therefore sits inside the spatial one: the 11 Archimedean tilings among the ",
            "28, and at every k the planar tilings among the N",
            sub("k"),
            ". ",
            to(Route.Lifts(None), "The prismatic lifts"),
            " page draws them all."
          )
        )
    ),
    Section(
      "words",
      "Stacking words",
      () =>
        Seq(
          p(
            "In a honeycomb of cubes and prisms the prism axes point in at most two directions, and in two they ",
            "are perpendicular. A honeycomb with two directions is a periodic stack of complete layers, so it is ",
            "written as a cyclic word of layers, read from the bottom up:"
          ),
          ul(
            li(m("C"), " a layer of cubes;"),
            li(m("Tu"), ", ", m("Tw"), " a row of triangular prisms with their axes along u or along w;"),
            li(
              m("Tu{o/p}"),
              ", ",
              m("Tw{o/p}"),
              " a hexagon row: in every p-th place, from offset o, six triangles spanning this row and the next ",
              "merge into a hexagonal prism."
            )
          ),
          p(
            "The class page draws a word as its bands of layers. The words are enumerated exactly: a bound on the ",
            "vertex orbits limits their length, the hexagon periods are 2, 3 and 4, and the symmetries of a word ",
            "decide its uniformity before it is built."
          )
        )
    ),
    Section(
      "symbols",
      "Symbols, chambers and keys",
      () =>
        Seq(
          p(
            "Two honeycombs are the same class when they are congruent. Each class is identified by its minimal ",
            "Delaney–Dress symbol, a finite combinatorial encoding of the honeycomb up to its symmetries: the ",
            "honeycomb is cut into small tetrahedral chambers, one for every chain of a vertex, an edge, a face and ",
            "a cell each lying on the next, and the symbol records how the symmetry classes of chambers fit together."
          ),
          p(
            "The number of chambers of the minimal symbol measures how intricate a class is: from a single chamber for ",
            "the cubic honeycomb to 336 for the largest classes of the atlas. The key is a short tag of the symbol's ",
            "canonical form: two classes are the same exactly when their keys are."
          )
        )
    ),
    Section(
      "certificates",
      "How the counts are certified",
      () =>
        Seq(
          p(
            "Every count is derived, not collected: no published list of honeycombs serves as a source of ",
            "candidates. Two mechanisms produce the rows."
          ),
          ul(
            li(
              strong("The census"),
              " (k ≤ 4): every admissible set of k stars is swept over the ways a minimal symbol can distribute ",
              "chambers among them, up to a bound; every symbol found is realized as a honeycomb, and the tails ",
              "beyond the bound are exhausted."
            ),
            li(
              strong("The reduction"),
              " (k ≥ 5): structure theorems show that a Krötenheerdt honeycomb with five or more kinds of vertex ",
              "is a prismatic lift or a two-direction stacking, so N",
              sub("k"),
              " = W",
              sub("k"),
              " + T",
              sub("k"),
              ", the stacking words with k species plus the planar count; the words are enumerated exactly."
            )
          ),
          p(
            "The census stays the independent check: before being trusted on new ground the word enumeration ",
            "reproduced every census class with two prism directions, and every word's symbol is derived again by ",
            "the census at its own folding tuple. Each class carries this record; its page tells it in words."
          )
        )
    ),
    Section(
      "vanishing",
      "Why the sequence stops at 8",
      () =>
        Seq(
          p(
            "From k = 8 on the planar sequence is zero, so only two-direction stackings could remain, and a ",
            "stacking word is built of cubes, triangular and hexagonal prisms only. The stars that can meet at a ",
            "junction of two layers number eleven. Carrying all eleven would need a plain level (cubes on cubes) ",
            "and a hexagon row of period other than 2, and a plain level forces every period to be 2: no word has ",
            "eleven or more species."
          ),
          p(
            "For k = 8, 9 and 10 the exact enumeration of words finds none. So N",
            sub("k"),
            " = 0 from k = 8 on — at the same point as the planar sequence, though the two sequences are nowhere ",
            "equal before it."
          )
        )
    ),
    Section(
      "viewer",
      "Using the viewer",
      () =>
        Seq(
          ul(
            li(
              "Drag (or slide a finger) to turn the patch; the wheel or a pinch zooms; a double tap or click resets."
            ),
            li(
              "Shrink pulls every cell towards its centre, to see between them; height peels the patch from the top, layer by layer."
            ),
            li(
              "Vertex orbits puts a coloured sphere on every vertex, one colour per orbit; point at an orbit in its card (or tap it) to see its vertices alone."
            ),
            li("The settings are remembered from one class to the next, and the arrow keys walk the atlas.")
          ),
          p(
            "A patch is a finite piece of an infinite honeycomb: a box around a vertex, wide enough to show the ",
            "pattern repeat, and for a prismatic lift a slab three floors high over a square of the tiling."
          )
        )
    )
  )

  def view(section: Option[String]): HtmlElement =
    div(
      cls := "guide",
      onMountCallback { _ =>
        section.foreach(s =>
          dom.window.requestAnimationFrame(_ =>
            Option(dom.document.getElementById(s"guide-$s")).foreach(_.scrollIntoView(true))
          )
        )
      },
      div(
        cls := "card intro",
        h2("Guide"),
        p(
          cls  := "prose",
          "The notions the atlas is written in, one at a time. Every technical term of the atlas links here."
        ),
        ol(cls := "toc", sections.map(s => li(a(href := Route.fragment(Route.Guide(Some(s.id))), s.title))))
      ),
      sections.map(s =>
        sectionTag(
          cls    := "card guide-section",
          idAttr := s"guide-${s.id}",
          h2(s.title),
          div(cls := "prose", s.body())
        )
      )
    )
