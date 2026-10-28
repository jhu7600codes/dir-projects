# CROSSI — plan

crossword game for android, basically the new york times crossword but not
just daily and more fun. kotlin + jetpack compose, material 3 with material you
dynamic color, dark theme by default, min sdk 26, offline first, no backend.
english + russian.

package: `dev.jhu.crossi`

status: **draft v2, waiting for approval. no code until approved.**

---

## 1. architecture

single gradle module `:app`, mvvm, unidirectional data flow.

```
crossi/
  app/src/main/java/dev/jhu/crossi/
    generator/   pure kotlin, zero android imports. block patterns, fill solver, validation, seeded rng
    data/        room db, daos, entities, datastore settings, dictionary loader, repositories
    game/        game rules: board state, checks, power ups, scoring, boss twists, modes
    ui/          compose screens, viewmodels, navigation, theme, keyboards
    di/          manual di (AppContainer), no hilt, keeps the build light
  app/src/main/assets/
    en/words.tsv  en/longwords.tsv  en/tricks.tsv  en/levels.json
    ru/words.tsv  ru/longwords.tsv  ru/tricks.tsv  ru/levels.json
    patterns.json   hand-made block patterns known to fill well
  app/src/main/res/values/ + values-ru/   ui strings
  app/src/test/   generator + game unit tests (plain jvm)
  tools/          jvm script that freezes levels.json from the generator
```

layers:
- **ui** (compose) observes `StateFlow<UiState>` from a viewmodel, sends intents (`onKey`, `onCellTap`, `onFinish`, ...)
- **viewmodel** holds a `GameSession` (from `game/`) and talks to repositories
- **game** is pure logic over a `Puzzle` + `BoardState`, fully testable
- **generator** builds `Puzzle`s from a dictionary + seed
- **data** persists everything (room for game data, datastore for settings)

`generator/` and `game/` never import android, tests run as plain jvm unit
tests (`./gradlew :app:testDebugUnitTest`).

libraries: compose bom, material3, navigation-compose (type safe routes with
kotlinx-serialization), room (ksp), datastore-preferences, kotlinx-coroutines,
junit4 + kotlin-test + turbine. versions pinned in `gradle/libs.versions.toml`,
latest stable at implementation time.

---

## 2. puzzle style + difficulties

nyt style: square grid, black blocks with 180° rotational symmetry, **every
white cell is part of an across AND a down word**, every word is 3+ letters,
all white cells connected. no unchecked cells, no 2-letter words.

| difficulty | grid  | feels like       | clue hardness |
|------------|-------|------------------|---------------|
| easy       | 5x5   | the nyt mini     | 1             |
| normal     | 7x7   | a midi           | 1–2           |
| hard       | 15x15 | the full daily   | 2–3           |

levels walk through the in-between sizes too (9x9, 11x11, 13x13) so the jump
from 7 to 15 isn't brutal. free roam and daily let you pick easy / normal / hard.

---

## 3. generator algorithm

### 3.1 inputs

```kotlin
data class GenConfig(
  val size: Int,                 // 5, 7, 9, 11, 13, 15
  val language: Language,        // EN, RU
  val clueHardness: IntRange,    // 1..3
  val trickCount: IntRange,      // letter-trick entries to allow (see 4.2)
  val allowLongWords: Boolean,   // only for size >= 11
  val seed: Long,
)
```

### 3.2 seeded rng

own `SplitMix64` in `generator/Rng.kt`, not `kotlin.random` / `java.util.Random`,
so output never shifts between jvm/android versions. this is what makes the
daily the same for everyone.

### 3.3 step 1: block pattern

- **5x5**: from a small list of known mini shapes (0, 2 or 4 corner blocks, etc)
- **7x7 and up**: pick from `patterns.json` (hand-made, tested fillable) or
  generate one:
  1. start empty, add blocks in symmetric pairs (rng)
  2. reject if any run of white cells is 1–2 long, if white cells disconnect, or
     if block count goes past the cap (~16% of cells; nyt allows ~17%)
  3. keep word count and average word length in a target band per size
- optional **theme slots**: on 11+ grids, if long words are allowed, the
  pattern is chosen so a symmetric pair of long across slots exists (rows 3 and
  size-4 for example) and the long fun word(s) get seeded there first

### 3.4 step 2: fill (constraint solver)

classic backtracking crossword filler:
1. slots = every maximal across/down run. each slot has a pattern like `C?T??`
2. dictionary index: per length, per (position, letter) a bitset of word ids.
   matching a pattern = and-ing bitsets, so it's fast even with 15k words
