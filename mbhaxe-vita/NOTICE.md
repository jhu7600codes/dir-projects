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
  `vita/VITA_PORT.md` for the full technical plan and current status.

Everything else in this directory is untouched upstream source, kept as-is
so the diff against upstream stays small and mergeable.
