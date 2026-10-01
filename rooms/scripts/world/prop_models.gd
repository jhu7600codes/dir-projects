class_name PropModels
## nicer built-in furniture and plant models (used when the manifest has no real model).
## plants are made of actual leaf meshes; their meshes are generated once and shared.
## everything returns a Node3D standing on the floor at its origin, facing +z.

static var _cache := {}
static var _mats := {}


# ---- plants ---------------------------------------------------------------

## kind: "fern", "snake" or "bush". variant picks one of a few pre-made shapes.
static func plant(kind: String, variant: int) -> Node3D:
	var root := Node3D.new()
	# tapered pot with a rim and soil
	var pot := _mat("pot", Color(0.62, 0.36, 0.24), 0.85)
	if kind == "snake":
		pot = _mat("pot_white", Color(0.9, 0.9, 0.88), 0.4)
	_cyl(root, 0.17, 0.125, 0.32, Vector3(0, 0.16, 0), pot)
	_cyl(root, 0.185, 0.185, 0.045, Vector3(0, 0.3, 0), pot)
	_cyl(root, 0.16, 0.16, 0.02, Vector3(0, 0.305, 0), _mat("soil", Color(0.16, 0.11, 0.08), 1.0))
	var mi := MeshInstance3D.new()
	mi.mesh = _leaves(kind, variant)
	mi.material_override = _leaf_mat()
	mi.position.y = 0.31
	root.add_child(mi)
	if kind == "bush":
		# a dark core so you can't see through the middle
		var core := MeshInstance3D.new()
		var sm := SphereMesh.new()
		sm.radius = 0.2
		sm.height = 0.36
		sm.radial_segments = 12
		sm.rings = 6
		core.mesh = sm
		core.material_override = _mat("leaf_dark", Color(0.08, 0.22, 0.09), 1.0)
		core.position.y = 0.52
		root.add_child(core)
	return root


static func _leaves(kind: String, variant: int) -> ArrayMesh:
	var key := "leaves_%s_%d" % [kind, variant]
	if _cache.has(key):
		return _cache[key]
	var rng := RandomNumberGenerator.new()
	rng.seed = hash(key)
	var st := SurfaceTool.new()
	st.begin(Mesh.PRIMITIVE_TRIANGLES)
	match kind:
		"fern":
			# big arching fronds, like an office palm
			var n := rng.randi_range(13, 17)
			for i in n:
				var a := TAU * i / n + rng.randf_range(-0.2, 0.2)
				var out := Vector3(cos(a), 0, sin(a))
				_leaf(st, rng, Vector3.ZERO, out, rng.randf_range(0.45, 0.7), rng.randf_range(0.09, 0.13), rng.randf_range(0.45, 0.8), rng.randf_range(0.5, 0.8))
			for i in 5:
				var a := rng.randf() * TAU
				_leaf(st, rng, Vector3.ZERO, Vector3(cos(a), 0, sin(a)), rng.randf_range(0.35, 0.5), 0.08, 1.4, 0.4)
		"snake":
			# tall stiff upright blades
			var n := rng.randi_range(8, 11)
			for i in n:
				var a := rng.randf() * TAU
				var r := rng.randf_range(0.0, 0.1)
				var base := Vector3(cos(a) * r, 0, sin(a) * r)
				_leaf(st, rng, base, Vector3(cos(a), 0, sin(a)) * 0.25, rng.randf_range(0.5, 0.75), rng.randf_range(0.06, 0.09), 1.1, 0.02)
		_:
			# round leafy bush: lots of short leaves pointing every way
			for i in 70:
				var dir := Vector3(rng.randf_range(-1, 1), rng.randf_range(-0.3, 1), rng.randf_range(-1, 1)).normalized()
				var base := Vector3(0, 0.22, 0) + dir * 0.12
				var flat := Vector3(dir.x, 0, dir.z)
				if flat.length() < 0.05:
					flat = Vector3(1, 0, 0)
				_leaf(st, rng, base, flat.normalized(), rng.randf_range(0.16, 0.24), rng.randf_range(0.07, 0.1), dir.y * 1.6 + 0.3, 0.15)
	st.generate_normals()
	var mesh := st.commit()
	_cache[key] = mesh
	return mesh


