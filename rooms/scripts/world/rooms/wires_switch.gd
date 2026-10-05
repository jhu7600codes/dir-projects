extends WiresRoom
## w-10, w-20, w-30, w-40: a breaker room. the door out has no power until you find the
## switch somewhere in here, and while it's off w-10 lives in the outlets on the walls.

var panel: PowerSwitch
var _w10_started := false


func build() -> void:
	room_type = "wires_switch"
	var length := rng.randf_range(11.0, 14.0)
	var w := 8.0
	shell(-w / 2, w / 2, length, 3.4, "front", rng.randf_range(-2.0, 2.0), "concrete_floor")
	var i := clampi(number / 10 - 1, 0, 4)
	var already: bool = Game.powered(Game.POWER_RANGES[i][0])
	# the panel goes somewhere on a side wall, not right by the doors
	var left := rng.randf() < 0.5
	var z := rng.randf_range(3.0, length - 3.0)
	panel = PowerSwitch.new()
	panel.range_i = i
	panel.on = already
	panel.position = Vector3((w / 2 - 0.08) * (1.0 if left else -1.0), 0, z)
	panel.rotation.y = -PI / 2 if left else PI / 2
	add_child(panel)
	panel.flipped.connect(_powered)
	# something in the way so the panel isn't the first thing you see
	for k in rng.randi_range(2, 4):
		crate(Vector3(rng.randf_range(-3.0, 3.0), 0, rng.randf_range(2.5, length - 2.5)), rng.randf_range(0.7, 1.0))
	for k in 2:
		box(Vector3(0.45, 3.4, 0.45), Vector3(rng.randf_range(-2.0, 2.0), 1.7, rng.randf_range(3.0, length - 3.0)), Mats.get_mat("pipe"), true)
	pipes_along(-w / 2 if left else w / 2, length, 3.4)
	raw_wires([Vector3(rng.randf_range(-2.5, 2.5), 0, rng.randf_range(3.0, length - 3.0))], 3.4)
	scatter_loot([Vector3(rng.randf_range(-3.0, 3.0), 0, length * 0.4)])
	make_path([Vector3(0, 0, length * 0.5)])
	if not already and exit_door:
		exit_door.locked = true


func _ready() -> void:
	super._ready()
	if exit_door and exit_door.locked:
		exit_door.code_lock(func(): Game.player.notify("no power. find the switch."), "no power", false)


func _process(delta: float) -> void:
	super._process(delta)
	# w-10 wakes up once you walk in while the power is still off
	if not _w10_started and not panel.on and Game.door == number and Game.entities:
		_w10_started = true
		Game.entities.spawn("w10", {"room_n": number})


func _powered() -> void:
	if exit_door:
		exit_door.unlock()
	if Game.entities and Game.entities.active.has("w10"):
		Game.entities.active["w10"].finish()
