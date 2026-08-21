# Spy

A pass-the-phone party game for Android, in Kotlin and Jetpack Compose --
one phone, one group, one hidden spy. Everyone else gets shown the same
secret word (a place, a profession, an event); the spy gets nothing.
Discuss, vote, and try to catch them out -- or, if you're the spy, try to
blend in. The whole interface is in Russian.

## How a game goes

1. **Настройка** -- add player names (3 minimum).
2. **Раздача ролей** -- pass the phone around; each player taps to reveal
   their own card in turn. One random player gets "ШПИОН"; everyone else
   sees the same word, drawn from a ~90-entry bank of Russian
   locations/professions/buildings/etc., grouped by category.
3. **Обсуждение** -- a 3-minute discussion timer (start/pause/reset), or
   call a meeting early.
4. **Голосование** -- forces landscape (this is the one screen meant to sit
   flat on the table while everyone looks at it together) and walks each
   still-active player through voting for a suspect, or skipping.
5. **Результат** -- top-voted player is eliminated and their role revealed;
   a tie or an all-skip round eliminates nobody.
6. **Конец игры** -- civilians win once the spy is caught; the spy wins once
   only two players are left. Either way, the secret word gets revealed,
   and "новая игра" starts over from setup.

## Architecture

- Single `MainActivity` + single `GameViewModel` (`GamePhase` enum:
  `SETUP → REVEAL → PLAY → VOTE → RESULT → END`). Screens are plain
  Compose functions rendering whatever `GameViewModel` currently holds;
  which one is on screen is driven directly off `GamePhase` (an
  `AnimatedContent` in `MainActivity`, not `androidx.navigation`) since the
  flow is strictly linear and a back stack doesn't really make sense for a
  card reveal you're not supposed to be able to rewind.
- Everything lives in memory in `GameViewModel` -- no persistence, no
  network. A process death just means starting a new game.
- The vote screen locks the activity into landscape at runtime
  (`requestedOrientation`) and restores whatever it was on the way out;
  `MainActivity` declares `configChanges` for orientation so that switch
  doesn't tear down and recreate the activity.
- `ui/theme` -- a single, permanently-dark color scheme (no light variant --
  see the comment in `Theme.kt`), red `#FF2D2D` as the primary accent.
- `ui/components` -- shared primitives (`SpyCard`, `SpyPrimaryButton`,
  `SpySecondaryButton`, `MascotImage`) so every screen shares the same
  rounded-card look.
- `data/WordBank.kt` -- the ~90-word bank, grouped into seven categories
  (cities, professions, buildings, transport, nature/leisure, events,
  institutions).

## Assets

`logo.png` and the three mascot doodles (`mascot_spiey`, `mascot_voter`,
`mascot_leicher`, cut from the supplied `icon.png`) live in
`app/src/main/res/drawable/` and are used contextually: spiey on the reveal
cards, voter on the vote/result/win screens, leicher as a small easter egg
on the empty player list. The launcher icon is generated from `logo.png` --
an adaptive icon (`mipmap-anydpi-v26`) plus flattened legacy PNGs at every
density, both on a near-black background.

## Building

Open the project root in Android Studio (Jellyfish+) and let Gradle sync,
or from the command line:

```
./gradlew :app:assembleDebug
```

Targets `compileSdk`/`targetSdk` 34, `minSdk` 24, AGP 8.5.2, Kotlin 1.9.24 --
the same toolchain as the repo's other Android modules. This sandbox has no
Android SDK installed, so that build hasn't been run end-to-end here; the
source was written and reviewed carefully against those exact library
versions, but treat the first sync in Android Studio as the real
smoke test.
