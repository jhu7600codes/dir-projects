extends WiresRoom
## an old generator humming in the middle of the room, cables all over the floor

func build() -> void:
	room_type = "wires_generator"
	var length := rng.randf_range(10.0, 12.0)
	var w := 8.0
	shell(-w / 2, w / 2, length, 3.4, "front", 0.0, "concrete_floor")
	var c := Vector3(rng.randf_range(-1.2, 1.2), 0, length * 0.5)
	box(Vector3(2.2, 1.6, 1.4), c + Vector3(0, 0.8, 0), Mats.get_mat("pipe"), true)
	box(Vector3(2.25, 0.15, 1.45), c + Vector3(0, 0.3, 0), Mats.get_mat("caution"), false)
	box(Vector3(0.3, 0.3, 0.05), c + Vector3(0.6, 1.2, 0.72), Mats.get_mat("switch_off"), false)
	for i in 5:
		box(Vector3(0.06, 0.06, rng.randf_range(2.0, 4.0)), c + Vector3(rng.randf_range(-3, 3), 0.03, rng.randf_range(-1.5, 1.5)), Mats.get_mat("wire"), false, rng.randf_range(-1.2, 1.2))
	var hum := AudioStreamPlayer3D.new()
	hum.stream = Assets.sound("w50_hum")
	hum.bus = "Ambience"
	hum.unit_size = 3.0
	hum.volume_db = -8.0
	hum.position = c + Vector3(0, 1.0, 0)
	hum.autoplay = true
	add_child(hum)
	wall_cables(w / 2, length)
	raw_wires([Vector3(c.x + (2.0 if c.x < 0 else -2.0), 0, rng.randf_range(2.5, length - 2.5))], 3.4)
	scatter_loot([c + Vector3(0, 1.62, 0)])
	make_path([Vector3(c.x + (2.2 if c.x < 0 else -2.2), 0, length * 0.5)])
