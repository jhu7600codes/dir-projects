class_name EntityManager
extends Node3D
## decides when entities spawn, using each entity script's RULES (see entity.gd),
## the door number, the room you just entered and cooldowns.
## to add an entity: write its script, give it RULES, add it to SCRIPTS. that's it.

const SCRIPTS := {
	"a60": preload("res://scripts/entities/a60.gd"),
	"a60b": preload("res://scripts/entities/a60b.gd"),
	"a90": preload("res://scripts/entities/a90.gd"),
	"a90b": preload("res://scripts/entities/a90b.gd"),
	"a120": preload("res://scripts/entities/a120.gd"),
	"a200": preload("res://scripts/entities/a200.gd"),
	# doors' hotel entities, only with the "got any more doors?" modifier
	"rush": preload("res://scripts/entities/rush.gd"),
	"ambush": preload("res://scripts/entities/ambush.gd"),
	"eyes": preload("res://scripts/entities/eyes.gd"),
	"screech": preload("res://scripts/entities/screech.gd"),
	"figure": preload("res://scripts/entities/figure.gd"),
	"seek": preload("res://scripts/entities/seek.gd"),
	# the wires subfloor (spawned by its rooms, not by chance)
	"w10": preload("res://scripts/entities/w10.gd"),
	"w50": preload("res://scripts/entities/w50.gd"),
	# from rooms: revisited
	"a70": preload("res://scripts/entities/a70.gd"),
	"a25": preload("res://scripts/entities/a25.gd"),
	# nico's unreleased star from rooms
	"a150": preload("res://scripts/entities/a150.gd"),
	# the coworkers, when you stare at one (spawned by the worker itself)
	"worker": preload("res://scripts/entities/overreact.gd"),
	# the great city
	"headlights": preload("res://scripts/entities/headlights.gd"),
	"billboard": preload("res://scripts/entities/billboard.gd"),
	"c15": preload("res://scripts/entities/c15.gd"),
	# the wires
	"w30": preload("res://scripts/entities/w30.gd"),
}
## chance that a-90 joins when a-60 or a-120 spawns (a-60 is slowed down when it does)
const A90_JOIN_CHANCE := 0.2

var active := {}         # id -> Entity
var frozen := false      # a-90 freezes everyone else
var natural_spawns := true  # admin panel / tests can turn off the automatic spawns
var time := 0.0
var _timers := {}        # id -> seconds until the next timer roll
var _cooldown := {}      # id -> time when it may spawn again
var _once_done := {}     # door_number entities that already came


func _ready() -> void:
	for id in SCRIPTS:
		if rules(id).get("trigger") == "timer":
			_timers[id] = _roll_timer(id)


var _rules_cache := {}


## an entity's RULES. a script that extends another entity uses OWN_RULES instead
func rules(id: String) -> Dictionary:
	if not _rules_cache.has(id):
		var c: Dictionary = SCRIPTS[id].get_script_constant_map()
		_rules_cache[id] = c.get("OWN_RULES", c.get("RULES", SCRIPTS[id].RULES))
	return _rules_cache[id]


func _roll_timer(id: String) -> float:
	var t: Array = rules(id).get("timer", [120.0, 300.0])
	var k := 1.0
	if id == "a90" and Game.mod("staring_contest"):
		k /= 3.0
	return randf_range(t[0], t[1]) * k


func _process(delta: float) -> void:
	if frozen or not natural_spawns or Game.player == null or Game.player.dead:
		return
	time += delta
	for id in _timers:
		var paused := false
		for other in rules(id).get("pause_timer_while", []):
			if active.has(other):
				paused = true
		if paused or active.has(id):
			continue
		_timers[id] -= delta
		if _timers[id] <= 0.0:
			_timers[id] = _roll_timer(id)
			if can_spawn(id) and randf() < _chance(id):
				spawn(id)


## called by the generator every time a door is opened
func on_room_entered(room: RoomBase) -> void:
	if not natural_spawns:
		return
	var n := room.number
	for id in SCRIPTS:
		var r := rules(id)
		if r.get("trigger") == "door_number" and n == int(r.at_door) and not _once_done.has(id):
			_once_done[id] = true
			if can_spawn(id, room) and randf() < float(r.get("chance", 1.0)):
				spawn(id)
	var door_ids := []
	for id in SCRIPTS:
		if rules(id).get("trigger") == "door":
			door_ids.append(id)
	door_ids.shuffle()
	for id in door_ids:
		if can_spawn(id, room) and randf() < _chance(id):
			var join := randf() < A90_JOIN_CHANCE and can_spawn("a90")
			spawn(id, {"slowed": join} if id == "a60" else {})
			if join:
				get_tree().create_timer(randf_range(2.0, 6.0)).timeout.connect(func():
					if can_spawn("a90"):
						spawn("a90"))
			break


