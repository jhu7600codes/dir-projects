class_name FireEscape
extends Node3D
## the FIRE ESCAPE box in a complex apartment: a red box on the wall with the fire alarm
## inside, and a striped square on the floor in front of it. pull the alarm and fire sweeps
## through the whole apartment: anything that isn't standing in the square burns. the
## anomaly gets knocked out for a while and the gate to the next floor opens.
## local space: the box is on the wall behind (-z), the square is in front (+z).

const FIRE_TIME := 4.0
const SAFE := 1.0  # half the size of the square

var room: Node3D
var used := false
var _it: Interactable


func _ready() -> void:
	_box(Vector3(0.7, 0.9, 0.18), Vector3(0, 1.35, 0.09), "extinguisher")
	_box(Vector3(0.24, 0.3, 0.06), Vector3(0, 1.3, 0.2), "fridge_white")
	var l := Label3D.new()
	l.text = "FIRE ESCAPE"
	l.font_size = 40
	l.pixel_size = 0.004
	l.modulate = Color(1, 1, 1)
	l.outline_size = 0
	l.position = Vector3(0, 1.95, 0.19)
	add_child(l)
	# the safe square, yellow and black stripes
	_box(Vector3(SAFE * 2, 0.01, SAFE * 2), Vector3(0, 0.006, 0.9), "caution")
	var glow := OmniLight3D.new()
	glow.light_color = Color(1.0, 0.3, 0.2)
	glow.omni_range = 2.0
	glow.light_energy = 0.7
	glow.position = Vector3(0, 1.4, 0.5)
	add_child(glow)
	_it = Interactable.make(Vector3(0.8, 1.0, 0.6), "pull the fire alarm")
	_it.position = Vector3(0, 1.3, 0.45)
	_it.used.connect(func(_p): pull())
	add_child(_it)


func _box(size: Vector3, pos: Vector3, mat: String) -> void:
	var mi := MeshInstance3D.new()
	var bm := BoxMesh.new()
	bm.size = size
	mi.mesh = bm
	mi.material_override = Mats.get_mat(mat)
	mi.position = pos
	add_child(mi)


## true if p (global) is standing in the square
func is_safe(p: Vector3) -> bool:
	var q := to_local(p)
	return absf(q.x) <= SAFE + 0.15 and absf(q.z - 0.9) <= SAFE + 0.15


func pull(from_net := false) -> void:
	if used:
		return
	used = true
	_it.enabled = false
	_it.prompt = "the alarm is ringing"
	if not from_net:
		Net.send_event(self, "pull")
	var alarm := AudioStreamPlayer.new()
	alarm.stream = Assets.sound("fire_alarm")
	alarm.bus = "SFX"
	add_child(alarm)
	alarm.play()
	# a moment of the alarm, then the fire
	get_tree().create_timer(1.2).timeout.connect(_burn)
	get_tree().create_timer(1.2 + FIRE_TIME + 1.0).timeout.connect(alarm.stop)


func _burn() -> void:
	if not is_instance_valid(room):
		return
	var b: AABB = room.apt_bounds
	var flames: Array = []
	for i in 40:
		var f := Sprite3D.new()
		f.texture = flame_texture()
		f.pixel_size = randf_range(1.2, 2.2) / 64.0
		f.billboard = BaseMaterial3D.BILLBOARD_FIXED_Y
		f.shaded = false
		f.modulate = Color(1, randf_range(0.5, 0.9), 0.3)
		room.add_child(f)
		var p := Vector3(randf_range(b.position.x, b.end.x), b.position.y + 0.6, randf_range(b.position.z, b.end.z))
		f.position = p
		if is_safe(room.to_global(p)):
			f.queue_free()
			continue
		flames.append(f)
	var fl := OmniLight3D.new()
	fl.light_color = Color(1.0, 0.45, 0.1)
	fl.light_energy = 4.0
	fl.omni_range = maxf(b.size.x, b.size.z)
	fl.position = b.get_center()
	room.add_child(fl)
	room.on_fire()
	_check_burn()
	var tw := create_tween()
	tw.tween_interval(FIRE_TIME)
	tw.tween_property(fl, "light_energy", 0.0, 1.0)
	for f in flames:
		tw.parallel().tween_property(f, "modulate:a", 0.0, 1.0)
	tw.tween_callback(func():
		fl.queue_free()
		for f in flames:
			if is_instance_valid(f):
				f.queue_free())
	# keep checking while it burns: walking out of the square is a bad idea
	for k in 4:
		get_tree().create_timer(0.8 * (k + 1)).timeout.connect(_check_burn)


func _check_burn() -> void:
	var p: Player = Game.player
	if p == null or p.dead or not is_instance_valid(room):
		return
	var local: Vector3 = room.to_local(p.global_position)
	var b: AABB = room.apt_bounds.grow(0.3)
	if b.has_point(local + Vector3(0, 0.5, 0)) and not is_safe(p.global_position):
		Game.death_details["fire"] = "outside"
		p.take_damage(999, "fire")


func net_event(what: String, _arg) -> void:
	if what == "pull":
		pull(true)


static var _flame: Texture2D


static func flame_texture() -> Texture2D:
	if _flame:
		return _flame
	var img := Image.create(64, 64, false, Image.FORMAT_RGBA8)
	img.fill(Color(0, 0, 0, 0))
	for y in 64:
		for x in 64:
			var dx := (x - 32) / 32.0
			var dy := (64 - y) / 64.0
			var width := (1.0 - dy) * (0.8 + 0.2 * sin(dy * 12.0))
			if absf(dx) < width:
				var a := clampf(1.0 - absf(dx) / maxf(0.01, width), 0.0, 1.0)
				img.set_pixel(x, y, Color(1.0, 0.9 - dy * 0.6, 0.3 - dy * 0.3, a))
	_flame = ImageTexture.create_from_image(img)
	return _flame
