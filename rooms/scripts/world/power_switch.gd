class_name PowerSwitch
extends Node3D
## a breaker panel on a wall in the wires. range_i 0-4 powers that part of the office
## (a-001..a-200 and so on), -1 is the elevator. local space: the panel faces +z.

signal flipped

var range_i := 0
var on := false
var requires := -1  # a power range that has to be on first (the elevator needs a-801..a-1000)
var _lamp: MeshInstance3D
var _lever: MeshInstance3D
var _it: Interactable


func _init() -> void:
	set_meta("no_merge", true)  # the lamp and lever change, keep them out of the room merge


func _ready() -> void:
	_box(Vector3(0.7, 0.9, 0.12), Vector3(0, 1.45, 0), "metal")
	_box(Vector3(0.74, 0.08, 0.13), Vector3(0, 1.0, 0), "caution")
	_lamp = _box(Vector3(0.12, 0.12, 0.05), Vector3(0.22, 1.75, 0.07), "switch_on" if on else "switch_off")
	_lever = _box(Vector3(0.08, 0.32, 0.08), Vector3(-0.1, 1.45, 0.1), "dark")
	_lever.rotation.x = -0.6 if on else 0.6
	var l := Label3D.new()
	if range_i < 0:
		l.text = "ELEVATOR POWER"
	else:
		var r: Array = Game.POWER_RANGES[range_i]
		l.text = "ENABLE ELECTRICITY FOR\n%s - %s" % [Game.a_label(r[0]), Game.a_label(r[1])]
	l.font_size = 36
	l.pixel_size = 0.004
	l.modulate = Color(0.95, 0.8, 0.25)
	l.outline_size = 0
	l.position = Vector3(0, 2.15, 0.07)
	add_child(l)
	_it = Interactable.make(Vector3(0.8, 1.0, 0.5), "flip the switch")
	_it.position = Vector3(0, 1.45, 0.3)
	_it.used.connect(func(_p): flip())
	add_child(_it)
	_it.enabled = not on
	if on:
		_it.prompt = "already on"


func _box(size: Vector3, pos: Vector3, mat: String) -> MeshInstance3D:
	var mi := MeshInstance3D.new()
	var bm := BoxMesh.new()
	bm.size = size
	mi.mesh = bm
	mi.material_override = Mats.get_mat(mat)
	mi.position = pos
	add_child(mi)
	return mi


func flip() -> void:
	if on:
		return
	if requires >= 0 and not Game.powered(Game.POWER_RANGES[requires][0]):
		var r: Array = Game.POWER_RANGES[requires]
		if Game.player:
			Game.player.notify("nothing happens. power %s - %s first" % [Game.a_label(r[0]), Game.a_label(r[1])])
		return
	on = true
	_it.enabled = false
	_it.prompt = "already on"
	_lamp.material_override = Mats.get_mat("switch_on")
	create_tween().tween_property(_lever, "rotation:x", -0.6, 0.15)
	var a := AudioStreamPlayer3D.new()
	a.stream = Assets.sound("switch_flip")
	a.bus = "SFX"
	add_child(a)
	a.play()
	if range_i >= 0:
		Game.power[range_i] = true
		var r: Array = Game.POWER_RANGES[range_i]
		if Game.player:
			Game.player.notify("power is back on for %s - %s" % [Game.a_label(r[0]), Game.a_label(r[1])])
	else:
		Game.elevator_power = true
	Game.play_ui("power_on")
	flipped.emit()
