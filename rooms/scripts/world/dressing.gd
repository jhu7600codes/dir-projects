class_name Dressing
## makes a plain office room look lived in. after a room is built, this walks along its
## walls and adds the stuff real offices have: posters and frames, a clock, a corkboard,
## outlets, a fire extinguisher, an exit sign and light switch by the door, vents in the
## ceiling, and along the walls a trash bin, filing cabinets, a water cooler, a printer.
## dark rooms get stains on the carpet and missing ceiling tiles.
## everything is static, so it all ends up in the room's merged mesh (cheap to draw).
## nothing is placed in front of doors or on top of lockers / furniture.

const POSTERS := ["poster_red", "poster_blue", "poster_green", "poster_yellow", "poster_grey"]


static func dress(room: RoomBase) -> void:
	var info: Dictionary = room.shell_info
	var x0: float = info.x0
	var x1: float = info.x1
	var length: float = info.length
	var height: float = info.height
	var rng := RandomNumberGenerator.new()
	rng.seed = room.rng.randi()
	var taken := _occupied(room)
	var once := {}
	# the four walls: [name, start point, direction along the wall, inward normal, wall length, yaw]
	var walls := [
		["back", Vector3(x0, 0, 0), Vector3.RIGHT, Vector3.BACK, x1 - x0, 0.0],
		["front", Vector3(x0, 0, length), Vector3.RIGHT, Vector3.FORWARD, x1 - x0, 0.0],
		["left", Vector3(x1, 0, 0), Vector3.BACK, Vector3.LEFT, length, PI / 2],
		["right", Vector3(x0, 0, 0), Vector3.BACK, Vector3.RIGHT, length, PI / 2],
	]
	var narrow := (x1 - x0) < 2.8
	for wl in walls:
		var name: String = wl[0]
		var gaps: Array = info.gaps.get(name, [])
		var s := rng.randf_range(0.9, 1.8)
		while s < float(wl[4]) - 0.9:
			var along: float = (s + x0) if name in ["back", "front"] else s
			var near_gap := false
			for g in gaps:
				if absf(float(g) - along) < 1.15:
					near_gap = true
			if not near_gap:
				var base: Vector3 = wl[1] + wl[2] * s
				_wall_item(room, rng, base, wl[3], wl[5], height, taken, once)
				if not narrow and rng.randf() < 0.22:
					_floor_item(room, rng, base, wl[3], wl[5], taken, once)
			s += rng.randf_range(1.8, 3.2)
	# by the entry door: an exit sign above it, a light switch next to it
	room.box(Vector3(0.36, 0.14, 0.05), Vector3(0, RoomBase.DOOR_H + 0.22, 0.13), Mats.get_mat("exit_green"), false)
	room.box(Vector3(0.08, 0.12, 0.02), Vector3(RoomBase.DOOR_W / 2 + 0.3, 1.2, 0.11), Mats.get_mat("plastic"), false)
	# ceiling vents
	for i in rng.randi_range(1, 2):
		var p := Vector3(rng.randf_range(x0 + 0.8, x1 - 0.8), height - 0.01, rng.randf_range(1.5, length - 1.5))
		room.box(Vector3(0.6, 0.02, 0.6), p, Mats.get_mat("frame"), false)
		for k in 4:
			room.box(Vector3(0.5, 0.025, 0.04), p + Vector3(0, -0.005, -0.18 + k * 0.12), Mats.get_mat("dark"), false)
	# wear and tear the deeper you go
	if room.darkness > 0.3:
		for i in rng.randi_range(1, 3):
			var sp := Vector3(rng.randf_range(x0 + 0.5, x1 - 0.5), 0.004, rng.randf_range(1.0, length - 1.0))
			room.box(Vector3(rng.randf_range(0.3, 0.9), 0.006, rng.randf_range(0.3, 0.8)), sp, Mats.get_mat("stain"), false, rng.randf() * PI)
	if room.darkness > 0.5:
		for i in rng.randi_range(1, 3):
			var tp := Vector3(rng.randf_range(x0 + 0.6, x1 - 0.6), height - 0.004, rng.randf_range(1.0, length - 1.0))
			room.box(Vector3(0.6, 0.01, 0.6), tp, Mats.get_mat("crack"), false)


