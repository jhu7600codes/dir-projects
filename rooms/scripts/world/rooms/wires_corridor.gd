extends WiresRoom
## a narrow concrete maintenance corridor with pipes and raw wires to weave around

func build() -> void:
	room_type = "wires_corridor"
	var length := rng.randf_range(10.0, 16.0)
	var w := 3.4
	var side := "front"
	var offset := 0.0
	if rng.randf() < 0.35:
		side = "left" if turn > 0 else "right"
		offset = length - 1.4
	shell(-w / 2, w / 2, length, 3.0, side, offset, "concrete_floor")
	pipes_along(w / 2 if rng.randf() < 0.5 else -w / 2, length, 3.0)
	wall_cables(-w / 2, length)
	var spots := []
	var z := rng.randf_range(3.0, 4.5)
	var flip := 1.0 if rng.randf() < 0.5 else -1.0
	while z < length - 3.0:
		if rng.randf() < 0.7:
			spots.append(Vector3(flip * rng.randf_range(0.4, 0.9), 0, z))
			flip = -flip
		z += rng.randf_range(2.5, 4.0)
	raw_wires(spots, 3.0)
	scatter_loot([Vector3(-w / 2 + 0.4, 0, length * 0.5)])
	make_path([Vector3(0, 0, length * 0.3), Vector3(0, 0, length * 0.7)])
