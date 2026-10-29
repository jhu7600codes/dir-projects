# venderian

an android app (kotlin, jetpack compose, material 3) that teaches venderian, a made-up language with its own angular script, plus a keyboard that turns english into venderian glyphs. offline, no accounts, no ads.

work in progress. built in stages:

1. **language design** (done): [`docs/venderian.md`](docs/venderian.md) is the source of truth, glyph strokes are in [`docs/alphabet.json`](docs/alphabet.json), the starter dictionary is [`app/src/main/assets/dictionary.json`](app/src/main/assets/dictionary.json)
2. **font** (done): svg glyphs in `glyphs/`, built into [`app/src/main/res/font/venderian.ttf`](app/src/main/res/font/venderian.ttf) with ligatures as an opentype `liga` feature
3. **converter** (done): english → phonemes (cmudict, or spelling rules for unknown words) → venderian glyphs, plus a translate mode that uses the dictionary. code in `app/src/main/java/app/venderian/convert/`, tests with `./gradlew :app:testDebugUnitTest`
4. the app: learn, practice (spaced repetition), quiz, keyboard, progress

## tools

- `python3 tools/render_chart.py` redraws `docs/alphabet.svg` from `docs/alphabet.json`
- `python3 tools/make_dictionary.py` rebuilds the dictionary and checks every word against the phonotactics
- `python3 tools/make_cmudict.py` rebuilds `app/src/main/assets/cmudict.txt.gz` from [cmudict](https://github.com/cmusphinx/cmudict) (bsd license, carnegie mellon university)
- `python3 tools/make_glyphs.py && python3 tools/build_font.py` redraws `glyphs/*.svg` and rebuilds the font (needs `pip install -r tools/requirements.txt`)
