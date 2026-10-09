extends RoomBase
## storage room: pillars, shelves, flipped and stacked tables, two lockers.
## it has lockers but nothing spawns when you enter it.


func build() -> void:
	room_type = "storage"
	var length := rng.randf_range(10.0, 12.0)
	var w := 8.0
	# exit stays between the two lockers so they can never block it
	shell(-w / 2, w / 2, length, 3.4, "front", rng.randf_range(-1.0, 0.9), "carpet_blue")
	for i in 3:
		Props.pillar(self, Vector3(-1.4, 0, 3.0 + i * 2.6), 3.4)
	for i in 3:
		Props.shelf(self, Vector3(w / 2 - 0.35, 0, 2.5 + i * 2.5), -PI / 2, i == 1)
	# upside down tables on the right, stacked on the left
	for i in 2:
		var t := Node3D.new()
		t.position = Vector3(-3.0, Props.TABLE_H, 2.5 + i * 2.5)
		t.rotation.z = PI
		add_child(t)
		box(Vector3(1.4, 0.05, 0.8), Vector3(0, 0.0, 0), Mats.get_mat("wood"), false, 0.0, t)
		box(Vector3(1.4, Props.TABLE_H, 0.8), Vector3(-3.0, Props.TABLE_H / 2, 2.5 + i * 2.5), Mats.get_mat("wood"), true).visible = false
	# three tables stacked on top of each other on the other side
	for i in 3:
		box(Vector3(1.4, 0.05, 0.8), Vector3(w / 2 - 1.6, Props.TABLE_H * (i + 1) - 0.025 + i * 0.02, length - 2.0), Mats.get_mat("wood"), false, rng.randf_range(-0.15, 0.15))
		for lx in [-0.6, 0.6]:
			for lz in [-0.32, 0.32]:
				box(Vector3(0.05, Props.TABLE_H - 0.05, 0.05), Vector3(w / 2 - 1.6 + lx, Props.TABLE_H * i + (Props.TABLE_H - 0.05) / 2 + i * 0.02, length - 2.0 + lz), Mats.get_mat("metal"), false)
	box(Vector3(1.4, Props.TABLE_H * 3, 0.8), Vector3(w / 2 - 1.6, Props.TABLE_H * 1.5, length - 2.0), Mats.get_mat("wood"), true).visible = false
	var near_exit := rng.randf() < 0.5
	var lz := length - 0.45 if near_exit else 0.45
	add_locker(Vector3(-w / 2 + 1.2, 0, lz), PI if near_exit else 0.0)
	add_locker(Vector3(w / 2 - 1.8, 0, lz), PI if near_exit else 0.0)
	scatter_loot([Vector3(w / 2 - 0.5, 0.735, 5.0), Vector3(0.5, 0, length * 0.6)])
	make_path([Vector3(0.4, 0, length * 0.3), Vector3(0.4, 0, length * 0.75)])
