class_name RoomBase
extends Node3D
## one room chunk from the pool. subclasses override build() and use the helpers below.
##
## local space: the entry doorway is at the origin (center of the gap, on the floor),
## the room extends toward +z. the exit gets a Door; the next room attaches at exit_local.
## left/right: when walking in (facing +z), +x is on your left and -x on your right.

const DOOR_W := 1.3
const DOOR_H := 2.3
const WALL_T := 0.2

## tags read by the generator and the entity manager
var room_type := "room"
var has_lockers := false
var entity_spawn_ok := false  # a-60 / a-120 may come when the player opens the door into this room
var is_special := false

var number := 0
var darkness := 0.0
var turn := 0                    # -1, 0, 1: set by the generator for rooms that can turn
var exit_local := Transform3D.IDENTITY
var path_local: Array[Vector3] = []  # line through the room that entities follow
var exit_door: Door = null
var lockers: Array = []
var rng := RandomNumberGenerator.new()

var _body: StaticBody3D
var _flicker: Array[Light3D] = []
var _flicker_t := 0.0
var _burst := 0.0
var _burst_lights: Array = []
var _fixture_i := 0  # low quality: only every other ceiling panel gets a real light
var _back_seal: CollisionShape3D
var _path_global: Array[Vector3] = []
var _height := 3.0  # ceiling height of the current shell, for the wall trim


func setup(num: int, seed_value: int) -> void:
	number = num
	rng.seed = seed_value
	darkness = Game.darkness(num)
	# some deep rooms got lucky and most of their lights still work
	if darkness > 0.4 and rng.randf() < 0.3:
		darkness *= 0.4
	name = "room_%04d" % num
	_body = StaticBody3D.new()
	_body.collision_layer = 1
	_body.collision_mask = 0
	add_child(_body)
	build()
	_clear_doorways()
	_add_back_seal()
	_merge_static()


## safety net against softlocks: any locker standing in front of the entry or the exit
## door gets removed
func _clear_doorways() -> void:
	for lk in lockers.duplicate():
		var p: Vector3 = lk.position
		var q: Vector3 = exit_local.affine_inverse() * p
		var blocks_entry := absf(p.x) < 1.3 and p.z < 1.4
		var blocks_exit := exit_door != null and absf(q.x) < 1.3 and q.z > -1.6 and q.z < 0.4
		if blocks_entry or blocks_exit:
			lockers.erase(lk)
			lk.free()
	# nowhere to hide modifier: keep a single locker
	if Game.mod("no_hiding"):
		while lockers.size() > 1:
			var lk = lockers[rng.randi() % lockers.size()]
			lockers.erase(lk)
			lk.free()
	has_lockers = not lockers.is_empty()
	if not has_lockers:
		entity_spawn_ok = false  # nothing to hide in, so a-60 / a-120 can't come here


## performance: after building, every static mesh in the room (walls, floor, ceiling,
## furniture, plants) is merged into one mesh per material, so a room costs a handful
## of draw calls instead of a hundred. moving / interactive things are left alone.
func _merge_static() -> void:
	var groups := {}  # material -> SurfaceTool
	var to_free: Array[Node] = []
	_collect_static(self, Transform3D.IDENTITY, groups, to_free)
	for n in to_free:
		n.get_parent().remove_child(n)
		n.free()
	for mat in groups:
		var mi := MeshInstance3D.new()
		mi.mesh = (groups[mat] as SurfaceTool).commit()
		mi.material_override = mat
		add_child(mi)


func _collect_static(node: Node, xf: Transform3D, groups: Dictionary, to_free: Array[Node]) -> void:
	for c in node.get_children():
		# desks: only the parts that never move get merged
		if c is Drawer:
			var s: Node = c.get_node_or_null("Static")
			if s:
				_collect_static(s, xf * (c as Node3D).transform * (s as Node3D).transform, groups, to_free)
			continue
		# these move, get picked up or are interactive: keep them as they are
		if c is Door or c is Locker or c is Drawer or c is Pickup or c is ExitDoor or c is ShopStand or c is Label3D or c is Light3D or c is StaticBody3D or c is Area3D:
			continue
		if not c is Node3D:
			continue
		var cxf: Transform3D = xf * (c as Node3D).transform
		if c is MeshInstance3D:
			var mi := c as MeshInstance3D
			if mi.get_child_count() == 0 and mi.mesh != null:
				if mi.visible and mi.material_override != null:
					var mat := mi.material_override
					if not groups.has(mat):
						var st := SurfaceTool.new()
						st.begin(Mesh.PRIMITIVE_TRIANGLES)
						groups[mat] = st
					(groups[mat] as SurfaceTool).append_from(mi.mesh, 0, cxf)
					to_free.append(mi)
				elif not mi.visible:
					to_free.append(mi)  # invisible collision helpers don't need a mesh
				continue
		_collect_static(c, cxf, groups, to_free)