3. loop:
   - pick the **most constrained** unfilled slot (fewest candidates)
   - candidates ordered by `score(word) + rng jitter` (common words first,
     harder words allowed by `clueHardness`)
   - place it, **forward check**: every crossing slot must still have >= 1
     candidate, otherwise undo
   - no word twice in a grid
4. dead end → backtrack. too many backtracks or time budget hit (~300 ms on a
   mid phone for 15x15, way less for minis) → new sub-seed, maybe new pattern,
   retry
5. if still nothing after N retries, fall back to a pattern from
   `patterns.json` with more blocks (always works, tested)

deterministic: same config + same dictionary version = same puzzle.

### 3.5 step 3: validate + number + clue

- `validate(puzzle)`: symmetry, every white cell checked, every run 3+, connected,
  every run is a real dictionary word, no duplicates. tests lean on this
- standard numbering, row-major
- pick one clue per word by rng, inside the hardness band

### 3.6 long words

`longwords.tsv` holds hand-picked fun ones, max 15 letters (onomatopoeia,
serendipity, flibbertigibbet, sesquipedalian, ...). only used when
`size >= 11` and the word fits. small grids never see them. no mega puzzles,
supercalifragilisticexpialidocious is out.

### 3.7 dictionary size reality check

dense nyt-style fill needs a lot of words, way more than a criss-cross grid.
targets:
- **en**: ~12–15k common words (3–15 letters), heavy on 3–7 letter words
- **ru**: same ballpark, lemmas + common forms, ё stored as е

the generator tests measure fill success rate per size. if 15x15 fails too
often with common words only, the fix is more blocks / more 3–5 letter words,
**not** obscure words.

---

## 4. clues

### 4.1 normal clues

`words.tsv`, one line per word:

```
WORD<TAB>commonness(1-5)<TAB>hardness(1-3)<TAB>clue 1|clue 2|clue 3
```

- common words only: public domain frequency list ∩ spellcheck list (scowl for
  en, a frequency list + opencorpora-style dictionary for ru). no proper nouns,
  abbreviations, profanity, archaic or obscure stuff
- clues are hand-written by me in batches during milestone 2, no scraping
- a word can have easy and hard clues so the same word works in every difficulty
- `DICTIONARY_VERSION` per language is baked into daily seeds

### 4.2 letter tricks (the dumb ones)

the answer is a single letter spelled how it sounds, the clue points at that
letter in a stupid way. these are normal entries in the grid, and they help the
fill too.

en spellings (`en/tricks.tsv`, all 3+ letters):
```
A aigh   B bee   C see   D dee   E eee   F eff   G gee   H aitch  I eye
J jay    K kay   L ell   M emm   N enn   O owe   P pee   Q cue    R arr
S ess    T tee   U you   V vee   W doubleyou   X ecks   Y why   Z zed
```

ru: letter names that are 3+ letters (ЭЛЬ, ЭМЭ-style spellings where they read
naturally, ША, ЩА are too short, so the set is smaller). final list in m2.

