# MBHaxe → PS Vita: porting notes

Status: **the actual game (`marblegame.c`, not a toy) compiles and links
clean into a native `arm-vita-eabi` ELF, against every native module it
needs.** A vitasdk cross toolchain was built from source; `libhl`
(HashLink's runtime), `hlsdl`+vitaGL (rendering), `hlopenal` (audio, over a
real Vita OpenAL backend), and `fmt` (image/audio codecs) all compile
clean, most with zero source changes; networking (`datachannel`/`uv`/`ssl`)
is stubbed out rather than ported. The full Haxe → hlc-C →
`marblegame.elf` pipeline has been run end to end in-session, and a
separate trivial "hello world" was carried all the way through packaging
into a real, installable `.vpk` proving that half of the pipeline too (see
below). **What hasn't been verified: whether the real game actually runs
on hardware** — there's no Vita in this environment, so linking clean is
as far as this got. That's the next thing to check, not a "does it
compile" question anymore.

Base: `mbu-port` branch of RandomityGuy/MBHaxe (Marble Blast Ultra), vendored
1:1 into `mbhaxe-vita/` one level up from this file.

## How the game actually builds (relevant background)

MBHaxe doesn't run on a Haxe VM at runtime. The desktop/UWP builds all follow
the same pipeline (see `.circleci/config.yml`):

1. `haxe compile-<platform>.hxml` compiles the Haxe source (using the
   `heaps`, `hlsdl`, `colyseus-websocket`, `datachannel` haxelibs) down to
   **one big generated C file**, `native/marblegame.c`, via HashLink's `hlc`
   backend. No JIT, no bytecode interpreter involved at runtime.
2. That C file is compiled and linked, in one translation unit, against:
   - `libhl` (the HashLink runtime: GC, threads, core types)
   - a handful of "hdll" modules that are just shared libraries exposing
     HashLink native primitives: `sdl` (hlsdl - windowing/GL context/input),
     `openal` (audio), `ui` (native dialogs), `fmt` (image/video decode),
     `uv` (libuv, used for networking), `ssl`, `datachannel` (WebRTC, for
     multiplayer)
   - on desktop these are literally linked as shared libs (`.hdll`/`.dylib`/
     `.dll`/`.so`); nothing is loaded dynamically at runtime via a plugin
     mechanism, they're resolved at link time.
3. `data/` (interiors, missions, sounds, textures...) ships alongside the
   binary and is read straight off disk through `hxd.fs.LocalFileSystem`
   (see `src/fs/TorqueFileSystem.hx` and `src/ResourceLoader.hx`).

This matters for Vita because homebrew Vita apps are one statically-linked
ELF (a `.self` inside a `.vpk`) — there's no equivalent of loading a `.dll`
at runtime. So **every one of those hdll modules has to become a static
library** built against vitasdk instead. That's the real work, not the
Haxe/game-logic layer.

## What genuinely doesn't need touching

Grepped the whole `src/` tree for platform conditionals — there are exactly
5 files (`ResourceLoader.hx`, `Settings.hx`, `Util.hx`, `gui/ImportExportGui.hx`,
`net/Net.hx`), and most of it is inert on a new platform by default:

- `ResourceLoader.hx`'s filesystem is `#if (hl && !android)` → `TorqueFileSystem(".", null)`
  by default (same as Linux/Windows). As long as the Vita binary's working
  directory is the VPK's `app0:` root with `data/` sitting next to it, this
  needs **no changes** — confirmed by reading the code, not assumed.
- Game logic, physics (`src/Marble.hx`, `src/collision`), DIF/DTS parsing,
  mission parsing, game modes, rewind, GUI — none of it is platform-gated.
  It's plain Haxe; if it compiles to C at all, it compiles the same on Vita.
- Touch controls already exist (`src/touch`) for the mobile ports (iOS/
  Android branches) and Steam Deck-style touch UI — the Vita's front
  touchscreen is a plausible fit if physical buttons aren't enough, though
  the Vita's face buttons + 2 sticks are enough to just play it like a
  normal platformer/controller game and touch may not be needed at all.

Two small **additive** edits were made in this vendor copy (both `#if vita`,
nothing else changed):
- `src/Util.hx` `getPlatform()` — returns `"Vita"`.
- `src/Settings.hx` `settingsDir` — points at `ux0:data/MBHAXEVITA` rather
  than `.` (cwd/`app0:`), since `app0:` is the read-only VPK contents;
  `ux0:data/<anything>` is the conventional writable location for Vita
  homebrew and is always the current install.

