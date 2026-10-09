extends RoomBase
## catwalk room (from doors' rooms): you walk in on a metal catwalk high above the floor,
## two lockers on each side of the landing. stairs on both sides go down to the bottom,
## where there's one more locker and a cubicle. the catwalk leads straight to the next door.
## a-60 / a-120 can come when you enter.

const DROP := 3.4
const H := 3.2


func build() -> void:
	room_type = "catwalk"
	entity_spawn_ok = true
	var length := rng.randf_range(15.0, 18.0)
	var x0 := -6.0
	var x1 := 6.0
	var w := x1 - x0
	_height = H
	box(Vector3(w + WALL_T, 0.2, length + WALL_T), Vector3(0, -DROP - 0.1, length / 2), Mats.get_mat("carpet_blue"))
	box(Vector3(w + WALL_T, 0.2, length + WALL_T), Vector3(0, H + 0.1, length / 2), Mats.get_mat("ceiling"), false)
	# walls: solid below the catwalk level, the doorways above it
	wall(Vector3(x1, -DROP, 0), Vector3(x0, -DROP, 0), DROP)
	wall(Vector3(x1, 0, 0), Vector3(x0, 0, 0), H, [[x1, DOOR_W]])
	wall(Vector3(x0, -DROP, length), Vector3(x1, -DROP, length), DROP)
	wall(Vector3(x0, 0, length), Vector3(x1, 0, length), H, [[-x0, DOOR_W]])
	wall(Vector3(x1, -DROP, 0), Vector3(x1, -DROP, length), DROP + H)
	wall(Vector3(x0, -DROP, 0), Vector3(x0, -DROP, length), DROP + H)
	set_exit(Vector3(0, 0, length), 0.0)
	# the landing and the catwalk
	var metal := Mats.get_mat("metal")
	box(Vector3(6.4, 0.15, 1.9), Vector3(0, -0.075, 0.95), metal)
	box(Vector3(2.0, 0.15, length - 1.9), Vector3(0, -0.075, 1.9 + (length - 1.9) / 2), metal)
	for s in [-1.0, 1.0]:
		# railings along the catwalk (solid to walk against, thin to look at)
		var rz := 1.9 + (length - 1.9) / 2
		box(Vector3(0.05, 1.0, length - 1.9), Vector3(s * 1.0, 0.5, rz), metal, false)
		box(Vector3(0.06, 0.06, length - 1.9), Vector3(s * 1.0, 1.0, rz), Mats.get_mat("dark"), false)
		box(Vector3(0.1, 1.1, length - 1.9), Vector3(s * 1.0, 0.55, rz), metal, true).visible = false
		# a rail on the open side of the landing, between the catwalk and the stairs
		box(Vector3(0.1, 1.1, 0.1), Vector3(s * 1.0, 0.55, 1.9), metal, true)
		# lockers on the landing, two per side
		add_locker(Vector3(s * 1.75, 0, 0.42), 0.0)
		add_locker(Vector3(s * 2.75, 0, 0.42), 0.0)
		_stairs(s * 2.6)
	# down at the bottom: a locker against one wall, a cubicle against the other
	var lz := rng.randf_range(length * 0.55, length - 2.5)
	var side := 1.0 if rng.randf() < 0.5 else -1.0
	add_locker(Vector3(side * (w / 2 - 0.4), -DROP, lz), -side * PI / 2)
	Props.cubicle(self, Vector3(-side * (w / 2 - 1.1), -DROP, lz - 1.0), side * PI / 2)
	for j in 3:
		ceiling_light(Vector3(-3.0, H, length * (j + 0.5) / 3.0))
		ceiling_light(Vector3(3.0, H, length * (j + 0.5) / 3.0))
	# lamps hanging lower so the bottom isn't a black pit
	for j in 2:
		var l := OmniLight3D.new()
		l.position = Vector3(0, -0.8, length * (0.35 + j * 0.4))
		l.light_color = Color(1.0, 0.97, 0.9)
		l.light_energy = 1.2
		l.omni_range = 9.0
		add_child(l)
	scatter_loot([Vector3(side * (w / 2 - 0.6), -DROP, lz - 2.5), Vector3(0.5, 0, length - 1.2)])
	make_path([Vector3(0, 0, 3.0), Vector3(0, 0, length - 2.0)])


## steps going down from the landing toward the exit side, with a smooth ramp to walk on
func _stairs(x: float) -> void:
	var run := 5.4
	var steps := 17
	for i in steps:
		var y := -(i + 1) * DROP / steps
		box(Vector3(1.2, 0.08, run / steps + 0.02), Vector3(x, y + DROP / steps - 0.04, 1.9 + (i + 0.5) * run / steps), Mats.get_mat("metal"), false)
	var ang := atan2(DROP, run)
	var cs := CollisionShape3D.new()
	var sh := BoxShape3D.new()
	sh.size = Vector3(1.2, 0.1, sqrt(run * run + DROP * DROP))
	cs.shape = sh
	cs.transform = Transform3D(Basis(Vector3.RIGHT, ang), Vector3(x, -DROP / 2 - 0.05, 1.9 + run / 2))
	_body.add_child(cs)
