#!/bin/bash
# cross-builds xray-core for ios arm64 with a minimum of ios 10.0, trying
# xray releases (each with the newest go its go.mod allows) until one passes
# verify-macho.sh. every candidate that passes is kept so older ones can be
# tried on the phone if the newest crashes at runtime.
#
# env:
#   SDK10        path to iPhoneOS10.3.sdk (required)
#   OUT          output dir (default ./dist)
#   CANDIDATES   xray tags to try, newest first ("latest" = newest release)
#   KEEP_GOING   1 = build every candidate, 0 = stop at the first pass
#   CONTROL      1 = also build the first candidate with an unpatched go to
#                show why the patch is needed
#   IOS_CC       linker/compiler wrapper override (for local linux testing)
set -uo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
SDK10="${SDK10:?set SDK10 to the iPhoneOS10.3.sdk path}"
OUT="$(mkdir -p "${OUT:-dist}" && cd "${OUT:-dist}" && pwd)"
WORK="${WORK:-$PWD/.xray-work}"
CANDIDATES="${CANDIDATES:-latest v25.12.8 v25.6.8 v24.12.31}"
KEEP_GOING="${KEEP_GOING:-1}"
CONTROL="${CONTROL:-1}"
mkdir -p "$WORK" "$OUT/xray-candidates"
REPORT="$OUT/xray-build-report.md"

case "$(uname -s)" in Darwin) HOST_OS=darwin ;; *) HOST_OS=linux ;; esac
case "$(uname -m)" in arm64|aarch64) HOST_ARCH=arm64 ;; *) HOST_ARCH=amd64 ;; esac

if [ -z "${IOS_CC:-}" ]; then
  IOS_CC="$WORK/ios-cc"
  cat > "$IOS_CC" <<EOF
#!/bin/sh
exec xcrun --sdk iphoneos clang -target arm64-apple-ios10.0 -isysroot "$SDK10" -Wno-unused-command-line-argument "\$@"
EOF
  chmod +x "$IOS_CC"
fi

curl_gh() {
  if [ -n "${GITHUB_TOKEN:-}" ]; then curl -fsSL -H "Authorization: Bearer $GITHUB_TOKEN" "$@"; else curl -fsSL "$@"; fi
}

