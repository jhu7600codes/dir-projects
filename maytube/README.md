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
6. **Downloads** (`download/SabrFragmentDownloader.kt`, `Mp4TrackMuxer.kt`,
   `VideoDownloader.kt`, `ui/DownloadsActivity.kt`) — see "Why downloads
   pull fragments directly" below; short version: maytube fetches the same
   5-second SABR fragments the live player consumes, in parallel, straight
   from `/sabr_playback`, and remuxes them on-device with Android's
   `MediaMuxer` — instead of the much slower path of asking the *server* to
   silently rebuild the whole file first. Falls back to yt2009's
   `/exp_hd`/`/get_480` + `DownloadManager` if the fast path fails for any
   reason. Results land in a Downloads screen, opened via `FileProvider` +
   `ACTION_VIEW` so any installed video player can play them back offline.

### Why downloads pull fragments directly instead of using /exp_hd

The first version of this did the "obvious" thing: point Android's
`DownloadManager` at yt2009's `/exp_hd`/`/get_480` resolver endpoints. Those
are simple and reliable, but reading `back/yt2009sabr.js`'s `download()`
(what those endpoints call into via `yt2009_utils.saveMp4_android`) shows
why that's a bad idea for anything but a short clip: it walks the video
**serially**, one 5-second window at a time, with a hardcoded 150ms pause
between each request, and it only responds — the redirect `DownloadManager`
is waiting on — once ffmpeg has finished stitching the *entire* file
server-side. For a 30-minute video that's hundreds of sequential round
trips to Google's video CDN, with zero bytes and zero progress reaching the
device until all of them finish.

Since `back/yt2009html.js` embeds the live player's SABR session directly
in the watch page (`var sabrBase = "/sabr_playback?pid=...";`), maytube
fetches that same page itself, extracts the session, and pulls fragments
with its own concurrency (default 4 in flight) instead of the server's one
at a time. Consecutive fragments for a given track are just continuation
pieces of one fragmented MP4 (only the very first one carries the
ftyp/moov init segment), so they can be byte-appended in order with no
container knowledge needed — that mirrors the first of yt2009's own two
ffmpeg passes. The second pass (muxing the resulting video-only and
audio-only files into one, `-map 0:a -map 1:v -c copy` in yt2009's ffmpeg
command) is done with `android.media.MediaExtractor`/`MediaMuxer` — no
ffmpeg or native code needed, since combining two already-valid tracks into
one file is a standard, well-supported Android SDK pattern. This also means
real progress can be shown (fetched-so-far vs. total duration, parsed from
the watch page) instead of DownloadManager's silent wait.

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
- The fast SABR-fragment downloader is the highest-risk piece of new code
  here: it's built entirely from reading `back/yt2009sabr.js`,
  `back/backend.js`, and `assets/site-assets/html5-player.js`, not from
  running against a live instance (none was available in this environment).
  `SabrFragmentParser` has real JVM unit tests (`./gradlew
  testDebugUnitTest`) checked byte-for-byte against the documented wire
  format, and `Mp4TrackMuxer` uses well-trodden `MediaExtractor`/
  `MediaMuxer` APIs, but the end-to-end path (session parsing, concurrent
  fetch, remux against a *real* video) hasn't been exercised against an
  actual yt2009 server. It falls back automatically to the slower
  `/exp_hd`/`/get_480` + `DownloadManager` path (the previous
  implementation, which mirrors what the site's own resolver already does)
  if anything about the fast path throws, so a download should still
  succeed even if the fast path needs a fix once tried for real.
- Both download paths depend on `/exp_hd`/`/get_480`/`/sabr_playback` being
  reachable un-signed, which is the default. If an instance has
  `trusted_context` turned on in its yt2009 config, those requests will
  403/reject and the download action reports failure.
- Downloading a live stream is explicitly rejected (SABR sessions for lives
  don't carry a fixed duration the way VOD ones do).
- File uploads (`my_videos_upload`) aren't wired up (`onShowFileChooser` is
  not overridden) — out of scope for a viewing/playback-focused client.
- Cancelling an in-progress fast download stops new fragment fetches from
  starting but won't interrupt ones already in flight (OkHttp calls block
  the thread they're on); the dialog closes once those wind down.
- No automated Android instrumentation/UI tests; verification here was a
  real `assembleDebug`, `testDebugUnitTest`, and `lintDebug` build against
  the Android SDK, not a device run against a live yt2009 instance.
