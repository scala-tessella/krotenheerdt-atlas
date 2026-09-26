#!/usr/bin/env bash
# Puts the data bundle named in data.version into public/data/<version>/: from a local export when ATLAS_EXPORT
# points at a directory holding <version>.tar.gz (the uniform-tilings repository's atlas/export/), otherwise from the
# release asset atlas-data-<version> of scala-tessella/uniform-tilings. Checks every file against the manifest.
set -euo pipefail
cd "$(dirname "$0")/.."
version="$(tr -d '[:space:]' < data.version)"
dest="public/data/$version"
if [ -f "$dest/manifest.json" ]; then echo "data $version already present"; exit 0; fi
tmp="$(mktemp -d)"; trap 'rm -rf "$tmp"' EXIT
if [ -n "${ATLAS_EXPORT:-}" ] && [ -f "$ATLAS_EXPORT/$version.tar.gz" ]; then
  cp "$ATLAS_EXPORT/$version.tar.gz" "$tmp/"
else
  url="https://github.com/scala-tessella/uniform-tilings/releases/download/atlas-data-$version/$version.tar.gz"
  curl -fsSL "$url" -o "$tmp/$version.tar.gz"
fi
tar -xzf "$tmp/$version.tar.gz" -C "$tmp"
# every file of the manifest, with its size and SHA-256
python3 - "$tmp/$version" <<'PY'
import hashlib, json, os, sys
root = sys.argv[1]; m = json.load(open(os.path.join(root, "manifest.json")))
bad = [f["path"] for f in m["files"]
       if hashlib.sha256(open(os.path.join(root, f["path"]), "rb").read()).hexdigest() != f["sha256"]]
if bad: sys.exit(f"manifest mismatch: {bad[:5]}")
print(f"data {m['version']}: {len(m['files'])} files verified")
PY
mkdir -p public/data; rm -rf "$dest"; mv "$tmp/$version" "$dest"
