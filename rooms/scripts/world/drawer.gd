class_name Drawer
extends Node3D
## office desk with a drawer. opening it gives some loot, mostly gold.
## local space: desk on the floor at the origin, the drawer side faces +z.

var loot_seed := 0
var _opened := false
var _drawer: MeshInstance3D


func _ready() -> void:
	var body := StaticBody3D.new()
	body.collision_mask = 0
	add_child(body)
	var cs := CollisionShape3D.new()
	var sh := BoxShape3D.new()
	sh.size = Vector3(1.4, 0.78, 0.7)
	cs.shape = sh
	cs.position = Vector3(0, 0.39, 0)
	body.add_child(cs)
	var m := Assets.model("desk")
	if m:
		add_child(m)
	else:
		_mesh(Vector3(1.4, 0.05, 0.7), Vector3(0, 0.76, 0), Mats.get_mat("wood"))
		_mesh(Vector3(0.05, 0.74, 0.66), Vector3(-0.66, 0.37, 0), Mats.get_mat("wood"))
		_mesh(Vector3(0.45, 0.74, 0.66), Vector3(0.45, 0.37, 0), Mats.get_mat("wood"))
	_drawer = _mesh(Vector3(0.4, 0.18, 0.6), Vector3(0.45, 0.6, 0.04), Mats.get_mat("plastic"))
	var it := Interactable.make(Vector3(0.6, 0.4, 0.4), "open drawer")
	it.position = Vector3(0.45, 0.6, 0.4)
	it.used.connect(_open.bind(it))
	add_child(it)


func _mesh(size: Vector3, pos: Vector3, mat: Material) -> MeshInstance3D:
	var mi := MeshInstance3D.new()
	var bm := BoxMesh.new()
	bm.size = size
	mi.mesh = bm
	mi.material_override = mat
	mi.position = pos
	add_child(mi)
	return mi


func _open(player, it: Interactable) -> void:
	if _opened:
		return
	_opened = true
	it.queue_free()
	create_tween().tween_property(_drawer, "position:z", 0.4, 0.25)
	var rng := RandomNumberGenerator.new()
	rng.seed = loot_seed
	var r := rng.randf()
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
