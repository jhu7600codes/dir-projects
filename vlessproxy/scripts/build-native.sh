#!/bin/bash
# builds the objc parts (app, vlessctl, vless2json) for arm64 ios 10.0
# against the ios 10.3 sdk.
#   SDK10=... OUT=dist VERSION=1.0.0 build-native.sh
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(dirname "$HERE")"
SDK10="${SDK10:?set SDK10}"
OUT="$(mkdir -p "${OUT:-dist}" && cd "${OUT:-dist}" && pwd)"
VERSION="${VERSION:-1.0.0}"
OBJ="$OUT/obj"
mkdir -p "$OBJ"

if [ -n "${IOS_CC:-}" ]; then
  CC=("$IOS_CC")
else
  CC=(xcrun --sdk iphoneos clang -target arm64-apple-ios10.0 -isysroot "$SDK10")
fi
CFLAGS=(-fobjc-arc -O2 -Wall -Wextra -Wno-unused-parameter -Werror -I"$ROOT/common")
# note: link steps do not get -fobjc-arc. with it, newer apple clang tries to
# link libarclite for old deployment targets and that lib is gone from xcode.

"${CC[@]}" "${CFLAGS[@]}" -c "$ROOT/common/VLESSConfig.m" -o "$OBJ/VLESSConfig.o"

"${CC[@]}" "${CFLAGS[@]}" -c "$ROOT/common/vless2json.m" -o "$OBJ/vless2json.o"
"${CC[@]}" "$OBJ/vless2json.o" "$OBJ/VLESSConfig.o" -framework Foundation -o "$OUT/vless2json"

"${CC[@]}" "${CFLAGS[@]}" -c "$ROOT/helper/vlessctl.m" -o "$OBJ/vlessctl.o"
"${CC[@]}" "$OBJ/vlessctl.o" -framework Foundation -framework SystemConfiguration -o "$OUT/vlessctl"

APP="$OUT/VLESSProxy.app"
rm -rf "$APP"
mkdir -p "$APP"
"${CC[@]}" "${CFLAGS[@]}" -c "$ROOT/app/main.m" -o "$OBJ/main.o"
"${CC[@]}" "$OBJ/main.o" "$OBJ/VLESSConfig.o" -framework UIKit -framework Foundation -framework CFNetwork \
  -framework CoreGraphics -o "$APP/VLESSProxy"
sed "s/@VERSION@/$VERSION/g" "$ROOT/app/Info.plist" > "$APP/Info.plist"
printf 'APPL????' > "$APP/PkgInfo"
python3 "$HERE/make-images.py" "$APP"

for b in "$APP/VLESSProxy" "$OUT/vlessctl" "$OUT/vless2json"; do
  bash "$HERE/verify-macho.sh" "$b" "$SDK10"
done