## override: build geometry, call set_exit(), fill path_local
func build() -> void:
	pass


func global_exit() -> Transform3D:
	return global_transform * exit_local


func path_global() -> Array[Vector3]:
	if _path_global.is_empty():
		for p in path_local:
			_path_global.append(global_transform * p)
	return _path_global


## when this is the oldest loaded room, its entry is sealed so you can't walk into the void
func set_back_sealed(on: bool) -> void:
	if _back_seal:
		_back_seal.set_deferred("disabled", not on)


func _add_back_seal() -> void:
	_back_seal = CollisionShape3D.new()
	var sh := BoxShape3D.new()
	sh.size = Vector3(DOOR_W + 0.2, DOOR_H, 0.3)
	_back_seal.shape = sh
	_back_seal.position = Vector3(0, DOOR_H / 2, -0.4)
	_back_seal.disabled = true
	_body.add_child(_back_seal)


## rush / ambush warning: every light in the room flickers for a few seconds
func flicker_burst(seconds: float) -> void:
	_burst = seconds
	_burst_lights.clear()
	for l in find_children("*", "Light3D", true, false):
		_burst_lights.append(l)


func _process(delta: float) -> void:
	if _burst > 0.0:
		_burst -= delta
		for l in _burst_lights:
			if is_instance_valid(l):
				l.visible = _burst <= 0.0 or rng.randf() > 0.5
	if _flicker.is_empty():
		return
	_flicker_t -= delta
	if _flicker_t <= 0.0:
		_flicker_t = rng.randf_range(0.05, 0.4)
		for l in _flicker:
			l.visible = rng.randf() > 0.35


# ---- geometry helpers -----------------------------------------------------

func box(size: Vector3, pos: Vector3, mat: Material, collide := true, yaw := 0.0, parent: Node3D = null) -> MeshInstance3D:
	var mi := MeshInstance3D.new()
	var bm := BoxMesh.new()
	bm.size = size
	mi.mesh = bm
	mi.material_override = mat
	mi.position = pos
	mi.rotation.y = yaw
	(parent if parent else self).add_child(mi)
	if collide:
		var cs := CollisionShape3D.new()
		var sh := BoxShape3D.new()
		sh.size = size
		cs.shape = sh
		cs.transform = (parent.transform if parent else Transform3D.IDENTITY) * mi.transform
		_body.add_child(cs)
	return mi


## wall along the floor line a -> b, with door gaps given as [offset along the wall, width]
func wall(a: Vector3, b: Vector3, height: float, gaps: Array = [], mat: Material = null) -> void:
	if mat == null:
		mat = Mats.get_mat("wall")
	var d := (b - a)
	var length := d.length()
	d = d.normalized()
	var yaw := atan2(d.x, d.z)
	gaps.sort_custom(func(x, y): return x[0] < y[0])
	var cursor := 0.0
	for g in gaps:
		var g0: float = g[0] - g[1] / 2.0
		var g1: float = g[0] + g[1] / 2.0
		_wall_piece(a, d, yaw, cursor, g0, 0.0, height, mat, length)
		_wall_piece(a, d, yaw, g0, g1, DOOR_H, height, mat, length)  # lintel
		cursor = g1
	_wall_piece(a, d, yaw, cursor, length, 0.0, height, mat, length)


