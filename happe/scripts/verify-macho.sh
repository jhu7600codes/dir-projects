#!/bin/bash
# checks that a mach-o really targets arm64 ios <= 10.0 and only imports
# symbols/dylibs that exist in the ios 10.3 sdk.
#   verify-macho.sh BINARY SDK10
# needs otool, lipo, nm (xcode on the macos runner).
set -uo pipefail
BIN="$1"
SDK="$2"
fail=0
bad() { echo "  FAIL: $*"; fail=1; }

echo "== verify $(basename "$BIN")"

info=$(lipo -info "$BIN" 2>&1)
echo "  lipo -info: $info"
case "$info" in
  *"Non-fat file"*"architecture: arm64"|*"Non-fat file"*"architecture: arm64 ") ;;
  *) bad "expected a thin arm64 binary" ;;
esac

loads=$(otool -l "$BIN")
minver=$(printf '%s\n' "$loads" | awk '/cmd LC_VERSION_MIN_IPHONEOS/{f=1} f&&/version/{print $2; exit}')
bv=$(printf '%s\n' "$loads" | awk '/cmd LC_BUILD_VERSION/{f=1} f&&/minos/{print $2; exit}')
if [ -n "$minver" ]; then
  echo "  LC_VERSION_MIN_IPHONEOS version $minver"
  [ "$minver" = "10.0" ] || bad "min version is $minver, want 10.0"
else
  # ios 10's dyld/kernel only understand LC_VERSION_MIN_IPHONEOS
  bad "no LC_VERSION_MIN_IPHONEOS (LC_BUILD_VERSION minos=${bv:-none})"
fi
for lc in LC_DYLD_CHAINED_FIXUPS LC_DYLD_EXPORTS_TRIE; do
  if printf '%s\n' "$loads" | grep -q "cmd $lc"; then bad "$lc present (needs ios 13.4+ dyld)"; fi
done

# every linked dylib must exist in the ios 10.3 sdk
names=$(grep -Rhs 'install-name:' --include='*.tbd' "$SDK" | awk '{print $2}' | tr -d "'\"")
while read -r lib; do
  [ -z "$lib" ] && continue
  if grep -qxF -- "$lib" <<< "$names"; then
    echo "  dylib ok: $lib"
  else
    bad "dylib not in ios 10.3 sdk: $lib"
  fi
done < <(otool -L "$BIN" | tail -n +2 | awk '{print $1}')

# every imported symbol must be exported by some tbd in the ios 10.3 sdk
syms=$(mktemp)
python3 "$(dirname "$0")/tbd-symbols.py" "$SDK" | LC_ALL=C sort -u > "$syms"
missing=$(nm -u "$BIN" | grep '^_' | LC_ALL=C sort -u | LC_ALL=C comm -23 - "$syms")
count=$(nm -u "$BIN" | grep -c '^_')
if [ -n "$missing" ]; then
  bad "imports not in the ios 10.3 sdk:"
  printf '%s\n' "$missing" | sed 's/^/        /'
else
  echo "  all $count imported symbols exist in the ios 10.3 sdk"
fi
rm -f "$syms"

if [ "$fail" = 0 ]; then echo "  PASS"; else echo "  => FAILED"; fi
exit $fail
