#!/bin/sh
# installs The ROOMS android signing key for godot's debug export (see keys/README.md).
# usage: sh tools/setup_keystore.sh /path/to/the-rooms.keystore
set -e
KEY="${1:?give the path to the-rooms.keystore}"
WANT="C7:9F:2F:F0:71:5D:2E:E7:46:D0:7C:FA:D6:81:58:EF:09:46:37:BD:FF:34:FE:49:4D:81:2E:94:5A:93:6C:74"
GOT=$(keytool -list -v -keystore "$KEY" -storepass android 2>/dev/null | grep "SHA256:" | head -1 | awk '{print $2}')
if [ "$GOT" != "$WANT" ]; then
	echo "that's not The ROOMS key (got $GOT)"; exit 1
fi
mkdir -p "$HOME/.local/share/godot/keystores"
cp "$KEY" "$HOME/.local/share/godot/keystores/debug.keystore"
echo "ok: the key is in place, godot's debug export will sign with it"
