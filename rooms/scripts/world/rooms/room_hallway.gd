extends RoomBase
## long plain hallway. no lockers, nothing spawns when you enter it.


func build() -> void:
	room_type = "hallway"
	var length := rng.randf_range(10.0, 16.0)
	var w := 3.0
	var ex := rng.randf_range(-0.6, 0.6)
	shell(-w / 2, w / 2, length, 3.0, "front", ex, "carpet_red" if rng.randf() < 0.5 else "carpet_blue")
	if rng.randf() < 0.5:
		Props.plant(self, Vector3(w / 2 - 0.4, 0, length * 0.5), rng.randf() < 0.3)
	scatter_loot([Vector3(-w / 2 + 0.35, 0, length * 0.3)])
	if rng.randf() < 0.3:
		add_atm(Vector3(-w / 2 + 0.3, 0, length * 0.6), PI / 2)
	make_path([Vector3(0, 0, length * 0.5), Vector3(ex, 0, length - 1.0)])
