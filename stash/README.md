# stash

A personal music app for Android in Kotlin and Jetpack Compose. It streams from youtube and a few other sources, downloads for offline, and imports your Spotify / Apple Music playlists. Personal use only, not for the Play Store.

## What's in it

All six milestones from the plan are in this first build:

1. **Player + youtube search**: Media3/ExoPlayer in a `MediaSessionService`, so you get background playback, a media notification and lockscreen controls. Search goes through yt-dlp (`ytsearch`).
2. **Library**: Room db with saved songs, liked songs, playlists, and albums/artists grouped from your saved songs.
3. **Downloads**: a download button on every song, album and playlist. They run on a WorkManager queue with resume, retries (3 tries with exponential backoff), a wi-fi only toggle and a foreground progress notification. Files go to app storage (`files/music/`). The player always uses the local file if there is one.
4. **Fallback providers**: youtube → soundcloud → bandcamp → jamendo → internet archive → free music archive.
5. **Playlist import**: Spotify (OAuth PKCE login, pick playlists + liked songs) and Apple Music (public playlist link, or the catalog API if you add a MusicKit token). There's a progress screen and a "couldn't match" list you can fix by hand.
6. **Polish**: Material You dynamic color (wallpaper colors on Android 12+, lavender fallback below that), light/dark/system theme, and an adaptive icon with a monochrome layer for themed icons.

## UI

- The bottom nav works like Spotify's: **home / search / your library**, with a mini player above the nav bar.
- Home has Spotify-style quick-pick tiles, plus Google Play Music style "listen now" carousels: recently played, your playlists, on repeat, ready offline.
- Search has a "browse all" grid (tonal Material You tiles instead of Spotify's fixed colors), provider filter chips, and results grouped by source. Long-press any song (or tap ⋮) for the Spotify-style action sheet.
- Your library has filter chips: playlists, songs, albums, artists, downloaded.
- Playlist/album pages have a big centered cover like GPM, with tonal download and shuffle buttons and a big play button.
- Now playing is a full-height sheet tinted with colors from the album art (androidx palette). The art shrinks with a bounce when paused, the play button morphs between pill and squircle, and there's a queue view.

## How playback finds audio

The queue only holds `stash://track/<id>` uris. When ExoPlayer opens one, `PlaybackService` resolves it right then (`ResolvingDataSource`):

1. downloaded file, if there is one
2. the track's own provider. For yt-dlp sources that means `-f bestaudio` right before playback. The url is kept in memory for 15 minutes so seeking doesn't re-run yt-dlp, and is never stored.
3. the fallback that worked last time (cached on the track)
4. search every other enabled provider in order and match by title + artist + duration (`Matcher`), then remember the winner

yt-dlp updates itself every time the app starts. There's also an "update now" button in settings.

## Adding a provider

Implement `MusicProvider` (search, resolveStream, metadata, downloadSource) and add it to the list in `ProviderRegistry`.

## Setup on the phone

- **Jamendo**: get a free client id at devportal.jamendo.com and paste it in settings. Until you do, Jamendo is skipped.
- **Spotify import**: create an app at developer.spotify.com, add the redirect uri `stash://spotify-callback`, and paste its client id in settings. Then go to library → import → log in with Spotify. Heads up: Spotify's developer-mode rules changed in 2026. If the playlist calls start returning 403, it's on their side.
- **Apple Music import**: in Apple Music, share a playlist → copy link, then paste it in the importer (or share it straight to stash). A MusicKit developer token in settings is optional.

## Known limits / things to test

- Not tested on a device yet. It builds and the unit tests pass, but there was no emulator in the build environment.
- YouTube keeps changing things. If youtube songs fail, hit "update now" in settings first. Newer yt-dlp versions want a JavaScript runtime for some YouTube formats, which this bundle doesn't have, so some videos may only play through a fallback.
- Bandcamp search uses the site's own autocomplete API, which sometimes sends a bot check. Playback still goes through yt-dlp.
- FMA has no public API anymore, so its search results are scraped from the search page and may break if they redesign the site.
- Only arm64 (all recent phones) and x86_64 (emulator) builds are included. Each one bundles a whole Python for yt-dlp.

## Building

```
./gradlew :app:assembleDebug
```

compileSdk/targetSdk 35, minSdk 26, AGP 8.7.3, Kotlin 2.0.21 (compose compiler plugin), Hilt + KSP, Room, WorkManager, Media3 1.5.1, youtubedl-android 0.18.1. A prebuilt debug apk is in `dist/`.

`icon.svg` is the icon mockup and matches the vector drawables in `res/drawable/ic_launcher_*.xml`.

## Legal-ish

YouTube, SoundCloud and Bandcamp extraction is against their terms, so keep this private. Jamendo, Internet Archive and FMA are the open, CC-licensed ones.
