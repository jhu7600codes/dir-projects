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
   layout matching a real device screenshot (fullscreen, controls up, one
   video tapped): a top bar (back / title / overflow) instead of a generic
   transport bar, and a *single* center play/pause - no flanking
   rewind/forward buttons (an earlier round added some off an ambiguous
   reference image that turned out to be a different feature's mockup,
   not the base player). Double-tapping either half of the screen does a
   10s skip (see the `GestureDetector` in
   `PlayerActivity.setUpPlayerTopBarAndGestures()`). The play/pause button
   itself is a solid white circle with a dark glyph
   (`drawable/bg_circle_white.xml`) rather than a bare icon with nothing
   behind it - that flat-icon-on-a-dim-scrim look is what stock,
   unstyled Media3/ExoPlayer controls look like by default, and was the
   main reason an earlier version of this screen still read as "generic
   ExoPlayer" rather than YouTube even after the top bar and layout were
   already custom. Reserved ids (`@id/exo_play_pause`, `@id/exo_progress`,
   etc.) must reference the *library's* pre-declared ids, never
   `@+id/exo_play_pause`, or PlayerControlView's Java code can't find them
   and the controls end up unwired and mispositioned (this was a real bug,
   caught from a device screenshot - see git history). Tapping the
   fullscreen button actually goes fullscreen now (rotates to landscape,
   hides the system bars, and expands the video off its normal 16:9 box to
   fill the screen - see `PlayerActivity.setFullscreen()`); previously the
   button was inert since nothing had ever registered
   `PlayerView.setFullscreenButtonClickListener`. Video descriptions and
   comments are rendered through `HtmlCompat.fromHtml(...,
   FROM_HTML_MODE_LEGACY)` rather than shown raw, since YouTube's innertube
   responses embed real HTML (`<br>`, `<a href>`) in that text.
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
8. **Mini player**: background playback above already kept a video going
   after leaving `PlayerActivity`, but there was no on-screen way to see or
   control it without reopening the full player - `MainActivity` now docks
   a small bar above the bottom nav (thumbnail, title, play/pause, close)
   whenever the shared `PlaybackService` session has something active,
   connecting its own `MediaController` to that same session
   (`MainActivity.connectMiniPlayer()`/`updateMiniPlayer()`) rather than
   tracking playback state itself. Hidden while the Shorts tab is showing,
   since that already renders its video full-bleed. Tapping it reopens
   `PlayerActivity` for whatever's playing, using the `MediaItem`'s
   `mediaId` - `StreamSelector.buildMediaItem()` now sets that to the
   original watch-page URL (plus title/artwork in `mediaMetadata`) for
   exactly this, alongside the stream URL itself.

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
  `/shorts/{id}` URLs the same way as `/watch?v={id}`. The per-item overlay
  (`layout/item_short.xml`) matches the classic (2021-era) Shorts chrome:
  channel avatar + name + a Subscribe pill above the title on the
  bottom-left, and a plain (no button-background) icon rail on the right -
  like/dislike/comment/share, count labels only where the feed response
  actually carries one (it doesn't carry per-item like/comment counts, so
  those stay hidden rather than showing a fake number). Tapping the
  channel row opens `ChannelActivity`; tapping comment opens the full
  player screen, which is where this app's actual comments UI lives. The
  bottom scrim (`drawable/scrim_bottom.xml`) had its gradient direction
  backwards - dark at the *top* of its 180dp strip instead of at the
  bottom screen edge - which showed as a hard dark line partway up the
  screen instead of a smooth fade behind the text; fixed by swapping the
  two stop colors. If a signed-in
  fetch comes back with zero videos, the fragment now shows an actual
  "no results" empty state instead of a blank black screen
  (`ShortsUiState.hasLoaded` in `ShortsViewModel.kt` distinguishes "still
  loading" from "loaded, but empty").
- **Signed-in Home/Subscriptions/Shorts returning zero items - root-caused
  and fixed.** A real device's `adb`/on-device-root logcat (tags
  `Innertube`/`InnertubeFeedClient`) turned up two distinct bugs:
  1. Home and Subscriptions were getting real `HTTP 200` responses with
     real signed-in data (`"logged_in":1"`, a real `datasyncId`) - but the
     response's own `context` string literally said
     `yt_web_unknown_form_factor_kevlar_w2w`: YouTube's web client has
     migrated these surfaces to a newer "Kevlar" view-model JSON schema
     (`lockupViewModel`/`shortsLockupViewModel`) that this app's parser
     didn't know about yet, only the classic `videoRenderer`/
     `reelItemRenderer` shape - so every item in an otherwise-successful
     response was silently skipped. `data/model/VideoUi.kt` now also parses
     `lockupViewModel` (regular videos) and `shortsLockupViewModel`
     (Shorts), alongside the old renderers, and `JsonWalk` gained
     `findFirstArray()` to support it.
  2. The Shorts tab's browse call used a guessed `browseId` of `"FEshorts"`,
     which comes back `HTTP 400 INVALID_ARGUMENT` - there's no standalone
     Shorts browse surface; the Shorts shelf only exists embedded inside
     the Home feed response. `InnertubeFeedClient.shorts()` now reuses the
     Home request and filters its result down to just the short-form items
     instead of calling a browseId that doesn't exist.
  `Innertube.post()` and `InnertubeFeedClient`'s per-call logging (tags
  `Innertube`/`InnertubeFeedClient`) is what surfaced both of these and is
  left in place for the next time YouTube reshuffles this.

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