resolve_tag() {
  if [ "$1" != latest ]; then echo "$1"; return; fi
  local t
  t=$(curl_gh https://api.github.com/repos/XTLS/Xray-core/releases/latest | python3 -c 'import json,sys; print(json.load(sys.stdin)["tag_name"])' 2>/dev/null)
  [ -z "$t" ] && t=$(git ls-remote --tags https://github.com/XTLS/Xray-core | awk -F/ '{print $3}' | grep -v '\^{}' | grep -E '^v[0-9.]+$' | sort -V | tail -1)
  echo "$t"
}

# newest stable go release for the go.mod's major.minor (or newer if the
# go.mod asks for a specific patch above it)
go_for_tag() {
  local tag="$1" want minor
  want=$(curl -fsSL "https://raw.githubusercontent.com/XTLS/Xray-core/$tag/go.mod" | awk '$1=="go"{print $2; exit}')
  minor=$(echo "$want" | cut -d. -f1-2)
  curl -fsSL 'https://go.dev/dl/?mode=json&include=all' | python3 -c '
import json, sys
minor = sys.argv[1]
vs = [r["version"][2:] for r in json.load(sys.stdin) if r.get("stable")]
vs = [v for v in vs if v == minor or v.startswith(minor + ".")]
key = lambda v: [int(x) for x in v.split(".")]
print(max(vs, key=key) if vs else "")
' "$minor"
}

fetch_go() {
  local ver="$1" dir="$WORK/go-$1-$2"
  if [ ! -x "$dir/go/bin/go" ]; then
    mkdir -p "$dir"
    curl -fsSL "https://go.dev/dl/go$ver.$HOST_OS-$HOST_ARCH.tar.gz" | tar -xz -C "$dir" || return 1
    if [ "$2" = patched ]; then bash "$HERE/patch-go-ios10.sh" "$dir/go" >&2 || return 1; fi
  fi
  echo "$dir/go"
}

build_one() {
  local tag="$1" gover="$2" flavor="$3" out="$4" goroot src log
  goroot=$(fetch_go "$gover" "$flavor") || { echo "go download failed"; return 1; }
  src="$WORK/xray-$tag"
  [ -d "$src" ] || git clone -q --depth 1 -b "$tag" https://github.com/XTLS/Xray-core "$src" || return 1
  log="$WORK/build-$tag-$gover-$flavor.log"
  ( cd "$src" && env -u GOROOT PATH="$goroot/bin:$PATH" GOTOOLCHAIN=local GOFLAGS=-mod=mod \
      CGO_ENABLED=1 GOOS=ios GOARCH=arm64 CC="$IOS_CC" CXX="$IOS_CC" \
      go build -trimpath -buildvcs=false -ldflags='-s -w -buildid=' -o "$out" ./main ) > "$log" 2>&1
  local rc=$?
  tail -25 "$log"
  return $rc
}

{
  echo "# xray ios 10 build report"
  echo
  echo "sdk: $(basename "$SDK10"), host: $HOST_OS/$HOST_ARCH"
  echo
  echo "| xray | go | go patch | build | verify (min ios 10.0, arm64, symbols) |"
  echo "|---|---|---|---|---|"
} > "$REPORT"

primary=""
first=1
for cand in $CANDIDATES; do
  tag=$(resolve_tag "$cand")
  gover=$(go_for_tag "$tag")
  echo "::group::xray $tag with go $gover"
  if [ -z "$tag" ] || [ -z "$gover" ]; then
    echo "| $cand | ? | - | could not resolve | - |" >> "$REPORT"
    echo "::endgroup::"
    continue
  fi

  if [ "$first" = 1 ] && [ "$CONTROL" = 1 ]; then
    echo "--- control: unpatched go $gover"
    if build_one "$tag" "$gover" stock "$WORK/xray-control"; then
      if bash "$HERE/verify-macho.sh" "$WORK/xray-control" "$SDK10"; then v=pass; else v=FAIL; fi
      echo "| $tag | $gover | no (control) | ok | $v |" >> "$REPORT"
    else
      why=$(grep -Eo '_?SecTrust[A-Za-z]+' "$WORK/build-$tag-$gover-stock.log" | sed 's/^_//' | sort -u | tr '\n' ' ')
      echo "| $tag | $gover | no (control) | link FAIL, missing on ios 10: ${why:-see log} | - |" >> "$REPORT"
    fi
  fi
  first=0

  bin="$OUT/xray-candidates/xray-$tag-go$gover"
  if build_one "$tag" "$gover" patched "$bin"; then
    if bash "$HERE/verify-macho.sh" "$bin" "$SDK10" | tee "$bin.verify.txt"; then
      echo "| $tag | $gover | yes | ok | pass |" >> "$REPORT"
      if [ -z "$primary" ]; then
        primary="$bin"
        cp "$bin" "$OUT/xray"
        echo "$tag go$gover" > "$OUT/xray.version"
      fi
    else
      echo "| $tag | $gover | yes | ok | FAIL |" >> "$REPORT"
      rm -f "$bin"
    fi
  else
    echo "| $tag | $gover | yes | FAIL | - |" >> "$REPORT"
    rm -f "$bin"
  fi
  echo "::endgroup::"
  if [ -n "$primary" ] && [ "$KEEP_GOING" != 1 ]; then break; fi
done

echo >> "$REPORT"
if [ -n "$primary" ]; then
  echo "**picked: xray $(cat "$OUT/xray.version")** (newest combo that passed)" >> "$REPORT"
  echo >> "$REPORT"
  echo '```' >> "$REPORT"
  cat "$primary.verify.txt" >> "$REPORT"
  echo '```' >> "$REPORT"
fi
cat "$REPORT"
[ -n "$primary" ]
