class_name Drawer
extends Node3D
## office desk with a drawer. opening it gives some loot, mostly gold.
## local space: desk on the floor at the origin, the drawer side faces +z.

var loot_seed := 0
var gives_key := false  # a-240: this drawer has the management key
var _opened := false
var _drawer: Node3D
var _static: Node3D
const FRONT_Z := 0.335  # front face of the pedestal


## the desk is built from a few meshes that every desk shares (one per material), made
## once and cached. building ~25 little boxes per desk made the cubicle room hitch.
static var _desk_cache := {}  # material key -> ArrayMesh (desk, two fixed drawers, nameplate)
static var _top_cache := {}   # material key -> ArrayMesh (the drawer that opens)


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
	make_cache()
	for key in _desk_cache:
		if m and key == "wood":
			continue  # the desk model replaces the plain wooden desk
		_shared(_desk_cache[key], key, _static)
	_drawer = Node3D.new()
	_drawer.position = Vector3(0.45, (Props.TABLE_H - 0.05) * 5.0 / 6.0, FRONT_Z)
	add_child(_drawer)
	for key in _top_cache:
		_shared(_top_cache[key], key, _drawer)
	_nameplate()
	var it := Interactable.make(Vector3(0.6, 0.4, 0.4), "open drawer")
	it.position = Vector3(0.45, Props.TABLE_H - 0.2, 0.4)
	it.used.connect(_open.bind(it))
	add_child(it)


func _shared(mesh: Mesh, key: String, parent: Node) -> void:
	var mi := MeshInstance3D.new()
	mi.mesh = mesh
	mi.material_override = Mats.get_mat(key)
	parent.add_child(mi)


## builds the shared desk meshes once (Prewarm calls this at startup too)
static func make_cache() -> void:
	if not _desk_cache.is_empty():
		return
	var desk := {}
	var top := {}
	var h := Props.TABLE_H
	_box(desk, "wood", Vector3(1.4, 0.05, 0.7), Transform3D(Basis(), Vector3(0, h - 0.025, 0)))
	_box(desk, "wood", Vector3(0.05, h - 0.05, 0.66), Transform3D(Basis(), Vector3(-0.66, (h - 0.05) / 2, 0)))
	_box(desk, "wood", Vector3(0.45, h - 0.05, 0.66), Transform3D(Basis(), Vector3(0.45, (h - 0.05) / 2, 0)))
	# three drawers stacked in the pedestal, each with a dark gap and a metal handle.
	# only the top one opens (it gets its own mesh so it can slide out)
	var dh := (h - 0.05) / 3.0
	for i in 3:
		var into := top if i == 0 else desk
		var o := Vector3.ZERO if i == 0 else Vector3(0.45, (h - 0.05) - dh * (i + 0.5), FRONT_Z)
		_box(into, "dark", Vector3(0.43, dh - 0.005, 0.01), Transform3D(Basis(), o + Vector3(0, 0, -0.004)))
		_box(into, "wood_dark", Vector3(0.41, dh - 0.03, 0.025), Transform3D(Basis(), o + Vector3(0, 0, 0.012)))
		_box(into, "metal", Vector3(0.16, 0.022, 0.022), Transform3D(Basis(), o + Vector3(0, dh * 0.18, 0.04)))
		for sx in [-0.07, 0.07]:
			_box(into, "metal", Vector3(0.015, 0.015, 0.03), Transform3D(Basis(), o + Vector3(sx, dh * 0.18, 0.025)))
		if i == 0:
			# the tray behind the front, hidden in the pedestal until it slides out
			_box(top, "wood_dark", Vector3(0.38, dh - 0.05, 0.5), Transform3D(Basis(), Vector3(0, -0.01, -0.25)))
			_box(top, "dark", Vector3(0.34, 0.01, 0.46), Transform3D(Basis(), Vector3(0, dh * 0.5 - 0.035, -0.25)))
	# the nameplate's little stand
	var plate := Transform3D(Basis(Vector3.RIGHT, -0.35), Vector3(-0.3, h, 0.22))
	_box(desk, "dark", Vector3(0.26, 0.07, 0.015), plate * Transform3D(Basis(), Vector3(0, 0.035, 0)))
	for k in desk:
		_desk_cache[k] = (desk[k] as SurfaceTool).commit()
	for k in top:
		_top_cache[k] = (top[k] as SurfaceTool).commit()


static func _box(into: Dictionary, key: String, size: Vector3, xf: Transform3D) -> void:
	if not into.has(key):
		var st := SurfaceTool.new()
		st.begin(Mesh.PRIMITIVE_TRIANGLES)
		into[key] = st
	var bm := BoxMesh.new()
	bm.size = size
	(into[key] as SurfaceTool).append_from(bm, 0, xf)


## a little name sign on the desk. it almost always says worker.
func _nameplate() -> void:
	var rng := RandomNumberGenerator.new()
	rng.seed = loot_seed + 77
	if rng.randf() < 0.5:
		return  # not every desk has one (each sign is a bit of text to lay out, keep rooms cheap)
	var l := Label3D.new()
	l.text = "Julian" if rng.randf() < 0.04 else "Worker"
	l.font_size = 40
	l.pixel_size = 0.0012
	l.modulate = Color(0.9, 0.8, 0.45)
	l.outline_size = 0
	l.transform = Transform3D(Basis(Vector3.RIGHT, -0.35), Vector3(-0.3, Props.TABLE_H, 0.22)) * Transform3D(Basis(), Vector3(0, 0.035, 0.009))
	add_child(l)


func _open(player, it: Interactable) -> void:
	if _opened:
		return
	if Game.office_powered() and not gives_key:
		player.notify("it's locked. you can't rob the place anymore")
		Game.play_ui("deny")
		return
	_opened = true
	it.queue_free()
	create_tween().tween_property(_drawer, "position:z", FRONT_Z + 0.38, 0.25)
	if gives_key:
		Game.has_key = true
		Game.play_ui("pickup")
		player.notify("you found the MANAGEMENT KEY")
		return
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
