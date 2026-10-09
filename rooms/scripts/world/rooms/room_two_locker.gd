extends RoomBase
## room1 (from nico's rooms): blue carpet and two lockers in the far corners. that's it.


func build() -> void:
	room_type = "two_locker"
	entity_spawn_ok = true
	var length := rng.randf_range(8.0, 10.0)
	var w := 7.0
	shell(-w / 2, w / 2, length, 3.0, "front", rng.randf_range(-1.2, 1.2), "carpet_blue")
	add_locker(Vector3(w / 2 - 0.5, 0, length - 0.45), PI)
	add_locker(Vector3(-w / 2 + 0.5, 0, length - 0.45), PI)
	scatter_loot([Vector3(w / 2 - 1.2, 0, length - 0.5)])
	make_path([Vector3(0, 0, length * 0.4), Vector3(0, 0, length * 0.75)])
