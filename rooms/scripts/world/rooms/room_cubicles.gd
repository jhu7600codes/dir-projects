extends RoomBase
## open office with cubicles (desks with drawers) and two lockers by the exit


func build() -> void:
	room_type = "cubicles"
	entity_spawn_ok = true
	var length := rng.randf_range(13.0, 16.0)
	var w := 11.0
	shell(-w / 2, w / 2, length, 3.2, "front", 0.0, "carpet_blue")
	var z := 3.0
	while z < length - 3.0:
		Props.cubicle(self, Vector3(-3.4, 0, z), PI / 2)
		Props.cubicle(self, Vector3(3.4, 0, z), -PI / 2)
		z += 2.6
	add_locker(Vector3(-1.4, 0, length - 0.45), PI)
	add_locker(Vector3(1.4, 0, length - 0.45), PI)
	if rng.randf() < 0.5:
		Props.plant(self, Vector3(0, 0, length * 0.5), true)
	make_path([Vector3(0, 0, length * 0.3), Vector3(0, 0, length * 0.7)])
