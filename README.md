# The atlas of the Krötenheerdt honeycombs

A web application for the k-uniform Krötenheerdt honeycombs of Euclidean 3-space: the sequence N_k, the table of
every class for k = 1 to 8, and a page per class with its solid cells and vertex orbits. Scala 3 with Scala.js and
Laminar, built with Vite, served as a static site at [atlas.tessell.art](https://atlas.tessell.art).

## Data

The application reads a data bundle exported from the verification code of the papers: an `index.json` and one
`classes/<id>.json` per class, with a manifest of sizes and SHA-256 digests. The bundle is never committed here;
`data.version` pins it, and `scripts/fetch-data.sh` places it in `public/data/<version>/`, from the asset
`<version>.tar.gz` of the release `atlas-data-<version>` of this repository, or from a local export when
`ATLAS_EXPORT` points at its directory. Every file is checked against the manifest.

The bundles are also deposited on Zenodo, licence CC BY 4.0: every version under
[10.5281/zenodo.23105749](https://doi.org/10.5281/zenodo.23105749), the bundle `a95f339` as
[10.5281/zenodo.23105750](https://doi.org/10.5281/zenodo.23105750).

## Development

    scripts/fetch-data.sh        # once per data version
    npm install
    npm run dev                  # Vite development server; the Scala.js code is linked by sbt on demand
    sbt test                     # the Scala.js tests, on Node

`npm run build` writes the static site to `dist/`. The GitHub workflow, started by hand from the Actions tab,
tests, builds and deploys `dist/` to Cloudflare Pages when run on `main`.

## Maintenance

[RUNBOOK.md](RUNBOOK.md) tells how a change reaches the site, how a new data bundle is published and archived, and
what to check after a deployment.

## License

Apache License 2.0.
