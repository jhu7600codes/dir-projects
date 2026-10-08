extends RoomBase
## the server room: rows of humming black racks with little status lights, cold blue light,
## cables everywhere. one locker by the door.


func build() -> void:
	room_type = "server_room"
	entity_spawn_ok = true
	var length := rng.randf_range(10.0, 13.0)
	var w := 8.0
	shell(-w / 2, w / 2, length, 3.0, "front", 0.0, "carpet_blue")
	var leds := ["led_blue", "led_green", "led_green", "led_red"]
	for row in [-2.4, 2.4]:
		var z := 2.6
		while z < length - 2.2:
			box(Vector3(1.0, 2.1, 0.9), Vector3(row, 1.05, z), Mats.get_mat("rack"), true)
			for k in 8:
				var face := -1.0 if row > 0 else 1.0
				box(Vector3(0.02, 0.025, 0.04), Vector3(row + face * 0.51, 0.4 + k * 0.2, z + rng.randf_range(-0.3, 0.3)), Mats.get_mat(leds[rng.randi() % leds.size()]), false)
			z += 1.1
	for i in 6:
		box(Vector3(0.05, 0.04, rng.randf_range(1.5, 3.5)), Vector3(rng.randf_range(-1.2, 1.2), 0.02, rng.randf_range(2.0, length - 2.0)), Mats.get_mat("wire"), false, rng.randf_range(-0.4, 0.4))
	add_locker(Vector3(-w / 2 + 0.4, 0, 1.6), PI / 2)
	var cool := OmniLight3D.new()
	cool.position = Vector3(0, 2.4, length * 0.5)
	cool.light_color = Color(0.5, 0.7, 1.0)
	cool.light_energy = 0.8
	cool.omni_range = 7.0
	add_child(cool)
	var hum := AudioStreamPlayer3D.new()
	hum.stream = Assets.sound("w50_hum")
	hum.bus = "Ambience"
	hum.volume_db = -14.0
	hum.unit_size = 4.0
	hum.position = Vector3(0, 1.5, length * 0.5)
	hum.autoplay = true
	add_child(hum)
	scatter_loot([Vector3(0, 0, length * 0.5)])
	make_path([Vector3(0, 0, length * 0.3), Vector3(0, 0, length * 0.7)])
