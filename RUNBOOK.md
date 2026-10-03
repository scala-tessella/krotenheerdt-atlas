# Runbook

How the atlas at [atlas.tessell.art](https://atlas.tessell.art) is maintained: where everything lives, how a change
reaches the site, how a new data bundle is published, and what to check afterwards.

## Where everything lives

| What | Where |
|---|---|
| The application | this repository, branch `main` |
| The data bundle | never committed; `data.version` names it, the release `atlas-data-<version>` of this repository holds `<version>.tar.gz` |
| The archived data | Zenodo, every version under [10.5281/zenodo.23105749](https://doi.org/10.5281/zenodo.23105749), licence CC BY 4.0 |
| The site | Cloudflare Pages, project `krotenheerdt-atlas` (direct upload), production branch `main`, custom domain `atlas.tessell.art` |
| The deployment | the workflow `deploy` (`.github/workflows/deploy.yml`), started by hand |
| The secrets | repository secrets `CLOUDFLARE_API_TOKEN` (permission Cloudflare Pages: Edit) and `CLOUDFLARE_ACCOUNT_ID` |
| The export | the verification code of the papers, a separate repository; it writes `atlas/export/<version>/` and `<version>.tar.gz` |

Nothing deploys on push. A change is live only after the workflow has run on `main`.

## Local development

    scripts/fetch-data.sh        # once per data version; ATLAS_EXPORT=<export directory> takes a local tarball
    npm install
    npm run dev                  # the development server
    sbt scalafmtCheckAll pages/scalafmtCheckAll test    # the format check and the tests
    npm run build                # the static site, in dist/: the application, then its pages (see below)
    npx vite preview             # serves dist/ as the site will be served

`public/data/` keeps every bundle ever fetched, and a local build copies them all into `dist/`. The workflow fetches
only the pinned one. Old directories under `public/data/` can be deleted at any time.

## The pages

Every place of the application has its own address (`/sequence`, `/class/k8-001`, `/star/16`, `/guide#trust`;
`Route.scala`), and `npm run build` writes one HTML file for each: after `vite build`, the Node program of the sbt
project `pages` (`pages/src/main/scala/atlas/pages/Pages.scala`) takes `dist/index.html` as the template and writes
`index.html`, `sequence.html`, `class/<id>.html` and so on, `404.html` for every other address, and `sitemap.xml`.
Each file carries the head of its page (title, description, canonical address, Open Graph, structured data;
`PageHead.scala`) and the page as the application draws it before its data is fetched: the program runs the
application's own views in a DOM of jsdom, so there is no second copy of the texts. The host serves
`class/k8-001.html` at `/class/k8-001`. In the browser that text stands until the index is fetched; then the
application takes its place.

A new kind of page needs its route in `Route.scala`, its head in `PageHead.of` and its place in `PageHead.pages`:
a page missing from that list is not written, and its address answers 404 when opened directly.

The earlier addresses kept the place in the fragment (`/#class/k8-001`). They still work: the application replaces
them with the new address when it starts (`Route.legacy`). They must keep working, since they are in print.

## Deploying a change to the application

1. `sbt scalafmtCheckAll pages/scalafmtCheckAll test` and `npm run build` pass.
2. Commit and push `main`.
3. Actions > deploy > Run workflow, on `main`. It tests, fetches the data bundle, builds and deploys.
4. Check the site (see [After a deployment](#after-a-deployment)).

The workflow fails at the step "Data bundle" when the release `atlas-data-<version>` named by `data.version` does not
exist: publish the release before pushing a commit that changes `data.version`.

## Publishing a new data bundle

In the verification repository:

1. Commit the change that produces the new data: the version of a bundle is the short hash of that commit. An
   export from a tree with uncommitted changes is named `<hash>-dirty`; never publish such a bundle.
2. From `atlas/app`: `python3 generate.py` (and the patch probes it names, when the classes or their words changed),
   then `python3 export.py`. The export checks every class file, reads everything back, and fails on any problem.
3. Compare the new `manifest.json` with the previous one, file by file, and confirm that only the expected files
   differ.

In this repository:

4. Write the new version into `data.version`.
5. `ATLAS_EXPORT=<export directory> scripts/fetch-data.sh`, then the tests and the build. Open a few pages in
   `npx vite preview`.
6. Create the release: tag `atlas-data-<version>`, asset named exactly `<version>.tar.gz`, with the SHA-256 of the
   tarball and the differences from the previous bundle in the description.
7. Check the anonymous download: in a scratch copy holding only `scripts/fetch-data.sh` and `data.version`, run the
   script without `ATLAS_EXPORT`; it must print `data <version>: <n> files verified`.
8. Commit, push, run the workflow.

On Zenodo:

9. Open the record of the data, choose "New version", replace the file with the new tarball, set the version field,
   update the description where it names the version, and publish. The title carries no version.
10. Add the new version and its version DOI to `Papers.dataDois` (`src/main/scala/atlas/Papers.scala`) and to the
    README's Data section; commit, push, run the workflow again. The about page shows the DOI of a version only
    when it is listed there.

### When the numbers change

The application reads the classes and the sequence from the bundle, but these places state them in fixed text and
need a manual update when the number of classes, the sequence or the papers change:

- `index.html`: the description, the Open Graph and Twitter descriptions, the structured data (the head of the home
  page; the heads of the other pages are computed from the bundle)
- `public/llms.txt`
- `public/social-card.jpg` (see [The social card](#the-social-card))
- the home page's tiles (`Home.scala`: the counts of prismatic lifts and of vertex stars) and the texts of the guide
  and of the sequence page that quote the sequence

### The nets

The field `net` of a class is the RCSR symbol of its net as Systre identifies it, for k = 2 only. To refresh it, in
the verification repository:

1. Run the probe that writes the Systre blocks, at development radius 12 (at the default radius it skips the
   classes with larger unit cells; the legend lists any skipped class and the run must skip none).
2. Run Systre on the blocks (`java -cp Systre-<version>.jar org.gavrog.apps.systre.SystreCmdline systre-k2.cgd`),
   once with its own archive and once with the current archive of rcsr.net given as a first argument; the two runs
   must agree.
3. `python3 nets.py` writes the table by key tag and checks it (the calibration net, one verdict per block, no net
   twice); `generate.py` joins it, or `python3 nets.py --apply` joins it into the existing index when only the nets
   changed.
4. Export and publish the bundle as above.

An empty `net` means that Systre found no match in its archive, never that the net is new. If the Systre version or
the archive changes, update the note in `nets.py`.

## When a paper or an artifact gets a new DOI

Every DOI of the application is in `src/main/scala/atlas/Papers.scala`. The same DOIs are repeated in `index.html`
(structured data) and in `public/llms.txt`. Cite version DOIs for papers and artifacts; the concept
DOI is used only for the data record, where "every version" is meant.

## The social card

`public/social-card.jpg` (1200 × 630) is the image of a shared link. It is a screenshot of a small HTML page: the
title, a line of description, the sequence, and a render of a class cropped from a screenshot of the site. To remake
it, take a headless screenshot of a class page, crop the patch, place it in the card page, and take a headless
screenshot of the card at 1200 × 630 (`firefox --headless --window-size=1200,630 --screenshot out.png file.html`;
a snap-packaged Firefox can write only under the home directory). Save as JPEG, under 300 KB.

Messaging applications cache the preview of a URL. To see a new card at once, share the address with a query
(`https://atlas.tessell.art/?v=2`).

## After a deployment

    curl -s https://atlas.tessell.art/ | grep -o '<title>[^<]*'
    curl -s https://atlas.tessell.art/class/k8-001 | grep -o '<title>[^<]*'
    curl -s -o /dev/null -w "%{http_code}\n" https://atlas.tessell.art/nowhere      # 404
    curl -s https://atlas.tessell.art/sitemap.xml | grep -c '<url>'                  # one per page
    curl -s https://atlas.tessell.art/data/$(cat data.version)/manifest.json | head -c 80
    for p in robots.txt sitemap.xml llms.txt favicon.svg social-card.jpg; do
      curl -s -o /dev/null -w "$p %{http_code} %{content_type}\n" https://atlas.tessell.art/$p; done

- The footer of the site shows the data version of `data.version`.
- The about page shows the citations, and the data's DOI for the current version.
- A class page opens and draws its patch; a class of k = 2 with a net shows the row "RCSR net".
- An address of the earlier kind (`https://atlas.tessell.art/#class/k8-001`) opens the class, at its new address.
- Every static file answers with its own content type. A missing file answers 404 with `404.html`, the application
  saying "no such page".

## Known limits

- Every shared link shows the same image: the pages have their own title and description, but one social card.
- The pages written at build time hold what the application shows before fetching: no patch, no drawing of a
  tiling or of a vertex star. The page of the classes lists every class; in the browser the table shows them by
  pages.
- A filtered table (`/classes?k=5`) and a class with its orbits on (`/class/k8-001?orbits`) are states of one page:
  their canonical address is the page's.
- The links to RCSR are `http://`: the RCSR site did not answer over HTTPS when they were written.

## Troubleshooting

| Symptom | Cause and remedy |
|---|---|
| The workflow fails at "Data bundle" with 404 | The release `atlas-data-<version>` is missing, or its asset is not named `<version>.tar.gz`, or the repository is not public |
| `manifest mismatch` from `fetch-data.sh` | The tarball is not the one the manifest describes: export again, never edit a bundle by hand |
| The workflow fails at "Deploy" | A secret is missing or the token lacks Cloudflare Pages: Edit; the Pages project must exist under the name `krotenheerdt-atlas` |
| The site shows the old version after a deployment | The workflow did not run, or ran on a commit before the push: check the commit of the latest run in the Actions tab |
| The domain does not resolve just after a DNS change | A cached negative answer on the local resolver; it clears by itself, another network shows the site at once |
| A page stays as it was written, without the viewer or the live table | The index is being fetched, or its fetch failed: check `/data/<version>/index.json`; the data is cached for a year under its versioned address |
| `npm run build` fails with "is not the template Vite builds" | The pages were written without a fresh `vite build`, or the element `#app` of `index.html` was changed without changing `Pages.container` |
| An address opens from a link inside the site but answers 404 when opened directly | Its page is not in `PageHead.pages`, so no file was written for it |

## Conventions

- Commit messages and every published text are impersonal.
- Nothing published refers to private material: no private repository by name, no internal notes or decision
  records.
- Pushes, releases, deposits and the workflow are started by the maintainer, never by a script.
