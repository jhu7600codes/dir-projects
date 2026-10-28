# CROSSI — plan

crossword puzzle game for android. kotlin + jetpack compose, material 3 with
material you dynamic color, dark theme by default, min sdk 26, offline first,
no backend.

status: **draft, waiting for approval. no code until approved.**

---

## 1. architecture

single gradle module `:app`, mvvm, unidirectional data flow.

```
crossi/
  app/src/main/java/dev/jhu/crossi/
    generator/   pure kotlin, zero android imports. grid, placement, validation, seeded rng
    data/        room db, daos, entities, datastore settings, dictionary asset loader, repositories
    game/        game rules: board state, checks, power ups, scoring, boss twists, modes
    ui/          compose screens, viewmodels, navigation, theme, keyboard
    di/          manual di (AppContainer), no hilt, keeps the build light
  app/src/main/assets/
    words.tsv        main dictionary with clues
    longwords.tsv    hand-picked long fun words
    levels.json      frozen hand-tuned levels
  app/src/test/      generator + game unit tests (plain jvm, no robolectric needed)
  tools/             jvm script that pre-generates levels.json from the generator
```

layers:
- **ui** (compose) observes `StateFlow<UiState>` from a viewmodel, sends intents (`onKey`, `onCellTap`, `onFinish`, ...)
- **viewmodel** holds a `GameSession` (from `game/`) and talks to repositories
- **game** is pure logic over a `Puzzle` + `BoardState`, no android, fully testable
- **generator** builds `Puzzle`s from a word pool + seed
- **data** persists everything (room for game data, datastore for settings)

`generator/` and `game/` never import android, so all their tests are plain jvm
unit tests (`./gradlew :app:testDebugUnitTest`).

libraries: compose bom, material3, navigation-compose (type safe routes with
kotlinx-serialization), room (ksp), datastore-preferences, kotlinx-coroutines,
junit4 + kotlin-test + turbine. versions pinned in `gradle/libs.versions.toml`,
latest stable at implementation time. target/compile sdk = latest stable.

---

## 2. generator algorithm

goal: a connected "american-free" crossword (not a fully filled grid, black
cells everywhere else), like the ones in newspapers' quick puzzle corner.
every word crosses at least one other, no illegal touching.

### 2.1 inputs

```kotlin
data class GenConfig(
  val width: Int, val height: Int,      // 7..15
  val targetWords: Int,                  // e.g. 8 for 7x7, ~30 for 15x15
  val minLen: Int = 3,
  val difficulty: IntRange,              // dictionary difficulty band
  val allowLongWords: Boolean,
  val phoneticCount: Int,               // how many letter-trick entries to mix in (0..3)
  val seed: Long,
)
```

### 2.2 seeded rng

own `SplitMix64` implementation in `generator/Rng.kt`. not `kotlin.random` or
`java.util.Random`, so output can never shift between jvm/android versions.
this is what makes the daily challenge identical for everyone.

### 2.3 placement loop

1. **pool**: filter dictionary to `minLen <= len <= max(width, height)` and
   difficulty band. shuffle with rng. long words are only added if
   `allowLongWords` and they fit (see 2.6).
2. **seed word**: pick a long-ish word (top 20% length of the pool), place it
   horizontally through the middle row, centered.
3. **grow**: repeat until `targetWords` placed or no candidate fits:
   - for each candidate word (bounded sample, ~200 per step), for each letter
     in it, for each placed cell with the same letter, try placing it
     perpendicular through that cell
   - run `canPlace()` (2.4). if legal, score it:
     `score = crossings * 10 + centerBias - edgeTouching + lengthBonus`
   - pick the best scoring placement (ties broken by rng), place it
4. **restarts**: run steps 2–3 up to `N` times (e.g. 30) with sub-seeds from the
   rng, keep the best grid by `fillRatio * 0.5 + crossingRatio * 0.5 + wordCount`.
   hard time budget (~150 ms on a mid phone) so it never stalls.
5. **trim**: optionally crop empty border rows/cols, then re-center in the
   requested size (keeps the look consistent).
6. **number**: standard crossword numbering, row-major, a cell gets a number if
   it starts an across or down entry.
7. **clue**: attach clues (2.5).

deterministic: same `GenConfig` + same dictionary version = same puzzle.

### 2.4 validation rules (`canPlace`)

for a word `w` at `(r, c)` in direction `d`:
- fits inside the grid
- the cell right **before** the start and right **after** the end (along `d`)
  is outside the grid or empty, so words never run into each other
- for each letter position:
  - cell empty: both **perpendicular neighbors** must be empty (no accidental
    2-letter fragments / side-by-side touching)
  - cell filled: letter must match AND that cell must not already belong to a
    word in direction `d` (no overlapping same-direction words)
