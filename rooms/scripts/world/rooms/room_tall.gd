extends RoomBase
## tall room (from nico's rooms): two floors. you come in downstairs (blue carpet, a cubicle
## with remi's nameplate, two lockers at the back, a pillar holding up the bridge). two
## staircases along the side walls go up to the top floor: diamond plate, a bridge across
## and a catwalk with railings to the next door. the stair railings have no collision,
## so you can drop straight down to the lockers. a-60 / a-120 can come when you enter.

const UP := 3.4   # height of the top floor
const H := 3.2    # top floor to ceiling


func build() -> void:
	room_type = "tall_room"
	entity_spawn_ok = true
	var length := rng.randf_range(15.0, 17.0)
	var x0 := -6.0
	var x1 := 6.0
	var w := x1 - x0
	_height = UP + H
	var plate := Mats.get_mat("metal")
	box(Vector3(w + WALL_T, 0.2, length + WALL_T), Vector3(0, -0.1, length / 2), Mats.get_mat("carpet_blue"))
	box(Vector3(w + WALL_T, 0.2, length + WALL_T), Vector3(0, UP + H + 0.1, length / 2), Mats.get_mat("ceiling"), false)
	wall(Vector3(x1, 0, 0), Vector3(x0, 0, 0), UP + H, [[x1, DOOR_W]])
	wall(Vector3(x0, 0, length), Vector3(x1, 0, length), UP)
	wall(Vector3(x0, UP, length), Vector3(x1, UP, length), H, [[-x0, DOOR_W]])
	wall(Vector3(x1, 0, 0), Vector3(x1, 0, length), UP + H)
	wall(Vector3(x0, 0, 0), Vector3(x0, 0, length), UP + H)
	set_exit(Vector3(0, UP, length), 0.0)
	var top0 := 9.0   # where the stairs arrive
	# the bridge across the room at the top of both staircases
	box(Vector3(w, 0.15, 1.6), Vector3(0, UP - 0.075, top0 + 0.8), plate)
	# the catwalk from the bridge to the exit landing
	var cz0 := top0 + 1.6
	var land := 2.0
	box(Vector3(2.0, 0.15, length - land - cz0), Vector3(0, UP - 0.075, cz0 + (length - land - cz0) / 2), plate)
	box(Vector3(w, 0.15, land), Vector3(0, UP - 0.075, length - land / 2), plate)
	# railings with collision on the top floor
	_rail(Vector3(-w / 2 + 1.5, UP, top0), Vector3(w / 2 - 1.5, UP, top0), true)
	for s in [-1.0, 1.0]:
		_rail(Vector3(s * 1.0, UP, cz0), Vector3(s * 1.0, UP, length - land), true)
		_rail(Vector3(s * 1.0, UP, cz0), Vector3(s * w / 2, UP, cz0), true)
		_rail(Vector3(s * 1.0, UP, length - land), Vector3(s * w / 2, UP, length - land), true)
		_stairs(s * (w / 2 - 0.75), top0)
	# the pillar holding up the catwalk
	box(Vector3(0.5, UP, 0.5), Vector3(0, UP / 2, cz0 + (length - land - cz0) / 2), plate)
	# downstairs: two lockers at the back, and remi's cubicle
	add_locker(Vector3(-1.6, 0, length - 0.45), PI)
	add_locker(Vector3(1.6, 0, length - 0.45), PI)
	var side := 1.0 if rng.randf() < 0.5 else -1.0
	Props.cubicle(self, Vector3(side * 2.6, 0, 4.0), PI)
	Props.plant(self, Vector3(-side * 2.8, 0, 1.0))
	for j in 3:
		ceiling_light(Vector3(-3.0, UP + H, length * (j + 0.5) / 3.0))
		ceiling_light(Vector3(3.0, UP + H, length * (j + 0.5) / 3.0))
	for j in 2:
		var l := OmniLight3D.new()
		l.position = Vector3(0, UP - 0.6, 3.0 + j * 7.0)
		l.light_color = Color(1.0, 0.97, 0.9)
		l.light_energy = 1.1
		l.omni_range = 9.0
		add_child(l)
	scatter_loot([Vector3(side * (w / 2 - 0.75), 0.25, 3.6), Vector3(0, 0, length - 1.6)])
	make_path([Vector3(0, 0, 4.0), Vector3(0, 0, top0), Vector3(0, 0, length - 1.5)])


## a railing from a to b. top floor ones block you, the stair ones don't
func _rail(a: Vector3, b: Vector3, solid: bool) -> void:
	var d := b - a
	var len := d.length()
	var yaw := atan2(d.x, d.z)
	var mid := (a + b) / 2
	box(Vector3(0.06, 0.06, len), mid + Vector3(0, 1.0, 0), Mats.get_mat("dark"), false, yaw)
	box(Vector3(0.04, 0.04, len), mid + Vector3(0, 0.5, 0), Mats.get_mat("metal"), false, yaw)
	var n := maxi(2, int(len / 1.2) + 1)
	for i in n:
		var p := a + d * (float(i) / (n - 1))
		box(Vector3(0.06, 1.0, 0.06), p + Vector3(0, 0.5, 0), Mats.get_mat("dark"), false)
	if solid:
		box(Vector3(0.1, 1.1, len), mid + Vector3(0, 0.55, 0), Mats.get_mat("dark"), true, yaw).visible = false


## steps along a side wall going up toward +z, ending at z = top. a ramp to walk on
func _stairs(x: float, top: float) -> void:
	var run := 6.0
	var steps := 17
	var z0 := top - run
	for i in steps:
		var y := (i + 1) * UP / steps
		box(Vector3(1.4, y, run / steps + 0.01), Vector3(x, y / 2, z0 + (i + 0.5) * run / steps), Mats.get_mat("metal"), false)
	var ang := atan2(UP, run)
	var cs := CollisionShape3D.new()
	var sh := BoxShape3D.new()
	sh.size = Vector3(1.4, 0.1, sqrt(run * run + UP * UP))
	cs.shape = sh
	cs.transform = Transform3D(Basis(Vector3.RIGHT, -ang), Vector3(x, UP / 2 - 0.05, z0 + run / 2))
	_body.add_child(cs)
	# a solid side so you can't walk under the stairs into them
	box(Vector3(1.4, UP, 0.1), Vector3(x, UP / 2, top - 0.05), Mats.get_mat("metal"), true).visible = false
	# railing on the open side, no collision (like the original)
	var inner := x - signf(x) * 0.7
	_rail(Vector3(inner, 0.2, z0), Vector3(inner, UP, top), false)