## one leaf: a pointed strip that rises then droops. color varies a bit per leaf.
static func _leaf(st: SurfaceTool, rng: RandomNumberGenerator, base: Vector3, out: Vector3, length: float, width: float, rise: float, droop: float) -> void:
	const SEG := 6
	var axis := out.normalized()
	var side := axis.cross(Vector3.UP).normalized()
	var col := Color(0.16, 0.42, 0.17).lerp(Color(0.33, 0.6, 0.22), rng.randf())
	var twist := rng.randf_range(-0.4, 0.4)
	var pts: Array[Vector3] = []
	for i in SEG + 1:
		var t := float(i) / SEG
		var p := base + out * length * t + Vector3.UP * length * (rise * t - droop * t * t * 1.6)
		pts.append(p)
	for i in SEG:
		var t0 := float(i) / SEG
		var t1 := float(i + 1) / SEG
		var w0 := width * pow(sin(PI * clampf(t0 * 0.95 + 0.05, 0, 1)), 0.7)
		var w1 := width * pow(sin(PI * clampf(t1 * 0.95 + 0.05, 0, 1)), 0.7)
		var s0 := side.rotated(axis, twist * t0)
		var s1 := side.rotated(axis, twist * t1)
		var a := pts[i] - s0 * w0 * 0.5
		var b := pts[i] + s0 * w0 * 0.5
		var c := pts[i + 1] + s1 * w1 * 0.5
		var d := pts[i + 1] - s1 * w1 * 0.5
		# slight fold along the middle vein looks more natural
		var mid0 := pts[i] + Vector3.UP * w0 * 0.12
		var mid1 := pts[i + 1] + Vector3.UP * w1 * 0.12
		for tri in [[a, mid0, mid1], [a, mid1, d], [mid0, b, c], [mid0, c, mid1]]:
			for v in tri:
				st.set_color(col)
				st.add_vertex(v)


static func _leaf_mat() -> StandardMaterial3D:
	if _mats.has("leaf"):
		return _mats.leaf
	var m := StandardMaterial3D.new()
	m.vertex_color_use_as_albedo = true
	m.cull_mode = BaseMaterial3D.CULL_DISABLED
	m.roughness = 0.7
	m.backlight_enabled = true
	m.backlight = Color(0.15, 0.3, 0.1)
	_mats.leaf = m
	return m


# ---- furniture --------------------------------------------------------------

## office chair: 5 legs with wheels, gas lift, seat, backrest, armrests
static func office_chair(seat_h: float) -> Node3D:
	var root := Node3D.new()
	var black := _mat("chair_black", Color(0.07, 0.07, 0.08), 0.7)
	var metal := _mat("chrome", Color(0.6, 0.6, 0.62), 0.25, 0.9)
	var cloth := _mat("chair_cloth", Color(0.13, 0.14, 0.17), 1.0)
	for i in 5:
		var a := TAU * i / 5.0
		var leg := Node3D.new()
		leg.rotation.y = a
		root.add_child(leg)
		_box(leg, Vector3(0.05, 0.04, 0.3), Vector3(0, 0.08, 0.15), black)
		var wheel := MeshInstance3D.new()
		var sm := SphereMesh.new()
		sm.radius = 0.035
		sm.height = 0.07
		sm.radial_segments = 6
		sm.rings = 3
		wheel.mesh = sm
		wheel.material_override = black
		wheel.position = Vector3(0, 0.035, 0.29)
		leg.add_child(wheel)
	_cyl(root, 0.025, 0.025, seat_h - 0.1, Vector3(0, (seat_h - 0.1) / 2 + 0.08, 0), metal)
	_box(root, Vector3(0.5, 0.06, 0.48), Vector3(0, seat_h, 0), black)
	_box(root, Vector3(0.48, 0.07, 0.46), Vector3(0, seat_h + 0.06, 0.01), cloth)
	_box(root, Vector3(0.05, 0.3, 0.05), Vector3(0, seat_h + 0.2, -0.24), black)
	_box(root, Vector3(0.46, 0.5, 0.07), Vector3(0, seat_h + 0.45, -0.26), cloth)
	for sx in [-1.0, 1.0]:
		_box(root, Vector3(0.04, 0.2, 0.04), Vector3(sx * 0.25, seat_h + 0.13, 0), black)
		_box(root, Vector3(0.07, 0.04, 0.3), Vector3(sx * 0.25, seat_h + 0.24, 0), black)
	return root


