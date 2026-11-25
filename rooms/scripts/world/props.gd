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
	# big enough tables can be hidden under
	if size.x >= 1.2 and size.z >= 0.7 and size.y >= 0.8:
		room.add_table_hide(pos, yaw)
	if _model(room, "table", pos, yaw):
		room.box(Vector3(size.x, size.y, size.z), pos + Vector3(0, size.y / 2, 0), Mats.get_mat("wood"), true, yaw).visible = false
		return
	var b := Basis(Vector3.UP, yaw)
	var top := room.box(Vector3(size.x, 0.05, size.z), pos + Vector3(0, size.y, 0), Mats.get_mat("wood"), true, yaw)
	# a dark edge band around the top and a metal apron under it
	room.box(Vector3(size.x + 0.02, 0.03, size.z + 0.02), pos + Vector3(0, size.y - 0.01, 0), Mats.get_mat("dark"), false, yaw)
	for sz in [-1, 1]:
		room.box(Vector3(size.x - 0.14, 0.08, 0.025), pos + b * Vector3(0, size.y - 0.08, sz * (size.z / 2 - 0.07)), Mats.get_mat("metal"), false, yaw)
	for sx in [-1, 1]:
		room.box(Vector3(0.025, 0.08, size.z - 0.14), pos + b * Vector3(sx * (size.x / 2 - 0.07), size.y - 0.08, 0), Mats.get_mat("metal"), false, yaw)
		for sz in [-1, 1]:
			var off := b * Vector3(sx * (size.x / 2 - 0.06), 0, sz * (size.z / 2 - 0.06))
			room.box(Vector3(0.05, size.y, 0.05), pos + off + Vector3(0, size.y / 2, 0), Mats.get_mat("metal"), false)
			room.box(Vector3(0.08, 0.02, 0.08), pos + off + Vector3(0, 0.01, 0), Mats.get_mat("dark"), false)
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
	c.set_meta("unclip", true)
	room.add_child(c)


static func plant(room: RoomBase, pos: Vector3, fallen := false) -> void:
	if _model(room, "plant_fallen" if fallen else "plant", pos, room.rng.randf() * TAU):
		return
	var kinds := ["fern", "fern", "snake", "bush"]
	if pos.y > 0.1:
		kinds = ["snake", "bush"]  # on a table: ferns droop their leaves through the top
	var p := PropModels.plant(kinds[room.rng.randi() % kinds.size()], room.rng.randi() % 3)
	p.set_meta("unclip", true)
	p.position = pos
	p.rotation.y = room.rng.randf() * TAU
	if fallen:
		p.rotation.z = PI / 2 - 0.1
		p.position.y += 0.17
		# a bit of spilled soil
		room.box(Vector3(0.5, 0.01, 0.35), pos + Vector3(0.35, 0.02, 0), PropModels._mat("soil", Color(0.16, 0.11, 0.08), 1.0), false, room.rng.randf() * TAU)
	room.add_child(p)


static func couch(room: RoomBase, pos: Vector3, yaw := 0.0) -> void:
	if not _model(room, "couch", pos, yaw):
		var c := PropModels.couch()
		c.position = pos
		c.rotation.y = yaw
		c.set_meta("unclip", true)
		room.add_child(c)
	# collision only
	var b := Basis(Vector3.UP, yaw)
	room.box(Vector3(2.0, 0.5, 0.85), pos + Vector3(0, 0.25, 0), Mats.get_mat("fabric"), true, yaw).visible = false
	room.box(Vector3(2.0, 0.6, 0.22), pos + b * Vector3(0, 0.8, -0.33), Mats.get_mat("fabric"), true, yaw).visible = false


static func shelf(room: RoomBase, pos: Vector3, yaw := 0.0, clear_spot := false) -> void:
	if not _model(room, "shelf", pos, yaw):
		var s := PropModels.shelf(room.rng, clear_spot)
		s.position = pos
		s.rotation.y = yaw
		s.set_meta("unclip", true)
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
	if room.rng.randf() < 0.5 and not Game.office_powered():  # (powered: the worker brings a chair)
		# it faces the desk, so a fallen one lies back out of the cubicle (not across the desk)
		var fallen := room.rng.randf() < 0.4
		var turn := room.rng.randf_range(-0.3, 0.3) if fallen else room.rng.randf_range(-0.8, 0.8)
		chair(room, pos + b * Vector3(room.rng.randf_range(-0.4, 0.4), 0, 0.25), yaw + PI + turn, fallen)


static func fridge(room: RoomBase, pos: Vector3, yaw := 0.0) -> void:
	if not _model(room, "fridge", pos, yaw):
		var f := PropModels.fridge()
		f.position = pos
		f.rotation.y = yaw
		f.set_meta("unclip", true)
		room.add_child(f)
	room.box(Vector3(0.8, 1.9, 0.7), pos + Vector3(0, 0.95, 0), Mats.get_mat("plastic"), true, yaw).visible = false


