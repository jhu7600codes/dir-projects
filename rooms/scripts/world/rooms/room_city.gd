extends RoomBase
## the great city: what's outside after a-1000. you walk out of the glass doors of the miles
## building (the office you were trapped in: the company is called miles, its logo is on the
## tower) into a city at dusk. the avenue is split into blocks by metal gates, c-01 to c-30.
## entities still find you out here: hide in the phone booths. if you powered the office in
## the wires, your coworkers walk the sidewalks too. at the end a bus takes you home.
## local space: you enter at the origin facing +z.

const ROAD := 7.0      # half width of the road
const WALK := 11.0     # outer edge of the sidewalk
const FACADES := ["facade_a", "facade_b", "facade_c"]

var length := 40.0


func build() -> void:
	room_type = "city"
	darkness = 0.0
	var last := number >= Game.last_door()
	length = 40.0 if number == 0 else rng.randf_range(38.0, 56.0)
	_ground()
	if number == 0:
		_miles_tower()
	_sides()
	_lamps_and_lights()
	_cars()
	if last:
		_bus_stop()
		_building(Vector3(0, 0, length + 10.0), Vector3(30.0, 45.0, 20.0))
		exit_local = Transform3D(Basis(), Vector3(0, 0, length - 2.0))
	else:
		_gate()
	if number > 0:
		_booths()
	if Game.fixed_office:
		_walkers()
	if number == 0:
		var amb := AudioStreamPlayer.new()
		amb.stream = Assets.sound("city_ambience")
		amb.bus = "Ambience"
		amb.volume_db = -8.0
		amb.autoplay = true
		amb.set_meta("no_merge", true)
		add_child(amb)
	make_path([Vector3(0, 0, length * 0.3), Vector3(0, 0, length * 0.7)])


## the whole width of the street is sealed behind you, not just a doorway
func _add_back_seal() -> void:
	_back_seal = CollisionShape3D.new()
	var sh := BoxShape3D.new()
	sh.size = Vector3(WALK * 2.0 + 4.0, 8.0, 0.3)
	_back_seal.shape = sh
	_back_seal.position = Vector3(0, 4.0, -0.4)
	_back_seal.disabled = true
	_body.add_child(_back_seal)


func _ground() -> void:
	box(Vector3(WALK * 2.0 + 60.0, 0.2, length), Vector3(0, -0.1, length / 2), Mats.get_mat("asphalt"))
	for sd in [-1.0, 1.0]:
		box(Vector3(WALK - ROAD, 0.02, length), Vector3(sd * (ROAD + WALK) / 2, 0.01, length / 2), Mats.get_mat("sidewalk"), false)
		box(Vector3(0.2, 0.12, length), Vector3(sd * ROAD, 0.06, length / 2), Mats.get_mat("frame"), false)
	if number == 0:
		box(Vector3(40, 0.02, 16), Vector3(0, 0.012, 7.0), Mats.get_mat("sidewalk"), false)
	var z := 4.0
	while z < length - 3.0:
		box(Vector3(0.15, 0.02, 3.0), Vector3(0, 0.015, z), Mats.get_mat("lane"), false)
		z += 7.0
	# a crosswalk before the gate
	for i in 7:
		box(Vector3(1.0, 0.02, 3.0), Vector3(-5.4 + i * 1.8, 0.016, length - 4.0), Mats.get_mat("plastic"), false)


## buildings on both sides, wall to wall
func _sides() -> void:
	for sd in [-1.0, 1.0]:
		var z := 0.0
		while z < length - 0.5:
			var d := minf(rng.randf_range(10.0, 16.0), length - z)
			var depth := rng.randf_range(16.0, 24.0)
			_building(Vector3(sd * (WALK + depth / 2), 0, z + d / 2), Vector3(depth, rng.randf_range(14.0, 48.0), d))
			z += d


## a tall fence of bars across the whole street, with the gate (the door) in the middle
func _gate() -> void:
	set_exit(Vector3(0, 0, length), 0.0)
	var gap := DOOR_W / 2 + 0.12
	for sd in [-1.0, 1.0]:
		var w := WALK - gap
		var cx: float = sd * (gap + w / 2)
		var col := box(Vector3(w, 3.4, 0.2), Vector3(cx, 1.7, length), Mats.get_mat("dark"), true)
		col.visible = false
		var x := gap + 0.1
		while x < WALK:
			box(Vector3(0.06, 3.4, 0.06), Vector3(sd * x, 1.7, length), Mats.get_mat("dark"), false)
			x += 0.32
		box(Vector3(w, 0.1, 0.1), Vector3(cx, 3.35, length), Mats.get_mat("dark"), false)
		box(Vector3(w, 0.1, 0.1), Vector3(cx, 0.4, length), Mats.get_mat("dark"), false)
	box(Vector3(DOOR_W + 0.3, 1.0, 0.1), Vector3(0, DOOR_H + 0.6, length), Mats.get_mat("dark"), true)


