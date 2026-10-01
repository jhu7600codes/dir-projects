extends RoomBase
## three lockers lined up on the far wall


func build() -> void:
	room_type = "three_locker"
	entity_spawn_ok = true
	var length := rng.randf_range(7.0, 9.0)
	shell(-3.0, 3.0, length, 3.0, "front", 2.4 if rng.randf() < 0.5 else -2.4)
	var ex: float = exit_local.origin.x
	for i in 3:
		add_locker(Vector3(-ex * 0.6 + (i - 1) * 1.0, 0, length - 0.4), PI)
	Props.plant(self, Vector3(2.5, 0, 0.6))
	scatter_loot([Vector3(-2.5, 0, 1.0)])
	make_path([Vector3(0, 0, length * 0.5), Vector3(ex, 0, length - 1.5)])