## something flat on the wall at this spot (if it's free)
static func _wall_item(room: RoomBase, rng: RandomNumberGenerator, base: Vector3, n: Vector3, yaw: float, height: float, taken: Array, once: Dictionary) -> void:
	var r := rng.randf()
	if r < 0.36:
		# a poster or framed picture
		var w := rng.randf_range(0.45, 0.8)
		var h := rng.randf_range(0.55, 0.9)
		var y := rng.randf_range(1.45, minf(1.85, height - h / 2 - 0.3))
		var at := base + n * 0.12 + Vector3.UP * y
		if _free(taken, at, Vector3(w, h, 0.08), yaw):
			room.box(_sz(w + 0.06, h + 0.06, 0.03, yaw), at, Mats.get_mat("dark" if rng.randf() < 0.5 else "wood_dark"), false)
			room.box(_sz(w, h, 0.035, yaw), at + n * 0.005, Mats.get_mat(POSTERS[rng.randi() % POSTERS.size()]), false)
			# a few lines of "text"
			for k in rng.randi_range(0, 3):
				room.box(_sz(w * rng.randf_range(0.4, 0.8), 0.03, 0.037, yaw), at + n * 0.008 + Vector3.UP * (h * 0.3 - k * 0.09), Mats.get_mat("plastic"), false)
	elif r < 0.42 and not once.has("clock"):
		var at := base + n * 0.12 + Vector3.UP * minf(2.35, height - 0.4)
		if _free(taken, at, Vector3(0.36, 0.36, 0.08), yaw):
			once["clock"] = true
			room.box(_sz(0.38, 0.38, 0.04, yaw), at, Mats.get_mat("dark"), false)
			room.box(_sz(0.32, 0.32, 0.045, yaw), at + n * 0.004, Mats.get_mat("plastic"), false)
			room.box(_sz(0.02, 0.13, 0.05, yaw), at + n * 0.008 + Vector3.UP * 0.05, Mats.get_mat("dark"), false)
			room.box(_sz(0.1, 0.02, 0.05, yaw), at + n * 0.008, Mats.get_mat("dark"), false)
	elif r < 0.52 and not once.has("extinguisher"):
		var at := base + n * 0.2 + Vector3.UP * 0.55
		if _free(taken, at, Vector3(0.25, 0.6, 0.3), yaw):
			once["extinguisher"] = true
			room.box(Vector3(0.16, 0.48, 0.16), at, Mats.get_mat("extinguisher"), false)
			room.box(Vector3(0.08, 0.1, 0.08), at + Vector3.UP * 0.29, Mats.get_mat("dark"), false)
			room.box(_sz(0.28, 0.6, 0.02, yaw), base + n * 0.11 + Vector3.UP * 0.62, Mats.get_mat("poster_red"), false)
	elif r < 0.72:
		var at := base + n * 0.11 + Vector3.UP * 0.32
		if _free(taken, at, Vector3(0.1, 0.14, 0.04), yaw):
			room.box(_sz(0.08, 0.12, 0.02, yaw), at, Mats.get_mat("plastic"), false)
	elif r < 0.8 and not once.has("cork"):
		var at := base + n * 0.12 + Vector3.UP * 1.55
		if _free(taken, at, Vector3(1.0, 0.7, 0.08), yaw):
			once["cork"] = true
			room.box(_sz(1.0, 0.7, 0.03, yaw), at, Mats.get_mat("cork"), false)
			for k in rng.randi_range(3, 6):
				var off := _along(yaw) * rng.randf_range(-0.38, 0.38) + Vector3.UP * rng.randf_range(-0.25, 0.25)
				room.box(_sz(0.14, 0.18, 0.035, yaw), at + off + n * 0.005, Mats.get_mat("paper" if rng.randf() < 0.7 else "poster_yellow"), false)


