class_name LootCabinet
extends Node3D
## a long dresser against the wall with three wide drawers, like the ones you loot in doors.
## pull a drawer out and whatever's inside is lying in it: gold, a battery, a bandage,
## vitamins... take them one by one. (in the fixed office they're locked, like the desks)
## local space: stands on the floor at the origin, the drawers face +z.

const W := 1.7
const H := 0.95
const D := 0.5
const ROWS := 3

var loot_seed := 0
var _drawers: Array[Node3D] = []
var _its: Array = []
var _opened := [false, false, false]


func _ready() -> void:
	var body := StaticBody3D.new()
	body.collision_mask = 0
	add_child(body)
	var cs := CollisionShape3D.new()
	var sh := BoxShape3D.new()
	sh.size = Vector3(W, H, D)
	cs.shape = sh
	cs.position = Vector3(0, H / 2, 0)
	body.add_child(cs)
	var wood := Mats.get_mat("wood_dark")
	# the carcass: sides, top, back, a plinth
	_box(self, Vector3(W, 0.04, D + 0.02), Vector3(0, H - 0.02, 0.01), Mats.get_mat("wood"))
	_box(self, Vector3(0.04, H - 0.04, D), Vector3(-W / 2 + 0.02, (H - 0.04) / 2, 0), wood)
	_box(self, Vector3(0.04, H - 0.04, D), Vector3(W / 2 - 0.02, (H - 0.04) / 2, 0), wood)
	_box(self, Vector3(W, H - 0.04, 0.03), Vector3(0, (H - 0.04) / 2, -D / 2 + 0.015), wood)
	_box(self, Vector3(W - 0.08, 0.08, D - 0.04), Vector3(0, 0.04, -0.01), Mats.get_mat("dark"))
	var rh := (H - 0.14) / ROWS
	for i in ROWS:
		var dr := Node3D.new()
		dr.position = Vector3(0, 0.1 + i * rh, 0)
		add_child(dr)
		# the drawer box (open top) and its front with a long handle
		_box(dr, Vector3(W - 0.1, 0.02, D - 0.06), Vector3(0, 0.02, -0.02), Mats.get_mat("wood"))
		_box(dr, Vector3(W - 0.1, rh * 0.6, 0.02), Vector3(0, rh * 0.3, -D / 2 + 0.05), Mats.get_mat("wood"))
		for s in [-1.0, 1.0]:
			_box(dr, Vector3(0.02, rh * 0.6, D - 0.06), Vector3(s * (W / 2 - 0.06), rh * 0.3, -0.02), Mats.get_mat("wood"))
		_box(dr, Vector3(W - 0.08, rh - 0.02, 0.03), Vector3(0, rh / 2 - 0.01, D / 2 - 0.005), wood)
		_box(dr, Vector3(0.5, 0.025, 0.03), Vector3(0, rh / 2, D / 2 + 0.025), Mats.get_mat("metal"))
		_drawers.append(dr)
		var it := Interactable.make(Vector3(W - 0.1, rh - 0.02, 0.3), "open")
		it.position = Vector3(0, 0.1 + i * rh + rh / 2, D / 2 + 0.1)
		it.used.connect(_open.bind(i, it))
		add_child(it)
		_its.append(it)


func _box(parent: Node3D, size: Vector3, pos: Vector3, mat: Material) -> void:
	var mi := MeshInstance3D.new()
	var bm := BoxMesh.new()
	bm.size = size
	mi.mesh = bm
	mi.material_override = mat
	mi.position = pos
	parent.add_child(mi)


func _open(player, i: int, it: Interactable) -> void:
	if _opened[i]:
		return
	if Game.office_powered():
		player.notify("it's locked. you can't rob the place anymore")
		Game.play_ui("deny")
		return
	Net.send_event(self, "open", i)
	_pull(i, it)


## multiplayer: someone else opened a drawer here
func net_event(what: String, arg) -> void:
	if what != "open":
		return
	var i := int(arg)
	if i >= 0 and i < _its.size():
		_pull(i, _its[i])


## slide the drawer out and put the loot in it (the same loot on every machine)
func _pull(i: int, it) -> void:
	if _opened[i]:
		return
	_opened[i] = true
	if it and is_instance_valid(it):
		it.queue_free()
	var dr := _drawers[i]
	var a := AudioStreamPlayer3D.new()
	a.stream = Assets.sound("locker")
	a.pitch_scale = 1.4
	a.volume_db = -6.0
	dr.add_child(a)
	a.play()
	create_tween().tween_property(dr, "position:z", D * 0.75, 0.3).set_trans(Tween.TRANS_QUAD).set_ease(Tween.EASE_OUT)
	var rng := RandomNumberGenerator.new()
	rng.seed = loot_seed + i * 101
	var n := rng.randi_range(0, 2)
	if Game.mod("empty_pockets") and rng.randf() < 0.6:
		n = 0
	for k in n:
		var r := rng.randf()
		var item := "gold"
		var amount := rng.randi_range(5, 25)
		if r > 0.88:
			item = "vitamins"
			amount = 1
		elif r > 0.76:
			item = "bandage"
			amount = 1
		elif r > 0.58:
			item = "battery"
			amount = 1
		var p := Pickup.new()
		p.item = item
		p.amount = amount
		p.name = "loot_%d_%d" % [i, k]
		p.position = Vector3(-0.45 + k * 0.6 + rng.randf_range(-0.1, 0.1), 0.04, -0.02)
		dr.add_child(p)