## phone booths to hide in (they work like lockers)
func _booths() -> void:
	var n := rng.randi_range(1, 3)
	for i in n:
		var sd := -1.0 if rng.randf() < 0.5 else 1.0
		var z := rng.randf_range(5.0, length - 8.0)
		add_locker(Vector3(sd * (WALK - 0.6), 0, z), -sd * PI / 2, "booth")
	entity_spawn_ok = true


## the building you came out of. MILES on the front, the logo up high
func _miles_tower() -> void:
	var h := 72.0
	box(Vector3(36, h, 40), Vector3(0, h / 2, -20.5), Mats.get_mat("facade_b"))
	box(Vector3(37, 0.6, 41), Vector3(0, h + 0.3, -20.5), Mats.get_mat("roof"), false)
	# the entrance: tall dark glass doors (the way you came, they're locked behind you now)
	box(Vector3(6.0, 4.0, 0.2), Vector3(0, 2.0, -0.45), Mats.get_mat("glass_dark"), false)
	box(Vector3(6.4, 0.3, 0.3), Vector3(0, 4.15, -0.4), Mats.get_mat("metal"), false)
	box(Vector3(0.1, 4.0, 0.25), Vector3(0, 2.0, -0.38), Mats.get_mat("metal"), false)
	box(Vector3(14, 2.6, 0.3), Vector3(0, 8.0, -0.4), Mats.get_mat("dark"), false)
	var name_sign := Label3D.new()
	name_sign.text = "MILES"
	name_sign.font_size = 160
	name_sign.pixel_size = 0.02
	name_sign.modulate = Color(0.95, 0.95, 0.92)
	name_sign.outline_size = 0
	name_sign.position = Vector3(0, 8.0, -0.2)
	add_child(name_sign)
	var logo := Sprite3D.new()
	logo.texture = load("res://assets/branding/icon.png")
	logo.pixel_size = 12.0 / maxf(1.0, logo.texture.get_height())
	logo.shaded = false
	logo.position = Vector3(0, 22.0, -0.3)
	add_child(logo)
	var glow := OmniLight3D.new()
	glow.position = Vector3(0, 6.0, 3.0)
	glow.light_color = Color(1.0, 0.9, 0.75)
	glow.light_energy = 1.4
	glow.omni_range = 12.0
	add_child(glow)


func _building(c: Vector3, size: Vector3) -> void:
	var mat := Mats.get_mat(FACADES[rng.randi() % FACADES.size()])
	box(size, c + Vector3(0, size.y / 2, 0), mat)
	box(Vector3(size.x + 0.6, 0.5, size.z + 0.6), c + Vector3(0, size.y + 0.25, 0), Mats.get_mat("roof"), false)
	# ground floor shop front: a dark band of glass and an awning now and then
	var face := Vector3(-signf(c.x), 0, 0) if absf(c.x) > 1.0 else Vector3(0, 0, -1)
	var front := c + face * (size.x / 2 if absf(c.x) > 1.0 else size.z / 2)
	if absf(c.x) > 1.0:
		box(Vector3(0.1, 2.6, size.z * 0.8), front + face * 0.05 + Vector3(0, 1.5, 0), Mats.get_mat("glass_dark"), false)
		if rng.randf() < 0.5:
			box(Vector3(1.4, 0.1, size.z * 0.6), front + face * 0.7 + Vector3(0, 3.2, 0), Mats.get_mat(["car_red", "car_blue", "poster_green"][rng.randi() % 3]), false)


func _lamps_and_lights() -> void:
	var z := 6.0
	var k := 0
	while z < length - 2.0:
		var sd := -1.0 if k % 2 == 0 else 1.0
		var base := Vector3(sd * (WALK - 0.6), 0, z)
		box(Vector3(0.14, 5.5, 0.14), base + Vector3(0, 2.75, 0), Mats.get_mat("dark"), false)
		box(Vector3(1.2, 0.1, 0.1), base + Vector3(-sd * 0.6, 5.45, 0), Mats.get_mat("dark"), false)
		box(Vector3(0.45, 0.12, 0.3), base + Vector3(-sd * 1.15, 5.35, 0), Mats.get_mat("lamp_glow"), false)
		var l := OmniLight3D.new()
		l.position = base + Vector3(-sd * 1.15, 5.0, 0)
		l.light_color = Color(1.0, 0.82, 0.55)
		l.light_energy = 1.3
		l.omni_range = 11.0
		l.distance_fade_enabled = true
		l.distance_fade_begin = 45.0
		l.distance_fade_length = 10.0
		add_child(l)
		z += 14.0
		k += 1
	if rng.randf() < 0.6:
		_tree(Vector3((-1.0 if rng.randf() < 0.5 else 1.0) * (WALK - 1.8), 0, rng.randf_range(8.0, length - 8.0)))