func _wall_piece(a: Vector3, d: Vector3, yaw: float, s: float, e: float, y0: float, y1: float, mat: Material, length: float) -> void:
	if e - s < 0.01 or y1 - y0 < 0.01:
		return
	# stretch the pieces at the wall ends a bit so corners close up
	if s <= 0.001:
		s -= WALL_T / 2.0
	if e >= length - 0.001:
		e += WALL_T / 2.0
	var center := a + d * ((s + e) / 2.0) + Vector3.UP * ((y0 + y1) / 2.0)
	box(Vector3(WALL_T, y1 - y0, e - s), center, mat, true, yaw)
	if mat == Mats.get_mat("wall"):
		var mid := a + d * ((s + e) / 2.0)
		var trim := Mats.get_mat("trim")
		if y0 <= 0.001:
			# baseboard along the floor
			box(Vector3(WALL_T + 0.05, 0.13, e - s), mid + Vector3.UP * 0.065, trim, false, yaw)
		if y1 >= _height - 0.01:
			# thin trim where the wall meets the ceiling
			box(Vector3(WALL_T + 0.035, 0.05, e - s), mid + Vector3.UP * (y1 - 0.025), trim, false, yaw)


## standard rectangular room: x from x0 to x1, z from 0 to length.
## exit: side is "front", "left" (+x wall) or "right" (-x wall); offset is x (front) or z (sides)
## side_holes: extra open gaps without a door, e.g. {"right": [[z, width]]}
func shell(x0: float, x1: float, length: float, height: float, exit_side: String, exit_offset: float, carpet := "carpet_red", side_holes := {}) -> void:
	_height = height
	var w := x1 - x0
	var cx := (x0 + x1) / 2.0
	box(Vector3(w + WALL_T, 0.2, length + WALL_T), Vector3(cx, -0.1, length / 2.0), Mats.get_mat(carpet))
	box(Vector3(w + WALL_T, 0.2, length + WALL_T), Vector3(cx, height + 0.1, length / 2.0), Mats.get_mat("ceiling"), false)
	# back wall with the entry gap at x = 0
	wall(Vector3(x1, 0, 0), Vector3(x0, 0, 0), height, [[x1 - 0.0, DOOR_W]])
	# front wall
	var front_gaps := []
	if exit_side == "front":
		front_gaps.append([exit_offset - x0, DOOR_W])
	wall(Vector3(x0, 0, length), Vector3(x1, 0, length), height, front_gaps)
	# side walls
	var left_gaps: Array = side_holes.get("left", []).duplicate()
	var right_gaps: Array = side_holes.get("right", []).duplicate()
	if exit_side == "left":
		left_gaps.append([exit_offset, DOOR_W])
	if exit_side == "right":
		right_gaps.append([exit_offset, DOOR_W])
	wall(Vector3(x1, 0, 0), Vector3(x1, 0, length), height, left_gaps)
	wall(Vector3(x0, 0, 0), Vector3(x0, 0, length), height, right_gaps)
	match exit_side:
		"front":
			set_exit(Vector3(exit_offset, 0, length), 0.0)
		"left":
			set_exit(Vector3(x1, 0, exit_offset), PI / 2)
		"right":
			set_exit(Vector3(x0, 0, exit_offset), -PI / 2)
	# a light grid
	var lx := maxi(1, int(w / 5.0))
	var lz := maxi(1, int(length / 5.5))
	for i in lx:
		for j in lz:
			ceiling_light(Vector3(x0 + w * (i + 0.5) / lx, height, length * (j + 0.5) / lz))


## exit transform + door. yaw 0 = straight ahead (+z), PI/2 = +x wall, -PI/2 = -x wall
func set_exit(pos: Vector3, yaw: float) -> void:
	exit_local = Transform3D(Basis(Vector3.UP, yaw), pos)
	exit_door = Door.new()
	exit_door.number = number + 1
	exit_door.transform = exit_local
	add_child(exit_door)


## floor-level line from entry to exit through the given waypoints (entities fly at head height)
func make_path(points: Array) -> void:
	path_local.clear()
	path_local.append(Vector3(0, 1.6, 0.4))
	for p in points:
		path_local.append(Vector3(p.x, 1.6, p.z))
	var ex := exit_local * Vector3(0, 1.6, -0.4)
	path_local.append(ex)


