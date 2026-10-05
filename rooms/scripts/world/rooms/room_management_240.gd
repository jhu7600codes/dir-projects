extends RoomBase
## a-240: an office with desks along the walls. one locker got pushed a little to the side,
## and behind it there's a crack in the wall. squeeze through and a short hallway full of
## vines ends at a door marked MANAGEMENT. the key is in one of the drawers here.

const CRACK_Z := 4.0
const CRACK_W := 1.1


func build() -> void:
	room_type = "management"
	var length := 14.0
	var w := 10.0
	shell(-w / 2, w / 2, length, 3.2, "front", 1.5, "carpet_blue", {"right": [[CRACK_Z, CRACK_W]]})
	# desks along the left wall and the middle, one of them has the key
	var desks := []
	for i in 3:
		desks.append(add_drawer_desk(Vector3(w / 2 - 0.5, 0, 3.0 + i * 3.4), -PI / 2))
	for i in 2:
		desks.append(add_drawer_desk(Vector3(0.6, 0, 6.0 + i * 3.6), PI if i == 0 else 0.0))
	desks.append(add_drawer_desk(Vector3(-w / 2 + 0.6, 0, 10.5), PI / 2))
	desks[rng.randi() % desks.size()].gives_key = true
	for d in desks:
		if rng.randf() < 0.5:
			Props.chair(self, d.position + d.basis * Vector3(0, 0, 0.8), d.rotation.y + PI + rng.randf_range(-0.5, 0.5), rng.randf() < 0.3)
	# the locker that got pushed aside, just enough to see the crack behind it
	var lk := add_locker(Vector3(-w / 2 + 0.42, 0, CRACK_Z + 0.95), PI / 2 + 0.35)
	lk.position.x += 0.12
	_crack(-w / 2)
	_vine_hall(-w / 2)
	make_path([Vector3(0, 0, length * 0.3), Vector3(1.0, 0, length * 0.75)])


## broken plaster around the hole: jagged dark bits on the edges and rubble on the floor
func _crack(x: float) -> void:
	var dark := Mats.get_mat("crack")
	for i in 9:
		var side := -1.0 if i % 2 == 0 else 1.0
		var y := rng.randf_range(0.2, DOOR_H + 0.2)
		box(Vector3(WALL_T + 0.04, rng.randf_range(0.15, 0.45), rng.randf_range(0.08, 0.2)), Vector3(x, y, CRACK_Z + side * (CRACK_W / 2 + 0.05)), dark, false, rng.randf_range(-0.4, 0.4))
	for i in 3:
		box(Vector3(WALL_T + 0.04, 0.1, rng.randf_range(0.3, 0.6)), Vector3(x, DOOR_H + rng.randf_range(0.0, 0.25), CRACK_Z + rng.randf_range(-0.3, 0.3)), dark, false, rng.randf_range(-0.3, 0.3))
	for i in 6:
		box(Vector3(rng.randf_range(0.08, 0.2), 0.06, rng.randf_range(0.08, 0.2)), Vector3(x + rng.randf_range(0.1, 0.8), 0.03, CRACK_Z + rng.randf_range(-0.8, 0.8)), Mats.get_mat("wall"), false, rng.randf())


## the hallway behind the crack: narrow, overgrown, one dim light, the management door
func _vine_hall(x: float) -> void:
	var len := 6.0
	var hw := CRACK_W + 0.3
	var x_end := x - len
	var cx := x - len / 2.0
	var h := 2.6
	var c := Mats.get_mat("concrete")
	box(Vector3(len, 0.2, hw + 0.4), Vector3(cx, -0.1, CRACK_Z), Mats.get_mat("concrete_floor"))
	box(Vector3(len, 0.2, hw + 0.4), Vector3(cx, h + 0.1, CRACK_Z), c, false)
	box(Vector3(len, h, 0.2), Vector3(cx, h / 2, CRACK_Z - hw / 2 - 0.1), c)
	box(Vector3(len, h, 0.2), Vector3(cx, h / 2, CRACK_Z + hw / 2 + 0.1), c)
	box(Vector3(0.2, h, hw + 0.4), Vector3(x_end - 0.1, h / 2, CRACK_Z), c)
	# vines hanging down and creeping along the walls
	var vine := Mats.get_mat("vine")
	for i in 22:
		var vx := rng.randf_range(x_end + 0.2, x - 0.3)
		var vl := rng.randf_range(0.4, 1.6)
		var vz := CRACK_Z + rng.randf_range(-hw / 2, hw / 2)
		box(Vector3(0.04, vl, 0.04), Vector3(vx, h - vl / 2, vz), vine, false, rng.randf())
	for i in 14:
		var wall_z := CRACK_Z + (hw / 2 + 0.0) * (1.0 if i % 2 == 0 else -1.0)
		box(Vector3(rng.randf_range(0.3, 1.0), rng.randf_range(0.05, 0.12), 0.05), Vector3(rng.randf_range(x_end + 0.3, x - 0.3), rng.randf_range(0.3, 2.3), wall_z), vine, false, 0.0)
	var l := OmniLight3D.new()
	l.position = Vector3(x_end + 1.2, h - 0.3, CRACK_Z)
	l.light_color = Color(0.75, 0.9, 0.6)
	l.light_energy = 0.7
	l.omni_range = 5.0
	add_child(l)
	var md := ManagementDoor.new()
	md.position = Vector3(x_end + 0.02, 0, CRACK_Z)
	md.rotation.y = PI / 2  # facing back into the hallway (+x)
	add_child(md)
