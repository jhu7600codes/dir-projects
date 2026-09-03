# maytube-vita

A homebrew PS Vita client for a self-hosted yt2009 instance (this
project's own `maytube` sibling app is the Android version of the same
idea) -- browse a server's `/videos` list and play a video, with playback
built on yt2009's SABR adaptive-streaming protocol.

Built and tuned primarily for the **original ("phat") PCH-1000 Vita**, per
the project brief. It runs on Slim (PCH-2000) too -- the clock-speed boost
in `main.c` is safe on both -- but phat is what v1 targets.

## What this is (v1 scope)

- **Browse**: scrapes the server's `/videos` page (no login/history in v1)
  and shows a scrollable title list.
- **Buffer-then-play**: picks a video, fetches *every* SABR fragment for it
  into two local files (video-only + audio-only -- SABR delivers real
  fragmented MP4 per track, so no muxing step is needed), showing a
  progress bar while it downloads, then plays the two finished files with
  ffmpeg decode + SDL2 present, audio-clock-synced.
- **Controls**: D-Pad to move the selection, X to select/confirm, Circle to
  stop playback and return to the list, Start to refresh the list.

This intentionally mirrors this project's *own* Android history: the
Android app started with a simple "wait for the whole thing, then play it"
`PlayerActivity`, and only later grew into `StreamingPlayer`'s true
incremental live streaming (fetch and play concurrently, via a
growing-file `DataSource` fed into ExoPlayer). maytube-vita v1 is
deliberately the *earlier, simpler* shape of that same idea, not the later
one -- see "Known limitations" for why.

## What's verified, and what isn't

This environment has no real Vita hardware or emulator, so verification
tops out differently for different parts of this codebase:

| Module | How it's verified |
|---|---|
| `sabr.c` (SABR wire-format + clock parsing) | **Host unit tests** (`test/sabr_test.c`, 17+ assertions) *and* cross-compiles clean for the Vita target |
| `scrape.c` (video list extraction) | **Host unit tests** (`test/scrape_test.c`) against a fixture built from yt2009's actual `videoCell()` template shape, *and* cross-compiles clean |
| `fetch.c` (fetch-loop orchestration) | Its pure termination logic (`fetch_should_continue`) has **host unit tests** (`test/fetch_test.c`); the full fetch loop (network I/O) cross-compiles clean but isn't host-testable |
| `http.c`, `player.c`, `main.c` | **Cross-compile clean** for the real `arm-vita-eabi-gcc`/VitaSDK target, full app links and packages into a real, well-formed `.vpk` -- but **not run on real hardware or an emulator**, so runtime behavior (does it actually decode/play/render correctly on a Vita) is unverified |

Run the host tests:

```sh
./test/run.sh
```

In short: the trickiest, most bug-prone logic (the SABR fragment format
itself, which nothing else in this codebase can fall back on if it's
wrong) has real automated test coverage. The SDL2/ffmpeg presentation
layer is standard, carefully-written ffmpeg+SDL2 boilerplate that compiles
and links correctly against the real target -- but "compiles clean" is a
materially weaker guarantee than "tested," and this project can't do
better than that without a Vita or a working emulator to run it on. If
something in `player.c` or `main.c` doesn't work right on real hardware,
that's the most likely place.

## Building

Requires [VitaSDK](https://vitasdk.org/) (`vdpm` packages: `sdl2`,
`sdl2_ttf`, `curl` (or `curl-mbedtls`), `ffmpeg`, `freetype`, `mpg123`
-- `vdpm pacman --noconfirm -S <name>` installs each).

```sh
mkdir build && cd build
cmake -DCMAKE_BUILD_TYPE=Release ..
make
```

Produces `build/maytube-vita.vpk`. Install it the usual homebrew way
(VitaShell, or a QCMA/FTP transfer + a package installer).

## Setting up on the Vita

v1 has no on-screen keyboard or in-app settings UI (see "Known
limitations"), so two files need to be dropped onto the memory card by
hand before first launch, both under `ux0:data/maytube/`:

- **`config.txt`** -- one line, your server's address, e.g.
  `http://192.168.1.20:3000` (the same address the Android app's Settings
  screen asks for).
- **`font.ttf`** -- any TTF font file, used to render the video list and
  status text. Not bundled here due to font licensing; bring your own
  (e.g. a copy of DejaVu Sans, or any other freely redistributable TTF you
  have on hand).

## Known limitations

- **Buffer-then-play, not live streaming.** The whole video downloads
  before playback starts, and there's no cancel button once a fetch is
  underway -- wait for it to finish (or hard-fail) before doing anything
  else. True incremental streaming (start playing before the fetch
  finishes, like the Android app's `StreamingPlayer`) would need ffmpeg
  fed via a custom blocking `AVIOContext` plus manual dual-track A/V sync,
  since there's no `MergingMediaSource` equivalent outside of ExoPlayer --
  a real follow-up, not attempted in this pass.
- **Serial fragment fetches**, not the Android client's concurrency-3
  fetch loop -- correctness over throughput, since nothing here could be
  verified against a real device's actual network conditions.
- **No on-screen keyboard / in-app settings, no search, no history/login,
  no channel pages, no comments.** Just a flat `/videos` list and a
  player. All of these are reasonable v2 candidates.
- **No fetch cancellation.** Once you press X on a video, the buffering
  step runs to completion (or fails) before you get control back.
- **One video buffered at a time** -- `video.mp4`/`audio.mp4` under
  `ux0:data/maytube/` are overwritten on every fetch, no local library.
- **Live videos aren't supported** (same restriction as the Android app's
  SABR path -- `fetch_video()` returns an error for a session flagged
  live).

## Project layout

```
src/
  http.c/.h     libcurl wrapper (+ Vita sceNet bring-up)
  sabr.c/.h     SABR session resolve + fragment wire-format parsing
  scrape.c/.h   /videos page -> video id + title list
  fetch.c/.h    fetch-loop orchestration (buffer-then-play)
  player.c/.h   ffmpeg decode + SDL2 present, audio-clock A/V sync
  main.c        SDL2 app: input, state machine, rendering
test/           host-runnable unit tests for the pure logic modules
CMakeLists.txt  full cross-compile + .self/.vpk packaging
```
