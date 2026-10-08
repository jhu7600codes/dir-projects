class_name RoomGenerator
extends Node3D
## builds the endless office. every door you open spawns the room after the next one,
## old rooms behind you get freed. only a few rooms exist at any time.
##
## door n leads into room n. after opening door n, rooms n-2 .. n+1 are loaded.

signal room_entered(room: RoomBase)

const KEEP_BEHIND := 2
const R := "res://scripts/world/rooms/"
const CITY := preload(R + "room_city.gd")

## the pool of normal rooms. weight = how common, min = first door it can show up at
var pool := [
	{"script": preload(R + "room_hallway.gd"), "weight": 9, "min": 1},
	{"script": preload(R + "room_plant.gd"), "weight": 5, "min": 1},
	{"script": preload(R + "room_turn.gd"), "weight": 8, "min": 2},
	{"script": preload(R + "room_locker.gd"), "weight": 6, "min": 3, "lockers": true},
	{"script": preload(R + "room_three_locker.gd"), "weight": 6, "min": 1, "lockers": true},
	{"script": preload(R + "room_meeting.gd"), "weight": 5, "min": 4},
	{"script": preload(R + "room_storage.gd"), "weight": 4, "min": 5, "lockers": true},
	{"script": preload(R + "room_break.gd"), "weight": 4, "min": 6},
	{"script": preload(R + "room_four_locker.gd"), "weight": 4, "min": 1, "lockers": true},
	{"script": preload(R + "room_cubicles.gd"), "weight": 5, "min": 8, "lockers": true},
	{"script": preload(R + "room_open_office.gd"), "weight": 5, "min": 6, "lockers": true},
	{"script": preload(R + "room_side_offices.gd"), "weight": 7, "min": 3},
	{"script": preload(R + "room_supply_closet.gd"), "weight": 4, "min": 5, "lockers": true},
	{"script": preload(R + "room_reception.gd"), "weight": 3, "min": 10},
	{"script": preload(R + "room_server.gd"), "weight": 3, "min": 20, "lockers": true},
	{"script": preload(R + "room_copy.gd"), "weight": 4, "min": 4, "lockers": true},
	{"script": preload(R + "room_cafeteria.gd"), "weight": 3, "min": 60, "lockers": true},
]
## fixed rooms at fixed doors
var special := {
	0: preload(R + "room_lobby.gd"),
	50: preload(R + "room_library_50.gd"),
	100: preload(R + "room_corridor_100.gd"),
	150: preload(R + "room_shop_150.gd"),
	240: preload(R + "room_management_240.gd"),
	1000: preload(R + "room_bridge_1000.gd"),
}
const EXIT_ROOM := preload(R + "room_exit.gd")
const WIRES_START := preload(R + "wires_start.gd")
const WIRES_CORRIDOR := preload(R + "wires_corridor.gd")
const WIRES_PIPES := preload(R + "wires_pipes.gd")
const WIRES_GENERATOR := preload(R + "wires_generator.gd")
const WIRES_SWITCH := preload(R + "wires_switch.gd")
const WIRES_END := preload(R + "wires_end.gd")
const HALLWAY := preload(R + "room_hallway.gd")

var rooms: Array[RoomBase] = []
var run_seed := 0
var _heading := 0       # -1, 0, 1: which way the hallway currently points


var _picks := {}  # door -> the room script picked for it this run

func start(seed_value: int, first_door := 0) -> void:
	run_seed = seed_value
	_picks.clear()
	for r in rooms:
		r.queue_free()
	rooms.clear()
	_heading = 0
	_spawn(first_door, Transform3D.IDENTITY)
	_spawn(first_door + 1)
	_update_seal()


func room(n: int) -> RoomBase:
	for r in rooms:
		if r.number == n and is_instance_valid(r):
			return r
	return null


func newest() -> RoomBase:
	return rooms.back() if not rooms.is_empty() else null


func oldest() -> RoomBase:
	return rooms.front() if not rooms.is_empty() else null


func _rng_for(n: int) -> RandomNumberGenerator:
	var rng := RandomNumberGenerator.new()
	rng.seed = hash(str(run_seed) + ":" + str(n))
	return rng


## which room goes at door n. only depends on the run seed and n (not on earlier rooms),
## so a saved run rebuilds exactly the same rooms when you continue it.
func _pick_script(n: int, _rng: RandomNumberGenerator) -> Script:
	if Game.floor == "city":
		return CITY
	if Game.floor == "wires":
		return _pick_wires(n)
	if _picks.has(n):
		return _picks[n]
	# picks depend on the real picks before them, so work forward from the last known one
	var k := n
	while k > 1 and not _picks.has(k - 1):
		k -= 1
	while k <= n:
		_picks[k] = _decide(k)
		k += 1
	return _picks[n]


