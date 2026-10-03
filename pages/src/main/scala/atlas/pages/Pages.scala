package atlas.pages

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

import atlas.{DataVersion, Main, PageHead, Route}
import atlas.Model.AtlasIndex

@js.native
@JSImport("node:fs", JSImport.Namespace)
private object Fs extends js.Object:
  def readFileSync(path: String, encoding: String): String = js.native
  def writeFileSync(path: String, data: String): Unit      = js.native
  def mkdirSync(path: String, options: js.Object): Unit    = js.native

@js.native
@JSImport("jsdom", "JSDOM")
private class JSDOM(html: String, options: js.Object) extends js.Object:
  val window: js.Dynamic = js.native

/** The pages of the atlas, written at build time: run from the repository's root after `vite build`, it takes
  * `dist/index.html` as the template and writes one HTML file per page of the application (`index.html`,
  * `sequence.html`, `class/<id>.html`, …, and `404.html` for every other address), each with its own head and
  * with the page as the application draws it before its data is fetched, and `sitemap.xml`. The pages are
  * drawn by the application itself, in a DOM of jsdom, so what a reader without JavaScript or a search engine
  * gets is what the application shows.
  */
object Pages:

  private val out = "dist"

  /** The element of the template the application is rendered into, and the page goes into. */
  private val container = """<div id="app"><p class="note">loading the atlas…</p></div>"""

  /** A window and a document for the application, as globals, with what jsdom lacks and the application
    * touches: the media queries (never matching) and a fetch that never answers, since nothing fetched is
    * written into a page.
    */
  private def browser(): js.Dynamic =
    val window                                =
      new JSDOM("<!doctype html><html><body></body></html>", js.Dynamic.literal(url = PageHead.site + "/"))
        .window
    val global                                = js.Dynamic.global.globalThis
    val noMatch: js.Function1[String, js.Any] =
      _ => js.Dynamic.literal(matches = false, addEventListener = (() => ()): js.Function0[Unit])
    val noAnswer: js.Function0[js.Any]        = () => new js.Promise[js.Any]((_, _) => ())
    window.matchMedia = noMatch
    for name <- js.Object.getOwnPropertyNames(window.asInstanceOf[js.Object]) do
      if !js.Object.hasProperty(global.asInstanceOf[js.Object], name) then
        global.updateDynamic(name)(window.selectDynamic(name))
    global.fetch = noAnswer
    window

  private def escape(s: String): String =
    s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

  /** The text with what stands between `start` and the next `end` replaced: the template must have it. */
  private def between(html: String, start: String, end: String, value: String): String =
    val from = html.indexOf(start)
    require(from >= 0, s"$out/index.html: no '$start'")
    val to   = html.indexOf(end, from + start.length)
    require(to >= 0, s"$out/index.html: no '$end' after '$start'")
    html.substring(0, from + start.length) + value + html.substring(to)

  /** The structured data of a page other than the home page: a page of the site. */
  private def structured(h: PageHead): String =
    "\n  " + js.JSON.stringify(
      js.Dynamic.literal(
        "@context"    -> "https://schema.org",
        "@type"       -> "WebPage",
        "@id"         -> h.canonical,
        "url"         -> h.canonical,
        "name"        -> h.title,
        "description" -> h.description,
        "inLanguage"  -> "en",
        "isPartOf"    -> js.Dynamic.literal("@id" -> (PageHead.site + "/#website"))
      )
    ).replace("<", "\\u003c") + "\n  "

  /** The template with the head of a page. The home page keeps the template's own head. */
  private def withHead(template: String, r: Route, h: PageHead): String =
    if r == Route.Home then template
    else
      val content = Seq(
        ("<title>", "</title>", h.title),
        ("""<meta name="description" content="""", "\"", h.description),
        ("""<link rel="canonical" href="""", "\"", h.canonical),
        ("""<meta property="og:title" content="""", "\"", h.title),
        ("""<meta property="og:description" content="""", "\"", h.description),
        ("""<meta property="og:url" content="""", "\"", h.canonical),
        ("""<meta name="twitter:title" content="""", "\"", h.title),
        ("""<meta name="twitter:description" content="""", "\"", h.description)
      ).foldLeft(template) { case (html, (start, end, value)) => between(html, start, end, escape(value)) }
      val data    = between(content, """<script type="application/ld+json">""", "</script>", structured(h))
      // the page of an address that is no page is not for the indexes, and has no address of its own
      if r == Route.NotFound then
        val start = "<link rel=\"canonical\""
        val from  = data.indexOf(start)
        data.substring(0, from) + "<meta name=\"robots\" content=\"noindex\"" +
          data.substring(data.indexOf(">", from))
      else data

  /** The file of a page under `dist/`: `index.html` for the home page, `<path>.html` for the others (the host
    * serves `class/k8-001.html` at `/class/k8-001`).
    */
  private def file(r: Route): String =
    if r == Route.Home then "index.html" else Route.path(r).stripPrefix("/") + ".html"

  private def write(path: String, text: String): Unit =
    val full = s"$out/$path"
    Fs.mkdirSync(full.substring(0, full.lastIndexOf('/')), js.Dynamic.literal(recursive = true))
    Fs.writeFileSync(full, text)

  def main(args: Array[String]): Unit =
    val template = Fs.readFileSync(s"$out/index.html", "utf8")
    require(
      template.contains(container),
      s"$out/index.html is not the template Vite builds: run `vite build` first (npm run build does both)"
    )
    val index    = js.JSON
      .parse(Fs.readFileSync(s"public/data/${DataVersion.value}/index.json", "utf8"))
      .asInstanceOf[AtlasIndex]
    val window   = browser()
    val pages    = PageHead.pages(index)
    for r <- pages :+ Route.NotFound do
      val page = Main.written(index, r).ref.outerHTML
      write(
        file(r),
        withHead(template, r, PageHead.of(index, r)).replace(container, s"""<div id="app">$page</div>""")
      )
    write(
      "sitemap.xml",
      pages
        .map(r => s"  <url><loc>${escape(PageHead.of(index, r).canonical)}</loc></url>")
        .mkString(
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n",
          "\n",
          "\n</urlset>\n"
        )
    )
    window.close()
    println(s"${pages.size + 1} pages and the site map written to $out/")
