extends RoomBase
## box room (from doors' rooms): shelves and tables stacked with cardboard boxes that you
## have to weave around. no lockers, nothing spawns when you enter it.


func build() -> void:
	room_type = "box_room"
	var length := rng.randf_range(11.0, 14.0)
	var w := 6.5
	var ex := rng.randf_range(-1.8, 1.8)
	shell(-w / 2, w / 2, length, 3.0, "front", ex, "carpet_blue" if rng.randf() < 0.6 else "carpet_red")
	var z := 2.4
	var side := 1.0 if rng.randf() < 0.5 else -1.0
	while z < length - 2.2:
		# alternate sides so you zig-zag through
		if rng.randf() < 0.5:
			Props.shelf(self, Vector3(side * (w / 2 - 1.3), 0, z), 0.0)
		else:
			Props.table(self, Vector3(side * (w / 2 - 1.3), 0, z), 0.0, Vector3(2.2, Props.TABLE_H, 0.9))
			_boxes(Vector3(side * (w / 2 - 1.3), Props.TABLE_H, z), 3)
		_boxes(Vector3(side * (w / 2 - 0.5), 0, z + 1.1), 2)
		side = -side
		z += 2.3
	scatter_loot([Vector3(-w / 2 + 0.4, 0, length - 1.0), Vector3(w / 2 - 0.4, 0, 1.2)])
	make_path([Vector3(0, 0, length * 0.3), Vector3(0, 0, length * 0.7), Vector3(ex, 0, length - 1.0)])


func _boxes(pos: Vector3, n: int) -> void:
	var y := pos.y
	for i in n:
		var s := Vector3(rng.randf_range(0.4, 0.7), rng.randf_range(0.3, 0.5), rng.randf_range(0.4, 0.6))
		box(s, Vector3(pos.x + rng.randf_range(-0.3, 0.3), y + s.y / 2, pos.z + rng.randf_range(-0.15, 0.15)), Mats.get_mat("cardboard"), i == 0 and pos.y < 0.1, rng.randf_range(-0.3, 0.3))
		y += s.y