## couch: frame, 3 seat cushions, 3 back cushions, arms, little legs
static func couch() -> Node3D:
	var root := Node3D.new()
	var frame := _mat("couch", Color(0.09, 0.09, 0.1), 0.9)
	var cushion := _mat("couch_cushion", Color(0.12, 0.12, 0.13), 1.0)
	var legs := _mat("couch_legs", Color(0.3, 0.2, 0.12), 0.6)
	_box(root, Vector3(2.0, 0.28, 0.85), Vector3(0, 0.24, 0), frame)
	_box(root, Vector3(2.0, 0.6, 0.22), Vector3(0, 0.5, -0.33), frame)
	for i in 3:
		var x := -0.6 + i * 0.6
		_box(root, Vector3(0.58, 0.13, 0.6), Vector3(x, 0.44, 0.08), cushion)
		_box(root, Vector3(0.58, 0.42, 0.14), Vector3(x, 0.72, -0.17), cushion)
	for sx in [-1.0, 1.0]:
		_box(root, Vector3(0.2, 0.55, 0.85), Vector3(sx * 0.95, 0.38, 0), frame)
		for sz in [-1.0, 1.0]:
			_box(root, Vector3(0.06, 0.1, 0.06), Vector3(sx * 0.9, 0.05, sz * 0.36), legs)
	return root


## metal shelf with uprights, 4 boards and some boxes / binders on it
static func shelf(rng: RandomNumberGenerator) -> Node3D:
	var root := Node3D.new()
	var metal := _mat("shelf_metal", Color(0.5, 0.52, 0.55), 0.45, 0.6)
	for sx in [-0.68, 0.68]:
		for sz in [-0.2, 0.2]:
			_box(root, Vector3(0.04, 2.0, 0.04), Vector3(sx, 1.0, sz), metal)
	var colors := [Color(0.75, 0.6, 0.4), Color(0.2, 0.3, 0.6), Color(0.6, 0.15, 0.15), Color(0.85, 0.85, 0.8), Color(0.25, 0.45, 0.25)]
	for i in 4:
		var y := 0.12 + i * 0.6
		_box(root, Vector3(1.4, 0.03, 0.44), Vector3(0, y, 0), metal)
		if i == 3:
			continue
		var x := -0.6
		while x < 0.55:
			if rng.randf() < 0.3:
				x += 0.15
				continue
			if rng.randf() < 0.5:
				# cardboard box
				var w := rng.randf_range(0.25, 0.4)
				var h := rng.randf_range(0.2, 0.35)
				_box(root, Vector3(w, h, 0.35), Vector3(x + w / 2, y + 0.015 + h / 2, 0), _mat("cardboard", colors[0], 0.95))
				x += w + 0.03
			else:
				# a few binders
				for k in rng.randi_range(2, 5):
					var c: Color = colors[rng.randi_range(1, colors.size() - 1)]
					_box(root, Vector3(0.05, 0.3, 0.26), Vector3(x + 0.025, y + 0.165, 0.05), _mat("binder_%s" % c.to_html(), c, 0.6))
					x += 0.055
				x += 0.05
	return root


## fridge with a freezer door, gaps and handles
static func fridge() -> Node3D:
	var root := Node3D.new()
	var white := _mat("fridge", Color(0.9, 0.9, 0.88), 0.35, 0.1)
	var dark := _mat("fridge_gap", Color(0.2, 0.2, 0.2), 0.8)
	var chrome := _mat("chrome", Color(0.6, 0.6, 0.62), 0.25, 0.9)
	_box(root, Vector3(0.8, 1.9, 0.68), Vector3(0, 0.95, -0.01), white)
	_box(root, Vector3(0.8, 0.015, 0.02), Vector3(0, 1.32, 0.335), dark)
	_box(root, Vector3(0.03, 0.4, 0.04), Vector3(-0.33, 1.05, 0.36), chrome)
	_box(root, Vector3(0.03, 0.25, 0.04), Vector3(-0.33, 1.5, 0.36), chrome)
	_box(root, Vector3(0.8, 0.06, 0.6), Vector3(0, 0.03, 0), dark)
	return root


# ---- helpers ------------------------------------------------------------------

static func _mat(key: String, c: Color, rough: float, metal := 0.0) -> StandardMaterial3D:
	if _mats.has(key):
		return _mats[key]
	var m := StandardMaterial3D.new()
	m.albedo_color = c
	m.roughness = rough
	m.metallic = metal
	_mats[key] = m
	return m


static func _box(parent: Node3D, size: Vector3, pos: Vector3, mat: Material) -> MeshInstance3D:
	var mi := MeshInstance3D.new()
	var bm := BoxMesh.new()
	bm.size = size
	mi.mesh = bm
	mi.material_override = mat
	mi.position = pos
	parent.add_child(mi)
	return mi


static func _cyl(parent: Node3D, top: float, bottom: float, h: float, pos: Vector3, mat: Material) -> MeshInstance3D:
	var mi := MeshInstance3D.new()
	var cm := CylinderMesh.new()
	cm.top_radius = top
	cm.bottom_radius = bottom
	cm.height = h
	cm.radial_segments = 12
	cm.rings = 1
	mi.mesh = cm
	mi.material_override = mat
	mi.position = pos
	parent.add_child(mi)
	return mi
