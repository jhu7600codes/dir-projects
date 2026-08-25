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

**Do not sign whatever `apktool b` outputs directly** - see "the resource-
rebuild trap" below. Graft its dex into an untouched copy of the original
APK first:

```
java -jar apktool.jar d -f -o v14_decoded youtube_14.34.54.apk
# apply the smali edit above to v14_decoded/smali/ynv.smali
java -jar apktool.jar b v14_decoded -o v14_rebuilt.apk
python3 patches/rebuild.py youtube_14.34.54.apk v14_rebuilt.apk v14_grafted_unsigned.apk
zipalign -f -p 4 v14_grafted_unsigned.apk v14_grafted_aligned.apk
apksigner sign --ks debug.keystore --ks-pass pass:android \
  --ks-key-alias ytproxy --key-pass pass:android \
  --out v14_patched.apk v14_grafted_aligned.apk
```

### The resource-rebuild trap

First attempt signed `apktool b`'s output directly (only the smali was
touched, so this seemed safe) - it crashed on launch with
`Resources$NotFoundException: Resource ID #0x0` while building an options
menu icon. `apktool b` has **no flag to skip rebuilding resources** (`-r`/
`--no-res` only exists for `apktool d`, decoding) - it always fully repacks
`resources.arsc` from the decoded XML sources, and that repack corrupted a
resource ID reference somewhere despite the change itself being
smali-only. `patches/rebuild.py` sidesteps this entirely: it takes only the
rebuilt `classes*.dex` files out of `apktool b`'s output and grafts them
into a fresh copy of the *original* APK's zip via Python's `zipfile`
(command-line `zip -X` was tried first for this and is **also unsafe** here -
it warned `Local Entry CRC does not match CD`, likely from not
understanding the APK Signing Block layout when updating entries in place).
Confirmed via `sha256sum`: `resources.arsc` and `AndroidManifest.xml` in the
grafted output are byte-identical to the original APK's.