## What actually needs porting, in rough order of how much it blocks a booting game

### 1. HashLink runtime (`libhl`) — DONE, verified

Ported and compiles + links clean for `arm-vita-eabi`. The source lives at
`vita/hashlink/` in this repo (vendored from RandomityGuy's hashlink fork,
which is what MBHaxe actually builds against) — see
`vita/hashlink/NOTICE.md` for the exact file-by-file diff, all of it gated
behind `#ifdef HL_VITA` so no other platform's codepath changed. In short:
newlib has no `<uchar.h>`/`mmap`/`termios`/`fork`/`readlink`, and a few
socket/pthread entry points aren't implemented — each has either an
existing platform carve-out in hashlink's own code to extend (it already
special-cases `HL_IOS`/`HL_MAC`/`HL_CONSOLE`/`HL_TVOS` for most of these) or
a small newlib-native equivalent (`memalign` instead of `mmap`, etc).

This was verified for real, not just "it should work": built a from-source
vitasdk toolchain, compiled a trivial Haxe program through `hlc`, linked it
against this ported `libhl.a`, and packaged the result into an installable
`.vpk` with vita-toolchain's own tools (`vita-elf-create` →
`vita-make-fself` → `vita-mksfoex` → `vita-pack-vpk`). See
`vita/hello-test/` to reproduce this yourself, and "toolchain gotchas"
below for two real bugs hit along the way (not code problems — build
environment ones) worth knowing about before you rebuild vitasdk yourself.

### 2. Rendering — compiles and links against the real game; unverified on hardware