templates the generator fills in (and code double checks they're true):
- *"the letter thAts cApitAl in this hint"* → AIGH
- *"the first letter of this Clue"*, *"the last letter of this cluE"*
- *"the only letter that shows up 4 times in this clue"*
- *"the letter missing from 'th_s cl_e'"*
- *"the letter this whole clue is written without"*

hand-written ones per letter:
- *"what a pirate says, but also a letter"* → ARR
- *"the grade nobody wants"* → EFF
- *"a question, but make it a letter"* → WHY
- *"the shape of a horseshoe magnet"* → YOU
- *"what you do to a debt, also a vowel"* → OWE

trick clues get a small "trick" tag in the clue bar.

`ClueKind { NORMAL, TRICK }`

---

## 5. modes

### levels
- 100 levels per language, frozen in `levels.json` (grid + answers + clues),
  made by `tools/` with picked seeds and hand checked, so a dictionary update
  never breaks a level
- size ramp: 1–15 5x5, 16–35 7x7, 36–55 9x9, 56–70 11x11, 71–85 13x13, 86–100 15x15
- 1–3 stars (checks used, power ups used, time)
- pickups on some levels, a boss every 10 levels

### free roam
- infinite generated puzzles, pick easy / normal / hard
- score per puzzle, run continues while you keep clearing them
- high score table per difficulty, big scores award power ups

### daily challenge
- three dailies: easy, normal, hard
- seed = `hash("crossi-daily-" + date + difficulty + language + DICTIONARY_VERSION)`
- same puzzle for everyone on the same app version + language
- the streak counts if you clear **any** daily that day, clearing all three
  gives a bonus power up

---

## 6. finish flow + checks

- each crossword starts with **5 checks**
- **finish** = spend one check on the whole board:
  - all correct → win screen
  - otherwise wrong letters go red (+ shake), empty cells get a soft outline,
    checks −1. fix and tap finish again
  - a red highlight clears once you change that cell
- **extra check** power up gives +1 (can go past 5)
- **0 checks left**: you can keep fixing letters but finish is disabled unless
  you use an extra check. a **give up** button reveals the answers and counts
  as a fail (retry for levels, run ends in free roam, daily counts as not cleared)
- checks used affects stars / score

pencil mode (gray "not sure" letters, like nyt) is a nice cheap extra, in m4.

---

## 7. power ups

| power up      | effect                                                   |
|---------------|----------------------------------------------------------|
| reveal letter | fills the selected cell correctly, locks it              |
| reveal word   | fills the active entry correctly, locks it               |
| extra check   | +1 check for this crossword                              |
| skip clue     | marks the active entry solved (revealed, counts as assisted) |

no coins. earned from:
- **daily streaks**: day 3, 7, 14, 30, then every 30
- **pickups in levels**: an icon on some entries, fill that entry correctly and
  you get it when the level is finished (so reveal farming doesn't work)
- **free roam high scores**: thresholds per difficulty + beating your best

inventory is shared between languages. bosses can disable some power ups.

---

## 8. bosses (every 10 levels)

| twist                  | what it does |
|------------------------|--------------|
| **timer**              | countdown, 0 = fail, retry |
| **locked cells**       | some cells padlocked until a crossing word is checked correct |
| **scrambled clues**    | numbers hidden, clue list shuffled, you figure out what goes where |
| **limited mistakes**   | 3 hearts, each finish that finds wrong letters costs a heart |
| **fog**                | cells hidden except next to letters you've placed |
| **trick fest**         | way more letter-trick entries, extra dumb clues |
| **scrambled keyboard** | keys shuffled, reshuffle on every finish |
| **one shot**           | 1 check, extra check power up disabled |

schedule: L10 timer, L20 locked, L30 scrambled clues, L40 limited mistakes,
L50 fog, L60 trick fest, L70 timer + locked, L80 scrambled clues + limited
mistakes, L90 fog + scrambled keyboard, L100 one shot + timer + trick fest.

bosses give a guaranteed power up bundle. each twist is a `BossTwist` in
`game/` that hooks into `GameSession` (`onTick`, `onFinish`, `onInput`,
`isCellLocked`, ...).

---

## 9. language

- selectable in settings (default = phone language if ru, else en)
- switches ui strings (`values-ru`), dictionary, tricks, levels, keyboard
- en keyboard qwerty, ru keyboard йцукен (ё folded into е, ъ kept)
- level progress, free roam scores and daily results are **per language**.
  power ups and the daily streak are shared

---

## 10. data models

### generator / game (pure kotlin)

```kotlin
enum class Language { EN, RU }
enum class Difficulty { EASY, NORMAL, HARD }
enum class Direction { ACROSS, DOWN }
enum class ClueKind { NORMAL, TRICK }

data class WordEntry(val word: String, val commonness: Int, val hardness: Int, val clues: List<String>)

data class Entry(
  val number: Int, val direction: Direction,
  val row: Int, val col: Int, val answer: String,
  val clue: String, val clueKind: ClueKind,
)

data class Puzzle(
  val id: String, val language: Language, val size: Int,
  val blocks: Set<Cell>, val entries: List<Entry>,
  val pickups: Map<Int, PowerUpType> = emptyMap(),
  val boss: List<BossTwist> = emptyList(),
)

data class BoardState(
  val letters: Array<CharArray>, val pencil: Set<Cell>,
  val wrong: Set<Cell>, val locked: Set<Cell>,
  val cursor: Cell, val direction: Direction,
  val checksLeft: Int, val elapsedMs: Long,
)

enum class PowerUpType { REVEAL_LETTER, REVEAL_WORD, EXTRA_CHECK, SKIP_CLUE }
sealed interface GameMode {
  data class Level(val n: Int) : GameMode
  data class FreeRoam(val difficulty: Difficulty) : GameMode
  data class Daily(val date: LocalDate, val difficulty: Difficulty) : GameMode
}
```

### room (`CrossiDatabase`)

| entity                | fields |
|-----------------------|--------|
| `LevelProgressEntity` | (language, levelId) pk, completed, stars, bestTimeMs, checksUsed, completedAt |
| `PowerUpEntity`       | type pk, count |
| `StreakEntity`        | single row: current, best, lastDailyDate, lastRewardedMilestone |
| `DailyResultEntity`   | (date, difficulty, language) pk, completed, timeMs, checksUsed, score |
| `HighScoreEntity`     | id pk, language, difficulty, score, puzzlesInRun, achievedAt |
| `SavedGameEntity`     | modeKey pk (`en:level:12`, `ru:daily:2026-10-28:hard`, ...), puzzleJson, boardJson, updatedAt |

daos expose `Flow`s. repositories: `ProgressRepository`, `PowerUpRepository`,
`StreakRepository`, `ScoreRepository`, `SavedGameRepository`,
`DictionaryRepository`.

### datastore (settings)

language, theme (dark default / light / system), dynamic color (on, falls back
to a crossi palette below android 12), auto-advance, skip filled cells, jump to
next clue at word end, haptics, show timer, highlight mode.

---

## 11. screens

compose navigation, type safe routes:

- **home**: CROSSI title, continue, levels, free roam, daily (streak flame),
  power up chips, settings
- **level map**: scrolling path, stars, locks, boss levels look different
- **game**:
  - top bar: level/mode, timer, checks left (dots), power up tray, pencil toggle
  - grid: black blocks, zoom/pan on 11x11+, active entry highlight, cursor cell,
    wrong cells red, padlocks, pickup icons
  - clue bar above the keyboard: tap = toggle direction, arrows = prev/next clue
  - **custom keyboard** (qwerty / йцукен), backspace, direction key, haptics
  - clue list bottom sheet (across / down)
  - finish button, give up in the overflow menu
- **result**: time, checks used, stars/score, rewards, next / retry
- **daily**: three cards (easy / normal / hard), streak calendar, next milestone
- **free roam**: difficulty pick, current run, high scores
- **power ups**: inventory + how to earn each
- **settings**, **how to play** (shown once)

keyboard: typing fills the cursor cell, auto-advances to the next empty cell in
the entry (or the next cell if "skip filled" is off). backspace clears or steps
back. tapping the selected cell toggles across/down. end of entry → next
unsolved entry (setting).

---

## 12. tests

- `RngTest`: same seed → same sequence, golden values
- `PatternTest`: symmetry, no 1–2 runs, connected, block cap, for 500 seeds per size
- `FillTest`:
  - every puzzle passes `validate()` for 500 seeds per size (5..15), both languages
  - fill success rate + time per size, fails if below target
  - long words never appear under 11x11
  - same config + seed = identical puzzle (golden snapshot)
- `TrickClueTest`: every template clue is actually true (capital count, letter
  counts, missing letter, lipogram)
- `NumberingTest`
- game tests: checks, give up, power ups, boss twists, streak across midnight

---

## 13. milestones

1. **m0 skeleton**: gradle project, theme (dynamic color, dark default), nav
   graph with empty screens, en/ru string resources, readme
2. **m1 generator**: rng, patterns, fill solver, validate, numbering + tests,
   using a temp test word list
3. **m2 dictionaries**: en + ru words.tsv with clues (big one, in batches),
   longwords, tricks, loaders + bitset index, fill success tuning
4. **m3 game screen**: grid, selection, both keyboards, auto-advance,
   direction toggle, clue bar + list, pencil
5. **m4 finish flow + power ups**: checks, highlights, give up, the 4 power ups
6. **m5 persistence**: room + datastore, resume, inventory, settings, language switch
7. **m6 modes**: free roam, daily x3 + streaks + rewards, levels (freezer
   tool, levels.json, level map, stars, pickups)
8. **m7 bosses**: twist framework + the 8 twists
9. **m8 polish**: animations, haptics, how to play, icon, release build (r8), readme

each milestone = its own commit(s), tests green before moving on.

---

## 14. decided

- package `dev.jhu.crossi`
- russian as a selectable language
- boss every 10 levels
- 0 checks: keep fixing, finish needs an extra check, give up = reveal + fail
- no mega puzzles, long words max 15 letters
- nyt style grids, easy 5x5 / normal 7x7 / hard 15x15

## 15. still open

1. daily x3 (easy/normal/hard) with streak on any one, ok? or just one daily?
2. levels ramp through 9/11/13 between normal and hard, ok? or only 5 / 7 / 15?
