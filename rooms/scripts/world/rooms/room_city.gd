extends RoomBase
## the great city: what's outside after a-1000. you walk out of the glass doors of the miles
## building (the office you were trapped in: the company is called miles, its logo is on the
## tower) into an empty city at dusk. one long avenue with cross streets, tall buildings,
## street lamps, parked cars, traffic lights. no one around... unless you powered the office
## in the wires, then people are walking the sidewalks. at the end of the avenue a bus is
## waiting to take you home.
## local space: you start at the origin facing +z, the miles tower is behind you (-z).

const AVE := 170.0     # avenue length
const ROAD := 7.0      # half width of the road
const WALK := 11.0     # outer edge of the sidewalk
const CROSS := [45.0, 95.0, 145.0]
const FACADES := ["facade_a", "facade_b", "facade_c"]

var walkers_on := false


func build() -> void:
	room_type = "city"
	is_special = true
	darkness = 0.0
	walkers_on = Game.fixed_office
	_ground()
	_miles_tower()
	_blocks()
	_lamps_and_lights()
	_cars()
	_bus_stop()
	_bounds()
	if walkers_on:
		_walkers()
	var amb := AudioStreamPlayer.new()
	amb.stream = Assets.sound("city_ambience")
	amb.bus = "Ambience"
	amb.volume_db = -8.0
	amb.autoplay = true
	add_child(amb)
	make_path([Vector3(0, 0, 10.0), Vector3(0, 0, AVE - 10.0)])


func _ground() -> void:
	box(Vector3(160, 0.2, AVE + 60), Vector3(0, -0.1, AVE / 2), Mats.get_mat("asphalt"))
	# sidewalks along the avenue (flush with the road, a curb line for looks)
	for s in [-1.0, 1.0]:
		box(Vector3(WALK - ROAD, 0.02, AVE + 6), Vector3(s * (ROAD + WALK) / 2, 0.01, AVE / 2), Mats.get_mat("sidewalk"), false)
		box(Vector3(0.2, 0.12, AVE + 6), Vector3(s * ROAD, 0.06, AVE / 2), Mats.get_mat("frame"), false)
	# a plaza in front of the miles tower
	box(Vector3(40, 0.02, 16), Vector3(0, 0.012, 7.0), Mats.get_mat("sidewalk"), false)
	# lane markings
	var z := 8.0
	while z < AVE:
		box(Vector3(0.15, 0.02, 3.0), Vector3(0, 0.015, z), Mats.get_mat("lane"), false)
		z += 7.0
	for cz in CROSS:
		for i in 7:
			box(Vector3(1.0, 0.02, 3.5), Vector3(-5.4 + i * 1.8, 0.016, cz - 7.5), Mats.get_mat("plastic"), false)


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


## rows of buildings along both sides of the avenue, a wall of buildings closing off the
## cross streets and the far end
func _blocks() -> void:
	var segments := [[5.0, CROSS[0] - 7.0], [CROSS[0] + 7.0, CROSS[1] - 7.0], [CROSS[1] + 7.0, CROSS[2] - 7.0], [CROSS[2] + 7.0, AVE + 4.0]]
	for s in [-1.0, 1.0]:
		for seg in segments:
			var z: float = seg[0]
			while z < seg[1] - 4.0:
				var d := minf(rng.randf_range(10.0, 18.0), seg[1] - z)
				var hgt := rng.randf_range(14.0, 55.0)
				var depth := rng.randf_range(18.0, 28.0)
				_building(Vector3(s * (WALK + 1.0 + depth / 2), 0, z + d / 2), Vector3(depth, hgt, d))
				z += d + rng.randf_range(0.0, 0.4)
		# the far side of the cross streets
		var zz := -2.0
		while zz < AVE + 10.0:
			var d2 := rng.randf_range(14.0, 22.0)
			_building(Vector3(s * 56.0, 0, zz + d2 / 2), Vector3(22.0, rng.randf_range(20.0, 60.0), d2))
			zz += d2
	# closing off the end of the avenue
	_building(Vector3(0, 0, AVE + 18.0), Vector3(30.0, 45.0, 20.0))


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
	while z < AVE:
		for s in [-1.0, 1.0]:
			var base := Vector3(s * (WALK - 0.6), 0, z)
			box(Vector3(0.14, 5.5, 0.14), base + Vector3(0, 2.75, 0), Mats.get_mat("dark"), false)
			box(Vector3(1.2, 0.1, 0.1), base + Vector3(-s * 0.6, 5.45, 0), Mats.get_mat("dark"), false)
			box(Vector3(0.45, 0.12, 0.3), base + Vector3(-s * 1.15, 5.35, 0), Mats.get_mat("lamp_glow"), false)
			if k % 2 == 0:
				var l := OmniLight3D.new()
				l.position = base + Vector3(-s * 1.15, 5.0, 0)
				l.light_color = Color(1.0, 0.82, 0.55)
				l.light_energy = 1.3
				l.omni_range = 11.0
				l.distance_fade_enabled = true
				l.distance_fade_begin = 45.0
				l.distance_fade_length = 10.0
				add_child(l)
		z += 14.0
		k += 1
	# traffic lights at the crossings
	for cz in CROSS:
		for s in [-1.0, 1.0]:
			var p := Vector3(s * (ROAD + 0.8), 0, cz - 7.0 * s)
			box(Vector3(0.15, 3.2, 0.15), p + Vector3(0, 1.6, 0), Mats.get_mat("dark"), false)
			box(Vector3(0.35, 0.9, 0.3), p + Vector3(0, 3.5, 0), Mats.get_mat("dark"), false)
			box(Vector3(0.2, 0.2, 0.32), p + Vector3(0, 3.75, 0), Mats.get_mat("tl_red" if rng.randf() < 0.5 else "dark"), false)
			box(Vector3(0.2, 0.2, 0.32), p + Vector3(0, 3.3, 0), Mats.get_mat("tl_green" if rng.randf() < 0.5 else "dark"), false)
		# trees in planters on the corners
		for s in [-1.0, 1.0]:
			_tree(Vector3(s * (WALK - 1.8), 0, cz + 9.0))


