class_name RoomGenerator
extends Node3D
## builds the endless office. every door you open spawns the room after the next one,
## old rooms behind you get freed. only a few rooms exist at any time.
##
## door n leads into room n. after opening door n, rooms n-2 .. n+1 are loaded.

signal room_entered(room: RoomBase)

const KEEP_BEHIND := 2
const R := "res://scripts/world/rooms/"

## the pool of normal rooms. weight = how common, min = first door it can show up at
var pool := [
	{"script": preload(R + "room_hallway.gd"), "weight": 10, "min": 1},
	{"script": preload(R + "room_plant.gd"), "weight": 6, "min": 1},
	{"script": preload(R + "room_turn.gd"), "weight": 8, "min": 2},
	{"script": preload(R + "room_locker.gd"), "weight": 7, "min": 3},
	{"script": preload(R + "room_three_locker.gd"), "weight": 7, "min": 1},
	{"script": preload(R + "room_meeting.gd"), "weight": 5, "min": 4},
	{"script": preload(R + "room_storage.gd"), "weight": 5, "min": 5},
	{"script": preload(R + "room_break.gd"), "weight": 4, "min": 6},
	{"script": preload(R + "room_four_locker.gd"), "weight": 4, "min": 1},
	{"script": preload(R + "room_cubicles.gd"), "weight": 6, "min": 8},
]
## fixed rooms at fixed doors
var special := {
	0: preload(R + "room_lobby.gd"),
	100: preload(R + "room_corridor_100.gd"),
	150: preload(R + "room_shop_150.gd"),
	1000: preload(R + "room_bridge_1000.gd"),
}
const EXIT_ROOM := preload(R + "room_exit.gd")
const HALLWAY := preload(R + "room_hallway.gd")

var rooms: Array[RoomBase] = []
var run_seed := 0
var _heading := 0       # -1, 0, 1: which way the hallway currently points
var _next_exit := 0     # door number of the next exit room
var _last_type := ""


func start(seed_value: int, first_door := 0) -> void:
	run_seed = seed_value
	_next_exit = 200 + _rng_for(7).randi_range(5, 60)
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


func _pick_script(n: int, rng: RandomNumberGenerator) -> Script:
	if special.has(n):
		return special[n]
	# exit rooms: on schedule every 50-100 rooms after a-200, and rarely earlier than planned
	if n > 200 and n < Game.LAST_DOOR - 5 and (n >= _next_exit or rng.randf() < 0.006):
		_next_exit = n + rng.randi_range(50, 100)
		return EXIT_ROOM
	var total := 0
	var options := []
	for p in pool:
		if n >= p.min and p.script.resource_path != _last_type:
			options.append(p)
			total += p.weight
	var roll := rng.randi_range(1, total)
	for p in options:
		roll -= p.weight
		if roll <= 0:
			return p.script
	return HALLWAY


func _spawn(n: int, at = null) -> RoomBase:
	if n > Game.LAST_DOOR:
		return null
	var rng := _rng_for(n)
	var script := _pick_script(n, rng)
	var r := _build(script, n, rng)
	if r == null:
		push_warning("room a-%03d failed to build, using a hallway" % n)
		r = _build(HALLWAY, n, rng)
	if r == null:
		Glitch.failsafe("room a-%03d could not be generated" % n)
		return null
	_last_type = script.resource_path
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
	n = clampi(n, 0, Game.LAST_DOOR)
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
