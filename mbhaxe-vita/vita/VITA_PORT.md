# MBHaxe → PS Vita: porting notes

Status: **the toolchain and runtime layer are real and verified; rendering
is unstarted.** A vitasdk cross toolchain was built from source, `libhl`
(HashLink's runtime) was ported and actually links clean for
`arm-vita-eabi`, and a full Haxe → hlc-C → native-ELF → `.vpk` pipeline was
run end to end producing a real, installable package (see "hello world"
below — it's a toolchain smoke test, not the game). The Haxe/game-logic
layer compiles unmodified. What's genuinely still unstarted is rendering
(vitaGL) and audio — see the numbered sections below, now ordered by what's
actually still open rather than by pure guesswork.

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

### 2. Rendering — the actual remaining hard blocker

Heaps renders through `h3d` which on the `hl`/native target goes through an
OpenGL (desktop) context via `hlsdl`. The Vita's actual GPU API is
**sceGxm**, a low-level, very un-OpenGL-like API. Writing an sceGxm backend
for Heaps directly is a large project (shader translation, the whole
render-target/fragment-program model is different).

The realistic path every Vita homebrew OpenGL port takes: **vitaGL**
(https://github.com/Rinnegatamante/vitaGL) — an OpenGL ES-ish shim over
sceGxm, paired with `vitashark`/`vitaShaRK` for runtime GLSL→sceGxm shader
compilation. `hlsdl`'s SDL2 window/GL-context creation would need a Vita
branch that talks to vitaGL's context setup instead of SDL2's normal GL
path (or use the vitasdk SDL2 port, which some vitaGL-based ports already
pair together). This is the single biggest unknown in the whole port —
Heaps' shader output (`src/shaders`, `hxsl`-generated) needs to actually
run correctly through vitaGL/vitashark, and that's untested here.

### 3. Networking (multiplayer) — soft blocker, stubbed out

`datachannel` (WebRTC via libdatachannel) is used unconditionally by
`Main.hx`/`net/Net.hx`/`net/ClientConnection.hx` — it's not behind any
`#if`, on any platform (even the mobile ports link a real WebRTC stack).
Porting libdatachannel (+ usrsctp, libjuice, mbedTLS/OpenSSL, plog) to
vitasdk is its own multi-week project, and not worth blocking a
single-player-first build on.

Rather than editing `Net.hx`/`Main.hx`/`ClientConnection.hx` (which would
make this vendor copy diverge from upstream in a way that's annoying to
keep in sync), **`vita/stubs/datachannel.c`** implements the exact same
HashLink primitive ABI hxDatachannel exposes, as no-ops: `RTC.init()`
"succeeds" (so nothing crashes at startup), but `create_peer_connection`
always hands back a null handle and nothing ever calls back into Haxe
(no `onOpen`/`onDataChannel`/etc.), so a host/join attempt just does
nothing rather than segfaulting. **Multiplayer will not work** on this
build until someone either ports libdatachannel or writes a real Vita
transport (ad-hoc Wi-Fi/LAN sockets over `sceNet` would be a far smaller
project than full WebRTC, if LAN-only play is an acceptable v2 goal).

Colyseus (`colyseus-websocket`, used for matchmaking/lobby presumably) has
the same problem in spirit — a `sceNet`-backed WebSocket implementation
would be needed; not investigated yet.

### 4. Audio — needs investigation

MBHaxe uses `hlopenal` (OpenAL) via HashLink. OpenAL doesn't have a
straightforward vitasdk port as of writing. Options, not yet evaluated:
- Write a minimal Vita `hlopenal`-compatible backend on top of `sceAudio`/
  `sceAudioOut` (Vita's native audio output) exposing just the subset of
  OpenAL entry points `hlopenal`'s HL primitives actually call.
- Check whether any existing homebrew project has already done an
  OpenAL-on-Vita shim (worth a search before writing one from scratch).

### 5. Other native deps — likely fine via vitasdk's package manager (vdpm)

`zlib`, `libpng`, `libjpeg-turbo`, `libogg`, `libvorbis`, `mbedtls` (for
`ssl.hdll`) all have existing vitasdk-pacman (`vdpm`) packages. `fmt.hdll`
and `ui.hdll` (native file dialogs) need checking — `ui.hdll` almost
certainly isn't meaningful on Vita (no native OS dialogs) and should
probably become a no-op stub the same way `datachannel` did, rather than
ported. `libuv` is used for hashlink's `sys.thread`/socket plumbing —
unclear yet if that's optional on a build with WebRTC/websockets stubbed
out, or if HashLink itself needs it regardless; needs checking against
what `libhl`'s own build actually requires.

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

- `vita/hashlink/` — the ported HashLink runtime (`libhl`), builds clean
  for `arm-vita-eabi` today. See `vita/hashlink/NOTICE.md`.
- `vita/hello-test/` — the toolchain smoke test: a trivial Haxe program
  proving the whole Haxe→hlc→libhl→`.vpk` pipeline. Actually built and
  packaged successfully; see its README to reproduce.
- `../compile-vita.hxml` — Haxe→C generation step for the real game,
  `-D vita`, mirrors `compile-linux.hxml`. Produces
  `vita/native/marblegame.c` (gitignored, generated, not checked in — same
  treatment as `native/` on other platforms per the existing `.gitignore`).
  This step alone was run and produces all 953 expected `.c` files with no
  errors — the Haxe/game-logic layer needs nothing further.
- `vita/stubs/datachannel.c` — no-op native module replacing
  `datachannel.hdll` so multiplayer's absence doesn't block linking the
  real game; see the networking section above.
- `vita/CMakeLists.txt` — starting point for linking the *real game*
  (`marblegame.c`, not the hello-test): still unverified past the
  dependency list, since it needs vitaGL/hlsdl (section 2, unstarted) to
  produce anything worth running. `vita/hello-test/README.md`'s manual
  command sequence is the proven-working reference for the link step
  itself; this file's job once rendering exists is doing that same
  sequence, plus vitaGL/SDL2/the data folder, as a build system instead of
  by hand.

## Suggested order of attack

1. ~~Get HashLink's runtime building for vitasdk as a static lib.~~ Done —
   see `vita/hashlink/`.
2. Get `hlsdl` linking against vitaGL + the vitasdk SDL2 port, and get a
   blank window/GL context up on real hardware. This is the point where
   you'll learn whether Heaps' shaders survive vitaGL/vitashark unmodified.
   This is the actual next step and the biggest remaining unknown in the
   whole port.
3. Swap in the `datachannel` stub and `ui`-stub (if needed), get
   `compile-vita.hxml`'s generated `marblegame.c` through the vitasdk
   compiler — expect missing-symbol errors that map onto whichever of
   `fmt`/`uv`/`ssl` turn out to still be required; port or stub each as it
   comes up the same way `libhl` was, file by file.
4. First boot: expect a black screen or crash in resource loading before
   rendering — get `data/` loading and a single interior rendering before
   worrying about gameplay correctness.
5. Only after single-player is solid: decide whether multiplayer is worth
   a `sceNet`-based transport, given libdatachannel itself is out of reach.