- at least one crossing (except the seed word)
- word not already used in this puzzle

plus a full `validate(puzzle)` pass after generation that re-reads every
maximal horizontal/vertical run of letters in the grid and asserts each run of
length >= 2 is exactly one placed entry. this is the safety net the tests lean
on.

### 2.5 clues

- each dictionary word has 1–3 normal clues, one picked by rng
- **phonetic jokes**: dumb trick clues where the **answer is a single letter
  spelled how it sounds**, and the clue points at that letter in some stupid
  way. they get mixed into normal puzzles as extra entries.

  answer spellings (`phonetic.tsv`):
  ```
  A aigh   B bee   C see   D dee   E eee   F eff   G gee   H aitch  I eye
  J jay    K kay   L ell   M emm   N enn   O owe   P pee   Q cue    R arr
  S ess    T tee   U you   V vee   W doubleyou   X ecks   Y why   Z zed
  ```
  (3+ letters each, so they fit the normal min word length)

  clue templates, filled in by the generator for the chosen letter:
  - *"the letter thAts cApitAl in this hint"* → AIGH
  - *"the first letter of this Clue"*, *"the last letter of this cluE"*
  - *"the only letter that shows up 4 times in this clue"* (generator builds a
    sentence where it's true)
  - *"the letter missing from 'th_s cl_e'"*
  - *"the letter this whole clue is written without"* (lipogram)
  plus hand-written dumb ones per letter:
  - *"what a pirate says, but also a letter"* → ARR
  - *"the grade nobody wants"* → EFF
  - *"a question, but make it a letter"* → WHY
  - *"the shape of a horseshoe magnet"* → YOU
  - *"what you do to a debt, also a vowel"* → OWE
  - *"the letter you see in 'sea'... wait"* → SEE

  a generated clue is always double checked by code (the capital letter really
  is the only capital, the letter really appears n times, etc), and they get a
  little 🤡-free "trick" tag in the clue bar.
- plus a small hand-written list of pun clues per word where they exist
  (`clueKind = PUN`)
- `ClueKind { NORMAL, PHONETIC, PUN }`

### 2.6 long words

`longwords.tsv` holds ~30–60 hand-picked fun ones (antidisestablishmentarianism,
onomatopoeia, sesquipedalian, flibbertigibbet, serendipity, ...). rule:
a long word is only a candidate if `len <= width` (across) or `len <= height`
(down), and only when the grid is >= 11 wide. small grids never see them.

**problem:** supercalifragilisticexpialidocious is 34 letters and the max grid
is 15x15, so it can never fit. see open question 1.

### 2.7 grid size per level

| levels   | grid  | target words |
|----------|-------|--------------|
| 1–10     | 7x7   | 6–8          |
| 11–25    | 9x9   | 9–12         |
| 26–45    | 11x11 | 13–17        |
| 46–70    | 13x13 | 18–23        |
| 71+      | 15x15 | 24–30        |

free roam scales the same way with how many puzzles you've cleared in a row.
daily is always 11x11 (mondays) up to 15x15 (sundays), harder through the week.

---

## 3. word source

`assets/words.tsv`, one line per word:

```
WORD<TAB>difficulty(1-3)<TAB>clue 1|clue 2|clue 3<TAB>pun clue (optional)
```

- **common words only**: start from a public domain frequency list intersected
  with an english spellcheck list (scowl size <= 35). drop proper nouns,
  abbreviations, profanity/slurs, archaic or obscure stuff, and anything a
  normal 13-year-old wouldn't know
- target **~5000 words**, lengths 3–15
- clues are hand-written (by me, in batches during milestone 2), not scraped,
  so there's no license mess
- loaded once at startup into an in-memory index `Map<Int, List<WordEntry>>`
  (by length) plus a letter-position index for fast candidate lookup
- `DICTIONARY_VERSION` constant baked into daily seeds so a dictionary update
  doesn't silently give two people different dailies

---

## 4. modes

### levels
- frozen hand-tuned puzzles in `assets/levels.json` (layout + answers + clues),
  produced by `tools/` running the generator with chosen seeds, then hand
  checked/edited. so a dictionary change never breaks a level
- 100 levels at launch, linear unlock, 1–3 stars (based on checks used + power
  ups used + time)
- some levels have **pickups** (2.6 below), every 10th level is a **boss**

### free roam
- infinite generated puzzles, random seed each time
- running streak: grid grows as you clear puzzles in a row
- score per puzzle, high score table. big scores award power ups

### daily challenge
- seed = `hash("crossi-daily-" + localDate + DICTIONARY_VERSION)`
- everyone on the same app version gets the same puzzle on the same date
- one attempt counts for the streak, can replay for fun
- streak milestones award power ups

---

## 5. finish flow + checks

- each crossword starts with **5 checks**
- **finish** button = use one check on the whole board:
  - all correct → win screen
  - otherwise wrong letters get a red highlight + shake, empty cells get a soft
    outline, check counter drops by 1. player fixes and taps finish again
  - highlights clear on the cell once you change it
- the **extra check** power up adds +1 (can go past 5)
- at 0 checks: see open question 2
- checks used affects stars / score

---

## 6. power ups

| power up      | effect                                                    |
|---------------|-----------------------------------------------------------|
| reveal letter | fills the selected cell correctly, locks it               |
| reveal word   | fills the active entry correctly, locks it                |
| extra check   | +1 check for this crossword                               |
| skip clue     | marks the active entry as solved (revealed, no star cost for that entry but counts as "assisted") |

no coins. earned from:
- **daily streaks**: day 3, 7, 14, 30, then every 30 (bigger bundles at bigger milestones)
- **pickups in levels**: some cells carry a little icon. fill that whole entry
  correctly and the power up is granted when the level is finished (granted on
  finish to avoid reveal-farming)
- **free roam high scores**: beat thresholds (e.g. 5k, 10k, 25k...) or beat
  your personal best by a margin

boss rules can disable some power ups (see 7).

---

## 7. bosses (every 10 levels)

proposed twist set, one twist at the first bosses, combos later:

| twist            | what it does                                                                 |
|------------------|------------------------------------------------------------------------------|
| **timer**        | countdown (e.g. 4 min on 9x9). hit 0 = fail, retry                           |
| **locked cells** | some cells are padlocked until one of their crossing words is solved (checked correct) |
| **scrambled clues** | clue text is fine but numbers are hidden and the list is shuffled, you figure out which clue goes where |
| **limited mistakes** | 3 hearts. every finish that finds wrong letters costs a heart instead of (or on top of) a check |
| **fog**          | cells are hidden except those next to letters you've already placed (starts from the first word revealed) |
| **trick fest**   | way more letter-trick entries (up to 6) and their clues get extra dumb       |
| **scrambled keyboard** | keyboard keys are shuffled (re-shuffles each finish)                  |
| **one shot**     | only 1 check, no extra check power up allowed                                |

schedule:
- L10 timer, L20 locked cells, L30 scrambled clues, L40 limited mistakes,
  L50 fog, L60 trick fest, L70 timer + locked, L80 scrambled clues + limited
  mistakes, L90 fog + scrambled keyboard, L100 one shot + timer + trick fest
- bosses give a guaranteed power up bundle on clear
- boss rule is a `BossTwist` sealed class in `game/`, each one hooks into
  `GameSession` events (`onTick`, `onFinish`, `onInput`, `isCellLocked`, ...)

---

## 8. data models

### generator / game (pure kotlin)

```kotlin
enum class Direction { ACROSS, DOWN }
enum class ClueKind { NORMAL, PHONETIC, PUN }

data class WordEntry(val word: String, val difficulty: Int, val clues: List<String>, val pun: String?)

data class Entry(
  val number: Int, val direction: Direction,
  val row: Int, val col: Int, val answer: String,
  val clue: String, val clueKind: ClueKind,
)

data class Puzzle(
  val id: String, val width: Int, val height: Int,
  val entries: List<Entry>,
  val pickups: Map<Int /*entry index*/, PowerUpType> = emptyMap(),
  val boss: List<BossTwist> = emptyList(),
) { val solution: Array<CharArray?> /* derived */ }

data class BoardState(
  val letters: Array<CharArray>, val wrong: Set<Cell>, val locked: Set<Cell>,
  val cursor: Cell, val direction: Direction,
  val checksLeft: Int, val elapsedMs: Long,
)

enum class PowerUpType { REVEAL_LETTER, REVEAL_WORD, EXTRA_CHECK, SKIP_CLUE }
sealed interface GameMode { data class Level(val n: Int); data object FreeRoam; data class Daily(val date: LocalDate) }
```

### room (`CrossiDatabase`)

| entity              | fields |
|---------------------|--------|
| `LevelProgressEntity` | levelId pk, completed, stars, bestTimeMs, checksUsed, completedAt |
| `PowerUpEntity`       | type pk, count |
| `StreakEntity`        | id pk (single row), current, best, lastDailyDate, lastRewardedMilestone |
| `DailyResultEntity`   | date pk, completed, timeMs, checksUsed, score |
| `HighScoreEntity`     | id pk auto, score, gridSize, puzzlesInRow, achievedAt |
| `SavedGameEntity`     | modeKey pk (e.g. `level:12`, `daily:2026-10-28`, `free`), puzzleJson, boardJson, updatedAt — resume an unfinished puzzle |

daos expose `Flow`s. repositories: `ProgressRepository`, `PowerUpRepository`,
`StreakRepository`, `ScoreRepository`, `SavedGameRepository`,
`DictionaryRepository` (asset loader).

### datastore (settings)

theme (dark default / light / system), dynamic color on/off (on by default,
falls back to a crossi palette below android 12), auto-advance, skip filled
cells, jump to next clue at word end, haptics, show timer, highlight mode
(word / word + crossing).

---

## 9. screens

compose navigation with type safe routes:

- **home**: big CROSSI title, buttons: continue, levels, free roam, daily
  (with streak flame + count), power ups inventory chips, settings
- **level map**: scrolling grid/path of levels, stars, lock state, boss levels
  styled differently
- **game** (the main one):
  - top bar: mode/level, timer, checks left (5 dots), power up tray
  - grid: zoom/pan for 13x13+ grids, active entry highlight, cursor cell
    stronger highlight, wrong cells red, locked cells padlock, pickup icons
  - current clue bar above the keyboard, tap to toggle direction, arrows for
    prev/next clue
  - **custom keyboard**: qwerty, backspace, direction toggle key, haptic tap
  - clue list in a bottom sheet (across / down tabs)
  - finish button
- **result**: time, checks used, stars or score, rewards earned, next/retry
- **daily**: today's card, streak calendar, next milestone
- **free roam**: start/continue, current run, high scores
- **power ups**: inventory and how to earn each
- **settings**
- **how to play**: short swipeable intro, shown once

keyboard behaviour: typing fills cursor cell and auto-advances to the next
empty cell in the entry (or next cell if "skip filled" is off). backspace
clears, or steps back if the cell is already empty. tapping the selected cell
again toggles across/down. at the end of an entry, jump to the next unsolved
entry (setting).

---

## 10. tests

- `RngTest`: same seed → same sequence, known golden values
- `GeneratorTest`:
  - every generated puzzle passes `validate()` (property style, 500 seeds per size 7..15)
  - all words connected (bfs over crossings)
  - no word longer than the grid, long words never appear when width < 11
  - same config + seed = identical puzzle (golden snapshot for one seed)
  - word count within target range for most seeds
  - generation time under budget
- `CanPlaceTest`: hand-built grids for each illegal case (touching sides,
  running into another word, same-direction overlap, letter mismatch, out of bounds)
- `NumberingTest`, `PhoneticClueTest`
- game tests (bonus, not just generator): checks consumption, power up effects,
  boss twist rules, streak rollover across days/midnight

---

## 11. milestones

1. **m0 skeleton**: gradle project, theme (dynamic color, dark default), nav
   graph with empty screens, ci-free local build. readme
2. **m1 generator**: rng, grid, canPlace, placement loop, numbering, validate,
   full unit tests
3. **m2 dictionary**: words.tsv (~5000 with clues, in batches), longwords.tsv,
   loader + index, phonetic clue builder
4. **m3 game screen**: grid rendering, selection, custom keyboard,
   auto-advance, direction toggle, clue bar + list
5. **m4 finish flow + power ups**: checks, wrong highlighting, the 4 power ups
6. **m5 persistence**: room + datastore, resume games, inventory, settings screen
7. **m6 modes**: free roam (scaling + scores), daily (seed + streaks + rewards),
   levels (tools/ level freezer, levels.json, level map, stars, pickups)
8. **m7 bosses**: twist framework + the 8 twists, boss levels
9. **m8 polish**: animations, haptics, how to play, app icon, release build
   config (r8), final readme

each milestone is its own commit(s), tests green before moving on.

---

## 12. open questions

1. **supercalifragilisticexpialidocious (34) can't fit a 15x15.** options:
   a) add rare "mega" puzzles (e.g. boss levels / free roam milestone) with a
   wide grid like 36x15 and pan/zoom, b) cap long words at 15 letters and drop
   the 34-letter one, c) keep 15x15 max but let one special across row be
   longer. i'd go with **a**.
2. **0 checks left + wrong board**: a) puzzle failed, retry (levels) / run ends
   (free roam), b) finish just reveals the answers with a big score penalty
   and 1 star max, c) you can keep fixing but can't check, only spend an extra
   check power up. i'd go with **c, then a "give up" button = reveal + fail**.
3. **boss every n**: proposed n = 10. ok?
4. **language**: english only for now? (russian would need a whole second
   dictionary + clue set, could be a later thing)
5. **package name**: `dev.jhu.crossi` ok?
