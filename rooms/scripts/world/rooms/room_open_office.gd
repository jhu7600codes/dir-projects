extends RoomBase
## open plan office: two long rows of desks back to back down the middle, a printer corner,
## a couple of lockers along the wall. a-60 can come here.


func build() -> void:
	room_type = "open_office"
	entity_spawn_ok = true
	var length := rng.randf_range(13.0, 17.0)
	var w := 12.0
	shell(-w / 2, w / 2, length, 3.2, "front", rng.randf_range(-3.0, 3.0), "carpet_blue" if rng.randf() < 0.6 else "carpet_red")
	var z := 3.5
	while z < length - 3.0:
		for side in [-1.0, 1.0]:
			# two rows, desks facing each other with a low divider between
			add_drawer_desk(Vector3(side * 2.4 - 0.75, 0, z), 0.0 if side < 0 else PI)
			add_drawer_desk(Vector3(side * 2.4 + 0.75, 0, z), 0.0 if side < 0 else PI)
		box(Vector3(3.2, 0.5, 0.06), Vector3(-2.4, Props.TABLE_H + 0.25, z), Mats.get_mat("cubicle"), false)
		box(Vector3(3.2, 0.5, 0.06), Vector3(2.4, Props.TABLE_H + 0.25, z), Mats.get_mat("cubicle"), false)
		z += 3.2
	# lockers along one wall, near the middle
	var lz := length * 0.5
	var lx := w / 2 - 0.4 if rng.randf() < 0.5 else -w / 2 + 0.4
	for i in 2:
		add_locker(Vector3(lx, 0, lz + i * 1.0), -PI / 2 if lx > 0 else PI / 2)
	if rng.randf() < 0.6:
		Props.plant(self, Vector3(-w / 2 + 0.5, 0, length - 0.6))
	scatter_loot([Vector3(0, 0, length * 0.4)])
	make_path([Vector3(0, 0, length * 0.3), Vector3(0, 0, length * 0.7)])
