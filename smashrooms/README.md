# Smash ROOMS

A Smash Bros-style platform fighter for Android, starring the entities from
DOORS and [the-rooms](https://github.com/jhu7600codes/the-rooms). It's written
in plain Kotlin with no libraries: one `SurfaceView` running a fixed 60hz loop,
drawn on a `Canvas`. You play in landscape against 1-3 CPUs, in stock matches.

All the art, sounds, music and the Caveat font come straight out of the
the-rooms build (see [Assets](#assets)).

## Fighters

There are 15 fighters: 14 entities plus doorsman, the player character from
DOORS. They share the same shapes for normal attacks, but each
one has its own reach, power, speed, weight, jumps and gravity. Every entity
also has four specials and a super that nobody else has:

| entity | special | side | up | down | super |
|---|---|---|---|---|---|
| rush | light flicker (stun) | rush pass | ceiling rush | ground rumble | the rush |
| ambush | rebound shriek (boomerang) | ambush (3 passes) | rebound rise | lurk (teleport behind) | rebound storm |
| eyes | gaze (hurts if they face you) | eye orb (homing) | ascend | eye swarm (trap) | room of eyes |
| screech | psst (bites unless they turn and look) | lunge bite | tendril climb | darkness (1.5x next hit) | feeding frenzy |
| figure | growl | grab and slam | heavy leap | stomp (trips the floor) | rampage |
| seek | seek eyes (turrets) | chase | ink rise | hands | the chase |
| a-60 | face swap (power / speed / armor) | rumble rush | teleport | rumble (delayed quake) | a-60 chase |
| a-60b | theme (burning aura) | prime dash | ascend | counter | overload |
| a-90 | stop (move and you get jumpscared) | knock | glitch up | despawn | everybody stop |
| a-90b | halt / proceed | attack | spawn rise | proceed (buff) | red light green light |
| a-120 | near (homing face) | phase walk | drift | hide | lights out |
| a-200 | scribble (bouncing) | voice | spring | doodle trap | happy room |
| a-150 | star shot | smile roll | star spin | falling star | starfall |
| a-220 | static beam | march (armored) | glitch jump | corrupt (flips controls) | system crash |
| doorsman | crucifix (chains you, then throws) | flashlight (beam + blind) | skeleton key (door warp) | lighter (fire on the floor) | guiding light (heals, then smites everyone) |

The "entities" screen in the game lists all of these, with a description for
each move.

## Controls

- **Left side of the screen:** a floating stick.
- **Attack:** jab or tilts, depending on which way the stick is held. Flick
  the stick and press attack together for a smash attack; keep holding attack
  to charge it. In the air you get nair, fair, bair, uair and dair.
- **Special:** neutral, side, up or down special, depending on the stick.
- **Jump:** the jump button, or flick the stick up. Fast-fall by flicking down
  in the air.
- **Shield:** hold it. Shield plus a stick direction rolls; shield in the air
  air-dodges.
- **Super:** the fear bar under your % fills as you hit and get hit. When it's
  full, a super button shows up.

Keyboards and gamepads work too: WASD or the d-pad / left stick to move,
J / X to attack, K / B to special, space / A to jump, L / shoulder buttons to
shield, U / triggers for the super, and start / escape to pause.

Items drop in from the ceiling: vitamins (heal and a speed boost), gold
(+50 fear) and a flashlight (stuns everyone near you). You can turn them off
on the select screen.

## Rooms

There are 9 stages, each using a background from the-rooms: the lobby, the
offices, the wires (moving platforms), the locker room (flat), the tall room,
the city (a platform that swings), the dark (the lights keep going out),
a-200's room, and the wire end. Every stage gets its own track from the game's
ambience and chase sounds. When it's down to two players on their last stock,
the music switches to a-60b's theme.

## Assets

`tools/import_the_rooms.py` regenerates everything under
`app/src/main/assets/` (plus the launcher icon) from the released the-rooms
APK and the images on its gh-pages branch:

```
python3 tools/import_the_rooms.py <the-rooms.apk> <the-rooms gh-pages img/>
```

`tools/gdres.py` reads Godot 4's imported resources:

- `.ctex` textures: pulls out the embedded WebP/PNG.
- `.mp3str`: the raw MP3.
- `.oggvorbisstr`: rebuilds a real Ogg file from Godot's packet sequence.
- `.sample`: decodes QOA-compressed WAVs.
- the zstd-packed font: pulls out the Caveat TTF.

Doorsman isn't in the-rooms, so his render, his item icons (crucifix,
flashlight, skeleton key, lighter) and his hurt and door sounds come from the
DOORS wiki instead. They're the `doors*` files in `assets/img` and
`assets/sfx`.

Every sound longer than 6 seconds is kept twice: in full as music, and as a
short sound-effect cut.

Credits match the-rooms: rooms by nicorocks5555, DOORS by LSPLASH, and a-60b /
a-90b from The Rooms: Revisited (Eclipse Games). This is an unofficial fan game
and isn't affiliated with Roblox. The art and sounds are for personal use only.

## Building

```
./gradlew :app:assembleDebug
```

The toolchain is the same as the repo's other Android modules: compileSdk and
targetSdk 34, minSdk 24, AGP 8.5.2, Kotlin 1.9.24. A prebuilt debug APK is in
`dist/`.

## Tests

```
./gradlew :app:testDebugUnitTest
```

The tests use Robolectric with native graphics, so no emulator is needed. They
check that:

- every move from every entity runs without crashing;
- 24 random CPU-only matches all finish;
- the menus and the fights render. Real frames are saved to `app/build/screens/`.
