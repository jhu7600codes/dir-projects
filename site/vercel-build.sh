#!/usr/bin/env bash
# vercel build: vercel only gives us a shallow copy of one branch, so clone the
# whole repo (every branch + tag) next to it and build the site data from that.
set -euo pipefail
SRC="$(cd "$(dirname "$0")/.." && pwd)"
REPO="${REPO:-jhu7600codes/dir-projects}"
WORK="$(mktemp -d)"
git clone --quiet --no-checkout "https://github.com/$REPO.git" "$WORK/repo"
cd "$WORK/repo"
git remote set-head origin --auto >/dev/null 2>&1 || true
python3 "$SRC/site/build.py" --repo "$REPO" --out "$SRC/_site"
