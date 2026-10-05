extends WiresRoom
## w-50, the last room of the wires. two panels near the door: the power for a-801..a-1000
## (w-10 guards it like the other breaker rooms) and the elevator power. turn the elevator
## on and w-50 "the shock" comes for you: run to the far end, where the elevator from a-001
## is waiting to take you back up into a powered office.

const LENGTH := 24.0
const W := 10.0
const EL_W := 1.8

var panel: PowerSwitch
var elevator_panel: PowerSwitch
var _w10_started := false
var _doors: Array[MeshInstance3D] = []
var _inside: Area3D
var _open := false
var _done := false


func build() -> void:
	room_type = "wires_end"
	shell(-W / 2, W / 2, LENGTH, 3.6, "none", 0.0, "concrete_floor", {"front": [[W / 2, EL_W + 0.4]]})
	panel = PowerSwitch.new()
	panel.range_i = 4
	panel.on = Game.powered(Game.POWER_RANGES[4][0])
	panel.position = Vector3(W / 2 - 0.08, 0, 4.0)
	panel.rotation.y = -PI / 2
	add_child(panel)
	panel.flipped.connect(_range_on)
	elevator_panel = PowerSwitch.new()
	elevator_panel.range_i = -1
	elevator_panel.requires = 4
	elevator_panel.position = Vector3(-W / 2 + 0.08, 0, 6.5)
	elevator_panel.rotation.y = PI / 2
	add_child(elevator_panel)
	elevator_panel.flipped.connect(_elevator_on)
	stencil("ELEVATOR", Vector3(0, 3.0, LENGTH - 0.12), PI, 64)
	# the long way to the elevator: pipes, crates, wires
	for i in 4:
		box(Vector3(0.5, 3.6, 0.5), Vector3(rng.randf_range(-3.5, 3.5), 1.8, 9.0 + i * 3.5), Mats.get_mat("pipe"), true)
	for i in 3:
		crate(Vector3(rng.randf_range(-4.0, 4.0), 0, rng.randf_range(10.0, 20.0)), 0.8)
	var spots := []
	for i in 4:
		spots.append(Vector3(rng.randf_range(-3.0, 3.0), 0, 10.0 + i * 3.2))
	raw_wires(spots, 3.6)
	pipes_along(W / 2, LENGTH, 3.6)
	pipes_along(-W / 2, LENGTH, 3.6)
	_build_elevator()
	make_path([Vector3(0, 0, 5.0), Vector3(0, 0, LENGTH - 2.0)])


## the same elevator as the one up in the office: a little metal box behind two doors
func _build_elevator() -> void:
	var z0 := LENGTH
	var d := 2.2
	var metal := Mats.get_mat("door_metal")
	box(Vector3(EL_W + 0.4, 0.2, d), Vector3(0, -0.1, z0 + d / 2), metal)
	box(Vector3(EL_W + 0.4, 0.2, d), Vector3(0, 2.6, z0 + d / 2), metal, false)
	box(Vector3(EL_W + 0.4, 2.6, 0.2), Vector3(0, 1.3, z0 + d), metal)
	box(Vector3(0.2, 2.6, d), Vector3(-EL_W / 2 - 0.2, 1.3, z0 + d / 2), metal)
	box(Vector3(0.2, 2.6, d), Vector3(EL_W / 2 + 0.2, 1.3, z0 + d / 2), metal)
	box(Vector3(0.3, 0.1, 0.3), Vector3(0, 2.45, z0 + d / 2), Mats.get_mat("light_panel"), false)
	var l := OmniLight3D.new()
	l.position = Vector3(0, 2.2, z0 + d / 2)
	l.light_color = Color(1.0, 0.95, 0.85)
	l.light_energy = 1.2
	l.omni_range = 4.0
	add_child(l)
	# sliding doors
	for s in [-1.0, 1.0]:
		var door := MeshInstance3D.new()
		var bm := BoxMesh.new()
		bm.size = Vector3(EL_W / 2, 2.4, 0.06)
		door.mesh = bm
		door.material_override = metal
		door.position = Vector3(s * EL_W / 4, 1.2, z0 + 0.05)
		door.set_meta("no_merge", true)  # they slide
		add_child(door)
		_doors.append(door)
	# the doors block you until they open
	var stop := StaticBody3D.new()
	stop.name = "ElevatorStop"
	var cs := CollisionShape3D.new()
	var sh := BoxShape3D.new()
	sh.size = Vector3(EL_W, 2.4, 0.1)
	cs.shape = sh
	cs.position = Vector3(0, 1.2, z0 + 0.05)
	stop.add_child(cs)
	add_child(stop)
	_inside = Area3D.new()
	_inside.collision_layer = 0
	_inside.collision_mask = 2
	var cs2 := CollisionShape3D.new()
	var sh2 := BoxShape3D.new()
	sh2.size = Vector3(EL_W - 0.2, 2.0, d - 0.6)
	cs2.shape = sh2
	cs2.position = Vector3(0, 1.0, z0 + d / 2 + 0.2)
	_inside.add_child(cs2)
	add_child(_inside)
	_inside.body_entered.connect(_on_enter)


func _process(delta: float) -> void:
	super._process(delta)
	if not _w10_started and not panel.on and Game.door == number and Game.entities:
		_w10_started = true
		Game.entities.spawn("w10", {"room_n": number})


func _range_on() -> void:
	if Game.entities and Game.entities.active.has("w10"):
		Game.entities.active["w10"].finish()


func _elevator_on() -> void:
	# the shock wakes up at the far side of the room. run!
	if Game.entities:
		Game.entities.spawn("w50", {"room_n": number})
	Game.subtitle.emit("run to the elevator!", Color(1.0, 0.9, 0.5))
	get_tree().create_timer(2.0).timeout.connect(open_elevator)


func open_elevator() -> void:
	if _open:
		return
	_open = true
	var a := AudioStreamPlayer3D.new()
	a.stream = Assets.sound("elevator")
	a.bus = "SFX"
	a.position = Vector3(0, 1.5, LENGTH)
	add_child(a)
	a.play()
	for i in _doors.size():
		var s := -1.0 if i == 0 else 1.0
		create_tween().tween_property(_doors[i], "position:x", s * (EL_W * 0.75 + 0.1), 0.8)
	var stop := get_node_or_null("ElevatorStop")
	if stop:
		stop.queue_free()


func _on_enter(body: Node) -> void:
	if not _open or _done or not (body is Player) or body.dead:
		return
	_done = true
	# doors close behind you, and up you go
	for i in _doors.size():
		var s := -1.0 if i == 0 else 1.0
		create_tween().tween_property(_doors[i], "position:x", s * EL_W / 4, 0.6)
	if Game.entities:
		Game.entities.clear_all()
	get_tree().create_timer(1.2).timeout.connect(func(): Game.run_finished.emit("wires"))