func _chance(id: String) -> float:
	var c := _base_chance(id)
	if Game.mod("rush_hour") and id in ["a60", "a120"]:
		c *= 2.0
	return minf(1.0, c)


## the city only has 30 gates, so entities there use their own (much lower) first door
func _min_door(r: Dictionary) -> int:
	if Game.floor == "city":
		return int(r.get("city_min", 2))
	return int(r.get("min_door", 0))


func _base_chance(id: String) -> float:
	var r := rules(id)
	if Game.door >= _min_door(r):
		return float(r.get("chance", 1.0))
	return float(r.get("rare_chance", 0.0))


func can_spawn(id: String, room: RoomBase = null) -> bool:
	var r := rules(id)
	if active.has(id) or Game.player == null or Game.player.dead:
		return false
	if time < float(_cooldown.get(id, -1.0)):
		return false
	var n := Game.door
	var fl = r.get("floor", "offices")
	if (fl is Array and not fl.has(Game.floor)) or (fl is String and fl != Game.floor):
		return false
	if Game.office_powered():
		return false  # the entities are gone. well. they work here now
	if r.has("needs_mod") and not Game.mod(r.needs_mod):
		return false
	if r.get("needs_dark", false) and Game.darkness(n) < 0.35:
		return false
	var min_door := _min_door(r)
	var rare_min := 1 << 30 if Game.floor == "city" else int(r.get("rare_min", 1 << 30))
	if n < min_door and n < rare_min:
		return false
	if n == 0 or n >= Game.last_door():
		return false
	if room != null and r.get("needs_spawn_room", false) and not room.entity_spawn_ok:
		return false
	for b in r.get("blocked_by", []):
		if active.has(b):
			return false
	for other in active:
		if rules(other).get("group", "") == r.get("group", "-"):
			return false
	return true


## entities that only concern one player (your own staring, your own fake locker)
const LOCAL_IDS := ["a70", "worker"]


func spawn(id: String, opts := {}) -> Entity:
	# multiplayer: only the host decides spawns, everyone else gets them from the host
	if Net.active and not Net.is_host and not id in LOCAL_IDS:
		return null
	var s := randi()
	if Net.active and not id in LOCAL_IDS:
		seed(s)  # same dice rolls inside the entity on every machine
	var e := _spawn(id, opts)
	if e and Net.active and not id in LOCAL_IDS:
		Net.entity_spawned(id, opts, s)
	return e


## multiplayer: the host spawned this one
func spawn_remote(id: String, opts: Dictionary, seed_value: int) -> void:
	if active.has(id):
		active[id].queue_free()
		active.erase(id)
	seed(seed_value)
	var e := _spawn(id, opts, true)
	if e:
		var f := Entity.NetFollow.new()
		e.add_child(f)


func _spawn(id: String, opts := {}, remote := false) -> Entity:
	if active.has(id) or not SCRIPTS.has(id):
		return null
	if Game.generator == null or Game.generator.rooms.is_empty():
		return null
	var e: Entity = SCRIPTS[id].new()
	e.manager = self
	e.player = Game.player
	e.generator = Game.generator
	for k in opts:
		e.set(k, opts[k])
	e.remote = remote
	e.name = id
	add_child(e)
	active[id] = e
	e.finished.connect(_on_finished)
	e.begin()
	if Game.player and is_instance_valid(Game.player):
		Game.player.star_warn(id)
	print("[entities] %s spawned at %s" % [id, Game.door_label(Game.door)])
	return e


func _on_finished(e: Entity, survived: bool) -> void:
	if Net.active and Net.is_host and not e.id in LOCAL_IDS:
		Net.entity_gone(e.id)
	active.erase(e.id)
	_cooldown[e.id] = time + float(rules(e.id).get("cooldown", 0.0))
	if survived:
		Achievements.unlock("survive_" + e.id)


func clear_all() -> void:
	for id in active.keys():
		active[id].queue_free()
	active.clear()
	frozen = false
