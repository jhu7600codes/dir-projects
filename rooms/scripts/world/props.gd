class_name Props
## office furniture. each prop tries the manifest model first and falls back to
## a few placeholder boxes. everything is added to the given room.

## furniture heights. these are a bit taller than real life on purpose, so they read right
## from the player's camera height (like in doors)
const TABLE_H := 1.0
const COUNTER_H := 1.05
const SEAT_H := 0.6


static func _model(room: RoomBase, key: String, pos: Vector3, yaw: float) -> bool:
	var m := Assets.model(key)
	if m == null:
		return false
	m.position = pos
	m.rotation.y = yaw
	room.add_child(m)
	return true


static func table(room: RoomBase, pos: Vector3, yaw := 0.0, size := Vector3(1.6, TABLE_H, 0.8)) -> void:
	if _model(room, "table", pos, yaw):
		room.box(Vector3(size.x, size.y, size.z), pos + Vector3(0, size.y / 2, 0), Mats.get_mat("wood"), true, yaw).visible = false
		return
	var top := room.box(Vector3(size.x, 0.05, size.z), pos + Vector3(0, size.y, 0), Mats.get_mat("wood"), true, yaw)
	for sx in [-1, 1]:
		for sz in [-1, 1]:
			var off := Basis(Vector3.UP, yaw) * Vector3(sx * (size.x / 2 - 0.06), 0, sz * (size.z / 2 - 0.06))
			room.box(Vector3(0.05, size.y, 0.05), pos + off + Vector3(0, size.y / 2, 0), Mats.get_mat("metal"), false)
	top.name = "table"


static func chair(room: RoomBase, pos: Vector3, yaw := 0.0, fallen := false) -> void:
	if _model(room, "chair_fallen" if fallen else "chair", pos, yaw):
		return
	var c := PropModels.office_chair(SEAT_H)
	c.position = pos
	c.rotation.y = yaw
	if fallen:
		# tipped over backwards, lying on its back
		c.rotation.x = -PI / 2 + 0.15
		c.position.y += 0.28
	room.add_child(c)


static func plant(room: RoomBase, pos: Vector3, fallen := false) -> void:
	if _model(room, "plant_fallen" if fallen else "plant", pos, room.rng.randf() * TAU):
		return
	var kinds := ["fern", "fern", "snake", "bush"]
	var p := PropModels.plant(kinds[room.rng.randi() % kinds.size()], room.rng.randi() % 3)
	p.position = pos
	p.rotation.y = room.rng.randf() * TAU
	if fallen:
		p.rotation.z = PI / 2 - 0.1
		p.position.y += 0.17
		# a bit of spilled soil
		room.box(Vector3(0.5, 0.01, 0.35), pos + Vector3(0.35, 0.005, 0), PropModels._mat("soil", Color(0.16, 0.11, 0.08), 1.0), false, room.rng.randf() * TAU)
	room.add_child(p)


static func couch(room: RoomBase, pos: Vector3, yaw := 0.0) -> void:
	if not _model(room, "couch", pos, yaw):
		var c := PropModels.couch()
		c.position = pos
		c.rotation.y = yaw
		room.add_child(c)
	# collision only
	var b := Basis(Vector3.UP, yaw)
	room.box(Vector3(2.0, 0.5, 0.85), pos + Vector3(0, 0.25, 0), Mats.get_mat("fabric"), true, yaw).visible = false
	room.box(Vector3(2.0, 0.6, 0.22), pos + b * Vector3(0, 0.8, -0.33), Mats.get_mat("fabric"), true, yaw).visible = false


static func shelf(room: RoomBase, pos: Vector3, yaw := 0.0) -> void:
	if not _model(room, "shelf", pos, yaw):
		var s := PropModels.shelf(room.rng)
		s.position = pos
		s.rotation.y = yaw
		room.add_child(s)
	room.box(Vector3(1.4, 2.0, 0.45), pos + Vector3(0, 1.0, 0), Mats.get_mat("metal"), true, yaw).visible = false


static func cubicle(room: RoomBase, pos: Vector3, yaw := 0.0) -> void:
	# three low walls + a desk with a drawer inside
	var b := Basis(Vector3.UP, yaw)
	var mat := Mats.get_mat("cubicle")
	room.box(Vector3(2.2, 1.6, 0.08), pos + b * Vector3(0, 0.8, -1.0), mat, true, yaw)
	room.box(Vector3(0.08, 1.6, 2.0), pos + b * Vector3(-1.1, 0.8, 0), mat, true, yaw)
	room.box(Vector3(0.08, 1.6, 2.0), pos + b * Vector3(1.1, 0.8, 0), mat, true, yaw)
	room.add_drawer_desk(pos + b * Vector3(0, 0, -0.6), yaw)
	if room.rng.randf() < 0.5:
		chair(room, pos + b * Vector3(room.rng.randf_range(-0.5, 0.5), 0, 0.2), yaw + room.rng.randf_range(-1, 1), room.rng.randf() < 0.4)


static func fridge(room: RoomBase, pos: Vector3, yaw := 0.0) -> void:
	if not _model(room, "fridge", pos, yaw):
		var f := PropModels.fridge()
		f.position = pos
		f.rotation.y = yaw
		room.add_child(f)
	room.box(Vector3(0.8, 1.9, 0.7), pos + Vector3(0, 0.95, 0), Mats.get_mat("plastic"), true, yaw).visible = false


static func counter(room: RoomBase, pos: Vector3, yaw := 0.0, length := 3.0) -> void:
	room.box(Vector3(length, COUNTER_H - 0.05, 0.65), pos + Vector3(0, (COUNTER_H - 0.05) / 2, 0), Mats.get_mat("wood"), true, yaw)
	room.box(Vector3(length + 0.05, 0.05, 0.7), pos + Vector3(0, COUNTER_H - 0.025, 0), Mats.get_mat("plastic"), false, yaw)


static func water_dispenser(room: RoomBase, pos: Vector3, broken := false) -> void:
	if _model(room, "water_dispenser", pos, 0.0):
		return
	var holder := Node3D.new()
	holder.position = pos
	if broken:
		holder.rotation.x = PI / 2.2
		holder.position.y += 0.2
	room.add_child(holder)
	room.box(Vector3(0.4, 1.0, 0.4), Vector3(0, 0.5, 0), Mats.get_mat("plastic"), false, 0.0, holder)
	var jug := MeshInstance3D.new()
	var cm := CylinderMesh.new()
	cm.top_radius = 0.15
	cm.bottom_radius = 0.15
	cm.height = 0.45
	jug.mesh = cm
	jug.material_override = Mats.get_mat("glass")
	jug.position = Vector3(0, 1.22, 0)
	holder.add_child(jug)


static func pillar(room: RoomBase, pos: Vector3, height: float, size := 0.7) -> void:
	room.box(Vector3(size, height, size), pos + Vector3(0, height / 2, 0), Mats.get_mat("wall"))


static func whiteboard(room: RoomBase, pos: Vector3, yaw := 0.0) -> void:
	room.box(Vector3(2.0, 1.1, 0.04), pos, Mats.get_mat("plastic"), false, yaw)