`apktool.jar` (from https://github.com/iBotPeaches/Apktool), `debug.keystore`,
and all APK artifacts are intentionally **not committed** - Google's compiled
APK is copyrighted, and the rest are regeneratable build outputs. Only the
patch itself and this writeup are checked in.

**Install note**: the patched APK is signed with a new (throwaway) key since
Google's real signing key obviously can't be reused - this means it **cannot
install over** a real Google-signed YouTube already on the device. Uninstall
the real one first, or use a separate work profile/user.

## Status / open questions

- Manifest and resources confirmed byte-identical to the original (only the
  four `classes*.dex` differ), patch confirmed present in the rebuilt dex
  (`strings classes.dex | grep 19.51.01`), signature verifies cleanly.
- The launch crash from the first (unsafe) build is fixed by the graft
  approach above. **Not yet confirmed whether `cver: 19.51.01` actually gets
  past whatever gate blocks `14.34.54`**, or whether the server's response
  schema at that reported version is something v14.34.54's old renderer can
  parse - both need the app to actually run signed-out through Home/search/
  watch to know.
- Video *playback* is a separate concern even if browsing works - YouTube's
  stream-URL signature cipher changes constantly, and this app's own
  bundled decipher logic may be stale regardless of what version it claims.
  Not investigated yet; only relevant once metadata/browsing is confirmed
  working.
- **Not a microG/GmsCore issue** - worth recording since it was the first
  suspicion when the app crashed: the actual crash (`Resources$NotFoundException`
  building a menu icon) had nothing to do with Google Play Services/microG at
  all, it was the apktool resource-rebuild bug above. No GmsCore/signature-
  spoofing patch has been applied (or shown to be needed) at this point.
- Ad-blocking (ReVanced/Vanced-style) requested as a follow-up, once the
  version patch itself is confirmed working end to end - not attempted yet,
  to keep each change independently testable rather than stacking untested
  patches.

## Second crash, on a real device: icon lookup returning 0

The v2 build (resource-rebuild fix above) still crashed identically -
**confirming the resource-rebuild theory was wrong for this particular
crash** (resources are byte-for-byte the original in both builds, same
crash either way). Real, useful signal from that: the app *did* reach the
server successfully first (real Shorts content rendered briefly on a
second launch) before crashing - the version patch itself works.

Traced the real cause in smali: `Lkel;->a(Landroid/view/MenuItem;)V`
(building an options-menu item's icon) calls `Laltp;->a(Lattn;)I` - an
icon-style lookup, `Lattn` being an enum describing which menu item this
is - and passes the result straight to `Resources.getDrawable(I)`. For at
least one `Lattn` value the *current* server now sends, that lookup
returns `0` (no local mapping), and `getDrawable(0)` throws
`Resources$NotFoundException` instead of degrading gracefully - a live
instance of the exact "old client's local mapping doesn't have an entry
the new server response needs" problem, this time actually observed
instead of theoretical.

`Laltp` is a plain interface (`smali_classes2/altp.smali`) with **108
call sites** across the app (`grep -rl "Laltp;->a(Lattn;)I" smali*`) - a
core, general-purpose icon resolver, not something specific to one menu.
Patching every caller wasn't practical or necessary; patching the actual
lookup implementations once is. Found 6 classes implementing `Laltp`:

- `afff`/`amog`: static `if`-chain / `switch` tables of known `Lattn` ->
  drawable-id mappings, falling through to `return 0` for anything else.
- `adon`/`ftn`: `EnumMap`-backed (populated at runtime, likely from
  server-pushed theme/config data), same `return 0` fallback when a key
  isn't present.
- `zur`: a subclass of `amog` with a few extra cases, `invoke-super`-ing
  into `amog` for everything else - fixing `amog` covers this one too.
- `ftq`: delegates to a nested `Laltp` instance (whichever concrete class
  is wired to its `d` field) for anything not in its own small
  theme-attribute map - fixing the delegate covers this one too.

So only 4 files needed an actual edit (`afff`, `amog`, `adon`, `ftn`,
saved in `patches/`) - replaced each `return 0` fallback with one of that
same class's own already-valid drawable ids instead (resource ids are
global to the app, not scoped to whichever class happens to reference
them, so borrowing one across classes is safe). Wrong icon in a rare
case beats a crash. Also patched `Lkel;->a(Landroid/view/MenuItem;)V`
itself (`patches/kel.smali`) to skip overriding the `ImageView`'s icon
entirely when the lookup returns 0, rather than relying only on the
lookup-side fix - belt and suspenders, since `kel` was the one call site
actually observed crashing.

Same graft process as before (`apktool b` -> `patches/rebuild.py` against
the original APK) - confirmed via `sha256sum` that `resources.arsc` is
still byte-identical to the original after these edits too.

**Not yet confirmed on a real device whether this actually stops the
crash** - only that the patches compile, graft cleanly, and the signed
APK verifies.

## Third round: the v3 fix traded a crash for a UI-wide "+ glyph" bug

v3 (the fix above) stopped the crash, but the real device then showed a
generic "+"-looking icon in place of many real icons app-wide, and the
Home tab/search results stopped rendering real content. No crash, no
fresh logcat available - screenshots only, so the diagnosis had to come
from static analysis instead of a stack trace this time.

**Root cause**: `Lattn` (`smali/attn.smali`) is a 285-value enum. The four
`Laltp` implementations' switch tables/`EnumMap`s only ever covered a
curated subset actually used by v14.34.54's own UI in 2019 - `amog`, for
example, handles 21 of the 285 values. Every one of the other ~264 was
*always* meant to fall through to "no local icon" (return 0), and the
rest of the app already expects that: a quick survey of all 108 call
sites of `Laltp;->a(Lattn;)I` found the overwhelming majority feed the
result straight into `ImageView.setImageResource(int)`, which is a
documented no-op/clears-the-image on `0` - by design, not a bug to work
around.

Patching all four `Laltp` implementations to return one fixed nonzero
fallback id (`0x7f0805b6`) instead of 0, in the v3 round, broke that
existing contract everywhere at once: instead of quietly showing no icon
(the original, correct behavior for an icon type this old client's art
never shipped), every miss across the whole app - now common, since the
version-spoofed client reaches real 2020s-era server responses using far
more of the 285-value icon vocabulary than 2019's UI ever wired up -
showed the *same* wrong icon. That's the "+ glyph everywhere."

**Fix**: reverted all four `Laltp` implementations
(`afff`/`amog`/`adon`/`ftn`) back to returning `0` for an unrecognized
`Lattn` - their original, correct behavior. Patched the actual crash risk
at the call-site level instead, narrowly:

- Surveyed all 108 call sites for which ones feed the lookup result
  directly into something that throws on `0` (`Resources.getDrawable(I)`)
  rather than tolerating it (`setImageResource(I)`, or a call site with
  its own pre-existing `if-lez`/`if-eqz` guard already - `kni.smali` and
  `kmc.smali` already guarded themselves this way and needed no patch).
- Found 5 real call sites across `kel.smali` (done in the v3 round),
  `frf.smali`, `lbi.smali`, and `fqv.smali` with no such guard - patched
  each to skip straight to that method's own existing "no icon" path
  (jumping to a label that already clears/skips the icon) when the lookup
  returns 0, instead of calling `getDrawable(0)`.
- Two call sites in `xan.smali` feed the `Drawable` into
  `setBounds`/`setCompoundDrawablesRelative` unconditionally with no
  null-safe path to jump to - skipping isn't an option there without
  restructuring more of the method, so those two (and only those two)
  keep a nonzero fallback substitution (`0x7f0805b6`, confirmed valid)
  right before the `getDrawable` call. Two call sites getting an
  occasionally-wrong icon is a very different, much smaller blast radius
  than all 108 getting one.

Same graft process as before, confirmed `sha256sum`-identical
`resources.arsc`/`AndroidManifest.xml`, `aapt dump badging` still shows
`versionName='14.34.54'`, and the `19.51.01` spoof string is still
present in the rebuilt dex.

**Whether this actually fixes the empty Home tab/search results is still
open** - that symptom might be unrelated to icons entirely (e.g. normal
signed-out empty-state UI, or a separate old-client/new-response-schema
mismatch in the feed-parsing code, the kind of risk flagged from the
start in this project's plan). Needs a fresh real-device test, and this
time a plain `logcat -d` even if nothing crashes - the v3 symptoms showed
up with no crash at all, so filtering for `FATAL EXCEPTION` alone won't
catch whatever's suppressing Home/search content if it's a caught
exception or empty response rather than a crash.