Heaps renders through `h3d` which on the `hl`/native target goes through an
OpenGL (desktop) context via `hlsdl`. The Vita's actual GPU API is
**sceGxm**, a low-level, very un-OpenGL-like API — the realistic path every
Vita homebrew OpenGL port takes, and the one used here, is **vitaGL**
(https://github.com/Rinnegatamante/vitaGL, built from source in-session
along with its own deps — vitaShaRK for runtime GLSL→sceGxm shader
compilation, math-neon, SceShaccCgExt — plus a vitasdk SDL2 build patched
with vitaGL's video backend, `libsdl-org/SDL` release-2.32.8 +
[vitasdk/packages' `sdl2_vitagl` patch](https://github.com/vitasdk/packages/blob/master/sdl2_vitagl/vitagl-backend.patch)).
All of that plus `hlsdl` (`libs/sdl/{sdl,gl}.c`, see
`vita/hashlink/NOTICE.md` for the `gl.c` diff) **compile and link clean
against the actual game** (`marblegame.c`, from `compile-vita.hxml`, not
just the hello-test) — confirmed by producing a fully linked
`marblegame.elf` in-session with zero undefined symbols. **What's not yet
verified: whether it runs.** No Vita hardware in this environment to test
against, and vitaGL only implements a subset of desktop GL (see
`vita/hashlink/NOTICE.md` for exactly which entry points are missing and
now `hl_error()` instead of link-failing) — whether Heaps' actual shader
and render-pass usage stays inside that subset in practice is the real
open question this doesn't answer. This is genuinely the next thing to
check, on real hardware, once you have a `.vpk` per "Build layout" below.

### 3. Audio — done, verified against the real game

`hlopenal` (`libs/openal/openal.c`) needed **zero changes** — it compiles
clean as-is against a real Vita OpenAL backend: kcat/openal-soft 1.19.1
patched with [isage's vita backend](https://github.com/isage/openal-soft)
(`openal-soft-1.19.1-vita-1.patch`, implements OpenAL over `sceAudio`
directly, not a hlopenal-level shim). Links clean into `marblegame.elf`
alongside everything else.

### 4. Image/audio codecs (`fmt.hdll`) — done, verified against the real game

`libs/fmt/{fmt,dxt,mikkt,sha1}.c` also needed **zero changes** — compiles
clean against standard vitasdk builds of zlib 1.3.2, libpng 1.6.58 (with
vitasdk's ARM NEON patch), libjpeg-turbo 3.2.0, libogg 1.3.6, and libvorbis
1.3.7, all built from source in-session using the same recipes
[vitasdk/packages](https://github.com/vitasdk/packages) uses. `minimp3`
and `mikktspace` (bundled single-header libs `fmt.c` needs) are vendored
into `vita/hashlink/include/`.

### 5. Networking (multiplayer) — soft blocker, stubbed out, links clean

Three native modules are networking-only and all genuinely out of reach
without porting a large external stack (libdatachannel/WebRTC, libuv,
mbedTLS) — each is stubbed instead, implementing its real HL primitive ABI
as harmless no-ops (nothing crashes; connects/handshakes/reads just always
fail or never complete) rather than editing the Haxe source that calls
them:
- **`vita/stubs/datachannel.c`** — WebRTC (`datachannel.hdll`). Used
  unconditionally by `Main.hx`/`net/Net.hx`/`net/ClientConnection.hx`, not
  behind any `#if` on any platform (even the mobile ports link a real
  WebRTC stack). `RTC.init()` "succeeds", but `create_peer_connection`
  always hands back a null handle and nothing ever calls back into Haxe.
- **`vita/stubs/uv.c`** — libuv (`uv.hdll`). Only reachable here through
  `hl.uv.Fs` (asset hot-reload — already disabled, see
  `ResourceLoader.hx`'s `LIVE_UPDATE = false`) and whatever TCP path
  colyseus-websocket might reach for.
- **`vita/stubs/ssl.c`** — mbedTLS (`ssl.hdll`). Only reachable through
  whatever TLS colyseus-websocket/matchmaking might want.

**Multiplayer will not work** on this build. Getting it working would mean
either porting libdatachannel (its own multi-week project) or writing a
real Vita transport (ad-hoc Wi-Fi/LAN sockets over `sceNet` would be a far
smaller project than full WebRTC, if LAN-only play is an acceptable v2
goal) and swapping the stub back out — same for `uv`/`ssl` if colyseus
ends up needed for anything real.

### 6. `ui.hdll` (native dialogs) — done, used as-is

`libs/ui/ui_stub.c` is hashlink's own pre-existing no-op stub for platforms
without native dialogs (Windows has a real `ui_win.c`; everyone else
already gets this). Used unmodified, no Vita-specific work needed.

## Memory budget

The Vita has 512MB total RAM, split with the system; homebrew apps
typically get on the order of ~200–330MB depending on how the app declares
itself (and whether it's running under henkaku/enso vs. a "real" bubble).
`data/` in this vendor copy is ~73MB uncompressed — fine. The bigger
question is Heaps' texture/mesh memory usage at runtime (interiors + DTS
shapes decoded to GPU-resident data): worth instrumenting on desktop first
(there's likely a way to log texture memory through Heaps) before assuming
it fits, and looking at whether texture resolution needs turning down for
Vita specifically (the original game already ships original-Torque-era
assets, which may already be small enough).

## Controls

Physical Vita controls map naturally: left stick = movement (already how
KB+gamepad works today), right stick or the rear touchpad = camera, X to
jump. `src/touch` already has a touch-overlay UI for mobile; the Vita's
front touchscreen could reuse pieces of it if physical buttons ever feel
insufficient (they probably won't — this is a marble-rolling platformer,
not something touch-first).

## Toolchain gotchas (hit and fixed while verifying the above — read before rebuilding vitasdk from source)

If you're building vitasdk from [vitasdk/buildscripts](https://github.com/vitasdk/buildscripts)
yourself rather than using a prebuilt release, two things bit us that cost
real time to diagnose:

1. **Locale**: a bare-minimum container/CI image with `LANG=`/`LC_ALL=`
   unset (POSIX locale) fails partway through extracting the GCC source
   tarball with `Pathname can't be converted from UTF-8 to current locale`
   — GCC's own source tree has a handful of non-ASCII filenames (i18n
   testsuite files) that CMake's `libarchive`-based `tar` can't place
   without a UTF-8 locale. Fix: `export LC_ALL=C.utf8` (or any installed
   UTF-8 locale — check `locale -a`) before running the build.
2. **`gcc-final-target-libs` doesn't build by default.** buildscripts'
   `BuildGccFinal.cmake` deliberately builds gcc in two steps — `all-gcc`
   first (the compiler driver + `cc1`, before newlib exists to give it
   headers), then a *separate* `gcc-final-target-libs` step (`libgcc.a`,
   `crti.o`, `crtbegin.o`/`crtend.o`, `libstdc++`, `libgomp`) once newlib
   and pthread-embedded are installed. That second step is marked
   `EXCLUDE_FROM_MAIN` and nothing in the default `all` target depends on
   it, so a plain `cmake --build .` finishes with exit 0 and a compiler
   that *runs* but can't link anything (`cc1`/`cc1plus` get built as
   0-byte files if a build is interrupted mid-link, which then falsely
   look "up to date" to `make` on a resume — delete them and re-link if you
   see `posix_spawnp: No such file or directory` executing `cc1`). Fix:
   after the main build finishes, explicitly run
   `cmake --build . --target gcc-final-target-libs`.
3. Separately (not a vitasdk bug): this vitasdk build's own `<limits.h>`
   (the one GCC bundles in `lib/gcc/arm-vita-eabi/<ver>/include/`) doesn't
   `#include_next` newlib's real one, so `PATH_MAX` isn't visible through
   plain `#include <limits.h>` — worked around in `hashlink/src/std/sys.c`
   by including `<sys/syslimits.h>` directly under `HL_VITA` rather than
   trying to fix the toolchain itself.

## Build layout in this repo

- `vita/hashlink/` — the ported HashLink runtime (`libhl`) plus the native
  modules (`libs/sdl`, `libs/openal`, `libs/fmt`, `libs/ui`), all building
  and linking clean for `arm-vita-eabi`. See `vita/hashlink/NOTICE.md` for
  the file-by-file diff.
- `vita/hello-test/` — the toolchain smoke test: a trivial Haxe program
  proving the whole Haxe→hlc→libhl→`.vpk` pipeline in isolation. Actually
  built and packaged successfully; see its README to reproduce.
- `vita/BUILD.md` — **the real, proven build recipe for the actual game**
  (not the hello-test): every dependency (vitaGL + its own deps, SDL2+
  vitaGL patch, openal-soft+Vita patch, zlib/libpng/libjpeg-turbo/libogg/
  libvorbis) in the order they need building, then compiling and linking
  `marblegame.c` itself. This produced a fully linked `marblegame.elf` and
  a packaged `marblegame.vpk` in-session — this file is the reference for
  redoing that, exactly as it was actually done, not a guess.
- `../compile-vita.hxml` — Haxe→C generation step for the real game,
  `-D vita`, mirrors `compile-linux.hxml`. Produces
  `vita/native/marblegame.c` (gitignored, generated, not checked in — same
  treatment as `native/` on other platforms per the existing `.gitignore`).
  Ran clean in-session: all 953 expected `.c` files, no errors, ~4.5
  minutes for the native compile.
- `vita/stubs/{datachannel,uv,ssl}.c` — no-op native modules replacing the
  three networking-dependent hdlls, see the networking section above.
- `vita/CMakeLists.txt` — an earlier, still-unverified sketch of a real
  build system for this; `vita/BUILD.md`'s manual sequence is what's
  actually been run and works. Turning that into this file (or a Makefile)
  is worth doing before iterating further by hand, but wasn't done here.

## Suggested order of attack

1. ~~Get HashLink's runtime + native modules building for vitasdk.~~ Done —
   `vita/hashlink/`, and the actual game links clean against all of it,
   see `vita/BUILD.md`.
2. **Install the built `.vpk` on real hardware and see what happens.**
   This is the actual next step now — everything up to this point is
   "does it compile and link," and the real unknowns (does vitaGL's subset
   of GL cover what Heaps actually draws with, does the GC's `memalign`-
   based allocator behave under the Vita's real memory pressure, does
   audio actually come out of the speakers) only show up at runtime, which
   nothing in this environment could test.
3. Whatever the first crash/hang/black-screen turns out to be, work it
   like the compile errors were worked throughout this port: find the
   specific call site, check whether it's a vitaGL gap (extend the
   `VGL_NOT_SUPPORTED` list or find a workaround), a memory issue (check
   the "memory budget" section), or something more basic (a missing
   `data/` file, a wrong working directory).
4. Once a single interior renders: worry about gameplay correctness,
   input mapping (see "Controls"), and performance.
5. Only after single-player is solid: decide whether multiplayer is worth
   a `sceNet`-based transport, given libdatachannel itself is out of reach.
