# CROSSI

crossword game for android, like the nyt crossword but not just daily and more
fun. kotlin + jetpack compose, material 3 / material you, dark by default,
offline, english + russian. design notes in [plan.md](plan.md).

## what's in

- **nyt-style grids**: symmetric blocks, every cell checked, 3+ letter words, generated on device
- **levels**: 100 fixed levels per language (5x5 up to 15x15), stars, pickups, a boss every 10
- **free roam**: endless generated puzzles, easy 5x5 / normal 7x7 / hard 15x15, run scores + high scores
- **daily challenge**: easy / normal / hard every day, seeded by date so everyone gets the same ones, streaks
- **finish = check**: 5 checks per crossword, wrong letters go red, fix and finish again, give up when out
- **power ups** (no coins): reveal letter, reveal word, extra check, skip clue (swaps in a different clue).
  earned from streaks, level pickups, bosses, big free roam runs
- **bosses**: timer, locked cells, scrambled clues, limited mistakes, fog, trick fest, scrambled keyboard, one shot
- **letter tricks**: dumb riddle clues whose answer is a letter spelled out (AIGH, ARR, ЭЛЬ, ИКС...)
- custom keyboard (qwerty / йцукен), auto-advance, direction toggle, pencil mode, pinch zoom on big grids
- room for progress / power ups / streaks / scores / saved games, datastore for settings, per-app language

## build

needs jdk 17+ and the android sdk (platform 35). put `sdk.dir=...` in `local.properties` or set `ANDROID_HOME`.

```
./gradlew :app:assembleDebug        # apk in app/build/outputs/apk/debug
./gradlew :app:testDebugUnitTest    # generator + game unit tests
```

## content pipeline

the dictionaries in `app/src/main/assets/<lang>/words.tsv` are generated, then levels are frozen from them:

```
# english: SCOWL word lists + Open English WordNet 2024 (clues)
python3 tools/build_en_dictionary.py <scowl>/final <english-wordnet-2024.xml> app/src/main/assets/en/words.tsv
# russian: tools/ru_words_src.txt (hand-written) + russian wiktionary nouns (kaikki.org) + FrequencyWords
python3 tools/build_ru_dictionary.py <kaikki-ru.jsonl> <ru_300k.txt>
# freeze levels (bump ContentRepository.dictionaryVersion when words change)
CROSSI_FREEZE=1 ./gradlew :app:testDebugUnitTest --tests '*LevelFreezer*'
```

## credits / licenses of the word data

- SCOWL, Kevin Atkinson (permissive)
- Open English WordNet, CC BY 4.0
- Russian Wiktionary via kaikki.org / wiktextract, CC BY-SA 4.0
- hermitdave/FrequencyWords, CC BY-SA 4.0
