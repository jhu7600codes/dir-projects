extends WiresRoom
## a wider room full of big pipes coming down from the ceiling, crates and loose wires

func build() -> void:
	room_type = "wires_pipes"
	var length := rng.randf_range(10.0, 13.0)
	var w := 7.0
	shell(-w / 2, w / 2, length, 3.4, "front", rng.randf_range(-1.5, 1.5), "concrete_floor")
	for i in rng.randi_range(3, 5):
		var p := Vector3(rng.randf_range(-2.6, 2.6), 0, rng.randf_range(2.5, length - 2.5))
		box(Vector3(0.45, 3.4, 0.45), p + Vector3(0, 1.7, 0), Mats.get_mat("rust" if rng.randf() < 0.4 else "pipe"), true)
	for i in rng.randi_range(1, 3):
		crate(Vector3(rng.randf_range(-3.0, 3.0), 0, rng.randf_range(2.0, length - 2.0)), rng.randf_range(0.6, 0.9))
	pipes_along(-w / 2, length, 3.4)
	var spots := []
	for i in rng.randi_range(2, 4):
		spots.append(Vector3(rng.randf_range(-2.5, 2.5), 0, rng.randf_range(3.0, length - 2.5)))
	raw_wires(spots, 3.4)
	scatter_loot([Vector3(rng.randf_range(-3.0, 3.0), 0, length * 0.6)])
	make_path([Vector3(0, 0, length * 0.5)])
