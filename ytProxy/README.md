# ytProxy

Not a network proxy despite the name (that idea got dropped - the user needs
their own VPN active for YouTube, and a system-wide MITM proxy doesn't
coexist with that). This is a narrow **bytecode patch** to the real YouTube
Android app, v14.34.54 (2019), so it self-reports a client version the
current backend still accepts, without touching anything else about how the
app identifies or renders itself locally.

## The problem

Community knowledge: v19.51.01 is "the oldest version that still works"
against today's backend - anything older than that gets version-gated.
v14.34.54 is a real, still-installable APK, but its own local UI/renderer
code (what makes this worth doing at all, vs. just installing v19.51.01) is
the 2019-era one.

**Already tried and confirmed unstable**: patching the APK's own
`AndroidManifest.xml` (`versionName`/`versionCode`) directly via `apktool`.
That changes the app's *entire* self-identity, not just what it tells the
server - internal feature-gating and the "everything blank"/"text turns
black" breakage that follows is server response *schema* changing based on
however far the version got bumped, hitting the old renderer with data
shapes it can't parse. Also just generally fragile (`apktool`'s full
resource-table repack on rebuild).

## Root cause, traced in smali

Working from `youtube_14.34.54.apk` (`aapt`/`aapt2`/`apktool` - none of this
is guessed):

- The manifest's `versionName="14.34.54"` is **not** what breaks things by
  itself - what matters is a single runtime helper,
  `Lynv;->b(Landroid/content/Context;)Ljava/lang/String;`, which reads
  `PackageManager.getPackageInfo(...).versionName` (falling back to a
  `pref_override_build_type` SharedPreferences suffix used for internal QA
  builds) and caches the result.
- Its return value is the sole input to `Lagsz`'s constructor
  (`smali/agsz.smali`), which builds the actual client-info map sent with
  every request: `cplatform`, `c`, **`cver`**, `cos`, `cosver`. Traced via
  the single call site in `smali/agsd.smali` (a Dagger `@Provides` method).
  There is exactly one call site for `Lagsz;-><init>`, and exactly one
  caller of `Lynv;->b` that feeds it - a genuinely narrow, single-purpose
  helper, not tangled into unrelated logic.
- (Two apparent leads that turned out to be **red herrings**, for the
  record: `minClientVersion`/`maxClientVersion` strings in `aym.smali` are
  Android's own `MediaRouter`/Cast plumbing, unrelated to YouTube's own
  version gate; a `clientVersion` int field in `raa.smali` is a generic
  Google Play Services telemetry bundle, also unrelated.)

## The patch

`smali/ynv.smali`, method `b(Landroid/content/Context;)Ljava/lang/String;` -
replaced the entire body (PackageInfo read + cache + QA-suffix logic) with:

```smali
.method public static b(Landroid/content/Context;)Ljava/lang/String;
    .locals 1

    const-string v0, "19.51.01"

    return-object v0
.end method
```

`AndroidManifest.xml` is untouched - confirmed via `aapt dump badging` on the
rebuilt APK still showing `versionName='14.34.54'`. Only the network-facing
`cver` value changes; every other `PackageInfo`-based check in the app still
sees the real v14.34.54 identity.

## Build steps (reproducible)

```
java -jar apktool.jar d -f -o v14_decoded youtube_14.34.54.apk
# apply the smali edit above to v14_decoded/smali/ynv.smali
java -jar apktool.jar b v14_decoded -o v14_patched_unsigned.apk
zipalign -f -p 4 v14_patched_unsigned.apk v14_patched_aligned.apk
apksigner sign --ks debug.keystore --ks-pass pass:android \
  --ks-key-alias ytproxy --key-pass pass:android \
  --out v14_patched.apk v14_patched_aligned.apk
```

`apktool.jar` (from https://github.com/iBotPeaches/Apktool), `debug.keystore`,
and all APK artifacts are intentionally **not committed** - Google's compiled
APK is copyrighted, and the rest are regeneratable build outputs. Only the
patch itself and this writeup are checked in.

**Install note**: the patched APK is signed with a new (throwaway) key since
Google's real signing key obviously can't be reused - this means it **cannot
install over** a real Google-signed YouTube already on the device. Uninstall
the real one first, or use a separate work profile/user.

## Status / open questions

- Manifest confirmed untouched, patch confirmed present in the rebuilt dex
  (`strings classes.dex | grep 19.51.01`), signature verifies cleanly.
- **Not yet tested on a real device.** Whether `cver: 19.51.01` actually gets
  past whatever gate blocks `14.34.54`, and whether the *response schema* the
  server sends back at that reported version is something v14.34.54's old
  renderer can actually parse, can only be answered by installing this and
  trying Home/search/watch for real.
- Video *playback* is a separate concern even if browsing works - YouTube's
  stream-URL signature cipher changes constantly, and this app's own
  bundled decipher logic may be stale regardless of what version it claims.
  Not investigated yet; only relevant once metadata/browsing is confirmed
  working.
