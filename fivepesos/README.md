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
- **Settings** (the square icon, top-right) holds the Spin Forever toggle
  and the coin skin picker.

## Coin skins

- **5 Pesos** (default) -- the real coin, from the provided product photos
  (`coin_5pesos_heads.png` / `coin_5pesos_tails.png`), cut out to
  transparent PNGs.
- **2014 Ruble** and **Gold Star** -- procedurally drawn coins (a gradient
  disc, rim rings, a symbol and a caption --
  `ui/components/CoinFaceView.kt#EngravedCoinFace`) rather than bundled
  photos, since no source art was provided for them.
- **Your Own Coin** -- pick two images (heads and tails) from the device via
  the system document picker; picking is guarded so the flip button won't
  do anything (it opens Settings instead) until both are set. Chosen images
  persist across restarts (`ContentResolver.takePersistableUriPermission`
  plus their `content://` Uris saved in DataStore).

## Architecture

- Single `MainActivity` + single `CoinViewModel` (`AndroidViewModel`,
  exposing one `CoinUiState` via `StateFlow`) -- one screen, no navigation
  back-stack.
- `data/CoinModels.kt` -- `Face`, `FlipPhase`, the `CoinArt` sealed
  interface (`Photo` / `Engraved` / `Custom`), and the bundled `CoinSkin`
  list.
- `data/SettingsRepository.kt` -- Preferences DataStore persistence for
  Spin Forever, the selected skin id, and the two custom-coin image Uris.
- `ui/components/CoinFaceView.kt` -- renders whichever `CoinArt` the current
  skin/face needs. `ui/components/UriImageBitmap.kt` -- shared helper that
  decodes a picked image's Uri off the main thread.
- `ui/screens/CoinScreen.kt` -- the coin, the button, the result line, and
  the settings gear. `ui/screens/SettingsOverlay.kt` -- the floating
  Settings card.
- `ui/theme` -- one flat color scheme (no light/dark split), the same
  gray-blue (`#6B7280`) baked into the coin photography and the launcher
  icon, so the UI, the icon, and the coin art all read as one look.

## Assets

`coin_5pesos_heads.png` / `coin_5pesos_tails.png` in `res/drawable` were cut
from the provided catalog photo (flood-filled background removal + edge
anti-aliasing, see the asset pipeline notes below) rather than hand-drawn.
The launcher icon is generated from the provided square coin render: a
legacy PNG at every mipmap density plus an adaptive icon
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
installable `app-debug.apk`. No APK is committed under `dist/` since the
build isn't reproducible from a fresh clone without that SDK setup.
