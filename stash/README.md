# stash

A personal music app for Android in Kotlin and Jetpack Compose. It streams from youtube and a few other sources, downloads for offline, and imports your Spotify / Apple Music playlists. Personal use only, not for the Play Store.

## What's in it

All six milestones from the plan are in this first build:

1. **Player + youtube search**: Media3/ExoPlayer in a `MediaSessionService`, so you get background playback, a media notification and lockscreen controls. Search shows one list of songs from every source (duplicates removed, best matches first). YouTube search uses YouTube Music's own search API (well under a second), with yt-dlp's `ytsearch` as the fallback. The next song's stream is looked up while the current one plays, so there's no gap between songs.
2. **Library**: Room db with saved songs, liked songs, playlists, and albums/artists grouped from your saved songs.
3. **Downloads**: a download button on every song, album and playlist. They run on a WorkManager queue with resume, retries (3 tries with exponential backoff), a wi-fi only toggle and a foreground progress notification. Files go to app storage (`files/music/`). The player always uses the local file if there is one.
4. **Fallback providers**: youtube → soundcloud → bandcamp → jamendo → internet archive → free music archive.
5. **Playlist import**: Spotify (public playlist/album link, or CSV/zip files from exportify.net) and Apple Music (public playlist link, or the catalog API if you add a MusicKit token). There's a progress screen and a "couldn't match" list you can fix by hand.
6. **Polish**: Material You dynamic color (wallpaper colors on Android 12+, lavender fallback below that), light/dark/system theme, and an adaptive icon with a monochrome layer for themed icons.

## UI

- The bottom nav works like Spotify's: **home / search / your library**, with a mini player above the nav bar.
- Home has Spotify-style quick-pick tiles, plus Google Play Music style "listen now" carousels: recently played, your playlists, on repeat, ready offline.
- Search has a "browse all" grid (tonal Material You tiles instead of Spotify's fixed colors), provider filter chips, and results grouped by source. Long-press any song (or tap ⋮) for the Spotify-style action sheet.
- Your library has filter chips: playlists, songs, albums, artists, downloaded.
- Playlist/album pages have a big centered cover like GPM, with tonal download and shuffle buttons and a big play button.
- Now playing is a full-height sheet tinted with colors from the album art (androidx palette). The art shrinks with a bounce when paused, the play button morphs between pill and squircle, and there's a queue view.

## Moving playlists (background)

Imports are a queue saved in the database (`import_jobs`). `ImportWorker` runs it in the background with a foreground notification, so 25 playlists keep going with the app closed, and it picks up where it stopped if Android kills it. It matches 4 songs at a time, and a song that shows up in several playlists is only searched once (`match_cache`).

- **Order is kept exactly**: playlist rows are keyed by position, so duplicates are allowed like on Spotify, and songs that couldn't be found show up greyed out in their spot. Fixing one by hand puts it back in that spot.
- **Spotify liked songs** (exportify's `Liked_Songs.csv`) go into stash's liked songs in the same order, not into a new playlist.
- **Missing songs**: the import screen lists them as soon as the first one isn't found, and the notification expands to show the latest ones.

## Account / backup (Supabase)

Optional. Settings → account, then email + password; Supabase sends a confirmation email the first time. Playlists (in order, missing songs included) and liked songs are saved to the `stash` Supabase project (`library_items`, row level security so every account only sees its own rows). It backs up by itself about 20 seconds after anything changes. Logging in on a new phone restores playlists it doesn't have yet.

## Made for you

Everything runs on the phone. Your plays and likes pick the seeds, and YouTube Music's radio for a song (the "up next" list its own app builds) supplies similar songs.

- **daily mix 1–4**: one per top artist. Your songs by them and by artists the radio calls similar, alternating with new ones (max 2 per artist). Rebuilt once a day.
- **discover weekly**: 30 songs you don't have, from 5 of your favourite songs' radios. Songs several radios agree on come first. New every Monday.
- **repeat rewind**: songs you played 3+ times but not in the last 30 days.
- **because you liked …**: a home row of songs similar to your latest like.
- **start radio** on any song (long-press), and **autoplay** (settings → playback) keeps the music going with similar songs when the queue ends.

- **genre mixes** ("For you by Stash"): every artist's main genre comes from Deezer's public API (cached), and the genres you listen to most each get a mix of your favourite artists from that genre plus similar songs.
- **artist mixes** ("Overtonight Mix"): the artist's songs, yours and their popular ones, mixed with similar artists.

Mixes are normal playlists (origin `mix`), so they can be downloaded. They aren't part of the cloud backup, since every phone rebuilds them.

## Home greetings

The title follows the time of day, with a few variants each. With "greet me by place" on (settings, asks for location), the line under it follows where you are: airport, cafe, station or train (offers your most common playlist artist), driving, gym, library, and so on. The place comes from one location fix plus OpenStreetMap's Overpass API, cached for 10 minutes.

## Covers

Saved songs get their real album cover instead of a YouTube video thumbnail. For Spotify imports the cover comes from Spotify itself (public oEmbed, using the track URI from the link or the CSV). Everything else gets Apple Music's cover through the iTunes Search API. `CoverWorker` works through saved songs slowly in the background, because Apple allows about 20 lookups a minute.

## Download size

Settings live on the downloads screen. **High** keeps the best stream (about 130–160 kbps). **Balanced** (the default) picks YouTube's ~70 kbps Opus stream and re-encodes other sources to 96 kbps AAC. **Data saver** goes down to about 50–64 kbps. Re-encoding uses media3 Transformer, so the phone's own encoder, no ffmpeg, and a file is only replaced if the result is actually smaller. "Shrink my downloads" re-encodes what's already on the phone in the background.

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
- **Spotify import**: no Spotify developer app needed (their Web API went premium-only in 2026). Either paste or share a public playlist/album link (read from Spotify's embed page, which only has the first 100 songs), or export from exportify.net and pick the CSVs or the "export all" zip in the importer. Exportify also covers private playlists and liked songs.
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
