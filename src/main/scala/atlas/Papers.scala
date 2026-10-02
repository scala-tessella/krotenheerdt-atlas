package atlas

import com.raquo.laminar.api.L.*

/** The papers behind the atlas and their deposited companions, as links: one place for every DOI. */
object Papers:

  def ext(url: String, text: String): HtmlElement =
    a(href := url, target := "_blank", rel := "noopener", text)

  private def doi(id: String): HtmlElement = ext(s"https://doi.org/$id", s"doi:$id")

  val honeycombsTitle = "The 28 convex uniform honeycombs: a completeness theorem"
  val sequenceTitle   = "The three-dimensional Krötenheerdt sequence and its vanishing point"

  /** The paper of the first row and its verification artifact. */
  def honeycombs: HtmlElement         = doi("10.5281/zenodo.22881686")
  def honeycombsArtifact: HtmlElement = doi("10.5281/zenodo.22868141")

  /** The paper of the rows from k = 2 on, its verification artifact, the Lean companion and the pinned
    * library.
    */
  def sequence: HtmlElement         = doi("10.5281/zenodo.23099603")
  def sequenceArtifact: HtmlElement = doi("10.5281/zenodo.23096848")
  def sequenceLean: HtmlElement     = doi("10.5281/zenodo.23077761")
  def researchCore: HtmlElement     = doi("10.5281/zenodo.23077376")
