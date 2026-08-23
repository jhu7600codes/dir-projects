# YouTube Classic

A from-scratch Android client (Kotlin) that recreates the 2018-2019 YouTube
app's UI - white chrome, bottom tab bar (Home / Shorts / Subscriptions /
Library), hard-edged thumbnails - on top of
[NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor) instead
of the official YouTube Data API. No API key, no OAuth client.

## Why no API key / OAuth

Everything that's normally an API call is either public-page scraping
(NewPipeExtractor) or a cookie-authenticated call to YouTube's own internal
`/youtubei/v1/*` endpoints, using a session captured by logging into
youtube.com in a plain embedded WebView - the same approach
[PipePipe](https://github.com/InfinityLoop1308/PipePipe) uses instead of
Google Sign-In. See `auth/LoginActivity.kt` and `network/Innertube.kt`.

## Project layout

```
app/src/main/kotlin/com/jhulian/android/youtube/classic/
├── YtClassicApp.kt          Application: NewPipe.init(), session/downloads init
├── extractor/                NewPipeExtractor plumbing (no auth needed)
│   ├── OkHttpDownloader.kt   The Downloader NewPipeExtractor requires to make HTTP calls
│   └── YouTubeRepository.kt  Coroutine wrappers: trending/search/streamInfo/comments/channel
├── auth/
│   ├── LoginActivity.kt      WebView login -> captures the youtube.com cookie jar
│   └── SessionManager.kt     Encrypted on-device storage for that cookie
├── network/                  Everything that needs the cookie session
│   ├── Innertube.kt          SAPISIDHASH auth + shared POST helper
│   ├── InnertubeActions.kt   like/dislike, subscribe, post a comment
│   ├── InnertubeFeedClient.kt Home/Subscriptions/Shorts feeds (browse endpoint)
│   └── SponsorBlockClient.kt  Public SponsorBlock API, k-anonymous hash-prefix lookup
├── playback/
│   ├── PlaybackService.kt    MediaSessionService - background/offline playback
│   ├── StreamSelector.kt     Picks HLS / progressive / video+audio-only streams
│   └── SponsorBlockController.kt  Auto-skip / manual-skip against playback position
├── download/                 Video+audio download -> single mp4 (MediaMuxer, no ffmpeg)
├── data/model/                UI models + mappers from NewPipeExtractor / innertube JSON
└── ui/                        MainActivity (tabs), player, search, shorts, channel, settings, downloads
```

## What's implemented

1. **Extraction**: search, trending, video/stream info, comments-with-replies
   all go through NewPipeExtractor (`extractor/YouTubeRepository.kt`) - no
   API key.
2. **Auth**: WebView cookie login (`auth/LoginActivity.kt`), persisted
   encrypted on-device (`auth/SessionManager.kt`).
3. **UI**: Home/Shorts/Subscriptions/Library bottom tabs, video list, player
   screen with comments, all styled off 2019 Android app screenshots (white
   top bar, red accents only, square thumbnails). Icons were redrawn
   against an actual period screenshot rather than from memory - Home and
   Library swap between dedicated outline/filled-red drawables by selection
   state, while Subscriptions is a fixed-red glyph that never changes
   color. The player's controls are a custom Media3 `PlayerControlView`
   layout matching the real 2019 chrome: a top bar (back / title /
   overflow) rather than a generic transport bar, a single center
   play/pause target, and no dedicated rewind/forward buttons - skipping
   back/forward 10s is a double-tap on either half of the screen (see the
   `GestureDetector` in `PlayerActivity.setUpPlayerTopBarAndGestures()`),
   same as the real app. Reserved ids (`@id/exo_play_pause`,
   `@id/exo_progress`, etc.) must reference the *library's* pre-declared
   ids, never `@+id/exo_play_pause`, or PlayerControlView's Java code can't
   find them and the controls end up unwired and mispositioned (this was a
   real bug, caught from a device screenshot - see git history). Video
   descriptions and comments are rendered through
   `HtmlCompat.fromHtml(..., FROM_HTML_MODE_LEGACY)` rather than shown raw,
   since YouTube's innertube responses embed real HTML (`<br>`, `<a href>`)
   in that text.
4. **Dark mode**: `Theme.YtClassic` is `DayNight` and follows the system
   theme; `values-night/` supplies the dark palette (colors, chip
   backgrounds, dividers, status bar) and icon tinting is driven off a
   single `@color/yt_icon` token that flips with it. Anything that sits
   *over media content* (thumbnail duration badges, the SponsorBlock skip
   chip, in-player text) intentionally stays hardcoded `@android:color/white`
   instead of using a theme-flipping token, since those sit on a
   thumbnail/video frame, not app chrome, and would go invisible in dark
   mode if they flipped too.
5. **Write actions**: like/dislike, subscribe/unsubscribe, and posting a
   top-level comment, all via cookie-authenticated innertube calls
   (`network/InnertubeActions.kt`). Comment *replies* are read-only for now.
6. **SponsorBlock**: segments fetched by SHA-256 hash-prefix (the same
   k-anonymity scheme the browser extension uses), spliced into playback via
   `playback/SponsorBlockController.kt` with both auto-skip and a manual
   "Skip" chip, plus segment markers on the seek bar.
7. **Background/offline playback**: a `MediaSessionService`
   (`playback/PlaybackService.kt`) keeps ExoPlayer alive independent of the
   Activity; video-only + audio-only adaptive streams are stitched back
   together by a custom `MediaSource.Factory` on the service side (the
   video/audio pairing is passed across the MediaController boundary via
   `MediaItem.requestMetadata.extras`, the one part of a MediaItem that
   actually survives session IPC). Downloads
   (`download/DownloadService.kt`) pull both tracks to disk and mux them
   into one playable mp4 with Android's `MediaMuxer` - no ffmpeg/native code.

## Known gaps / where to look if something's off

- **Home feed** falls back to Trending when signed out (there's no public
  "recommended for you" surface to scrape); when signed in it calls
  innertube's `browse` endpoint directly (`network/InnertubeFeedClient.kt`),
  which NewPipeExtractor deliberately doesn't cover.
- **Channel tabs**: uses NewPipeExtractor's tab-based channel API
  (`ChannelInfo.tabs` + `ChannelTabInfo`). This is the API surface most
  likely to have moved between NewPipeExtractor versions - if channel
  videos come back empty, check `YouTubeRepository.channel()` against the
  NewPipeExtractor version actually resolved in the build.
- **Comment posting params**: `InnertubeActions.postComment` fishes
  `createCommentParams` out of a throwaway `/next` call by walking the
  whole JSON tree (`util/JsonWalk.kt`) rather than modeling YouTube's exact
  (frequently-reshuffled) response schema. Subscribe/unsubscribe's opaque
  `params` blobs are reverse-engineered constants and are the most likely
  thing to need refreshing against a live browser capture if YouTube
  changes that flow.
- **PiP, reply posting, and a true multi-select SponsorBlock category
  picker** aren't wired up yet - the plumbing (settings keys, controller
  hooks) is there, the UI for them isn't.
