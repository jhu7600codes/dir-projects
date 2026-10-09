extends RoomBase
## e-shaped room (from doors' rooms): a hall across the room with three dead-end prongs.
## one locker at the end of the middle one, nothing else. the exit is at the end of one
## of the outer prongs (mirrored sometimes). nothing spawns when you enter it.


func build() -> void:
	room_type = "e_shaped"
	var length := rng.randf_range(9.0, 11.0)
	var w := 10.5
	var flip := 1.0 if rng.randf() < 0.5 else -1.0
	shell(-w / 2, w / 2, length, 3.0, "front", flip * 4.0, "carpet_red" if rng.randf() < 0.5 else "carpet_blue")
	# the two blocks between the prongs
	var depth := length - 3.0
	for s in [-1.0, 1.0]:
		box(Vector3(1.5, 3.0, depth), Vector3(s * 1.95, 1.5, 3.0 + depth / 2), Mats.get_mat("wall"))
	add_locker(Vector3(0, 0, length - 0.45), PI)
	make_path([Vector3(0, 0, 1.6), Vector3(flip * 4.0, 0, 1.6), Vector3(flip * 4.0, 0, length - 1.5)])
