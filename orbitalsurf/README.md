# Orbital Surf

A 3D endless-parkour game for Android: you control a ball that can't stop
moving forward, rolling and jumping across the rooftops of an
infinitely-generating procedural city. No lanes -- you steer freely across
each rooftop, ramp, and gap. Score climbs with distance and pickups, and the
ball gets faster as your score grows. Complete rolling sets of 3 missions to
build a run multiplier, chase harder daily challenges, spend "plates" (the
in-game currency, earned only by playing) in the shop on cosmetic skins,
mission-skip vouchers, and headstarts into checkpoints you've unlocked by
reaching them inside buildings during a run. A couple of achievements/skins
are tied to the developer's [Appteka profile](https://appteka.store/profile/575675).

## Module layout

This project is deliberately split into two Gradle modules with very
different build requirements:

- **`:core`** -- a plain `org.jetbrains.kotlin.jvm` module. Every actual game
  system lives here as plain Kotlin: procedural world generation, ball
  physics stepping, scoring/speed curve, missions, daily challenges,
  achievements, the plates economy/shop, and save-game
  serialization -- all as pure, deterministic, unit-tested logic with **no
  Android dependency whatsoever**. This module builds and its tests run with
  nothing but a JDK:

  ```
  ./gradlew :core:test
  ```

- **`:app`** -- the real Android application: OpenGL ES rendering
  (procedural low-poly geometry -- there's no external art pipeline behind
  this project, so buildings/ramps/the ball/trails are all generated at
  runtime rather than modeled), touch controls, screens (menu, shop,
  achievements, daily challenges, results), and a DataStore-backed
  persistence layer. It depends on `:core` for all game logic and only adds
  the platform glue around it. Building it requires a real Android SDK
  (`ANDROID_HOME` / `local.properties`) -- open the project in Android
  Studio, let it sync, and run `:app` on a device or emulator.

## Why the split

Game logic in `:core` is where correctness actually matters and where a
regression is easy to introduce silently (e.g. procedural generation isn't
deterministic anymore, a mission's completion math is off by one, the speed
curve stops being monotonic). Keeping it Android-free means it can be
built and tested anywhere a JDK exists, independent of whether an Android
SDK/emulator is available.

## Checkpoints, unified

"Checkpoints" mean one thing in this project: special rooms *inside*
buildings, placed at fixed, deterministic distances along the infinite path
(checkpoint 1, 2, 3, ... -- always the same distance, because the world
generator is seeded). The ball flows through them without stopping, same as
everywhere else. Reaching one for the first time unlocks it permanently;
the shop then sells a "Headstart" per unlocked checkpoint that starts your
*next* run already at that checkpoint's distance.
