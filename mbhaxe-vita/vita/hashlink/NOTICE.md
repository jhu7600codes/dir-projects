# Provenance

`src/` and `include/pcre/` here are vendored from
[HaxeFoundation/hashlink](https://github.com/HaxeFoundation/hashlink), by
way of [RandomityGuy/hashlink](https://github.com/RandomityGuy/hashlink)
(the fork MBHaxe actually builds against — see the `mbu-port` branch's
`.circleci/config.yml`), MIT licensed (see `LICENSE`). This is the runtime
half only (`libhl`: `src/gc.c` + `src/std/*.c` + PCRE2) — the bytecode
interpreter/JIT (`code.c`, `jit.c`, `debugger.c`, `module.c`, `profile.c`)
isn't included because it isn't needed: MBHaxe ships as HashLink's `hlc` C
output (see `../../compile-vita.hxml`), precompiled ahead of time, so no
JIT or interpreter runs on-device.

## PS Vita porting status: done for libhl, unstarted for everything else

`src/gc.c`, `src/std/*.c` and `include/pcre/*` all compile and **link
clean** for `arm-vita-eabi` as of this commit — verified in-session against
a from-source vitasdk build, down to producing and packaging a real `.vpk`
(see `../VITA_PORT.md`, "hello world" section). Every change is gated
behind `#ifdef HL_VITA`/`#elif defined(HL_VITA)` so no other platform's
codepath is touched. What changed, file by file:

- **`src/hl.h`** — `<uchar.h>` isn't in newlib; added to the existing
  `HL_IOS`/`HL_MAC` exclusion list that already hand-rolls `char16_t`/
  `char32_t` from `<stdint.h>` instead.
- **`src/gc.c`** — newlib has no `mmap`; `hl_alloc_executable_memory`
  returns NULL like `HL_CONSOLE` does (unused — no JIT on this target
  either), and the GC's page allocator uses `memalign(GC_PAGE_SIZE, size)`
  / `free()` instead of `mmap`/`munmap`.
- **`src/std/sys.c`** — no real tty (`termios.h`) or subprocess model
  (`sys/wait.h`) on Vita, so `hl_sys_get_char`/`hl_sys_command` follow the
  existing `HL_CONSOLE`/`HL_IOS` "not available" pattern. `hl_sys_string()`
  gained a `"Vita"` branch. `hl_sys_exe_path()` returns NULL (no `/proc`,
  and unneeded — the game already runs relative to the vpk root, see
  `ResourceLoader.hx`'s `TorqueFileSystem(".", null)`). One real toolchain
  quirk worth knowing about if you rebuild this: this vitasdk build's
  `gcc`-bundled `<limits.h>` doesn't `#include_next` newlib's real one (no
  `PATH_MAX`), so this file reaches past it to `<sys/syslimits.h>` directly
  under `HL_VITA` — see "toolchain gotchas" in `../VITA_PORT.md` before
  assuming that's an upstream bug to fix.
- **`src/std/socket.c`** — `MSG_NOSIGNAL` isn't defined on newlib (shimmed
  to `0`, a no-op flag — newlib doesn't raise `SIGPIPE` on this either), and
  `gethostbyname_r`/`gethostbyaddr_r` aren't implemented (added to the
  existing plain-`gethostbyname`/`gethostbyaddr` platform list).
- **`src/std/thread.c`** — `sys/syscall.h` (used only for
  `syscall(SYS_gettid)`, already behind `#if defined(SYS_gettid)`) isn't
  needed since the macro will just be undefined; `pthread_setname_np` isn't
  implemented, skipped (display-only, used by the profiler/debugger).
- **`src/std/process.c`** — no `fork`/`exec` model; extended the existing
  `HL_TVOS` "not available on this platform" branches to `HL_VITA` too, and
  switched to `<sys/wait.h>` (which exists) instead of bare `<wait.h>`
  (which doesn't) for the still-needed `WIFEXITED` etc. macros.

None of this touches multiplayer/rendering — see `../VITA_PORT.md` for
what's still unstarted (vitaGL rendering backend, `hlsdl`, audio).
