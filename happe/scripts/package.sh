#!/bin/bash
# fakesigns everything with ldid and builds the .deb and .ipa.
#   OUT=dist VERSION=1.0.0 package.sh
# expects in $OUT: xray, happectl, vless2json, Happe.app, cacert.pem
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(dirname "$HERE")"
OUT="$(cd "${OUT:-dist}" && pwd)"
VERSION="${VERSION:-1.0.0}"
ENT="$ROOT/entitlements"
export COPYFILE_DISABLE=1

for f in xray happectl vless2json Happe.app cacert.pem; do
  [ -e "$OUT/$f" ] || { echo "missing $OUT/$f"; exit 1; }
done

STAGE="$OUT/deb-root"
rm -rf "$STAGE"
mkdir -p "$STAGE"
cp -R "$ROOT/layout/." "$STAGE/"
mkdir -p "$STAGE/Applications" "$STAGE/usr/libexec/happe" "$STAGE/usr/bin" "$STAGE/usr/share/happe"

cp -R "$OUT/Happe.app" "$STAGE/Applications/"
cp "$OUT/xray" "$STAGE/usr/libexec/happe/xray"
cp "$OUT/happectl" "$STAGE/usr/libexec/happe/happectl"
cp "$OUT/vless2json" "$STAGE/usr/bin/vless2json"
cp "$OUT/cacert.pem" "$STAGE/usr/share/happe/cacert.pem"
# "v26.10.29 go1.27.3", shown in the app
cp "$OUT/xray.version" "$STAGE/usr/share/happe/xray.version" 2>/dev/null || echo unknown > "$STAGE/usr/share/happe/xray.version"

ldid -S"$ENT/app.plist" "$STAGE/Applications/Happe.app/Happe"
ldid -S"$ENT/happectl.plist" "$STAGE/usr/libexec/happe/happectl"
ldid -S"$ENT/xray.plist" "$STAGE/usr/libexec/happe/xray"
ldid -S "$STAGE/usr/bin/vless2json"

chmod 0755 "$STAGE/Applications/Happe.app/Happe" "$STAGE/usr/libexec/happe/xray" "$STAGE/usr/bin/vless2json"
chmod 4755 "$STAGE/usr/libexec/happe/happectl"
chmod 0644 "$STAGE/usr/share/happe/com.jhu.happe.plist" "$STAGE/usr/share/happe/cacert.pem"
chmod 0755 "$STAGE/DEBIAN/postinst" "$STAGE/DEBIAN/prerm" "$STAGE/DEBIAN/postrm"

SIZE=$(du -sk "$STAGE" | awk '{print $1}')
sed -i.bak -e "s/@VERSION@/$VERSION/" -e "s/@SIZE@/$SIZE/" "$STAGE/DEBIAN/control"
rm -f "$STAGE/DEBIAN/control.bak"
find "$STAGE" -name '.DS_Store' -delete

DEB="$OUT/com.jhu.happe_${VERSION}_iphoneos-arm.deb"
# gzip: the dpkg shipped with ios 10 era cydia does not know zstd
dpkg-deb -Zgzip --root-owner-group -b "$STAGE" "$DEB"
dpkg-deb -I "$DEB"
dpkg-deb -c "$DEB"

# .ipa: just the app (signed the same way). the daemon/helper only come with the .deb.
IPA_DIR="$OUT/ipa"
rm -rf "$IPA_DIR"
mkdir -p "$IPA_DIR/Payload"
cp -R "$STAGE/Applications/Happe.app" "$IPA_DIR/Payload/"
( cd "$IPA_DIR" && rm -f "$OUT/Happe.ipa" && zip -qry "$OUT/Happe.ipa" Payload )

# stage 1 bundle: signed xray on its own, for testing over ssh before installing anything
mkdir -p "$OUT/stage1"
cp "$STAGE/usr/libexec/happe/xray" "$OUT/stage1/xray"
cp "$STAGE/usr/bin/vless2json" "$OUT/stage1/vless2json"
cp "$OUT/cacert.pem" "$OUT/stage1/cacert.pem"

echo "signatures:"
for b in "$STAGE/Applications/Happe.app/Happe" "$STAGE/usr/libexec/happe/happectl" "$STAGE/usr/libexec/happe/xray"; do
  echo "== $b"
  ldid -e "$b" || true
done
ls -la "$OUT"
