# Vault

A 3-lane rooftop-parkour endless runner for Android -- Subway Surfers-style movement (swipe
left/right to switch lanes, swipe up to jump, swipe down to slide), original setting: a street
skater kid sprinting, jumping, and sliding across an endlessly-generating city skyline at
sunset. Score climbs with distance and coins; the run speeds up the further you go. Collect
magnets, jetpacks, speed boosts, shields, and coin multipliers along the way. Complete rolling
sets of 3 missions for a score-multiplier boost, chase a harder daily challenge, and spend
"plates" (the in-game currency, earned only by playing) in the shop on cosmetic skins, powerup
upgrades, mission-skip vouchers, and headstarts into checkpoints you've unlocked.

## Module layout

This project is split into two Gradle modules with very different build requirements, the same
split used by this repo's other Android game projects:

- **`:core`** -- a plain `org.jetbrains.kotlin.jvm` module. Every actual game system lives here
  as plain Kotlin: procedural rooftop generation, the lane/jump/slide movement simulator,
  collision resolution, powerups, scoring, missions, daily challenges, the daily login streak,
  achievements, the plates economy/shop, save-game serialization, and run orchestration -- all
  pure, deterministic, unit-tested logic with **no Android dependency whatsoever**. This module
  builds and its tests run with nothing but a JDK:

  ```
  ./gradlew :core:test
  ```

  (90 tests as of this writing, covering procedural-generation determinism and solvability, the
  physics simulator's obstacle/collision rules, powerup timers, mission/daily-challenge/
  achievement/login-streak logic, the shop's purchase rules, GameSave JSON round-tripping, and
  full-session integration.)

- **`:app`** -- the real Android application: a Canvas-driven (not OpenGL) 2D renderer with a
  cheap pseudo-3D "road narrows to the horizon" perspective projection, swipe input, screens
  (main menu, in-run HUD + pause, results, shop, skins, missions, daily challenge, achievements,
  leaderboard), and a DataStore-backed persistence layer. It depends on `:core` for all game
  logic and only adds the platform glue around it. Building it requires a real Android SDK
  (`ANDROID_HOME` / `local.properties`) -- open the project in Android Studio, let it sync, and
  run `:app` on a device or emulator. This environment doesn't have an Android SDK installed, so
  `:app` has been written and carefully hand-reviewed (types, view-binding IDs, resource
  references all cross-checked) but not compiler-verified the way `:core` has via its test
  suite -- treat a first Android Studio sync/build as the remaining verification step.

## Why the split

Game logic in `:core` is where correctness actually matters and where a regression is easy to
introduce silently (procedural generation stops being solvable, a mission's completion math goes
off by one, the speed curve stops being monotonic, a shield fails to absorb a hit). Keeping it
Android-free means it can be built and tested anywhere a JDK exists, independent of whether an
Android SDK/emulator is available -- which is exactly the constraint this environment has.

## Core gameplay loop

- **Movement**: [`RunSimulator`](core/src/main/kotlin/com/vaultgame/core/physics/RunSimulator.kt)
  steps lane transitions, jump/slide arcs, and forward distance every tick, then resolves
  collisions against the currently-buffered obstacles/pickups.
- **Procedural rooftops**: [`SegmentGenerator`](core/src/main/kotlin/com/vaultgame/core/world/SegmentGenerator.kt)
  produces 30m segments seeded off the world seed, picking from 7 visual themes that unlock with
  distance (residential rowhouses -> warehouses -> a construction site -> neon downtown ->
  billboard plaza -> a rail-yard overpass -> skyline penthouses). Every obstacle "slot" is
  guaranteed solvable: either one full-span obstacle (a single required jump or slide clears it
  in any lane) or up to two lane-local obstacles, always leaving at least one lane clear.
- **Difficulty**: [`DifficultyCurve`](core/src/main/kotlin/com/vaultgame/core/world/DifficultyCurve.kt)
  ramps base speed (7 -> 18 m/s) and obstacle density (35% -> 85%) with distance, both capped.
- **Powerups**: magnet (coin pull radius), jetpack (flies over every obstacle), speed boost (on
  top of the base speed curve), shield (absorbs exactly one hit, no timer), coin multiplier --
  all tracked in [`ActivePowerups`](core/src/main/kotlin/com/vaultgame/core/powerups/ActivePowerups.kt),
  each with 3 purchasable upgrade tiers (shield excluded -- it's binary, nothing to scale).

## Progression

- **Missions**: rolling sets of exactly 3 goals; completing all 3 banks a score multiplier
  (default x1.5) spent on your next run's score, then immediately rolls a fresh set.
- **Daily challenge**: one harder goal per UTC calendar day, bigger plates payout than a mission.
- **Daily login streak**: a small plates bonus on each new consecutive day, ramping up to a cap.
- **Achievements**: lifetime distance/coin/no-hit-streak/powerup-usage/games-played milestones,
  several of which unlock a cosmetic skin.
- **Shop**: skins, powerup upgrades, mission-skip vouchers (consumable, stackable), and run
  headstarts (one-time unlock per checkpoint distance) -- cosmetics and convenience only, nothing
  that changes obstacle survivability.

## Setup

```
git clone <repo>
cd vault
./gradlew :core:test          # runs everywhere -- just needs a JDK 17+
```

To run the app itself, open the `vault/` folder in Android Studio (Iguana+), let Gradle sync
(this pulls AGP 8.5.2 / Kotlin 1.9.24 from Google's and Maven Central's repositories), and run
the `app` configuration on a device or emulator running API 24+.
