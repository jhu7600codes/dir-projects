#!/bin/sh
# packs an exported linux build into a .deb with a menu entry and the rooms icon.
# 1. export the "Linux" preset in godot (project -> export) to export/linux/
# 2. run: sh tools/make_deb.sh            -> export/rooms-the-hallway_1.0.0_amd64.deb
# 3. install: sudo apt install ./export/rooms-the-hallway_1.0.0_amd64.deb
# the build contains assets from the original games: personal use only, don't share it.
set -e
cd "$(dirname "$0")/.."
VERSION=1.0.0
BIN=export/linux/rooms-the-hallway.x86_64
D=export/deb/rooms-the-hallway_${VERSION}_amd64
[ -f "$BIN" ] || { echo "export the linux build to $BIN first"; exit 1; }
rm -rf "export/deb"
mkdir -p "$D/DEBIAN" "$D/usr/games" "$D/usr/share/applications" "$D/usr/share/icons/hicolor/256x256/apps"
install -m 755 "$BIN" "$D/usr/games/rooms-the-hallway"
[ -f assets/local/icon.png ] && cp assets/local/icon.png "$D/usr/share/icons/hicolor/256x256/apps/rooms-the-hallway.png"
cat > "$D/usr/share/applications/rooms-the-hallway.desktop" <<DESK
[Desktop Entry]
Type=Application
Name=rooms: the hallway
Comment=unofficial fan horror game in an endless office
Exec=/usr/games/rooms-the-hallway
Icon=rooms-the-hallway
Terminal=false
Categories=Game;
DESK
cat > "$D/DEBIAN/control" <<CTRL
Package: rooms-the-hallway
Version: $VERSION
Section: games
Priority: optional
Architecture: amd64
Depends: libc6, libx11-6, libxcursor1, libxinerama1, libxrandr2, libxi6, libgl1
Recommends: libasound2t64 | libasound2, libpulse0, libwayland-client0, libvulkan1
Maintainer: jhulian
Description: rooms: the hallway
 unofficial first person horror fan game. personal use only.
CTRL
dpkg-deb --root-owner-group --build "$D" "export/rooms-the-hallway_${VERSION}_amd64.deb"
