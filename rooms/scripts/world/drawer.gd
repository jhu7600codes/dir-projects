class_name Drawer
extends Node3D
## office desk with a drawer. opening it gives some loot, mostly gold.
## local space: desk on the floor at the origin, the drawer side faces +z.

var loot_seed := 0
var _opened := false
var _drawer: Node3D
var _static: Node3D
const FRONT_Z := 0.335  # front face of the pedestal


## called by the room right after loot_seed is set, before the room merges its static meshes.
## everything that never moves goes under _static so the merge picks it up
func build() -> void:
	_static = Node3D.new()
	_static.name = "Static"
	add_child(_static)
	var body := StaticBody3D.new()
	body.collision_mask = 0
	add_child(body)
	var cs := CollisionShape3D.new()
	var sh := BoxShape3D.new()
	sh.size = Vector3(1.4, Props.TABLE_H, 0.7)
	cs.shape = sh
	cs.position = Vector3(0, Props.TABLE_H / 2, 0)
	body.add_child(cs)
	var m := Assets.model("desk")
	if m:
		_static.add_child(m)
	else:
		var h := Props.TABLE_H
		_mesh(Vector3(1.4, 0.05, 0.7), Vector3(0, h - 0.025, 0), Mats.get_mat("wood"), _static)
		_mesh(Vector3(0.05, h - 0.05, 0.66), Vector3(-0.66, (h - 0.05) / 2, 0), Mats.get_mat("wood"), _static)
		_mesh(Vector3(0.45, h - 0.05, 0.66), Vector3(0.45, (h - 0.05) / 2, 0), Mats.get_mat("wood"), _static)
	_build_drawers()
	_nameplate()
	var it := Interactable.make(Vector3(0.6, 0.4, 0.4), "open drawer")
	it.position = Vector3(0.45, Props.TABLE_H - 0.2, 0.4)
	it.used.connect(_open.bind(it))
	add_child(it)


## three drawers stacked in the pedestal, each with a dark gap and a metal handle.
## only the top one opens.
func _build_drawers() -> void:
	var h := Props.TABLE_H - 0.05
	var dh := h / 3.0
	for i in 3:
		var y := h - dh * (i + 0.5)
		var d := Node3D.new()
		d.position = Vector3(0.45, y, FRONT_Z)
		(self if i == 0 else _static).add_child(d)
		_mesh(Vector3(0.43, dh - 0.005, 0.01), Vector3(0, 0, -0.004), Mats.get_mat("dark"), d)  # gap around the front
		_mesh(Vector3(0.41, dh - 0.03, 0.025), Vector3(0, 0, 0.012), Mats.get_mat("wood_dark"), d)
		_mesh(Vector3(0.16, 0.022, 0.022), Vector3(0, dh * 0.18, 0.04), Mats.get_mat("metal"), d)
		for sx in [-0.07, 0.07]:
			_mesh(Vector3(0.015, 0.015, 0.03), Vector3(sx, dh * 0.18, 0.025), Mats.get_mat("metal"), d)
		if i == 0:
			# the tray behind the front, hidden in the pedestal until it slides out
			_mesh(Vector3(0.38, dh - 0.05, 0.5), Vector3(0, -0.01, -0.25), Mats.get_mat("wood_dark"), d)
			_mesh(Vector3(0.34, 0.01, 0.46), Vector3(0, dh * 0.5 - 0.035, -0.25), Mats.get_mat("dark"), d)
			_drawer = d


## a little name sign on the desk. it almost always says worker.
func _nameplate() -> void:
	var rng := RandomNumberGenerator.new()
	rng.seed = loot_seed + 77
	var plate := Node3D.new()
	plate.position = Vector3(-0.3, Props.TABLE_H, 0.22)
	plate.rotation.x = -0.35
	_static.add_child(plate)
	_mesh(Vector3(0.26, 0.07, 0.015), Vector3(0, 0.035, 0), Mats.get_mat("dark"), plate)
	var l := Label3D.new()
	l.text = "Julian" if rng.randf() < 0.04 else "Worker"
	l.font_size = 40
	l.pixel_size = 0.0012
	l.modulate = Color(0.9, 0.8, 0.45)
	l.outline_size = 0
	l.position = Vector3(0, 0.035, 0.009)
	plate.add_child(l)


func _mesh(size: Vector3, pos: Vector3, mat: Material, parent: Node = null) -> MeshInstance3D:
	var mi := MeshInstance3D.new()
	var bm := BoxMesh.new()
	bm.size = size
	mi.mesh = bm
	mi.material_override = mat
	mi.position = pos
	(parent if parent else self).add_child(mi)
	return mi


func _open(player, it: Interactable) -> void:
	if _opened:
		return
	_opened = true
	it.queue_free()
	create_tween().tween_property(_drawer, "position:z", FRONT_Z + 0.38, 0.25)
	var rng := RandomNumberGenerator.new()
	rng.seed = loot_seed
	var r := rng.randf()
	if Game.mod("empty_pockets") and rng.randf() < 0.65:
		r = 1.0  # empty
	if r < 0.55:
		player.inventory.add("gold", rng.randi_range(5, 30))
	elif r < 0.7:
		player.inventory.add("battery", 1)
	elif r < 0.8:
		player.inventory.add("bandage", 1)
	elif r < 0.85:
		player.inventory.add("vitamins", 1)
	else:
		player.notify("empty")
