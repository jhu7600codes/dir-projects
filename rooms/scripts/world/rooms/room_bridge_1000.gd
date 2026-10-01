extends RoomBase
## a-1000: a bridge over a dark void with a glowing door at the end. the real ending.


func build() -> void:
	room_type = "bridge_1000"
	is_special = true
	var length := 55.0
	# small landing behind the entry door so the doorway has something to stand in
	box(Vector3(3.0, 0.2, 2.0), Vector3(0, -0.1, 1.0), Mats.get_mat("carpet_red"))
	wall(Vector3(1.5, 0, 0), Vector3(-1.5, 0, 0), 3.0, [[1.5, DOOR_W]])
	# the bridge
	box(Vector3(2.4, 0.3, length), Vector3(0, -0.15, length / 2 + 1.0), Mats.get_mat("frame"))
	for sx in [-1.25, 1.25]:
		box(Vector3(0.08, 0.9, length), Vector3(sx, 0.45, length / 2 + 1.0), Mats.get_mat("metal"))
	# dim little lights along the bridge so you can see where you walk
	for i in range(0, int(length), 9):
		var l := OmniLight3D.new()
		l.position = Vector3(0, 1.2, 4.0 + i)
		l.light_color = Color(0.7, 0.75, 0.9)
		l.light_energy = 0.35
		l.omni_range = 4.0
		add_child(l)
	var ed := ExitDoor.new()
	ed.style = "void"
	ed.position = Vector3(0, 0, length + 0.9)
	add_child(ed)
	# far away white doorways floating in the dark
	for i in 14:
		var side := -1.0 if i % 2 == 0 else 1.0
		var p := Vector3(side * rng.randf_range(15, 45), rng.randf_range(-8, 10), rng.randf_range(5, length + 20))
		var g := Node3D.new()
		g.position = p
		g.rotation.y = rng.randf_range(-0.6, 0.6) + (PI / 2 if side > 0 else -PI / 2)
		add_child(g)
		box(Vector3(0.15, 2.4, 0.15), Vector3(-0.7, 1.2, 0), Mats.get_mat("white_glow"), false, 0.0, g)
		box(Vector3(0.15, 2.4, 0.15), Vector3(0.7, 1.2, 0), Mats.get_mat("white_glow"), false, 0.0, g)
		box(Vector3(1.55, 0.15, 0.15), Vector3(0, 2.4, 0), Mats.get_mat("white_glow"), false, 0.0, g)
	exit_local = Transform3D(Basis(), Vector3(0, 0, length + 1.0))
	make_path([Vector3(0, 0, length * 0.5)])
