# MSF Viewer

A single-screen Android app (Kotlin + Jetpack Compose + Material 3) that
opens `.msf` files. The MESF format carries no pixel data at all -- the
whole image is generated from the filename, per the fixed spec below.

## The MESF spec

Every character of the filename (with a trailing `.msf` stripped, case
insensitive) becomes a "unit":

- `a`-`i` -> colors 1-9 directly.
- `j`-`z` -> colors 10-26, as **one** unit (not digit-split).
- An uppercase letter splits into the decimal digits of its alphabet
  position, each digit becoming its own unit (`J` is the 10th letter ->
  digits `1`, `0` -> red, black).
- A digit right after a letter is a repeat count for that letter (`a2b` ->
  `a, a, b`).
- A digit with nothing before it is used directly as a color number.
- A space is a transparent unit.
- Punctuation isn't a unit itself -- it bumps the pixel size of the unit
  immediately before and after it by +2 base units each. Punctuation with
  no letter/number unit on either side becomes its own bigger transparent
  unit instead.
- Colors 0-3 are fixed (black/red/green/blue); 4-26 are derived by
  averaging pairs of earlier colors, per a fixed table (see
  `MesfColorTable`).
- Units flow left-to-right at their own size, wrapping to a new row past a
  16-base-unit row width. Row height is the tallest unit in that row.
  Nothing is ever cropped or dropped.
- The filename "fish" (case-insensitive, extension stripped) skips the
  algorithm entirely and renders an easter egg screen instead.

A few points the spec states by example rather than exhaustively (repeat
count applying to an uppercase letter's whole digit group, punctuation
adjacency being resolved against the nearest *unit* rather than the
nearest letter/digit) are called out as documented assumptions in
`MesfParser`'s doc comment.

## Architecture

- **`parser/`** -- the whole format, as a standalone module with no
  Android framework dependency: `MesfColorTable` (the color table, computed
  once), `MesfParser` (filename -> `List<MesfUnit>`, plus easter-egg
  detection), `MesfLayoutEngine` (units -> a flow layout in base-unit
  coordinates). Covered by JVM unit tests in `app/src/test`.
- **`ui/`** -- `MsfViewerScreen` (the one screen: rendered image or the
  fish easter egg, plus a Material 3 card of facts about the file),
  `MesfImageCanvas` (draws a computed layout with `Canvas`/`drawRect`),
  `ui/theme` (Material You dynamic color on Android 12+, static fallback
  below that).
- **`util/FileNameResolver`** -- resolves the real filename from whatever
  Uri the file manager handed over. Most file managers pass a `content://`
  Uri, whose path is often an opaque document id -- the actual name comes
  from `ContentResolver` querying `OpenableColumns.DISPLAY_NAME`. `file://`
  Uris are read directly. File *content* is never read; an empty/zero-byte
  `.msf` file works exactly the same as one with data in it, since only
  the filename matters.
- **`MainActivity`** -- no launcher entry point, no menus, no settings.
  The only way in is the manifest's `VIEW` intent-filter for `.msf` files.

## No launcher icon, on purpose

The manifest declares no `android:icon` and no `MAIN`/`LAUNCHER`
intent-filter -- the task calls for the plain default Android system icon
and no home-screen entry, so the app is only reachable through a file
manager's "open with" chooser for `.msf` files.

## Building

```bash
export ANDROID_HOME=/path/to/android-sdk   # cmdline-tools + platform-tools
                                            # + platforms;android-34 +
                                            # build-tools;34.0.0
./gradlew :app:testDebugUnitTest   # parser/layout engine unit tests
./gradlew :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`. Verified building a
real debug APK in this environment (Android SDK platform 34 + build-tools
34.0.0), all 25 unit tests green, `assembleDebug` green.

## Known gaps

- Not run on a real device/emulator in this environment -- only verified
  to compile, pass unit tests, and package correctly.
- `pathPattern` matching on `content://` Uris is inherently
  provider-dependent (some document providers use opaque ids with no
  visible filename in the path); local-storage-backed file managers
  (the common case) preserve the real name and extension in the path, so
  the chooser still shows up for those.
