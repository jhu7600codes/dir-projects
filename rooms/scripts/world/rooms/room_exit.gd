extends RoomBase
## exit room (after a-200): pillar at the entrance, broken water dispenser, a tipped plant,
## and a big hole in the wall with the exit door behind it. the normal door continues on.


func build() -> void:
	room_type = "exit_room"
	is_special = true
	darkness = minf(darkness, 0.6)  # a few lights still work in here
	var length := 11.0
	var w := 8.0
	var x0 := -w / 2
	var az := length * 0.55
	shell(x0, w / 2, length, 3.0, "front", 1.5, "carpet_blue", {"right": [[az, 2.4]]})
	Props.pillar(self, Vector3(0, 0, 1.8), 3.0, 1.4)
	Props.water_dispenser(self, Vector3(w / 2 - 0.5, 0, 3.5), true)
	Props.plant(self, Vector3(-2.5, 0, length - 1.5), true)
	Props.table(self, Vector3(-2.2, 0, 3.0), 0.7)
	# alcove behind the hole in the -x wall, with the exit door at the back
	var ax := x0 - 3.0
	box(Vector3(3.2, 0.2, 3.2), Vector3(x0 - 1.5, -0.1, az), Mats.get_mat("carpet_blue"))
	box(Vector3(3.2, 0.2, 3.2), Vector3(x0 - 1.5, 3.1, az), Mats.get_mat("ceiling"), false)
	wall(Vector3(x0, 0, az - 1.5), Vector3(ax, 0, az - 1.5), 3.0)
	wall(Vector3(x0, 0, az + 1.5), Vector3(ax, 0, az + 1.5), 3.0)
	var ed := ExitDoor.new()
	ed.position = Vector3(ax + 0.1, 0, az)
	ed.rotation.y = -PI / 2
	add_child(ed)
	wall(Vector3(ax, 0, az - 1.5), Vector3(ax, 0, az + 1.5), 3.0, [[1.5, DOOR_W]])
	make_path([Vector3(1.5, 0, 3.0), Vector3(1.5, 0, length - 1.5)])

