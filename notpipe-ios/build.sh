#!/bin/bash
# Builds notPipe for iOS into a .deb for jailbroken devices (iOS 10+, arm64), on Linux or macOS.
# Needs: clang + lld (ld64.lld), an iPhoneOS SDK (e.g. github.com/theos/sdks) and ldid.
#   SDK=/path/to/iPhoneOS12.4.sdk LDID=/path/to/ldid ./build.sh
set -e
cd "$(dirname "$0")"
VERSION=0.1.0
SDK=${SDK:?set SDK to an iPhoneOS sdk folder}
LDID=${LDID:-ldid}
OUT=build
APP=$OUT/root/Applications/notPipe.app
rm -rf $OUT && mkdir -p $APP $OUT/root/DEBIAN

clang -target arm64-apple-ios10.0 -isysroot "$SDK" -fobjc-arc -fobjc-runtime=ios-10.0 -fno-objc-convert-messages-to-runtime-calls -O2 -Wall -Wno-unused-property-ivar \
    -fuse-ld=lld -framework UIKit -framework Foundation -framework AVFoundation -framework AVKit -framework CoreGraphics -framework CoreMedia \
    src/*.m -o $APP/notPipe

sed "s/VERSION/$VERSION/g" res/Info.plist > $APP/Info.plist
cp res/*.png $APP/
printf 'APPL????' > $APP/PkgInfo
"$LDID" -S $APP/notPipe

cat > $OUT/root/DEBIAN/control <<CONTROL
Package: io.github.gohoski.notpipe.ios
Name: notPipe
Version: $VERSION
Architecture: iphoneos-arm
Description: A YouTube client for legacy iOS, sibling of notPipe for Android
Maintainer: jhu7600
Author: jhu7600
Section: Multimedia
Depends: firmware (>= 10.0)
CONTROL
cat > $OUT/root/DEBIAN/postinst <<'POST'
#!/bin/sh
uicache 2>/dev/null || su mobile -c uicache 2>/dev/null || true
exit 0
POST
cat > $OUT/root/DEBIAN/postrm <<'POST'
#!/bin/sh
uicache 2>/dev/null || su mobile -c uicache 2>/dev/null || true
exit 0
POST
chmod 755 $OUT/root/DEBIAN/postinst $OUT/root/DEBIAN/postrm
mkdir -p dist
dpkg-deb -Zgzip -b $OUT/root dist/notPipe-ios-$VERSION.deb >/dev/null
echo "dist/notPipe-ios-$VERSION.deb"
