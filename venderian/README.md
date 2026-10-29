# venderian

an android app (kotlin, jetpack compose, material 3) that teaches venderian, a made-up language with its own angular script, plus a keyboard that turns english into venderian glyphs. offline, no accounts, no ads.

work in progress. built in stages:

1. **language design** (done): [`docs/venderian.md`](docs/venderian.md) is the source of truth, glyph strokes are in [`docs/alphabet.json`](docs/alphabet.json), the starter dictionary is [`app/src/main/assets/dictionary.json`](app/src/main/assets/dictionary.json)
2. **font** (done): svg glyphs in `glyphs/`, built into [`app/src/main/res/font/venderian.ttf`](app/src/main/res/font/venderian.ttf) with ligatures as an opentype `liga` feature
3. converter: english → phonemes (cmudict + fallback rules) → venderian
4. the app: learn, practice (spaced repetition), quiz, keyboard, progress

## tools

- `python3 tools/render_chart.py` redraws `docs/alphabet.svg` from `docs/alphabet.json`
- `python3 tools/make_dictionary.py` rebuilds the dictionary and checks every word against the phonotactics
- `python3 tools/make_glyphs.py && python3 tools/build_font.py` redraws `glyphs/*.svg` and rebuilds the font (needs `pip install -r tools/requirements.txt`)
