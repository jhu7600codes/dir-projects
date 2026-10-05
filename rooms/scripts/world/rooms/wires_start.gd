extends WiresRoom
## w-00: where you come in. a small concrete room at the bottom of the maintenance stairs.

func build() -> void:
	room_type = "wires_start"
	var length := 8.0
	var w := 6.0
	shell(-w / 2, w / 2, length, 3.0, "front", 0.0, "concrete_floor")
	# the stairs you came down, blocked off behind you
	for i in 6:
		box(Vector3(2.0, 0.2, 0.35), Vector3(-1.8, 0.1 + i * 0.2, 0.4 + i * 0.35), Mats.get_mat("concrete"), true)
	stencil("THE WIRES", Vector3(w / 2 - 0.1, 2.0, length * 0.5), -PI / 2, 96)
	stencil("MAINTENANCE ACCESS ONLY", Vector3(w / 2 - 0.1, 1.6, length * 0.5), -PI / 2, 40)
	pipes_along(-w / 2, length, 3.0)
	wall_cables(w / 2, length)
	crate(Vector3(2.0, 0, 6.5))
	make_path([Vector3(0, 0, length * 0.5)])
