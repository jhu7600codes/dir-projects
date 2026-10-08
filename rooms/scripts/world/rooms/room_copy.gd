extends RoomBase
## the copy room: two big copiers, stacks of paper, a shelf of supplies, one locker.


func build() -> void:
	room_type = "copy_room"
	entity_spawn_ok = true
	var length := rng.randf_range(7.0, 8.5)
	var w := 6.0
	shell(-w / 2, w / 2, length, 3.0, "front", rng.randf_range(-1.5, 1.5), "carpet_blue")
	for i in 2:
		var p := Vector3(w / 2 - 0.55, 0, 2.2 + i * 1.6)
		box(Vector3(0.8, 1.0, 1.0), p + Vector3(0, 0.5, 0), Mats.get_mat("plastic"), true)
		box(Vector3(0.7, 0.05, 0.9), p + Vector3(0, 1.03, 0), Mats.get_mat("dark"), false)
		box(Vector3(0.3, 0.05, 0.5), p + Vector3(-0.45, 0.75, 0), Mats.get_mat("paper"), false)
	for i in rng.randi_range(2, 4):
		box(Vector3(0.3, rng.randf_range(0.1, 0.4), 0.22), Vector3(rng.randf_range(-1.5, 0.5), 0.1, rng.randf_range(1.5, length - 1.5)), Mats.get_mat("paper"), false, rng.randf())
	Props.shelf(self, Vector3(-w / 2 + 0.3, 0, length * 0.55), PI / 2)
	add_locker(Vector3(-w / 2 + 0.4, 0, length - 1.0), PI / 2)
	scatter_loot([Vector3(w / 2 - 0.55, 1.06, 2.2)])
	make_path([Vector3(0, 0, length * 0.5)])
