extends RoomBase
## locker room: rows of lockers on both side walls. a-60 / a-120 can come when you enter.


func build() -> void:
	room_type = "locker_room"
	entity_spawn_ok = true
	var length := rng.randf_range(10.0, 13.0)
	var w := 7.0
	shell(-w / 2, w / 2, length, 3.2, "front", rng.randf_range(-1.5, 1.5), "carpet_blue")
	var z := 2.0
	while z < length - 1.5:
		add_locker(Vector3(w / 2 - 0.4, 0, z), -PI / 2)
		add_locker(Vector3(-w / 2 + 0.4, 0, z), PI / 2)
		z += 1.0
	# busted variants: only 3 still open (nico's rooms) or 5 (doors' rooms)
	var usable: int = [0, 3, 5][rng.randi() % 3]
	if usable > 0 and lockers.size() > usable:
		room_type = "locker_room_broken"
		var keep := lockers.duplicate()
		keep.shuffle()
		for lk in keep.slice(usable):
			lockers.erase(lk)
			lk.ready.connect(func(): lk.break_open(true), CONNECT_ONE_SHOT)
	Props.table(self, Vector3(0, 0, length * 0.5), 0.0, Vector3(1.0, 0.8, 2.4))
	scatter_loot([Vector3(0, 0.82, length * 0.5)])
	make_path([Vector3(1.0, 0, length * 0.3), Vector3(1.0, 0, length * 0.7)])
