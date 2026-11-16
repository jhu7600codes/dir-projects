# patternbox

a dumb sandbox fighting game where everything is an android lock pattern. you draw a fighter's body, weapon and armour on 3x3 grids, then they fight.

- `app/` android app: kotlin, jetpack compose, material 3 with material you dynamic colour (fighters take your wallpaper colours on android 12+, themed icon too)
- `core/` the game itself in plain kotlin: pattern rules, stats, names, share codes, the whole fight sim. builds and tests without an android sdk
- `web/` the original browser version, single html file

## how stats work

- **body** (4+ dots): more dots and longer lines = more hp, more turns = faster
- **weapon** (2+ dots): held at the body's last dot. longer = more damage, slower swing
- **armour** (2+ dots): each line is a plate that blocks damage and cracks off when hit. more dots = more block, but heavier

share codes are the dots numbered 1-9 like a pin, `body-weapon-armour`, e.g. `14789-159-456`.

## sandbox

grab and throw fighters (walls and floor deal impact damage), boom, poke, bots on/off per player, gravity (earth / moon / off / jupiter), slowmo, chaos, heal, rematch. on the phone each player gets a little control pad, touching it turns that player's bot off.

## build

```
./gradlew :core:test            # no sdk needed
./gradlew :app:assembleRelease  # needs ANDROID_HOME or local.properties
```

the release apk is signed with the debug key so it installs straight away. github actions builds it on every push that touches `patternbox/`, grab it from the run's artifacts.
