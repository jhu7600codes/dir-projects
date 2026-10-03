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


func rules(id: String) -> Dictionary:
	return SCRIPTS[id].RULES


func _roll_timer(id: String) -> float:
	var t: Array = rules(id).get("timer", [120.0, 300.0])
	if id == "a90" and Game.mod("staring_contest"):
		return randf_range(t[0], t[1]) / 3.0
	return randf_range(t[0], t[1])


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
	if Game.mod("rush_hour") and id in ["a60", "a120"]:
		return minf(1.0, _base_chance(id) * 2.0)
	return _base_chance(id)


func _base_chance(id: String) -> float:
	var r := rules(id)
	if Game.door >= int(r.get("min_door", 0)):
		return float(r.get("chance", 1.0))
	return float(r.get("rare_chance", 0.0))


func can_spawn(id: String, room: RoomBase = null) -> bool:
	var r := rules(id)
	if active.has(id) or Game.player == null or Game.player.dead:
		return false
	if time < float(_cooldown.get(id, -1.0)):
		return false
	var n := Game.door
	if n < int(r.get("min_door", 0)) and n < int(r.get("rare_min", 1 << 30)):
		return false
	if n == 0 or n >= Game.LAST_DOOR:
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


func spawn(id: String, opts := {}) -> Entity:
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
	e.name = id
	add_child(e)
	active[id] = e
	e.finished.connect(_on_finished)
	e.begin()
	print("[entities] %s spawned at %s" % [id, Game.door_label(Game.door)])
	return e


func _on_finished(e: Entity, survived: bool) -> void:
	active.erase(e.id)
	_cooldown[e.id] = time + float(rules(e.id).get("cooldown", 0.0))
	if survived:
		Achievements.unlock("survive_" + e.id)


func clear_all() -> void:
	for id in active.keys():
		active[id].queue_free()
	active.clear()
	frozen = false
