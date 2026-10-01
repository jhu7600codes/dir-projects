class_name ItemModels
## 3d models for the items, used for pickups on the floor and the item in your hand.
## a manifest model "item_<name>" (glb/gltf) replaces the built-in one if it exists.
## built-in models point "up" (+y), the hand rotates them to point forward.

static var _mats := {}


static func build(item: String) -> Node3D:
	var custom := Assets.model("item_" + item) if Assets.has_real("models", "item_" + item) else null
	if custom:
		return custom
	var root := Node3D.new()
	root.name = item
	match item:
		"flashlight":
			_cyl(root, 0.022, 0.022, 0.17, Vector3(0, 0.085, 0), _mat("black", Color(0.08, 0.08, 0.09), 0.4, 0.6))
			_cyl(root, 0.036, 0.024, 0.06, Vector3(0, 0.2, 0), _mat("black", Color(0.08, 0.08, 0.09), 0.4, 0.6))
			_cyl(root, 0.032, 0.032, 0.004, Vector3(0, 0.231, 0), _mat("lens", Color(1.0, 0.97, 0.85), 0.1, 0.0, 1.5))
			_box(root, Vector3(0.012, 0.02, 0.01), Vector3(0, 0.13, 0.022), _mat("rubber", Color(0.6, 0.1, 0.1), 0.9, 0.0))
			for i in 4:
				_cyl(root, 0.0235, 0.0235, 0.006, Vector3(0, 0.03 + i * 0.018, 0), _mat("grip", Color(0.2, 0.2, 0.22), 0.8, 0.3))
		"shakelight":
			_cyl(root, 0.026, 0.026, 0.2, Vector3(0, 0.1, 0), _mat("green_body", Color(0.16, 0.5, 0.22), 0.5, 0.1))
			_cyl(root, 0.034, 0.027, 0.05, Vector3(0, 0.225, 0), _mat("green_dark", Color(0.08, 0.25, 0.1), 0.5, 0.1))
			_cyl(root, 0.03, 0.03, 0.004, Vector3(0, 0.251, 0), _mat("green_lens", Color(0.5, 1.0, 0.55), 0.1, 0.0, 1.2))
			for i in 3:
				_cyl(root, 0.028, 0.028, 0.008, Vector3(0, 0.05 + i * 0.04, 0), _mat("green_dark", Color(0.08, 0.25, 0.1), 0.5, 0.1))
			# the magnet you can see sliding inside
			_cyl(root, 0.012, 0.012, 0.03, Vector3(0, 0.12, 0), _mat("metal_shiny", Color(0.7, 0.7, 0.72), 0.2, 1.0))
		"battery":
			_cyl(root, 0.017, 0.017, 0.09, Vector3(0, 0.045, 0), _mat("battery", Color(0.1, 0.1, 0.1), 0.5, 0.2))
			_cyl(root, 0.0172, 0.0172, 0.025, Vector3(0, 0.078, 0), _mat("copper", Color(0.85, 0.5, 0.2), 0.3, 0.9))
			_cyl(root, 0.006, 0.006, 0.006, Vector3(0, 0.093, 0), _mat("metal_shiny", Color(0.7, 0.7, 0.72), 0.2, 1.0))
		"bandage":
			var roll := Node3D.new()
			roll.rotation.z = PI / 2
			roll.position.y = 0.045
			root.add_child(roll)
			_cyl(roll, 0.045, 0.045, 0.07, Vector3.ZERO, _mat("cloth", Color(0.95, 0.93, 0.88), 1.0, 0.0))
			_cyl(roll, 0.015, 0.015, 0.072, Vector3.ZERO, _mat("cloth_core", Color(0.8, 0.78, 0.72), 1.0, 0.0))
			_box(root, Vector3(0.068, 0.002, 0.08), Vector3(0, 0.001, 0.07), _mat("cloth", Color(0.95, 0.93, 0.88), 1.0, 0.0))
		"vitamins":
			_cyl(root, 0.035, 0.035, 0.1, Vector3(0, 0.05, 0), _mat("bottle", Color(0.95, 0.55, 0.1, 0.85), 0.2, 0.0))
			_cyl(root, 0.0355, 0.0355, 0.045, Vector3(0, 0.05, 0), _mat("label", Color(0.96, 0.96, 0.92), 0.8, 0.0))
			_cyl(root, 0.025, 0.025, 0.012, Vector3(0, 0.106, 0), _mat("bottle", Color(0.95, 0.55, 0.1, 0.85), 0.2, 0.0))
			_cyl(root, 0.03, 0.03, 0.025, Vector3(0, 0.122, 0), _mat("cap", Color(0.95, 0.95, 0.95), 0.5, 0.0))
		"gold":
			var rng := RandomNumberGenerator.new()
			rng.seed = 7
			var gm := _mat("gold_coin", Color(1.0, 0.78, 0.25), 0.25, 1.0, 0.15)
			for i in 9:
				var c := _cyl(root, 0.03, 0.03, 0.008, Vector3(rng.randf_range(-0.07, 0.07), 0.004 + (i / 3) * 0.008, rng.randf_range(-0.07, 0.07)), gm)
				c.rotation = Vector3(rng.randf_range(-0.2, 0.2), 0, rng.randf_range(-0.2, 0.2))
			for i in 4:
				_cyl(root, 0.03, 0.03, 0.008, Vector3(0.02, 0.004 + i * 0.008, -0.01), gm)
		_:
			_box(root, Vector3(0.1, 0.1, 0.1), Vector3(0, 0.05, 0), _mat("unknown", Color.MAGENTA, 1.0, 0.0))
	return root


static func _mat(key: String, c: Color, rough: float, metal: float, glow := 0.0) -> StandardMaterial3D:
	if _mats.has(key):
		return _mats[key]
	var m := StandardMaterial3D.new()
	m.albedo_color = c
	m.roughness = rough
	m.metallic = metal
	if c.a < 1.0:
		m.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	if glow > 0.0:
		m.emission_enabled = true
		m.emission = c
		m.emission_energy_multiplier = glow
	_mats[key] = m
	return m


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


static func _box(parent: Node3D, size: Vector3, pos: Vector3, mat: Material) -> MeshInstance3D:
	var mi := MeshInstance3D.new()
	var bm := BoxMesh.new()
	bm.size = size
	mi.mesh = bm
	mi.material_override = mat
	mi.position = pos
	parent.add_child(mi)
	return mi
