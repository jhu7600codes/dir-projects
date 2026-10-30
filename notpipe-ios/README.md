# notPipe for iOS

YouTube client for legacy iOS (10+, arm64, jailbroken), sibling of notPipe for Android.
Uses the same Invidious / yt2009 instance list (notPipe.json).

## Build (Linux or macOS)

Needs clang + lld, an iPhoneOS SDK (e.g. iPhoneOS12.4.sdk from github.com/theos/sdks) and ldid.

    SDK=/path/to/iPhoneOS12.4.sdk LDID=/path/to/ldid ./build.sh

The .deb lands in `dist/`. Install it with Filza or `dpkg -i`, then respring if the icon doesn't show.
