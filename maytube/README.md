# maytube

An Android WebView client for [yt2009](https://github.com/ftde0/yt2009), a
self-hosted retro (~2009) YouTube frontend/backend. Instead of reimplementing
yt2009's UI or its SABR video pipeline natively, maytube loads your instance
straight into a WebView and injects CSS/JS to make the desktop-oriented site
usable on a phone. Playback, search, browsing, accounts and recommendations
all come from the site itself — maytube's job is the wrapper: connecting to
your instance, making it fit a phone screen, and adding a couple of
native conveniences (a settings screen and offline downloads) that don't
otherwise exist in a plain browser tab.

## Why WebView, and why this actually works for SABR

yt2009 enables SABR/MSE streaming client-side entirely inside
`assets/site-assets/html5-player.js`, gated purely by whether the raw
`Cookie` header sent to the server contains the substring `exp_sabr`
(`back/yt2009html.js`, `back/backend.js` — it's a literal
`req.headers.cookie.includes("exp_sabr")`, not a structured flag). The
server side (`back/yt2009sabr.js`) does the actual SABR negotiation with
Google's video servers and streams the response back over a custom
`/sabr_playback` endpoint; the page's own JS parses that response and feeds
it into a `MediaSource`/`SourceBuffer` pair. Since Android's WebView is a
full Chromium engine with MSE support, none of that needs to be
reimplemented — maytube only has to make sure:

1. The `exp_sabr` cookie substring is present before the first request
   (`MaytubeWebViewClient` + `CookieManager`, re-asserted via injected JS on
   every page in case a page resets `document.cookie`).
2. JavaScript, DOM storage, and mixed content are enabled, and cleartext
   HTTP is allowed (self-hosted instances are almost always plain HTTP on a
   LAN IP).

Accounts work the same way: yt2009's "sign in" (`signin.htm`) doesn't
validate a password against anything — it just sets a `login_simulate<name>`
cookie flag (see the inline `login()` script in `signin.htm`) that the rest
of the site reads to decide what "signed in" content (recommendations,
comment identity, etc.) to show. Persistent cookies in the WebView session
are therefore the entire "account" story; there's no separate auth flow for
maytube to implement. (There's also a much heavier "connect your real
YouTube account" flow via `/mh_pc_intro` involving manually extracting an
OAuth token from a browser's network inspector — that's a deliberately
manual, PC-only power-user feature in yt2009 itself, out of scope for a
mobile client wrapper.)

## What's implemented

1. **Settings screen** (`ui/SettingsActivity.kt`) — enter the instance's
   IP/hostname + port, optionally HTTPS, persisted via SharedPreferences
   (`data/ServerConfig.kt`). Shown automatically on first run.
2. **Mobile CSS/JS injector** (`webview/MobileInjector.kt`) — injects a
   viewport meta tag and a stylesheet on every page load that collapses
   yt2009's fixed-960px two-column desktop layout to a single responsive
   column, and turns the HTML5 player container into a responsive 16:9 box
   (via `!important` rules, which per the CSS cascade override the inline
   pixel-width styles `html5-player.js` sets).
3. **SABR playback** — see above; `MobileInjector.flagCookieValue()` builds
   the flag cookie, `MainActivity`/`MaytubeWebViewClient` apply it through
   `CookieManager` before load and re-assert it via JS after.
4. **Search + browse** — comes for free from the site; the injected CSS is
   intentionally conservative (structural selectors + a few known IDs) so it
   reflows layout without touching functional JS/DOM.
5. **Accounts/recommendations** — persistent WebView cookies
   (`CookieManager.setAcceptCookie` + `.flush()` on pause) so signing in via
   the site's own `/signin` page sticks across app restarts.
6. **Downloads** (`download/VideoDownloader.kt`, `ui/DownloadsActivity.kt`) —
   yt2009's `/exp_hd` and `/get_480` endpoints (`back/backend.js`) do a
   server-side SABR/DASH-to-MP4 reconstruction (`yt2009_utils.saveMp4_android`)
   and redirect to a plain static `/assets/<id>-<quality>.mp4` file once
   it's ready — a normal progressive MP4, unlike the fragmented stream the
   live player consumes. maytube points Android's `DownloadManager` at that
   resolver endpoint (which follows the redirect itself) and lists the
   result in a simple Downloads screen, opened via `FileProvider` +
   `ACTION_VIEW` so any installed video player can play it back offline.

## Setup

1. Open this project (the `maytube/` directory) in Android Studio, or build
   from the command line with `./gradlew assembleDebug`.
2. Install on a device/emulator that can reach your yt2009 instance (same
   LAN, VPN, etc).
3. On first launch, enter your instance's IP/hostname and port. maytube
   loads `http://<host>:<port>/`.
4. From the toolbar menu you can reopen Settings, force-reload, jump back
   home, download the currently open video, or view past downloads.

This was verified against a locally installed Android SDK
(`compileSdk 34`, `minSdk 24`) — `./gradlew assembleDebug` and
`./gradlew lintDebug` both succeed cleanly.

## Known limitations / follow-ups

- The CSS injector is deliberately general (structural rules + a handful of
  known container IDs gathered by reading yt2009's templates) rather than
  hand-tuned per page. It hasn't been visually verified against a live
  instance in this environment (no running yt2009 server + real device were
  available here) — expect some pages to need small selector tweaks once
  tried against a real instance.
- Downloads depend on `/exp_hd` / `/get_480` being reachable un-signed,
  which is the default. If an instance has `trusted_context` turned on in
  its yt2009 config, those resolver requests will 403 and the download
  action will just report failure.
- File uploads (`my_videos_upload`) aren't wired up (`onShowFileChooser` is
  not overridden) — out of scope for a viewing/playback-focused client.
- No automated Android instrumentation/UI tests; verification here was a
  real `assembleDebug` + `lintDebug` build against the Android SDK, not a
  device run against a live yt2009 instance.
