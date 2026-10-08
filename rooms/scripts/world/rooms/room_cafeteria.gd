extends RoomBase
## the cafeteria (the office version of halt's long hallway in doors): a long hall with
## rows of tables and benches, a serving counter with trays, vending machines glowing at the
## end, a menu board. two lockers by the exit.


func build() -> void:
	room_type = "cafeteria"
	entity_spawn_ok = true
	var length := rng.randf_range(20.0, 26.0)
	var w := 10.0
	shell(-w / 2, w / 2, length, 3.4, "front", rng.randf_range(-2.0, 2.0), "carpet_red")
	# serving counter along the left wall with trays and a sneeze guard
	Props.counter(self, Vector3(w / 2 - 0.5, 0, length * 0.35), PI / 2, 6.0)
	box(Vector3(0.05, 0.4, 6.0), Vector3(w / 2 - 0.75, Props.COUNTER_H + 0.35, length * 0.35), Mats.get_mat("glass"), false)
	for i in 5:
		box(Vector3(0.35, 0.02, 0.45), Vector3(w / 2 - 0.45, Props.COUNTER_H + 0.01, length * 0.35 - 2.4 + i * 1.2), Mats.get_mat("poster_grey"), false)
	var menu := Label3D.new()
	menu.text = "TODAY\nsoup .......... 3\nsandwich ...... 5\ncoffee ........ 1"
	menu.font_size = 36
	menu.pixel_size = 0.004
	menu.modulate = Color(0.95, 0.95, 0.9)
	menu.outline_size = 0
	menu.position = Vector3(w / 2 - 0.12, 2.4, length * 0.35)
	menu.rotation.y = -PI / 2
	add_child(menu)
	box(Vector3(0.04, 0.9, 1.6), Vector3(w / 2 - 0.1, 2.4, length * 0.35), Mats.get_mat("dark"), false)
	# rows of tables with benches
	var z := 3.0
	while z < length - 3.5:
		for x in [-2.6, 0.6]:
			box(Vector3(2.4, 0.06, 0.9), Vector3(x, 0.76, z), Mats.get_mat("plastic"), true)
			for lx in [-1.0, 1.0]:
				box(Vector3(0.06, 0.74, 0.06), Vector3(x + lx, 0.37, z), Mats.get_mat("metal"), false)
			for bz in [-0.75, 0.75]:
				box(Vector3(2.4, 0.06, 0.3), Vector3(x, 0.45, z + bz), Mats.get_mat("wood"), true)
			if rng.randf() < 0.4:
				box(Vector3(0.35, 0.02, 0.45), Vector3(x + rng.randf_range(-0.8, 0.8), 0.8, z), Mats.get_mat("poster_grey"), false, rng.randf())
		z += 2.6
	# vending machines at the far end
	for i in 2:
		var p := Vector3(-w / 2 + 0.55 + i * 1.0, 0, length - 0.6)
		box(Vector3(0.9, 1.9, 0.8), p + Vector3(0, 0.95, 0), Mats.get_mat("car_red" if i == 0 else "car_blue"), true)
		box(Vector3(0.6, 1.2, 0.02), p + Vector3(-0.08, 1.1, -0.41), Mats.get_mat("white_glow"), false)
	add_locker(Vector3(w / 2 - 0.4, 0, length - 1.5), -PI / 2)
	add_locker(Vector3(w / 2 - 0.4, 0, length - 2.5), -PI / 2)
	scatter_loot([Vector3(w / 2 - 0.45, Props.COUNTER_H + 0.02, length * 0.35), Vector3(0.6, 0.8, 3.0)])
	make_path([Vector3(-0.8, 0, length * 0.3), Vector3(-0.8, 0, length * 0.7)])
