package atlas

import com.raquo.laminar.api.L.*

/** The about page: what the atlas is, the papers behind it, how to cite it, and how it is made. */
object About:

  private def ext(url: String, text: String): HtmlElement =
    a(href := url, target := "_blank", rel := "noopener", text)

  def view(total: Int): HtmlElement =
    div(
      cls := "about",
      div(
        cls := "card",
        h2("About the atlas"),
        p(
          cls := "prose",
          s"The atlas shows the $total k-uniform Krötenheerdt honeycombs of Euclidean 3-space, for every k: the ",
          "face-to-face honeycombs by unit-edge convex uniform polyhedra whose vertices fall into exactly k orbits ",
          "with k pairwise distinct vertex stars. Each class is drawn from its certified realization and ",
          "identified by its minimal Delaney–Dress symbol."
        )
      ),
      div(
        cls := "card",
        h2("The papers"),
        ul(
          cls := "refs",
          li(
            "M. Càllisto, ",
            em("The 28 convex uniform honeycombs: a completeness theorem"),
            ", preprint, Zenodo, 2026, ",
            ext("https://doi.org/10.5281/zenodo.22881686", "doi:10.5281/zenodo.22881686"),
            "; verification artifact, ",
            ext("https://doi.org/10.5281/zenodo.22868141", "doi:10.5281/zenodo.22868141"),
            ". The first row: the 28 are all there is."
          ),
          li(
            "M. Càllisto, ",
            em("The three-dimensional Krötenheerdt sequence and its vanishing point"),
            ", in preparation. The rows k = 2 to 8 and the vanishing of the sequence."
          ),
          li(
            "O. Krötenheerdt, Die homogenen Mosaike n-ter Ordnung in der euklidischen Ebene I–III, ",
            em("Wiss. Z. Martin-Luther-Univ. Halle-Wittenberg Math.-Natur. Reihe"),
            " 18 (1969) 273–290; 19 (1970) 19–38, 97–122. The planar sequence."
          )
        )
      ),
      div(
        cls := "card",
        h2("How to cite"),
        p(
          cls := "cite mono",
          s"M. Càllisto, The atlas of the Krötenheerdt honeycombs, data version ${DataVersion.value}, 2026, https://atlas.tessell.art"
        ),
        p(
          cls := "note",
          "The data version names the state of the atlas a page shows; it is also in the footer of every page."
        )
      ),
      div(
        cls := "card",
        h2("How it is made"),
        p(
          "The classes, their patches and their records come from the verification code of the papers (Scala, on ",
          "the ",
          ext("https://github.com/scala-tessella", "scala-tessella"),
          " libraries), exported as a data bundle; this application reads it. It is written in Scala 3 with ",
          "Scala.js and Laminar, and draws every patch itself, in the browser."
        ),
        p(
          "Source: ",
          ext("https://github.com/scala-tessella/krotenheerdt-atlas", "scala-tessella/krotenheerdt-atlas"),
          ". Licence: Apache 2.0. A sibling of ",
          ext("https://www.tessell.art", "tessell.art"),
          ", the editor of planar tessellations."
        )
      )
    )
