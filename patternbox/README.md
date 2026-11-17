# patternbox

a dumb sandbox fighting game where everything is an android lock pattern. you draw a fighter's body, weapon, armour and hat on 3x3 grids, pick what they're made of, then they fight.

- `app/` android app: kotlin, jetpack compose, material 3 with material you (fighters take your wallpaper colours on android 12+, themed icon too)
- `core/` the game in plain kotlin: patterns, stats, items, the fight sim, modes, tournaments. builds and tests without an android sdk
- `web/` the original browser version (only has the 0.1 stuff)

## fighters

four patterns per fighter:

- **body** (4+ dots): more dots and longer lines = more hp, more turns = faster
- **weapon** (2+ dots): held at the body's last dot. longer = more damage, slower swing
- **armour** (2+ dots): each line is a plate that blocks damage and cracks off when hit
- **hat** (optional): pure drip

then pick:

| | options |
|---|---|
| weapon type | blade, hammer (slow, huge knockback), spear (fast poke, long reach), bow (shoots the pattern), boomerang (comes back), bomb (lobbed, hurts you too) |
| weapon material | steel, fire (burns), ice (slows), shock (stuns), bouncy (launches), glass (huge damage, can shatter) |
| armour material | steel, gold (tanky, heavy), paper (light, fast), rubber (bounces attackers), spiky (hurts attackers), ice (freezes attackers) |
| ability | dash, double jump, slam, heal, teleport, rage, bubble |

share codes look like `14789-159-456-123-0000`: body-weapon-armour-hat, dots numbered 1-9 like a pin, then the four picks. old 3 part codes still load.

## modes

1v1, 2v2, free for all (up to 6), survival (endless waves, best wave saved), tournament (4 or 8 bracket, watch or simulate each match, winner gets a cup), sandbox.

maps: flat, platforms, spike pit, trampoline, ice rink, warehouse (crates + tnt).

sandbox tools: grab and throw (walls and floor deal impact damage), boom, poke, fire, freeze, zap, heal, spawn crates / tnt / bounce pads / spikes / ledges / fighters, erase. plus gravity (earth / moon / off / jupiter), slowmo, pause, chaos.

up to two people can play on one phone with the on screen pads, everyone else is a bot.

## build

```
./gradlew :core:test                # no sdk needed
./gradlew :app:testDebugUnitTest    # robolectric click-through of every screen
./gradlew :app:assembleRelease      # needs ANDROID_HOME or local.properties
```

github actions builds the apk on every push that touches `patternbox/` and puts it on the `patternbox-latest` release.
