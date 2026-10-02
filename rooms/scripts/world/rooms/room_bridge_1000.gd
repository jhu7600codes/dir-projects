extends RoomBase
## a-1000: an old wooden bridge over a dark void with a glowing door at the end. the real ending.


func build() -> void:
	room_type = "bridge_1000"
	is_special = true
	var length := 55.0
	# small landing behind the entry door so the doorway has something to stand in
	box(Vector3(3.0, 0.2, 2.0), Vector3(0, -0.1, 1.0), Mats.get_mat("carpet_red"))
	wall(Vector3(1.5, 0, 0), Vector3(-1.5, 0, 0), 3.0, [[1.5, DOOR_W]])
	# the bridge: wooden planks with small gaps on two long beams, wooden railings.
	# one invisible box is the floor you walk on, the planks are just for looks.
	var mid := length / 2 + 1.0
	box(Vector3(2.4, 0.2, length), Vector3(0, -0.1, mid), Mats.get_mat("wood_old"), true).visible = false
	var z := 1.0
	while z < length + 1.0:
		var w := rng.randf_range(0.22, 0.3)
		var tilt := rng.randf_range(-0.015, 0.015)
		var plank := box(Vector3(2.4 + rng.randf_range(-0.08, 0.08), 0.07, w), Vector3(rng.randf_range(-0.04, 0.04), -0.035, z + w / 2), Mats.get_mat("wood_old" if rng.randf() < 0.7 else "wood_dark"), false, tilt)
		plank.name = "plank"
		z += w + 0.035
	for sx in [-0.85, 0.85]:
		box(Vector3(0.18, 0.25, length), Vector3(sx, -0.2, mid), Mats.get_mat("wood_dark"), false)
	for sx in [-1.2, 1.2]:
		var p := 1.2
		while p < length + 1.0:
			box(Vector3(0.1, 1.0, 0.1), Vector3(sx, 0.5, p), Mats.get_mat("wood_dark"), false)
			p += 2.5
		box(Vector3(0.08, 0.08, length), Vector3(sx, 0.98, mid), Mats.get_mat("wood_old"), true)
		box(Vector3(0.05, 0.06, length), Vector3(sx, 0.55, mid), Mats.get_mat("wood_old"), false)
	# dim little lights along the bridge so you can see where you walk
	for i in range(0, int(length), 9):
		var l := OmniLight3D.new()
		l.position = Vector3(0, 1.2, 4.0 + i)
		l.light_color = Color(1.0, 0.85, 0.6)  # warm, like old lanterns
		l.light_energy = 0.8
		l.omni_range = 5.5
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
