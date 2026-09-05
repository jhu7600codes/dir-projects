# Provenance

This directory is a vendored 1:1 copy of the `mbu-port` branch (Marble Blast
Ultra) of [RandomityGuy/MBHaxe](https://github.com/RandomityGuy/MBHaxe),
copied in as the base for a PlayStation Vita port. Original code and assets
are unmodified except where noted below and in `vita/VITA_PORT.md`.

MBHaxe is MIT licensed — see `LICENSE`. All credit for the recreation of
Marble Blast Ultra's engine, physics, and content pipeline goes to
RandomityGuy and MBHaxe contributors.

## Changes made for the Vita port so far

- `src/Util.hx`, `src/Settings.hx`: added additive `#if vita` branches
  (platform name, settings save directory). No existing platform branch is
  touched.
- Added `compile-vita.hxml` (mirrors `compile-linux.hxml`, targets `-D vita`).
- Added `vita/` — build scaffolding and porting notes for the native Vita
  target, since none of this exists in upstream MBHaxe yet. See
  `vita/VITA_PORT.md` for the full technical status and `vita/BUILD.md`
  for the exact, proven build recipe. This now includes a real, verified
  port of HashLink's runtime and every native module the game needs
  (`vita/hashlink/`, see its own `NOTICE.md`) — the actual game
  (`marblegame.c`, generated from this repo's unmodified Haxe source)
  compiles and links clean into a real `arm-vita-eabi` ELF, packaged as
  `vita/marblegame.vpk` (code only — see its own note on the data folder).
  **Unverified on real hardware** — linking clean isn't the same as
  running clean; see `VITA_PORT.md`'s rendering section for what's
  actually still an open question.

Everything else in this directory is untouched upstream source, kept as-is
so the diff against upstream stays small and mergeable.
