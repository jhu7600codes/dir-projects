# plan

notes from reading the references (doors wiki "the rooms", rooms wiki, rooms: revisited wiki: entities, game mechanics, room types, items, achievements, a-60 / a-60b / a-90 / a-90b / a-200 pages), and how each part maps to code.

## what the references say (summary, in my own words)

- **the rooms (doors)**: 1000 office rooms after a-000. white walls, red/blue carpet, gets darker the further you go. gold, batteries, bandages lying around. shakelight dispenser in a-000. exit rooms after a-200 roughly every 0-100 rooms, exit door has its own ambient sound you hear from the room before. a-1000 is a bridge over a void with a glowing door.
- room types: locker room, three-locker, catwalk, projector/meeting (a-60 / a-120 can come when you enter these), l-shaped, e-shaped, plant, storage, box, break room, reference, four-locker (they can't).
- **a-60**: rush style from behind, only when opening into a room with lockers, usually after 60, rarely earlier. crackling static, then a high hiss when close. line of sight. can linger and re-check rooms (rooms: revisited).
- **a-120**: comes from ahead, slow, metallic clanging getting louder, can rebound like ambush.
- **a-90**: knock, other audio paused, face somewhere on screen, then the stop sign. any movement including camera or hiding = damage. freezes the other entities. can join a-60 / a-120, slowing a-60.
- **a-90b** (rooms: revisited): red flush, 5-10 halt / proceed orders, re-evaluated at random intervals. wrong = small damage and it ends. after room 60, rare, timer 200-600s.
- **a-200** (rooms: revisited, nico's "happy scribble"): after 100, timer 250-325s paused while a-60 is around. bit-crushed spawn sound, ~5s later goes front to back, rebounds ~3 times with voice lines, may switch white (kills unhidden in sight) / purple (kills hidden) each rebound. screen edges glow in its color.
- **a-60b** (rooms: revisited): blue a-60, guaranteed once, boss music, ~10s warning, bounces between the oldest and newest loaded rooms until the player traverses 20 rooms. forced sprint at double speed, star indicator through walls, blocks a-60 / a-90b / a-200 (a-90 still allowed).

nothing was unclear, so no questions before coding.

## decisions

- **a-60b at a-505** instead of a-1005, because our game ends at a-1000. it's one constant.
- **rooms built from code, not .tscn**: each room type is one small script that builds itself with helpers (walls with door gaps, lights, lockers, props). easier to read and change than hand-edited scenes, and every prop can still be swapped for a real model through the manifest.
- **placeholders that work**: missing art becomes drawn faces / signs / noise textures, missing sounds become synthesized cues (static crackle for a-60, clanging for a-120, knock for a-90...), so the horror is playable before any asset exists.
- **original assets stay local**: the fetch script downloads them into the gitignored `assets/local/`, the repo never contains them (personal use only).
- **gold is saved between runs** and spent at a-000 / a-150.

## milestones (built all at once, as asked)

- [x] m1 project setup, player movement, flashlight, rooms
- [x] m2 room generator, doors, door counter, fog progression
- [x] m3 lockers, a-60, audio cues
- [x] m4 a-90 with the stop sign, a-120
- [x] m5 items, shakelight, gold, health, shop
- [x] m6 a-100 corridor, exit rooms, a-1000
- [x] m7 a-60b, a-90b, a-200, glitch, achievements, credits, settings, admin panel, crouching, drawers + a-150 shop
- [x] m8 export presets for android / windows / linux, quality setting, rooms freed behind you

## ideas for later

- real models for lockers / desks / chairs (sketchfab, keep `credits.json` up to date)
- the e-shaped, catwalk and box room types
- a-70 (the locker mimic) from rooms: revisited
- a proper a-60 jumpscare / death animation
