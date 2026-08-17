# androdrop — Android app

Kotlin + Jetpack Compose + Material 3 (dynamic color on Android 12+, static
seed-color fallback below that, matching the web app's palette). Talks to the
androdrop Next.js API (`../androdrop`) over plain REST — no LocalSend-style
local network discovery, no Bluetooth/Wi-Fi Direct; every transfer goes
through the deployed server and Supabase Storage, same as the web/PWA client.

## Modules

- **`:app`** — the real app. Share-sheet target, pairing screen, foreground
  service that polls for incoming transfers, notifications, and an optional
  root-enhanced instant overlay popup.
- **`:xposed`** — **experimental**, unverified. See `xposed/README.md` before
  touching it.

## What's in `:app`

- **Pairing** (`ui/MainActivity.kt`) — registers the device (`DeviceStore`,
  DataStore-backed, mirrors the web app's localStorage device id), shows a
  QR code + 6-character pairing code.
- **Share target** (`ui/ShareReceiverActivity.kt`) — declared in the
  manifest as an `ACTION_SEND` / `ACTION_SEND_MULTIPLE` handler, so androdrop
  shows up in every app's native Android share sheet with zero extra setup
  (no root, no Xposed). Also reachable directly from the home screen's "Send
  a file" button, which launches the same screen with a system file picker
  instead of an incoming share intent.
- **Incoming transfers** (`service/IncomingTransferService.kt`) — a
  foreground service polling `GET /api/transfer/incoming` every 5s (Android
  has no equivalent to the web app's Supabase Realtime subscription without
  pulling in FCM, which this deliberately doesn't use — see
  `data/ApiService.kt`). New pending transfers get a high-priority
  notification with inline Accept/Decline actions and a full-screen intent
  (a "call screen"-style interruption, standard Android API, no root
  needed).
- **Root-enhanced popup** (`overlay/`) — entirely optional layer on top of
  the above. If the device is rooted, `RootOverlayManager` silently grants
  `SYSTEM_ALERT_WINDOW` via `appops` (through `libsu`) instead of sending the
  user to Settings, and `OverlayPopupService` draws the same accept/decline
  card as a `TYPE_APPLICATION_OVERLAY` window that appears instantly,
  layered on top of the notification. Non-rooted devices simply never
  trigger this path.

## Building

```bash
export ANDROID_HOME=/path/to/android-sdk   # cmdline-tools + platform-tools
                                            # + platforms;android-34 +
                                            # build-tools;34.0.0
./gradlew :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`. Verified building a real
debug APK in this environment (no device/emulator available here to install
or run it on).

### Pointing at your server

`BuildConfig.API_BASE_URL` defaults to `http://10.0.2.2:3000` (the Android
emulator's loopback to the host machine, for testing against `npm run dev`
in `../androdrop`). Override at build time:

```bash
./gradlew :app:assembleDebug -PapiBaseUrl=https://your-deployment.vercel.app
```

## Known gaps

- Not run on a real device or emulator in this environment — only verified
  to compile and package correctly (`assembleDebug`). Worth a real device
  pass before relying on it, especially the notification/overlay/root paths
  and content-Uri file resolution (`util/FileUtils.kt`), which are the parts
  most likely to be device/OEM-sensitive.
- No app icon polish beyond the adaptive-icon vector reusing the web app's
  glyph (`res/drawable/ic_launcher_foreground.xml`).
- No instrumented/unit tests yet.
