extends Node
## finds furniture that clips into walls, into other furniture, or floats:
## godot --headless --path . res://tests/overlap_test.tscn [-- seeds] [room filter]
## builds every room type with many seeds and checks the boxes of everything in it.

const TOL := 0.03  # overlaps smaller than this are just touching

var issues := {}  # "room: text" -> count


func _ready() -> void:
	get_tree().current_scene = null
	await get_tree().process_frame
	var args := OS.get_cmdline_user_args()
	var seeds := int(args[0]) if args.size() > 0 else 12
	var only := args[1] if args.size() > 1 else ""
	Game.start_run(true, false, "offices", {"sightings": "office"})
	for i in 20:
		await get_tree().process_frame
	Game.entities.natural_spawns = false
	var gen: RoomGenerator = Game.generator
	var scripts := []
	for p in gen.pool:
		scripts.append(p.script)
	for k in gen.special:
		scripts.append(gen.special[k])
	scripts.append(gen.EXIT_ROOM)
	var floors_of := {}
	for sc in [gen.WIRES_START, gen.WIRES_CORRIDOR, gen.WIRES_PIPES, gen.WIRES_GENERATOR, gen.WIRES_SWITCH, gen.WIRES_END]:
		scripts.append(sc)
		floors_of[sc] = "wires"
	for sc in [gen.COMPLEX_LOBBY, gen.COMPLEX_FLOOR]:
		scripts.append(sc)
		floors_of[sc] = "complex"
	for sc in scripts:
		Game.floor = floors_of.get(sc, "offices")
		var nm: String = sc.resource_path.get_file().get_basename()
		if only != "" and not nm.contains(only):
			continue
		for k in seeds:
			var r: RoomBase = sc.new()
			r.setup(20 + k * 7, 999 + k * 131)
			add_child(r)
			_check(nm, r)
			remove_child(r)
			r.free()
		await get_tree().process_frame
	var keys := issues.keys()
	keys.sort()
	for key in keys:
		print("%3dx  %s" % [issues[key], key])
	print("overlap test done: %d kinds of issues" % keys.size())
	get_tree().quit()


func _report(room: String, text: String) -> void:
	var key := room + ": " + text
	issues[key] = issues.get(key, 0) + 1


## everything solid in the room, in room space: [name, aabb, kind]
func _check(nm: String, r: RoomBase) -> void:
	var walls := []
	var solids := []
	var floors := []
	# collision-only boxes (invisible meshes) belong to a model that's checked on its own
	var hidden := []
	for c in r.get_children():
		if c is MeshInstance3D and not c.visible:
			hidden.append(c.transform.origin)
		elif c is Node3D:
			for m in c.get_children():
				if m is MeshInstance3D and not m.visible:
					hidden.append((c.transform * m.transform).origin)
	for cs in r._body.get_children():
		if not (cs is CollisionShape3D and cs.shape is BoxShape3D):
			continue
		if hidden.any(func(o): return o.distance_to(cs.transform.origin) < 0.001):
			continue
		var sz: Vector3 = cs.shape.size
		var bb: AABB = cs.transform * AABB(-sz / 2, sz)
		if absf(sz.x - RoomBase.WALL_T) < 0.01 or absf(sz.z - RoomBase.WALL_T) < 0.01:
			walls.append(bb)
		elif sz.y <= 0.25 and (sz.x > 2.5 or sz.z > 2.5):
			floors.append(["floor", bb, "floor"])  # floors, ceilings, catwalks
		elif cs.disabled:
			continue
		else:
			solids.append(["box %s" % _fmt(sz), bb, "box"])
	for c in r.get_children():
		if c is Locker:
			if c.style == "table":
				continue
			solids.append(["locker", _mesh_aabb(c, r), "locker"])
		elif c is Drawer:
			solids.append(["desk", _mesh_aabb(c, r), "desk"])
		elif c is LootCabinet:
			solids.append(["dresser", _mesh_aabb(c, r), "dresser"])
		elif c is Door or c is Pickup or c is Label3D or c is Light3D or c is Area3D or c is StaticBody3D or c is AudioStreamPlayer3D:
			continue
		elif c is MeshInstance3D:
			if c.visible and c.mesh:
				floors.append(["loose", c.transform * c.mesh.get_aabb(), "loose"])  # only as something to stand on
			continue  # loose boxes: walls, trim, decor (the colliding ones are in _body)
		elif c is Node3D and c.get_child_count() > 0:
			var bb := _mesh_aabb(c, r)
			if bb.size.length() > 0.05 and minf(bb.size.x, bb.size.z) > 0.1:
				solids.append([_guess(c), bb, "prop"])
	# into walls
	for s in solids:
		for w in walls:
			var d := _depth(s[1], w)
			if d > TOL:
				_report(nm, "%s pokes %.2f m into a wall" % [s[0], d])
				break
	# into each other
	for i in solids.size():
		for j in range(i + 1, solids.size()):
			var a = solids[i]
			var b = solids[j]
			var d := _depth(a[1], b[1])
			var tol := 0.05 if a[2] == "box" and b[2] == "box" else TOL
			if d > tol and not _allowed(a, b):
				_report(nm, "%s overlaps %s by %.2f m" % [a[0], b[0], d])
				if OS.get_cmdline_user_args().size() > 2:
					print("   ", nm, " ", a[0], " ", a[1], "  /  ", b[0], " ", b[1])
	# floating: bottom above the floor and nothing under it
	for s in solids:
		var bb: AABB = s[1]
		var bottom := bb.position.y
		if bottom < 0.04 and bottom > -0.04:
			continue
		var held := false
		for o in solids + floors:
			var ob: AABB = o[1]
			if ob == bb:
				continue
			if absf(ob.end.y - bottom) < 0.08 and _xz_overlap(bb, ob):
				held = true
				break
		if not held and bottom > 0.04:
			_report(nm, "%s floats %.2f m up" % [s[0], bottom])


