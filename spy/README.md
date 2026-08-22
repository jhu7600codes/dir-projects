# Spy

A pass-the-phone party game for Android, in Kotlin and Jetpack Compose --
one phone, one group, one hidden spy. Everyone else gets shown the same
secret word (a place, a profession, an event); the spy gets nothing.
Discuss, vote, and try to catch them out -- or, if you're the spy, try to
blend in. The whole interface is in Russian.

## How a game goes

1. **Настройка** -- add player names (3 minimum). "Как играть — демо-игра"
   on this screen plays a scripted walkthrough of a full round (see Demo
   mode below) for anyone who wants to see it before playing for real.
2. **Раздача ролей** -- pass the phone around; each player taps to reveal
   their own card in turn. One random player gets "ШПИОН"; everyone else
   sees the same word, drawn from a ~6,700-word pool (see Word bank below).
3. **Подсказки** -- players take turns giving a one-word hint about the
   secret word. If nobody's ready to accuse anyone yet, "Никто не догадался"
   starts a new round with hints one word longer (round 2 = two words, and
   so on) -- for as long as it takes. The moment somebody's ready to accuse
   someone, they press the big red button.
4. **Обсуждение** -- pressing the button starts a 3-minute discussion timer
   automatically; there's also a "Голосовать досрочно" button to skip ahead
   if everyone's ready sooner.
5. **Голосование** -- forces landscape (this is the one screen meant to sit
   flat on the table while everyone looks at it together) and walks each
   still-active player through voting for a suspect, or skipping.
6. **Исключение** -- a brief animation of the top-voted player's name card
   getting thrown out, then their role is revealed; a tie or an all-skip
   round eliminates nobody instead.
7. **Конец игры** -- civilians win once the spy is caught; the spy wins once
   only two players are left. Either way, the secret word gets revealed,
   and "новая игра" starts over from setup.

## Architecture

- Single `MainActivity` + single `GameViewModel` (`GamePhase` enum:
  `SETUP → REVEAL → PLAY → VOTE → ELIMINATION → RESULT → END`). Screens are
  plain Compose functions rendering whatever `GameViewModel` currently
  holds; which one is on screen is driven directly off `GamePhase` (an
  `AnimatedContent` in `MainActivity`, not `androidx.navigation`) since the
  flow is strictly linear and a back stack doesn't really make sense for a
  card reveal you're not supposed to be able to rewind.
- Everything lives in memory in `GameViewModel` -- no persistence, no
  network. A process death just means starting a new game.
- `PLAY` has two sub-states tracked on the ViewModel (`hintRoundNumber`,
  `discussionStarted`) rather than being its own phase: before the big red
  button is pressed it shows the hint-round UI; after, it shows the
  3-minute countdown. `startDiscussion()` (the button) and the timer
  hitting zero both funnel into the same `callMeeting()` that was already
  there for the manual "call a meeting early" action.
- `ELIMINATION` is a fire-and-forget animation screen: it plays a ~1s
  "thrown out" animation (or a shorter neutral beat if nobody was
  eliminated) and calls `finishElimination()` itself to move on to
  `RESULT` -- nothing the player does drives that transition.
- The demo/tutorial (`DemoScreen`, launched from Setup) lives outside
  `GamePhase` entirely -- it's a fixed, scripted walkthrough played by the
  three mascots (with Лейхер as the spy), not a simulation of the real
  game engine, so it's just a plain `Boolean` overlaid on top of the phase
  switch in `MainActivity`.
- The vote screen locks the activity into landscape at runtime
  (`requestedOrientation`) and restores whatever it was on the way out;
  `MainActivity` declares `configChanges` for orientation so that switch
  doesn't tear down and recreate the activity.
- `ui/theme` -- a single, permanently-dark color scheme (no light variant --
  see the comment in `Theme.kt`), red `#FF2D2D` as the primary accent.
- `ui/components` -- shared primitives (`SpyCard`, `SpyPrimaryButton`,
  `SpySecondaryButton`, `MascotImage`) so every screen shares the same
  rounded-card look, plus `SpyBigRedButton` -- a physical arcade-button
  illusion (a darker fixed base layer under a lighter glossy face that
  animates down onto it on press) used for "I know who the spy is."
- `data/WordBank.kt` -- see Word bank below.

## Word bank

The pool isn't a fixed hand-picked list -- it's two sources merged together:

- ~90 curated entries in `WordBank.kt` itself, grouped into seven real
  categories (cities, professions, buildings, transport, nature/leisure,
  events, institutions), always available.
- `app/src/main/assets/words_ru.txt` -- ~6,550 general Russian words, loaded
  once at startup (`WordBank.loadDictionary`) and shown under the generic
  "Слово" category, since the source list carries no category data. It's
  filtered down from [hingston/russian](https://github.com/hingston/russian)'s
  10,000-most-common-Russian-words list (Leeds University Russian corpus
  frequency data, [CC BY 2.5](https://creativecommons.org/licenses/by/2.5/))
  by dropping pronouns/prepositions/particles, most infinitive verbs (by
  suffix), patronymics, profanity, and personal names -- ~150 common Russian
  first names plus specific well-known surnames (authors, politicians) that
  showed up in the raw frequency list -- so secret words read as things to
  describe, not people to guess. What's left is mostly concrete nouns and
  adjectives; it isn't a perfect part-of-speech filter (there's no
  morphological analyzer running on-device), so the occasional adjective or
  odd word still slips through -- an accepted trade-off against bundling a
  full NLP model for a party game's word list. A handful of words that are
  technically also common first names (вера/"faith", надежда/"hope",
  любовь/"love") were deliberately kept, since their everyday meaning as
  ordinary nouns is the dominant one.

## Demo mode

`DemoScreen` (reachable from Setup via "Как играть — демо-игра") is a
tap-to-advance, twelve-step scripted walkthrough of one full round, acted
out by the three mascots -- Спай and Вотер get a normal word, Лейхер is the
spy -- covering reveal, escalating one-/two-word hint rounds, pressing the
big red button, the 3-minute discussion, voting, the "thrown out" animation,
and the civilians-win result. It's a fixed script (not wired to
`GameViewModel`), meant purely to show new players how a round plays out
before they start a real game.

## Assets

`logo.png` and the three mascot doodles (`mascot_spiey`, `mascot_voter`,
`mascot_leicher` -- the original individual character art, each already a
transparent-background PNG) live in `app/src/main/res/drawable/` and are
used contextually: spiey on the reveal cards and in the demo, voter on the
vote/result/win screens, leicher as a small easter egg on the empty player
list and as the demo's spy. `logo.png` alone is used on the Setup screen.

The launcher icon is generated from the full composite artwork (the S logo
with all three mascots and the "VOTE" sign) rather than the bare logo --
an adaptive icon (`mipmap-anydpi-v26`, foreground scaled up ~1.35x past the
108dp canvas so mask-cropping the busy edges reads as intentional rather
than as missing content) plus flattened legacy PNGs at every density, both
on the artwork's own sampled navy background (`#000F81`) so the background
layer matches seamlessly under the foreground.

## Building

Open the project root in Android Studio (Jellyfish+) and let Gradle sync,
or from the command line:

```
./gradlew :app:assembleDebug
```

Targets `compileSdk`/`targetSdk` 34, `minSdk` 24, AGP 8.5.2, Kotlin 1.9.24 --
the same toolchain as the repo's other Android modules. `:app:assembleDebug`
has been run end-to-end in this environment (Android SDK platform 34 +
build-tools 34.0.0, `compileDebugKotlin` and the full `assembleDebug` task
graph both green) and produces an installable `app-debug.apk`.
