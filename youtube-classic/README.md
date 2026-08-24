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
   encrypted on-device (`auth/SessionManager.kt`). Settings previously
   always showed "Signed in as you" - the paste-cookie sign-in path
   (`LoginActivity.trySaveCookie()`) hardcoded a null account name instead
   of trying to read one at all. It now seeds `CookieManager` with the
   pasted cookies and loads youtube.com in the same WebView so it goes
   through the same account-name scrape the normal WebView login path
   uses, which itself now tries the avatar button's `aria-label`/alt text
   as a fallback when the `#account-name` selector doesn't match -
   unverified against a live page (this app doesn't touch any account
   credential to test with), but a strict broadening of what it can match
   rather than a narrowing.
3. **UI**: Home/Shorts/Subscriptions/Library bottom tabs, video list, player
   screen with comments, all styled off 2019 Android app screenshots (white
   top bar, red accents only, square thumbnails). Icons were redrawn
   against an actual period screenshot rather than from memory - Home and
   Library used to swap between an outline (unselected) and a differently-
   shaped filled (selected) drawable, which wasn't actually how the real
   app's tab icons work: a real APK teardown shows one plain white
   alpha-mask PNG per tab, tinted red/gray by selection state, never a
   different *shape*. Fixed to match - selected/unselected are the exact
   same traced path now, just recolored, for Home/Trending (Subscriptions
   is a fixed-red glyph that never changes color either way). `ic_share.xml`
   was the three-connected-nodes Android share icon; the real app's
   `ic_share.png` is the classic forward-arrow "swoosh" glyph instead. The
   brand red itself is `#CD201F`, not a flat `#FF0000` - pulled
   directly from `color/youtube_red` in a real YouTube 14.34.54 APK's
   compiled resources (`aapt dump --values resources`, not eyeballed off a
   screenshot) - and the Subscriptions/Library tab glyphs were retraced off
   that APK's actual `ic_tab_subscriptions.png`/`ic_tab_library.png` assets:
   Subscriptions gained the two horizontal bars above its play-box that
   were missing before, and Library turned out to be a plain folder
   silhouette with no play triangle in it at all, not the rounded
   play-box glyph this app had been using. The player's controls are a custom Media3 `PlayerControlView`
   layout, iterated against several real device screenshots rather than a
   generic transport bar:
   - The top row sits directly on the video with a light gradient scrim of
     its own (`drawable/scrim_top.xml`) behind white title/channel text and
     white back/captions/settings icons - not the plain black text this
     screen briefly had with no scrim at all, which turned out to be
     matched off a screenshot from a modified/patched YouTube build with a
     genuine display bug, not the real design.
   - The bottom info row + scrubber sits on its own solid black bar -
     `exo_position`/`exo_duration` plus a manually-updated current-quality
     label in one row above a full-width, edge-to-edge `exo_progress`,
     with `exo_fullscreen` at the row's end.
   - A *single* center play/pause, no flanking rewind/forward buttons (an
     earlier round added some off an ambiguous reference image that turned
     out to be a different feature's mockup, not the base player).
     Double-tapping either half of the screen does a 10s skip instead (see
     the `GestureDetector` in `PlayerActivity.setUpPlayerTopBarAndGestures()`).
     The button is a solid white circle with a dark glyph
     (`drawable/bg_circle_white.xml`) rather than a bare icon with nothing
     behind it - matching a real device screenshot cropped down to just
     that button - but PlayerControlView's Java overwrites `exo_play_pause`'s
     *icon* on every state change with its own bundled
     `exo_styled_controls_play`/`_pause` drawables (a solid circle with the
     glyph cut out of it as one path, not a plain glyph), regardless of
     this layout's `android:src` - so a black tint on that combined
     drawable rendered as an inverted black-circle-white-holes button
     sitting *inside* our own white circle background, instead of the
     intended white-circle-black-glyph look. `drawable/exo_styled_controls_play.xml`
     and `_pause.xml` in this app override those two library resource
     names outright (AGP's resource merger lets an app module's drawable
     of the same name win over a library's) with plain glyphs, so our
     background + tint actually apply the way the XML says.

   Reserved ids (`@id/exo_play_pause`, `@id/exo_progress`, etc.) must
   reference the *library's* pre-declared ids, never `@+id/exo_play_pause`,
   or PlayerControlView's Java code can't find them and the controls end up
   unwired and mispositioned (this was a real bug, caught from a device
   screenshot - see git history). Tapping the fullscreen button actually
   goes fullscreen now (rotates to landscape, hides the system bars, and
   expands the video off its normal 16:9 box to fill the screen - see
   `PlayerActivity.setFullscreen()`); previously the button was inert since
   nothing had ever registered `PlayerView.setFullscreenButtonClickListener`.
   The settings (gear) button opens a quality picker (`PlayerActivity.showQualityMenu()`)
   rather than the share/download menu it opened before - those two already
   have their own home in the action-chip row below the video, so nothing
   was lost by moving them out of here. The captions button
   (`PlayerActivity.toggleCaptions()`) toggles a real text track on/off via
   `Player.trackSelectionParameters`, using subtitle URLs NewPipeExtractor
   already exposes on `StreamInfo.subtitles` -
   `StreamSelector.buildMediaItem()` attaches them (unselected by default)
   to every `MediaItem` it builds, including on the merged video-only +
   audio-only playback path, which needed its own manual
   `SingleSampleMediaSource` merge since that path bypasses
   `DefaultMediaSourceFactory`'s automatic subtitle handling (see
   `PlaybackService.kt`). Video descriptions and comments are rendered
   through `HtmlCompat.fromHtml(..., FROM_HTML_MODE_LEGACY)` rather than
   shown raw, since YouTube's innertube
   responses embed real HTML (`<br>`, `<a href>`) in that text. The
   quality label reads the height ExoPlayer is *actually* decoding
   (`Player.Listener.onVideoSizeChanged`) rather than a guess made before
   playback starts - that guess never accounted for the HLS branch of
   `StreamSelector.buildMediaItem()` at all, so any video that played back
   via an HLS manifest showed a fixed, unrelated "360p" no matter its real
   resolution.
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
   "Skip" chip, plus segment markers on the seek bar. Reported as "doesn't
   work" without a live repro to debug against - `SponsorBlockClient` now
   logs the HTTP status/response body on a failed lookup and a warning when
   a hash-prefix bucket comes back with entries but none matching the
   video, and the fetch's exception was previously swallowed silently
   (`runCatching { }.getOrDefault(emptyList())` with no logging on
   failure) - all real, but unverified without a live logcat capture. See
   `PlayerViewModel`/`SponsorBlockClient` tags in logcat if this is still
   silent next time.
7. **Background/offline playback**: a `MediaSessionService`
   (`playback/PlaybackService.kt`) keeps ExoPlayer alive independent of the
   Activity; video-only + audio-only adaptive streams are stitched back
   together by a custom `MediaSource.Factory` on the service side (the
   video/audio pairing is passed across the MediaController boundary via
   `MediaItem.requestMetadata.extras`, the one part of a MediaItem that
   actually survives session IPC). Downloads
   (`download/DownloadService.kt`) pull both tracks to disk and mux them
   into one playable mp4 with Android's `MediaMuxer` - no ffmpeg/native code.
   The system media notification no longer shows previous/next-track
   buttons - a prepared ExoPlayer reports those as available commands by
   default even with nothing to skip to, and Media3's notification shows
   whatever the player reports; `PlaybackService`'s `MediaSession.Callback`
   now strips them from what's exposed to any controller (in-app controls
   never used them either way, so this only affects that notification).
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
9. **2016 UI toggle**: Settings → Appearance → UI style switches between
   the 2018-2019 chrome above and a second one modeled on the app's
   pre-Sept-2016 look - a solid red app bar with a tab-icon strip
   (Home/Trending/Subscriptions/a star tab) directly under it instead of a
   bottom nav, and no Shorts tab (Shorts didn't exist yet). Built as a
   whole separate Activity (`ui/MainActivity2016.kt` +
   `layout/activity_main_2016.xml`) sharing the same Fragments/ViewModels
   as `MainActivity`, rather than one Activity branching on the setting -
   the two chrome styles differ enough (top strip vs. bottom nav, a
   dynamic per-tab title, no Shorts) that keeping them separate stays far
   more readable. `MainActivity` is still the actual launcher Activity; it
   (and `MainActivity2016`) redirect to each other in `onCreate()`/
   `onResume()` based on the pref, so switching either direction from
   Settings and backing out takes effect immediately without relaunching
   the app. Every real detail (the app bar's `#DD0000` red, the tab strip
   position, and the Home/Trending/Subscriptions/star icon shapes) came
   from two real sources rather than a guess: a GSMArena article's
   screenshot of this exact redesign, and a teardown of a real YouTube
   v6.0.13 (2015-2016) APK from the Internet Archive's "YoutubePreV10"
   collection - see `ic_2016_tab_*.xml` and `MainActivity2016`'s kdoc for
   specifics, including a real surprise: that era's fourth tab icon
   (`ic_tab_library.png` in that old APK) is genuinely a star, not a
   folder or a play-box. One deliberate scope cut: the real Trending tab
   back then also showed a row of colored category chips (Music/Gaming/
   News/Live) under the strip - left out since there's no real
   "trending by category" surface to back it with, rather than building
   a decoration with nothing behind it.

   Building this surfaced a real, previously-unnoticed bug in
   [MainActivity]'s own bottom nav, not just this new screen: its selected
   tab's *icon* never actually turned red, only its label text did,
   because `Widget.YtClassic.BottomNav`'s parent Material style
   (`Widget.MaterialComponents.BottomNavigationView`) sets its own default
   `itemIconTint`, and simply not mentioning that attribute in the app's
   own style doesn't clear it - it silently kept recoloring every icon
   regardless of the selected/unselected drawable this app was already
   using. Fixed by setting `itemIconTint` to `@null` explicitly. The 2016
   tab strip had its own version of "selection barely visible" for a
   different reason: its unselected tint was a literal ~40%-black alpha
   value traced off the real (light-only, pre-dark-mode) app, which reads
   as near-invisible black-on-near-black once this DayNight theme is
   actually in dark mode - swapped to the existing theme-aware
   `yt_nav_unselected` token instead. Also gave `MainActivity2016` its own
   theme (`Theme.YtClassic.Main2016`) so its status bar is actually the
   dark red `yt_2016_status_bar` color that existed as a resource but was
   never wired to anything, rather than silently inheriting the plain
   white/black one.

## Known gaps / where to look if something's off

- **Channel avatars weren't loading on Home/Subscriptions/Shorts** - all
  four of the innertube-JSON-based `VideoUi` mappers in
  `data/model/VideoUi.kt` (`videoRenderer`, `reelItemRenderer`,
  `lockupViewModel`, `shortsLockupViewModel`) hardcoded
  `channelAvatarUrl = null`; only the NewPipeExtractor-based path (search/
  trending/channel tabs) ever populated it. `videoRenderer` now reads the
  real, stable `channelThumbnailSupportedRenderers` field; `lockupViewModel`
  reads it via a generic `JsonWalk` search for an `avatarViewModel` node
  (exact nesting isn't consistently documented, unlike `videoRenderer`'s).
  Shorts items (`reelItemRenderer`/`shortsLockupViewModel`) still don't -
  the real Shorts shelf card design doesn't show a channel avatar either,
  so there was nothing to wire up there.
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