- **Runtime correctness beyond compiling**: `:app:assembleDebug` and
  `:app:lintDebug` both pass clean, so the NewPipeExtractor/Media3 API
  surface used throughout (including the channel-tabs call and the merged
  video-only/audio-only playback path) is confirmed to compile against
  v0.26.5 and Media3 1.4.1. It hasn't been run on a device/emulator in this
  environment (no KVM available to run one), so behavior against live
  YouTube is verified by diffing `extractor/OkHttpDownloader.kt` line for
  line against NewPipe's own proven `DownloaderImpl` (real device reports
  had caught a real discrepancy there - see git history) rather than by
  an actual run. If extraction ever throws again, `Log.e` calls in
  `VideoListViewModel`/`PlayerViewModel` now put a full stack trace in
  logcat under those tags.
- **WebView Google sign-in**: Google's sign-in page blocks embedded
  WebViews outright ("This browser or app may not be secure"). The
  `X-Requested-With` header fix (`WebSettingsCompat.setRequestedWithHeaderOriginAllowList`)
  is applied, but Google's detection isn't limited to that one header and
  can still trigger regardless of client-side WebView tweaks - this is a
  moving target Google actively defends, not something any app can
  permanently guarantee past. Because of that, `LoginActivity` also has a
  guaranteed-to-work fallback: "Trouble signing in? Paste a cookie
  instead" opens a dialog to paste the `Cookie` header value copied from an
  already-signed-in browser tab (devtools' Network panel, or a cookie
  export extension) - the same mechanism yt-dlp's browser-cookie import
  relies on, and immune to the WebView block since no WebView is involved.
- **Shorts** has its own tab now (`ui/shorts/`), replacing Trending -
  Trending itself lives on as Home's signed-out fallback rather than a tab
  of its own. It's a real vertical, one-swipe-per-video feed
  (`RecyclerView` + `PagerSnapHelper`), with one shared `MediaController`
  attached only to whichever item is centered (see `ShortsAdapter`) rather
  than one ExoPlayer per item. Needs a session - there's no public/
  signed-out Shorts surface NewPipeExtractor or an anonymous browse call
  can reach, so it shows a sign-in prompt when signed out. Playback itself
  needs no special handling: NewPipeExtractor already resolves
  `/shorts/{id}` URLs the same way as `/watch?v={id}`. If a signed-in
  fetch comes back with zero videos, the fragment now shows an actual
  "no results" empty state instead of a blank black screen
  (`ShortsUiState.hasLoaded` in `ShortsViewModel.kt` distinguishes "still
  loading" from "loaded, but empty").
- **Signed-in Home/Subscriptions/Shorts returning zero items**: seen on a
  real device, not yet root-caused - reproducing it needs a live signed-in
  session and this environment has no emulator (no KVM) to test against.
  `network/Innertube.kt`'s request `buildContext()` was fleshed out with
  the fuller set of client fields a real WEB client sends (platform,
  browser/OS strings, form factor, etc.) and both `Innertube.post()` and
  `InnertubeFeedClient.browse()` now `Log.e`/`Log.w` the HTTP status and
  the first ~2000 chars of the response body whenever a call fails or
  comes back with zero items (tags `Innertube` / `InnertubeFeedClient`).
  If this is still happening, `adb logcat -s Innertube InnertubeFeedClient`
  while reproducing it is the fastest way to get a real answer instead of
  another guess.

## Building

Standard Android Studio / Gradle project - open the `youtube-classic/`
folder in Android Studio, or:

```
./gradlew :app:assembleDebug
```

Requires an Android SDK (compileSdk 34, build-tools 34.0.0) and a JDK 17.
Debug builds clean with `./gradlew :app:lintDebug` too (the one intentional
suppression, `UnsafeOptInUsageError`, is documented next to it in
`app/build.gradle.kts` - it's Media3's `@UnstableApi` surface, opted into
module-wide since the custom playback path in `playback/PlaybackService.kt`
has to reach into it).

## Legal

Unofficial, unaffiliated with Google/YouTube. It only does what a browser
logged into youtube.com can already do - no bypassing of paid features, no
ad-server spoofing, no bulk scraping infrastructure. SponsorBlock segment
data comes from https://sponsor.ajay.app's public API and belongs to that
project and its contributors.
