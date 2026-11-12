# the android signing key

every apk since 1.39.0 is signed with one key. android only installs an update over the
old app (and keeps your saves) when the key is the same, so **always build with this key**.

the key file (`the-rooms.keystore`) is **not in git on purpose**: this repo is public, and
anyone holding the key could sign a fake "update" that installs over the real game. jhu
keeps the file. to build on a fresh machine:

    sh tools/setup_keystore.sh /path/to/the-rooms.keystore

check it's the right one (sha256 of the certificate):

    C7:9F:2F:F0:71:5D:2E:E7:46:D0:7C:FA:D6:81:58:EF:09:46:37:BD:FF:34:FE:49:4D:81:2E:94:5A:93:6C:74

alias `androiddebugkey`, store and key password `android` (godot's debug export uses these).