func _allowed(a, b) -> bool:
	var ka: String = a[2]
	var kb: String = b[2]
	# chairs tucked under desks and tables, plants and things standing on tables
	if ka == "prop" or kb == "prop":
		var other = b if ka == "prop" else a
		var prop = a if ka == "prop" else b
		var ob: AABB = other[1]
		var pb: AABB = prop[1]
		if pb.position.y >= ob.end.y - 0.06:
			return true  # standing on top
		if (prop[0] as String).begins_with("chair") and other[2] in ["desk", "box"]:
			return pb.end.y <= ob.end.y + 0.6 and _depth(pb, ob) < 0.45
	# one thing resting on top of another
	if absf(a[1].position.y - b[1].end.y) < 0.06 or absf(b[1].position.y - a[1].end.y) < 0.06:
		return true
	return false


func _depth(a: AABB, b: AABB) -> float:
	var i := a.intersection(b)
	if i.size.x <= 0 or i.size.y <= 0 or i.size.z <= 0:
		return 0.0
	return minf(i.size.x, minf(i.size.y, i.size.z))


func _xz_overlap(a: AABB, b: AABB) -> bool:
	return a.position.x < b.end.x - 0.02 and b.position.x < a.end.x - 0.02 and a.position.z < b.end.z - 0.02 and b.position.z < a.end.z - 0.02


func _mesh_aabb(n: Node3D, r: Node3D) -> AABB:
	var out := AABB()
	var first := true
	for m in n.find_children("*", "MeshInstance3D", true, false):
		if not m.visible or m.mesh == null:
			continue
		var xf: Transform3D = r.global_transform.affine_inverse() * m.global_transform
		var bb: AABB = xf * m.mesh.get_aabb()
		out = bb if first else out.merge(bb)
		first = false
	if n is MeshInstance3D and n.mesh:
		var bb2: AABB = (r.global_transform.affine_inverse() * n.global_transform) * n.mesh.get_aabb()
		out = bb2 if first else out.merge(bb2)
	return out


func _guess(n: Node) -> String:
	var names := {}
	for k in Mats._cache:
		names[Mats._cache[k]] = k
	for k in PropModels._mats:
		names[PropModels._mats[k]] = k
	var mats := {}
	for m in n.find_children("*", "MeshInstance3D", true, false):
		if m.material_override:
			mats[names.get(m.material_override, "?")] = true
	var s := str(n.name)
	if s.begins_with("@"):
		s = n.get_class()
	var k := mats.keys()
	if k.has("chair_black") or k.has("chair_cloth"):
		return "chair"
	if k.has("soil") or k.has("leaf"):
		return "plant"
	return s + str(k.slice(0, 3))


func _fmt(v: Vector3) -> String:
	return "%.2fx%.2fx%.2f" % [v.x, v.y, v.z]
