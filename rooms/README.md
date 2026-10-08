# rooms: the hallway

a first person horror game in an endless office. open doors, walk through generated rooms, hide in lockers and survive the entities. made in godot 4 (gdscript), runs on windows, linux and android from one project.

> **this is an unofficial fan project.** it's inspired by nicorocks5555's roblox game *rooms*, the "the rooms" subfloor in lsplash's *doors*, and *rooms: revisited*. it's not affiliated with any of them or with roblox.
> the entity art, sounds and the rooms icon belong to the original games. lsplash and redibles allowed personal use only, so those files are **never committed** (they live in the gitignored `assets/local/`). **get permission before releasing any assets from the original games publicly.**

## run it

1. install [godot 4.4+](https://godotengine.org/download) (the standard build, not .net)
2. optional but recommended: download the original art/sounds for personal use
   ```
   python3 tools/fetch_assets.py
   ```
   this grabs 44 files (entity faces, a-90 / a-90b signs, sounds, music and the official rooms icon) from the doors, rooms and rooms: revisited wikis into `assets/local/`. without it the game still works with generated placeholders (drawn faces, synthesized sounds).
3. open `rooms/project.godot` in godot and press f5

## controls

| | keyboard / mouse | gamepad | touch |
|---|---|---|---|
| move | wasd | left stick | left stick (drag on the left side) |
| look | mouse | right stick | drag on the right side |
| sprint | shift (hold) | l3 | run (toggle) |
| crouch | c or ctrl (hold) | b | crouch (toggle) |
| interact (lockers, drawers, shops; hold for exit doors) | e | x | use (hold it for exit doors) |
| use the item in your hand | left click | y or rb | item (tap) |
| pick item (flashlight, shakelight, bandage, vitamins) | 1 2 3 4 or scroll | dpad left / right | item (hold) |
| pause | esc | start | \|\| |
| fullscreen | f11 (also in settings) | | |
| admin panel (admin runs) | f1 or ` | back | adm |

## what's in it

- **endless generated office**: 10 room types from a weighted pool (hallways, locker rooms, meeting rooms, l-turns, storage, break room, cubicles...), each tagged `has_lockers` / `entity_spawn_ok`. only ~4 rooms are loaded at once. door counter a-000, a-001...
- **fog and darkness**: normal until a-30, foggy after, dark from a-150 on (some rooms keep more lights working). lights break and flicker more the deeper you go.
- **special rooms**: a-000 lobby (couches, skylight, shakelight dispenser), a-100 message corridor, a-150 shop, exit rooms after a-200 (every 50-100 rooms, rarely earlier, you can hear them from the room before), a-1000 wooden bridge over the void with the glowing door, the a-050 library (find the four glowing books for the code to the exit), and from a-060 on the cafeteria (rows of tables, a serving counter, vending machines).
- **entities** (one script each, spawn rules in each script's `RULES`):
  - **a-60** rushes from behind after you open a door into a room with lockers. crackle, then hiss. hide.
  - **a-120** comes from the rooms ahead, slow, metallic clanging, can rebound. some lockers in your room get torn open when it spawns (2-3, or 5-7 in big locker rooms, never all). wait until the sound is completely gone.
  - **a-90** knock, audio cuts out, face flashes, stop sign. any input = 90 damage. freezes other entities. can join a-60 / a-120 (a-60 gets slowed).
  - **a-90b** gives 5-10 orders: halt (don't walk) or proceed (keep walking). wrong = 20-30 damage.
  - **a-200** (the happy scribble) from the front, rebounds ~3 times. white = hide, purple = get out of the locker.
  - **a-60b** has a tiny (2%) chance to show up at a-505, bounces back and forth with boss music until you get through 20 more doors. you're forced to sprint at double speed, a star marks where it is. (in rooms: revisited it's after a-1005, but our game ends at a-1000, so it's moved. change `AT_DOOR` in `a60b.gd`.)
  - **glitch** failsafe puts you back in the current room if a room fails to generate or you fall out of the world.
- **the curious light**: after you die a warm yellow light tells you what went wrong (which entity, and what exactly you did, like leaving a locker too early or touching the screen during a-90).
- **floors**: pressing play lets you pick a floor.
  - **the offices**: the normal endless office (a-000 to a-1000).
  - **the wires**: the maintenance floor under it (w-00 to w-50, no timer). concrete, pipes and cage lamps. breaker rooms at w-10/20/30/40/50: the door on has no power until you find the switch, and while it's off **w-10** (a glitchy outlet) keeps popping out of the walls and lunging at you. **w-15** raw wires hang from the ceiling from w-15 on (how many rooms get them depends on the seed) and shock you for 15. at w-50 power the elevator and **w-50 the shock** (a white fog with a faint face, 5.5 damage while you're in it) comes after you. ride the elevator up and the rest of that run is the **fixed office**: every light works, people sit at their desks typing, the drawers are locked (no robbing), there are atms to bank your gold, and no entities... except the ones that work here now. stare at one and they yell "IM AN INTROVERT", snap back to their spot and come through the rooms after you. hide in a locker. they overreact.
  - **the great city**: walk through the a-1000 door and you're outside, at dusk, on the street in front of the **miles** tower (the company you work for, logo up top). the avenue is split into blocks by metal gates (c-01 to c-30) and the entities still find you out here, plus two city ones: **headlights** (a horn, then lights tear down the street, hide in a phone booth) and **the billboard** (don't look up at it). if you came through the wires, your coworkers walk the sidewalks too. the bus stop at c-30 takes you home.
  - **a-240** has a secret way down: a locker got pushed aside from a crack in the wall. one of the room's drawers has the management key for the door at the end of the vine hallway behind it.
- **modifiers** (unlocked by leaving through an exit door): lights out, rush hour, in a hurry, fragile, staring contest, cheap batteries, gold rush, nowhere to hide, out of shape, empty pockets, inflation and "got any more doors?" (rush, ambush, eyes, screech, figure and seek from doors move into the office, like doors' "room for more"). seek's chase has obstacles (beams to crouch under, hands on one side of the room) and the curious light slams the last door in its face. figure's room is locked with a code: find the paper with it somewhere in the room. pick them on the title screen.
- **journal popups** when your best door unlocks a new page.
- **easter egg**: like in doors, a-90 very rarely shows up in a-000.
- **doors open by themselves** when you walk up to them (turn it off in settings).
- **items**: flashlight + batteries, shakelight, gold, bandages, vitamins. drawers in desks hold gold. shops at a-000 and a-150. gold is saved between runs.
- **saving**: the run saves on every door (and when you quit or switch apps). the title screen then shows "continue (a-xxx)". same rooms, same items, same health. dying or escaping clears it.
- **100 starter gold** for new saves (older saves get it once too).
- **doors style camera**: step bob, a lean when strafing / turning, wider fov when sprinting, a dip when landing. items show in your hand.
- **title screen** with a live foggy hallway behind the menu, and **the worker's journal**: 8 handwritten pages of backstory that unlock as your best door gets further (a-050, a-100, a-150, a-200, a-500, a-1000).
- **achievements** (saved in `user://save.json`) with popups, **settings** (volume, sensitivity, fov, quality, touch layout), **credits** screen.
- **admin panel**: tick it on the title screen. noclip, god mode, speed, jumping, sliding, infinite stamina, spawn any entity, open the next door, jump to any door. admin runs have their own separate progress (best door, gold, achievements, continue) in `user://save_admin.json`, your normal progress is never touched.

## logo and icon

`assets/branding/` has the logo (title screen + boot splash) and the icon (window / linux / windows; the android launcher uses it on a white background). made by jhulian.

## assets and the manifest

every object loads its files through `assets/manifest.json` (key -> path). swap a path to use a different file, no code changes. if a file is missing the game uses a placeholder (generated texture, box model, synthesized or silent sound) and prints a warning, so it always runs.

- `source` in an entry = where `tools/fetch_assets.py` downloads it from
- no `source` (walls, carpet, ceiling, lockers, furniture models...) = drop your own file at the `path`. the roblox wall / carpet / ceiling textures aren't on the wikis, so those are procedural for now
- 3d models (glb/gltf from sketchfab etc.) go in `assets/local/models/`. add every model to `assets/credits.json` with author, title, license and url. the credits screen lists them and the game warns at startup if a license doesn't allow reuse (nd, nc, all rights reserved) or needs attribution (cc-by).

## project layout

```
scripts/autoload/   settings (input map, buses), assets (manifest loader), save, achievements, game (run state)
scripts/world/      room_base + rooms/*.gd (one per room type), generator, door, locker, drawer, pickups, shop, props
scripts/entities/   entity (base), rusher (shared movement), screen_entity, one file per entity, entity_manager, glitch
scripts/player/     player, player_lights (flashlight/shakelight), inventory
scripts/ui/         hud, touch controls, menus, admin panel, credits
tests/              headless smoke test + screenshot script
tools/              fetch_assets.py
```

**adding a room**: copy `scripts/world/rooms/room_plant.gd`, change `build()`, add it to `pool` in `room_generator.gd`.
**adding an entity**: extend `Rusher` (moves through rooms) or `ScreenEntity` (on your screen), give it a `RULES` dictionary, add it to `SCRIPTS` in `entity_manager.gd`.

## exporting

project -> export. presets for linux, windows and android are included (you need godot's export templates, and for android the android sdk + a debug keystore set up in editor settings). desktop uses the forward+ renderer, android uses the mobile (vulkan) renderer. phones are capped at 60 fps, "uncap fps" in settings removes the cap and vsync. the "quality" setting (low / medium / high) changes msaa, shadows, glow, ssao and how many lights rooms use. "resolution" sets the 3d render scale (phones start at 60%), "show fps" puts a counter in the corner.

## tests

```
godot --headless --path . res://tests/smoke_test.tscn
```
also `res://tests/save_test.tscn` (saving / continuing, starter gold, the locker exit fix).
the smoke test opens 45 doors, spawns every entity, visits the special rooms, checks that a locker saves you from a-60, that a-60 kills you in the open and that a-90 only hurts you when you press something.
