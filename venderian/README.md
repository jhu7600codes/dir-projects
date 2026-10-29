# venderian

an android app (kotlin, jetpack compose, material 3) that teaches venderian, a made-up language with its own angular script, plus a keyboard that turns english into venderian glyphs. offline, no accounts, no ads.

work in progress. built in stages:

1. **language design** (done): [`docs/venderian.md`](docs/venderian.md) is the source of truth, glyph strokes are in [`docs/alphabet.json`](docs/alphabet.json), the starter dictionary is [`app/src/main/assets/dictionary.json`](app/src/main/assets/dictionary.json)
2. font: svg glyphs in `glyphs/` and a fonttools build script for `venderian.ttf`
3. converter: english → phonemes (cmudict + fallback rules) → venderian
4. the app: learn, practice (spaced repetition), quiz, keyboard, progress

## tools

- `python3 tools/render_chart.py` redraws `docs/alphabet.svg` from `docs/alphabet.json`
- `python3 tools/make_dictionary.py` rebuilds the dictionary and checks every word against the phonotactics