func _tree(p: Vector3) -> void:
	box(Vector3(1.2, 0.5, 1.2), p + Vector3(0, 0.25, 0), Mats.get_mat("concrete"), true)
	box(Vector3(0.2, 2.6, 0.2), p + Vector3(0, 1.8, 0), Mats.get_mat("wood_dark"), false)
	box(Vector3(2.2, 1.8, 2.2), p + Vector3(0, 3.6, 0), Mats.get_mat("tree"), false, rng.randf())


func _cars() -> void:
	var colors := ["car_red", "car_blue", "car_white", "car_black"]
	var z := 6.0
	while z < length - 9.0:
		for sd in [-1.0, 1.0]:
			if rng.randf() > 0.4:
				continue
			var c := Vector3(sd * (ROAD - 1.3), 0, z)
			var mat := Mats.get_mat(colors[rng.randi() % colors.size()])
			box(Vector3(1.8, 0.7, 4.2), c + Vector3(0, 0.6, 0), mat, true)
			box(Vector3(1.6, 0.6, 2.2), c + Vector3(0, 1.2, -0.2), Mats.get_mat("glass_dark"), false)
			for wx in [-0.8, 0.8]:
				for wz in [-1.3, 1.3]:
					box(Vector3(0.25, 0.6, 0.6), c + Vector3(wx, 0.3, wz), Mats.get_mat("dark"), false)
		z += 8.0


## the end of the avenue: a bus stop and the night bus home
func _bus_stop() -> void:
	var p := Vector3(ROAD + 2.4, 0, length - 12.0)
	for dz in [-1.6, 1.6]:
		box(Vector3(0.1, 2.5, 0.1), p + Vector3(0.6, 1.25, dz), Mats.get_mat("metal"), false)
	box(Vector3(1.6, 0.1, 3.6), p + Vector3(0, 2.55, 0), Mats.get_mat("metal"), false)
	box(Vector3(0.05, 1.9, 3.4), p + Vector3(0.75, 1.3, 0), Mats.get_mat("glass"), false)
	box(Vector3(0.45, 0.45, 2.4), p + Vector3(0.4, 0.22, 0), Mats.get_mat("wood"), true)
	var sign := Label3D.new()
	sign.text = "BUS"
	sign.font_size = 64
	sign.pixel_size = 0.006
	sign.modulate = Color(1, 1, 1)
	sign.outline_size = 0
	sign.position = p + Vector3(-0.2, 2.9, 1.8)
	add_child(sign)
	var bus := Vector3(ROAD - 1.8, 0, length - 12.0)
	box(Vector3(2.6, 3.0, 11.0), bus + Vector3(0, 1.8, 0), Mats.get_mat("car_white"), true)
	box(Vector3(2.62, 0.9, 9.0), bus + Vector3(0, 2.4, -0.5), Mats.get_mat("glass_dark"), false)
	box(Vector3(2.64, 0.4, 11.0), bus + Vector3(0, 0.6, 0), Mats.get_mat("car_blue"), false)
	var l := OmniLight3D.new()
	l.position = bus + Vector3(2.0, 2.4, 0)
	l.light_color = Color(1.0, 0.95, 0.8)
	l.light_energy = 1.5
	l.omni_range = 8.0
	add_child(l)
	var it := Interactable.make(Vector3(1.0, 3.0, 2.0), "get on the bus. go home")
	it.position = bus + Vector3(1.6, 1.5, 3.5)
	it.used.connect(func(_p): Game.run_finished.emit("city"))
	add_child(it)


## only after the wires: people walking up and down the sidewalks
func _walkers() -> void:
	for i in rng.randi_range(2, 5):
		var w := CityWalker.new()
		var sd := -1.0 if i % 2 == 0 else 1.0
		w.lane_x = sd * rng.randf_range(ROAD + 1.0, WALK - 1.5)
		w.z_min = 2.0
		w.z_max = length - 3.0
		w.position = Vector3(w.lane_x, 0, rng.randf_range(3.0, length - 4.0))
		add_child(w)
