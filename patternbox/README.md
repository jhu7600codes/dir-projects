# patternbox

a dumb sandbox fighting game where everything is an android lock pattern. you draw a fighter's body, weapon, armour and hat on 3x3 grids, pick what they're made of, then they fight.

- `app/` android app: kotlin, jetpack compose, material 3 with material you (fighters take your wallpaper colours on android 12+, themed icon too)
- `desktop/` linux app (compose desktop), packaged as a .deb with its own java inside
- `ui/` the screens, shared by both apps (only saving, sounds, wifi discovery and file dialogs differ, see `Platform.kt`)
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
| weapon type | blade, hammer (slow, huge knockback), spear (fast poke, long reach), bow (shoots the pattern), boomerang (comes back), bomb (lobbed, hurts you too), laser (instant beam), saw (always spinning, hit = lunge) |
| weapon material | steel, fire (burns), ice (slows), shock (stuns), bouncy (launches), glass (huge damage, can shatter), poison (long slow damage), vampire (heals you) |
| armour material | steel, gold (tanky, heavy), paper (light, fast), rubber (bounces attackers), spiky (hurts attackers), ice (freezes attackers), mirror (reflects shots), balloon (floaty) |
| ability | dash, double jump, slam, heal, teleport, rage, bubble, summon (tiny minion), magnet (pulls enemies in), invisible |

share codes look like `14789-159-456-123-0000`: body-weapon-armour-hat, dots numbered 1-9 like a pin, then the four picks. old 3 part codes still load.

## modes

1v1, 2v2, free for all (up to 6), survival (endless waves, best wave saved), tournament (4 or 8 bracket, watch or simulate each match, winner gets a cup), king of the hill (hold the zone, respawns, first to 30), boss fight (one giant vs everyone), sandbox.

maps: flat, platforms, spike pit, trampoline, ice rink, warehouse (crates + tnt), lava, windy, moon base.

## wifi multiplayer

play > play with friends. one phone hosts (it runs the real game), up to 5 others join from the same wifi or the host's hotspot. games show up by themselves (nsd), or type the ip the host screen shows. friends send their buttons, the host sends the whole arena back ~30 times a second. port 47474, plain tcp, code in `core/.../Net.kt`.

13 achievements, listed in settings.

sandbox tools: grab and throw (walls and floor deal impact damage), boom, poke, fire, freeze, zap, heal, spawn crates / tnt / bounce pads / spikes / ledges / fighters, erase. plus gravity (earth / moon / off / jupiter), slowmo, pause, chaos.

up to two people can play on one phone with the on screen pads, everyone else is a bot.

## linux

`sudo apt install ./dist/patternbox.deb`, then it's in the app menu (or run `patternbox`). saves go to `~/.local/share/patternbox`.

keyboard: p1 a/d move, w or space jump, f hit, g ability. p2 arrows, enter hit, right shift ability. with one player both sets work. esc goes back. sandbox tools use the mouse.

## importing fighters

anything with share codes in it imports: a chat message, a text file, the "copy all" export. `name: code` lines keep the name.

- phone: "paste codes" on the fighters tab, or share text from any app to patternbox
- pc: drop a file or text on the window, ctrl+i imports the clipboard, or "import file". "export file" saves everyone
- wifi games work between the phone and the pc (type the ip on the pc)

## build

```
./gradlew :core:test                # no sdk needed
./gradlew :app:testDebugUnitTest    # robolectric click-through of every screen
./gradlew :app:assembleRelease      # needs ANDROID_HOME or local.properties
./gradlew :desktop:run              # linux app straight from source
./gradlew :desktop:packageDeb       # the .deb (needs fakeroot + dpkg-deb)
```

the ready to install apk lives in `dist/patternbox.apk`, the linux one in `dist/patternbox.deb`. it's built locally and signed with the patternbox release key (kept out of the repo), pass it in with `PB_KEYSTORE_FILE` and `PB_KEYSTORE_PASSWORD`. without those, builds fall back to the debug key and won't install over the real apk.