func _tree(p: Vector3) -> void:
	box(Vector3(1.2, 0.5, 1.2), p + Vector3(0, 0.25, 0), Mats.get_mat("concrete"), true)
	box(Vector3(0.2, 2.6, 0.2), p + Vector3(0, 1.8, 0), Mats.get_mat("wood_dark"), false)
	box(Vector3(2.2, 1.8, 2.2), p + Vector3(0, 3.6, 0), Mats.get_mat("tree"), false, rng.randf())


func _cars() -> void:
	var colors := ["car_red", "car_blue", "car_white", "car_black"]
	var z := 10.0
	while z < AVE - 8.0:
		for s in [-1.0, 1.0]:
			var near_cross := false
			for cz in CROSS:
				if absf(z - cz) < 9.0:
					near_cross = true
			if near_cross or rng.randf() > 0.45:
				continue
			var c := Vector3(s * (ROAD - 1.3), 0, z)
			var mat := Mats.get_mat(colors[rng.randi() % colors.size()])
			box(Vector3(1.8, 0.7, 4.2), c + Vector3(0, 0.6, 0), mat, true)
			box(Vector3(1.6, 0.6, 2.2), c + Vector3(0, 1.2, -0.2), Mats.get_mat("glass_dark"), false)
			for wx in [-0.8, 0.8]:
				for wz in [-1.3, 1.3]:
					box(Vector3(0.25, 0.6, 0.6), c + Vector3(wx, 0.3, wz), Mats.get_mat("dark"), false)
		z += 8.0


## the end of the avenue: a bus stop and the night bus home
func _bus_stop() -> void:
	var p := Vector3(ROAD + 2.4, 0, AVE - 12.0)
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
	var bus := Vector3(ROAD - 1.8, 0, AVE - 12.0)
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


func _bounds() -> void:
	for x in [-70.0, 70.0]:
		box(Vector3(1, 40, AVE + 80), Vector3(x, 20, AVE / 2), Mats.get_mat("dark"), true).visible = false
	box(Vector3(160, 40, 1), Vector3(0, 20, -1.0), Mats.get_mat("dark"), true).visible = false
	box(Vector3(160, 40, 1), Vector3(0, 20, AVE + 40), Mats.get_mat("dark"), true).visible = false


## only after the wires: people walking up and down the sidewalks
func _walkers() -> void:
	for i in 16:
		var w := CityWalker.new()
		var s := -1.0 if i % 2 == 0 else 1.0
		w.lane_x = s * rng.randf_range(ROAD + 1.0, WALK - 1.5)
		w.z_min = 4.0
		w.z_max = AVE - 6.0
		w.position = Vector3(w.lane_x, 0, rng.randf_range(6.0, AVE - 10.0))
		add_child(w)
