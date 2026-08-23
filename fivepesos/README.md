# 5 Pesos

A native Android coin-flip app, in Kotlin and Jetpack Compose, built from an
existing web prototype ("Made with Anything" -- that badge is gone here, it
was the web builder's own branding, not part of the app). Flip an Argentine
5 Pesos coin, or switch to one of a few other coin skins in Settings.

## How it works

- Tap the button below the coin. What it says and does depends on the
  **Spin Forever** setting (Settings, top-right):
  - **Off** (default "flip" mode): tapping runs a ~1.1s flip that lands on a
    random face and shows a result line, then the button becomes
    "flip again".
  - **On**: tapping starts an open-ended spin (button becomes "stop");
    tapping "stop" lands on a random face immediately.
- Flipping/spinning is **not an animation** -- it's a plain interval swap
  between the two flat face images every ~90ms (`CoinViewModel.startFlip`),
  matching the request to drop the rotating-coin animation from the web
  version and just cycle through heads and tails.
- The coin itself is sized as a fraction of screen width
  (`Modifier.fillMaxWidth(0.32f).aspectRatio(1f)` in `CoinScreen.kt`), not a
  fixed dp value -- an earlier fixed `220.dp` rendered far larger
  (~56% of screen width) than the original design's coin (~24-30%).
- **Settings** (the square icon, top-right) holds the Spin Forever toggle
  and the coin skin picker.

## Coin skins

- **5 Pesos** (default) and **2014 Ruble** -- real coins, cut from the
  provided product photos into transparent PNGs (`res/drawable/`).
- **Gold Star** -- a procedurally drawn coin (a gradient disc, rim rings, a
  symbol and a caption -- `ui/components/CoinFaceView.kt#EngravedCoinFace`)
  rather than a bundled photo, since no source art was provided for it.
- **Your Own Coin** -- pick two images (heads and tails), either from the
  device via the system document picker, or from **"search google for a
  coin"** in Settings -- an in-app WebView (Google Images, `q=coin` by
  default) where holding down on any photo brings up a "use as heads / use
  as tails" chooser (`ui/screens/GoogleCoinImportScreen.kt`); the picked
  image is downloaded and saved into the app's private storage
  (`data/ImageDownloader.kt`). Picking is guarded so the flip button won't
  do anything (it opens Settings instead) until both faces are set. Gallery
  picks persist via `ContentResolver.takePersistableUriPermission` on their
  `content://` Uri; Google-imported photos are the app's own files, so no
  permission grant is needed for those. Either way the Uri is saved in
  DataStore and survives restarts.

## Settings is "Metro"

The Settings panel is a deliberate change of register from the rest of the
app: a full-screen, flat, black Windows-Phone-style ("Metro") page
(`ui/screens/SettingsScreen.kt`) rather than a small floating card --
solid black background, a large lowercase "settings" title, one accent
color (`MetroAccent`, `#1BA1E2`) for selection state and toggles, plain
list rows with hairline dividers, no cards, no elevation, no rounded
corners. The in-app Google search screen and its "use as heads/tails"
chooser follow the same look.

## Architecture

- Single `MainActivity` + single `CoinViewModel` (`AndroidViewModel`,
  exposing one `CoinUiState` via `StateFlow`) -- one screen plus two
  full-screen overlays (Settings, the Google coin search), no navigation
  back-stack.
- `data/CoinModels.kt` -- `Face`, `FlipPhase`, `ImageTarget`, the `CoinArt`
  sealed interface (`Photo` / `Engraved` / `Custom`), and the bundled
  `CoinSkin` list.
- `data/SettingsRepository.kt` -- Preferences DataStore persistence for
  Spin Forever, the selected skin id, and the two custom-coin image Uris.
  `data/ImageDownloader.kt` -- downloads and saves a picked web image.
- `ui/components/CoinFaceView.kt` -- renders whichever `CoinArt` the current
  skin/face needs. `ui/components/UriImageBitmap.kt` -- shared helper that
  decodes an image Uri off the main thread.
- `ui/screens/CoinScreen.kt` -- the coin, the button, the result line, and
  the settings gear; hosts the two full-screen overlays.
  `ui/screens/SettingsScreen.kt` -- the Metro settings page.
  `ui/screens/GoogleCoinImportScreen.kt` -- the WebView coin search + the
  heads/tails chooser dialog.
- `ui/theme` -- the main screen is one flat gray-blue color scheme (no
  light/dark split), the same `#6B7280` baked into the coin photography and
  the launcher icon; the Metro screens use their own black/accent palette
  (also defined in `ui/theme/Color.kt`) by design, not a light/dark variant
  of the main theme.

## Assets

`coin_5pesos_heads.png` / `coin_5pesos_tails.png` and
`coin_ruble2014_heads.png` / `coin_ruble2014_tails.png` in `res/drawable`
were cut from provided product photos (flood-filled background removal +
edge anti-aliasing for the 5 Pesos photo, which had a flat white
background; the Ruble photos already had clean alpha) rather than
hand-drawn. The launcher icon is generated from the provided square coin
render: a legacy PNG at every mipmap density plus an adaptive icon
(`mipmap-anydpi-v26`) whose foreground is that same coin inset to the
adaptive safe zone over a flat `#6B7280` background -- the seam is
invisible because that's the exact color already baked into the corners of
the source photo.

## Building

Open the project root in Android Studio (Jellyfish+) and let Gradle sync,
or from the command line:

```
./gradlew :app:assembleDebug
```

Targets `compileSdk`/`targetSdk` 34, `minSdk` 24, AGP 8.5.2, Kotlin 1.9.24 --
the same toolchain as the repo's other Android modules (see `spy/`).
`:app:assembleDebug` has been run end-to-end in this environment (Android
SDK platform 34 + build-tools 34.0.0, `compileDebugKotlin` and the full
`assembleDebug` task graph both green, no warnings) and produces an
installable `app-debug.apk`. That verifies the code compiles and packages
correctly; the long-press-in-WebView flow (`GoogleCoinImportScreen.kt`)
could not be interactively exercised here (no emulator/display in this
sandbox), so it's implementation-reviewed rather than tap-tested. No APK is
committed under `dist/` since the build isn't reproducible from a fresh
clone without that SDK setup.
