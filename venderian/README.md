# venderian

an android app (kotlin, jetpack compose, material 3) that teaches venderian, a made-up language with its own angular script, plus a keyboard that turns english into venderian glyphs. offline, no accounts, no ads.

**install:** [`dist/venderian.apk`](dist/venderian.apk) (android 8+)

| learn | lesson | practice | quiz | keyboard | english → venderian | progress |
|---|---|---|---|---|---|---|
| ![](docs/screenshots/learn.png) | ![](docs/screenshots/lesson.png) | ![](docs/screenshots/practice.png) | ![](docs/screenshots/quiz.png) | ![](docs/screenshots/keyboard.png) | ![](docs/screenshots/keyboard-english.png) | ![](docs/screenshots/progress.png) |

built in stages:

1. **language design** (done): [`docs/venderian.md`](docs/venderian.md) is the source of truth, glyph strokes are in [`docs/alphabet.json`](docs/alphabet.json), the starter dictionary is [`app/src/main/assets/dictionary.json`](app/src/main/assets/dictionary.json)
2. **font** (done): svg glyphs in `glyphs/`, built into [`app/src/main/res/font/venderian.ttf`](app/src/main/res/font/venderian.ttf) with ligatures as an opentype `liga` feature
3. **converter** (done): english → phonemes (cmudict, or spelling rules for unknown words) → venderian glyphs, plus a translate mode that uses the dictionary. code in `app/src/main/java/app/venderian/convert/`, tests with `./gradlew :app:testDebugUnitTest`
4. **the app** (done):
   - **learn**: 5 alphabet lessons grouped by letter family, stroke order animation for every letter, tap-to-hear sound hints (the phone's offline text to speech)
   - **practice**: flashcards both ways (glyph ↔ sound, word ↔ meaning) with sm-2 spaced repetition in room. finished lessons add their letters, and 8 new words show up each day
   - **quiz**: 10 questions mixing multiple choice and type-it-out (with an ë button)
   - **keyboard**: a venderian keyboard with glyph keys, and an english box that converts live as you type (spell by sound, or translate). copy the venderian text or its romanization
   - **progress**: streak, xp and levels, the last 7 days, letters and words learned

## building

needs jdk 17+ and the android sdk. `./gradlew assembleDebug` builds the app, `./gradlew :app:testDebugUnitTest` runs the tests (converter, spaced repetition, and robolectric screenshots of every screen into `app/build/outputs/roborazzi/`).

## tools

- `python3 tools/render_chart.py` redraws `docs/alphabet.svg` from `docs/alphabet.json`
- `python3 tools/make_dictionary.py` rebuilds the dictionary and checks every word against the phonotactics
- `python3 tools/make_cmudict.py` rebuilds `app/src/main/assets/cmudict.dat` from [cmudict](https://github.com/cmusphinx/cmudict) (bsd license, carnegie mellon university)
- `python3 tools/make_glyphs.py && python3 tools/build_font.py` redraws `glyphs/*.svg` and rebuilds the font (needs `pip install -r tools/requirements.txt`)