## something standing against the wall at this spot (if it's free)
static func _floor_item(room: RoomBase, rng: RandomNumberGenerator, base: Vector3, n: Vector3, yaw: float, taken: Array, once: Dictionary) -> void:
	var r := rng.randf()
	if r < 0.35:
		var at := base + n * 0.35 + Vector3.UP * 0.2
		if _free(taken, at, Vector3(0.35, 0.4, 0.35), 0.0):
			room.box(Vector3(0.3, 0.4, 0.3), at, Mats.get_mat("dark"), true)
			room.box(Vector3(0.32, 0.03, 0.32), at + Vector3.UP * 0.2, Mats.get_mat("metal"), false)
			taken.append(AABB(at - Vector3(0.2, 0.2, 0.2), Vector3(0.4, 0.4, 0.4)))
	elif r < 0.6:
		# filing cabinet
		var at := base + n * 0.42 + Vector3.UP * 0.65
		if _free(taken, at, Vector3(0.55, 1.3, 0.65), yaw):
			room.box(_sz(0.5, 1.3, 0.6, yaw), at, Mats.get_mat("cabinet"), true)
			for k in 4:
				room.box(_sz(0.2, 0.025, 0.62, yaw), at + Vector3.UP * (0.5 - k * 0.32) + n * 0.005, Mats.get_mat("dark"), false)
			taken.append(_aabb(at, Vector3(0.6, 1.3, 0.7), yaw))
	elif r < 0.75 and not once.has("cooler"):
		var at := base + n * 0.4
		if _free(taken, at + Vector3.UP * 0.7, Vector3(0.5, 1.4, 0.5), 0.0):
			once["cooler"] = true
			Props.water_dispenser(room, at)
			taken.append(AABB(at - Vector3(0.25, 0, 0.25), Vector3(0.5, 1.5, 0.5)))
	elif r < 0.88 and not once.has("printer"):
		var at := base + n * 0.38
		if _free(taken, at + Vector3.UP * 0.5, Vector3(0.75, 1.0, 0.6), yaw):
			once["printer"] = true
			room.box(_sz(0.7, 0.7, 0.55, yaw), at + Vector3.UP * 0.35, Mats.get_mat("wood_dark"), true)
			room.box(_sz(0.55, 0.3, 0.45, yaw), at + Vector3.UP * 0.85, Mats.get_mat("plastic"), false)
			room.box(_sz(0.4, 0.04, 0.3, yaw), at + Vector3.UP * 1.01, Mats.get_mat("dark"), false)
			room.box(_sz(0.3, 0.02, 0.2, yaw), at + Vector3.UP * 0.75 + n * 0.25, Mats.get_mat("paper"), false)
			taken.append(_aabb(at + Vector3.UP * 0.5, Vector3(0.75, 1.0, 0.6), yaw))
	else:
		var at := base + n * 0.45
		if _free(taken, at + Vector3.UP * 0.5, Vector3(0.6, 1.0, 0.6), 0.0):
			Props.plant(room, at)
			taken.append(AABB(at - Vector3(0.3, 0, 0.3), Vector3(0.6, 1.0, 0.6)))


## everything already in the room that decor must not overlap: furniture collision boxes,
## lockers, desks, shops, and the space in front of every doorway
static func _occupied(room: RoomBase) -> Array:
	var out := []
	for cs in room._body.get_children():
		if cs is CollisionShape3D and cs.shape is BoxShape3D:
			var sz: Vector3 = (cs.shape as BoxShape3D).size
			# skip the walls, floor and ceiling themselves (decor sits next to them)
			if sz.y < 0.25 and (sz.x > 2.5 or sz.z > 2.5):
				continue
			if absf(sz.x - RoomBase.WALL_T) < 0.01 or absf(sz.z - RoomBase.WALL_T) < 0.01:
				continue
			out.append(cs.transform * AABB(-sz / 2.0, sz))
	for c in room.get_children():
		if c is Locker or c is Drawer or c is ShopStand or c is Atm or c is ExitDoor:
			out.append(AABB(c.position - Vector3(0.9, 0, 0.9), Vector3(1.8, 2.4, 1.8)))
	out.append_array(room.decor_block)
	# doorways
	out.append(AABB(Vector3(-1.1, 0, -0.2), Vector3(2.2, 3.0, 1.8)))
	if room.exit_door:
		var c: Vector3 = room.exit_local * Vector3(0, 0, -0.8)
		out.append(AABB(c - Vector3(1.1, 0, 1.1), Vector3(2.2, 3.0, 2.2)))
	return out


static func _free(taken: Array, center: Vector3, size: Vector3, yaw: float) -> bool:
	var a := _aabb(center, size, yaw)
	for t in taken:
		if a.intersects(t):
			return false
	return true


static func _aabb(center: Vector3, size: Vector3, yaw: float) -> AABB:
	var s := size if absf(yaw) < 0.1 else Vector3(size.z, size.y, size.x)
	return AABB(center - s / 2.0, s)


## a size given as (along the wall, up, out of the wall) turned into room axes
static func _sz(along: float, up: float, out: float, yaw: float) -> Vector3:
	return Vector3(along, up, out) if absf(yaw) < 0.1 else Vector3(out, up, along)


static func _along(yaw: float) -> Vector3:
	return Vector3.RIGHT if absf(yaw) < 0.1 else Vector3.BACK