static func counter(room: RoomBase, pos: Vector3, yaw := 0.0, length := 3.0) -> void:
	room.box(Vector3(length, COUNTER_H - 0.05, 0.65), pos + Vector3(0, (COUNTER_H - 0.05) / 2, 0), Mats.get_mat("wood"), true, yaw)
	room.box(Vector3(length + 0.05, 0.05, 0.7), pos + Vector3(0, COUNTER_H - 0.025, 0), Mats.get_mat("plastic"), false, yaw)


static func water_dispenser(room: RoomBase, pos: Vector3, broken := false) -> void:
	if _model(room, "water_dispenser", pos, 0.0):
		return
	# a floor-standing cooler: cabinet, a dark recess with a red and a blue tap over a drip
	# tray, and a blue bottle upside down on top
	var holder := Node3D.new()
	holder.position = pos
	holder.set_meta("unclip", true)
	if broken:
		holder.rotation.x = PI / 2.2
		holder.position.y += 0.2
	room.add_child(holder)
	room.box(Vector3(0.34, 0.9, 0.34), Vector3(0, 0.45, 0), Mats.get_mat("plastic"), false, 0.0, holder)
	room.box(Vector3(0.36, 0.04, 0.36), Vector3(0, 0.02, 0), Mats.get_mat("dark"), false, 0.0, holder)
	room.box(Vector3(0.3, 0.06, 0.3), Vector3(0, 0.93, 0), Mats.get_mat("plastic"), false, 0.0, holder)
	room.box(Vector3(0.24, 0.22, 0.02), Vector3(0, 0.72, 0.165), Mats.get_mat("dark"), false, 0.0, holder)
	room.box(Vector3(0.035, 0.05, 0.05), Vector3(-0.06, 0.78, 0.18), Mats.get_mat("tap_blue"), false, 0.0, holder)
	room.box(Vector3(0.035, 0.05, 0.05), Vector3(0.06, 0.78, 0.18), Mats.get_mat("tap_red"), false, 0.0, holder)
	room.box(Vector3(0.22, 0.025, 0.07), Vector3(0, 0.62, 0.19), Mats.get_mat("metal"), false, 0.0, holder)
	room.box(Vector3(0.2, 0.004, 0.012), Vector3(0, 0.634, 0.19), Mats.get_mat("dark"), false, 0.0, holder)
	if not broken:
		PropModels._cyl(holder, 0.05, 0.05, 0.08, Vector3(0, 1.0, 0), Mats.get_mat("jug"))
		PropModels._cyl(holder, 0.15, 0.06, 0.08, Vector3(0, 1.08, 0), Mats.get_mat("jug"))
		PropModels._cyl(holder, 0.15, 0.15, 0.3, Vector3(0, 1.27, 0), Mats.get_mat("jug"))
		PropModels._cyl(holder, 0.13, 0.15, 0.04, Vector3(0, 1.44, 0), Mats.get_mat("jug"))


## a grey filing cabinet: four drawers with a dark gap between them, a pull handle and a
## little white label holder on each. stands on the floor at pos, drawers facing +z
static func filing_cabinet(room: RoomBase, pos: Vector3, yaw := 0.0) -> void:
	var holder := Node3D.new()
	holder.position = pos
	holder.rotation.y = yaw
	room.add_child(holder)
	var cab := Mats.get_mat("cabinet")
	room.box(Vector3(0.5, 1.3, 0.58), Vector3(0, 0.65, -0.01), cab, false, 0.0, holder)
	room.box(Vector3(0.46, 0.04, 0.02), Vector3(0, 0.02, 0.28), Mats.get_mat("dark"), false, 0.0, holder)
	var dh := 0.305
	for k in 4:
		var y := 0.07 + dh * (k + 0.5)
		room.box(Vector3(0.46, dh - 0.02, 0.025), Vector3(0, y, 0.285), cab, false, 0.0, holder)
		room.box(Vector3(0.2, 0.022, 0.03), Vector3(0, y + 0.04, 0.31), Mats.get_mat("metal"), false, 0.0, holder)
		room.box(Vector3(0.12, 0.045, 0.006), Vector3(0, y + 0.1, 0.3), Mats.get_mat("paper"), false, 0.0, holder)
	room.box(Vector3(0.5, 1.3, 0.6), Vector3(0, 0.65, 0), cab, true, 0.0, holder).visible = false


static func pillar(room: RoomBase, pos: Vector3, height: float, size := 0.7) -> void:
	room.box(Vector3(size, height, size), pos + Vector3(0, height / 2, 0), Mats.get_mat("wall"))


static func whiteboard(room: RoomBase, pos: Vector3, yaw := 0.0) -> void:
	room.box(Vector3(2.0, 1.1, 0.04), pos, Mats.get_mat("plastic"), false, yaw)