func ceiling_light(pos: Vector3) -> void:
	var broken := rng.randf() < darkness * 0.6
	box(Vector3(1.2, 0.04, 0.6), pos - Vector3(0, 0.02, 0), Mats.get_mat("light_off" if broken else "light_panel"), false)
	box(Vector3(1.26, 0.03, 0.66), pos - Vector3(0, 0.01, 0), Mats.get_mat("frame"), false)
	if broken:
		return
	var energy := lerpf(1.0, 0.45, darkness)
	var q := int(Settings.data.quality)
	if q == 0:
		# low (phones): every light re-shades the whole merged room mesh, so use half
		# as many, a bit stronger and wider so the room looks about the same
		_fixture_i += 1
		if _fixture_i % 2 == 0:
			return
		var o := OmniLight3D.new()
		o.position = pos - Vector3(0, 0.75, 0)
		o.omni_range = 8.0
		o.omni_attenuation = 1.1
		o.light_energy = energy * 1.35
		o.light_specular = 0.0
		o.light_color = Color(1.0, 0.97, 0.9)
		_fade_far(o)
		add_child(o)
		if rng.randf() < 0.06 + darkness * 0.25:
			_flicker.append(o)
		return
	if q < 2:
		# low / medium: one cheap light per fixture, a bit under the panel so the
		# ceiling doesn't blow out
		var o := OmniLight3D.new()
		o.position = pos - Vector3(0, 0.75, 0)
		o.omni_range = 6.5
		o.omni_attenuation = 1.3
		o.light_energy = energy * 1.1
		o.light_specular = 0.2
		o.light_color = Color(1.0, 0.97, 0.9)
		_fade_far(o)
		add_child(o)
		if rng.randf() < 0.06 + darkness * 0.25:
			_flicker.append(o)
		return
	# high: a soft downward spot for the floor + a weak fill for the walls
	var s := SpotLight3D.new()
	s.position = pos - Vector3(0, 0.06, 0)
	s.rotation.x = -PI / 2
	s.spot_angle = 68.0
	s.spot_range = pos.y + 1.0
	s.spot_attenuation = 0.9
	s.light_energy = energy * 2.2
	s.light_specular = 0.25
	s.light_color = Color(1.0, 0.97, 0.9)
	add_child(s)
	var f := OmniLight3D.new()
	f.position = pos - Vector3(0, 0.9, 0)
	f.omni_range = 5.5
	f.omni_attenuation = 1.6
	f.light_energy = energy * 0.35
	f.light_specular = 0.0
	f.light_color = Color(1.0, 0.97, 0.9)
	_fade_far(s)
	_fade_far(f)
	add_child(f)
	if rng.randf() < 0.06 + darkness * 0.25:
		_flicker.append(s)
		_flicker.append(f)



## lights far away from the camera switch off, they're hidden by fog anyway
func _fade_far(l: Light3D) -> void:
	l.distance_fade_enabled = true
	var low := int(Settings.data.quality) == 0
	l.distance_fade_begin = 15.0 if low else 24.0
	l.distance_fade_length = 4.0 if low else 6.0


func add_locker(pos: Vector3, yaw: float) -> Locker:
	var lk := Locker.new()
	lk.position = pos
	lk.rotation.y = yaw
	add_child(lk)
	lockers.append(lk)
	has_lockers = true
	return lk


func add_pickup(pos: Vector3, item: String, amount := 1) -> void:
	var p := Pickup.new()
	p.item = item
	p.amount = amount
	p.position = pos
	add_child(p)


## scatter a little loot: gold piles, batteries, bandages
func scatter_loot(spots: Array) -> void:
	for s in spots:
		var r := rng.randf()
		if Game.mod("empty_pockets") and rng.randf() < 0.6:
			continue
		if r < 0.25:
			add_pickup(s, "gold", rng.randi_range(5, 20))
		elif r < 0.33:
			add_pickup(s, "battery")
		elif r < 0.38:
			add_pickup(s, "bandage")
		elif r < 0.40:
			add_pickup(s, "vitamins")


func add_drawer_desk(pos: Vector3, yaw: float) -> void:
	var d := Drawer.new()
	d.position = pos
	d.rotation.y = yaw
	d.loot_seed = rng.randi()
	d.build()
	add_child(d)


func label(text: String, pos: Vector3, yaw: float, size := 64, color := Color(0.1, 0.1, 0.1), font_key := "") -> Label3D:
	var l := Label3D.new()
	l.text = text
	l.position = pos
	l.rotation.y = yaw
	l.font_size = size
	l.pixel_size = 0.004
	l.modulate = color
	l.outline_size = 0
	if font_key != "":
		l.font = Assets.font(font_key)
	add_child(l)
	return l