func _decide(n: int) -> Script:
	if special.has(n):
		return special[n]
	if _is_exit_room(n):
		return EXIT_ROOM
	var recent := [_picks.get(n - 1), _picks.get(n - 2)]
	# lockers at least every few rooms once things start hunting you, so a-60 is fair
	var need_lockers := n > 15 and not _has_lockers(n - 1) and not _has_lockers(n - 2) and not _has_lockers(n - 3)
	# no room type twice within three doors
	for attempt in 8:
		var sc := _weighted_pick(n, attempt, need_lockers)
		if not recent.has(sc):
			return sc
	return _weighted_pick(n, 8, need_lockers)


## does the room picked for door n have lockers (doors before the first count as yes)
func _has_lockers(n: int) -> bool:
	if n < 1:
		return true
	var sc: Script = _picks.get(n)
	for p in pool:
		if p.script == sc:
			return p.get("lockers", false)
	return false


## exit rooms after a-200: one in every block of 75 doors, placed 25-50 doors into the
## block, so they're 50-100 rooms apart. rarely one shows up early as a bonus.
## the wires: a start room, switch rooms every 10 doors, the elevator room at w-50
func _pick_wires(n: int) -> Script:
	if n == 0:
		return WIRES_START
	if n == Game.WIRES_LAST:
		return WIRES_END
	if n % 10 == 0:
		return WIRES_SWITCH
	var rng := _rng_for(n * 13 + 5)
	var opts := [WIRES_CORRIDOR, WIRES_CORRIDOR, WIRES_PIPES, WIRES_GENERATOR]
	var s: Script = opts[rng.randi() % opts.size()]
	return s


func _is_exit_room(n: int) -> bool:
	if Game.floor != "offices":
		return false
	if n <= 200 or n >= Game.last_door() - 5:
		return false
	var block := (n - 201) / 75
	var offset := _rng_for(-1000 - block).randi_range(25, 50)
	if (n - 201) % 75 == offset:
		return true
	return _rng_for(n * 31 + 7).randf() < 0.006


func _weighted_pick(n: int, attempt: int, lockers_only := false) -> Script:
	var rng := _rng_for(n * 17 + attempt * 100003 + (7 if lockers_only else 0))
	var total := 0
	var options := []
	for p in pool:
		if n >= p.min and (not lockers_only or p.get("lockers", false)):
			options.append(p)
			total += p.weight
	if options.is_empty():
		return HALLWAY
	var roll := rng.randi_range(1, total)
	for p in options:
		roll -= p.weight
		if roll <= 0:
			return p.script
	return HALLWAY


func _spawn(n: int, at = null) -> RoomBase:
	if n > Game.last_door():
		return null
	var rng := _rng_for(n)
	var script := _pick_script(n, rng)
	var r := _build(script, n, rng)
	if r == null:
		push_warning("room %s failed to build, using a hallway" % Game.door_label(n))
		r = _build(HALLWAY, n, rng)
	if r == null:
		Glitch.failsafe("room %s could not be generated" % Game.door_label(n))
		return null
	var xf: Transform3D = at if at != null else (newest().global_exit() if newest() else Transform3D.IDENTITY)
	r.transform = xf
	add_child(r)
	rooms.append(r)
	if r.exit_door:
		r.exit_door.opened.connect(_on_door_opened)
	# keep track of where the hallway points (turn rooms rotate it)
	var yaw := r.exit_local.basis.get_euler().y
	_heading += int(round(yaw / (PI / 2)))
	return r


func _build(script: Script, n: int, rng: RandomNumberGenerator) -> RoomBase:
	var r = script.new()
	if not r is RoomBase:
		return null
	# turn rooms must not fold the hallway back on itself
	if _heading >= 1:
		r.turn = -1
	elif _heading <= -1:
		r.turn = 1
	else:
		r.turn = 1 if rng.randf() < 0.5 else -1
	r.setup(n, rng.randi())
	if r.path_local.size() < 2:
		r.free()
		return null
	return r


func _on_door_opened(door: Door) -> void:
	var n := door.number
	Game.set_door(n)
	if newest() and newest().number == n:
		_spawn(n + 1)
	for r in rooms.duplicate():
		if r.number < n - KEEP_BEHIND:
			rooms.erase(r)
			r.queue_free()
	_update_seal()
	var entered := room(n)
	if entered:
		room_entered.emit(entered)


func _update_seal() -> void:
	for i in rooms.size():
		rooms[i].set_back_sealed(i == 0 and rooms[i].number > 0)


## admin: rebuild the hallway starting at door n and put the player there
func jump_to(n: int) -> void:
	n = clampi(n, 0, Game.last_door())
	if Game.entities:
		Game.entities.clear_all()
	start(run_seed, n)
	Game.set_door(n)
	if Game.player:
		var r := room(n)
		Game.player.teleport(r.global_transform * Transform3D(Basis(Vector3.UP, PI), Vector3(0, 0.1, 1.2)))
	room_entered.emit(room(n))


## a spot to put the player back on track (used by the glitch failsafe)
func safe_spot() -> Transform3D:
	var r := room(Game.door)
	if r == null:
		r = newest()
	if r == null:
		return Transform3D.IDENTITY
	return r.global_transform * Transform3D(Basis(Vector3.UP, PI), Vector3(0, 0.1, 1.2))
