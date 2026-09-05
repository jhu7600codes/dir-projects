# Vita Taxi

An endless, no-timer top-down taxi game for PS Vita homebrew. Pick up a fare,
drive them across town, get paid, repeat forever. No clock, no fail state —
just drive.

## What it is

- Top-down 2D driving, rendered entirely with [vita2d](https://github.com/xerpi/libvita2d)
  primitives (circles, rectangles, lines) — no sprite art or asset pipeline.
- One bounded map with a decorative road grid. World edges bounce you back
  a bit instead of hard-stopping you; no building collision yet.
- Endless loop: drive to the yellow PICKUP marker, then the green DROPOFF
  marker, get paid based on trip distance, repeat forever.
- A rotating HUD arrow always points at your current objective, classic
  Crazy-Taxi style.

## Controls

| Input | Action |
|---|---|
| Left stick (left/right) | Steer |
| Left stick (up/down) | Throttle / brake / reverse |
| START | Pause / unpause |
| START + SELECT | Quit to LiveArea |

## Building

Requires [vitasdk](https://vitasdk.org/) installed with `$VITASDK` set and
`$VITASDK/bin` on your `PATH`.

```sh
mkdir build && cd build
cmake .. -DCMAKE_TOOLCHAIN_FILE=$VITASDK/share/vita.toolchain.cmake
make -j$(nproc)
```

This produces `vita_taxi.self` and `vita_taxi.vpk`.

## Running

Copy `vita_taxi.vpk` to your Vita (e.g. over FTP with VitaShell) and install
it, or run it in the [Vita3K](https://vita3k.org/) emulator on desktop.

## Scope / known limitations

This is a v1 core loop, kept intentionally small:

- No sprite art — car and markers are simple primitives.
- No building/obstacle collision, only world-bounds collision.
- No save data between sessions; score resets on restart.
- No LiveArea icon/assets bundled — the app installs with a default icon.
