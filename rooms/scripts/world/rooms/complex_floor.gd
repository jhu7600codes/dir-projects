extends RoomBase
## the complex (your apartment complex, after the bus home): one floor = one flight of the
## stairwell, up to a landing. on the landing: the gate up to the next flight, the GATE
## CONTROL button, and the door into that floor's apartment.
##   gate open      - go on up
##   gate shut      - press gate control. no beep: it opens. a BEEP: something is wrong in
##                    the apartment (an anomaly). go in, find the FIRE ESCAPE box and pull
##                    the alarm: the fire burns everything that isn't standing in the box's
##                    marked square, the anomaly too (it's out cold for ~7 minutes) and the
##                    gate opens. then get back to the stairs.
## the apartments can be anything: a normal flat, a hotel floor, a hospital ward...
## everything is built off to the +x side of the stairwell, so floors stack on top of
## each other like a real building.

const UP := 3.2    # one floor
const H := 3.0     # landing to ceiling
const X0 := -2.2
const X1 := 2.2
const L := 10.0
const STAIR0 := 1.2
const STAIR1 := 6.8
const APT_H := 2.9
const THEMES := ["apartment", "apartment", "hotel", "hospital"]

var apt_theme := "apartment"
var anomaly_here := false
var gate_open := false
var apt_bounds := AABB()       # local, the apartment's inside
var safe_spot := Vector3.ZERO  # local, the middle of the fire escape square
var fire: FireEscape
var anomaly: Anomaly
var _lamp: MeshInstance3D
var _door_z := 8.4
var home := false  # floor 150: your own apartment, no gate


func build() -> void:
	room_type = "complex_floor"
	theme = "complex"
	home = number >= Game.COMPLEX_LAST
	if home:
		apt_theme = "home"
		_stairwell()
		_home()
		make_path([Vector3(0, 0, STAIR0), Vector3(0, UP, STAIR1 + 0.6), Vector3(0, UP, L - 1.0)])
		return
	# what's on this floor (only depends on the seed, so everyone in a game gets the same)
	apt_theme = THEMES[rng.randi() % THEMES.size()]
	var chance := 0.0 if number <= 3 else lerpf(0.3, 0.6, number / 150.0)
	anomaly_here = rng.randf() < chance
	gate_open = not anomaly_here and rng.randf() < 0.45
	_stairwell()
	_gate_control()
	match apt_theme:
		"hotel": _hotel()
		"hospital": _hospital()
		_: _apartment()
	make_path([Vector3(0, 0, STAIR0), Vector3(0, UP, STAIR1 + 0.6), Vector3(0, UP, L - 1.0)])
	if not gate_open:
		_lock_gate.call_deferred()  # the door sets itself up once it's in the tree


# ---------- the stairwell ----------

func _stairwell() -> void:
	var conc := Mats.get_mat("concrete")
	var floor_m := Mats.get_mat("concrete_floor")
	_height = UP + H
	box(Vector3(X1 - X0, 0.2, STAIR0), Vector3(0, -0.1, STAIR0 / 2), floor_m)
	box(Vector3(X1 - X0, 0.2, L - STAIR1), Vector3(0, UP - 0.1, STAIR1 + (L - STAIR1) / 2), floor_m)
	box(Vector3(X1 - X0, 0.2, L), Vector3(0, UP + H + 0.1, L / 2), conc, false)
	wall(Vector3(X1, 0, 0), Vector3(X0, 0, 0), UP + H, [[X1, DOOR_W]], conc)
	wall(Vector3(X0, 0, L), Vector3(X1, 0, L), UP, [], conc)
	wall(Vector3(X0, UP, L), Vector3(X1, UP, L), H, [] if home else [[-X0, DOOR_W]], conc)
	wall(Vector3(X0, 0, 0), Vector3(X0, 0, L), UP + H, [], conc)
	wall(Vector3(X1, 0, 0), Vector3(X1, 0, L), UP, [], conc)
	wall(Vector3(X1, UP, 0), Vector3(X1, UP, L), H, [[_door_z, DOOR_W]], conc)
	if home:
		exit_local = Transform3D(Basis(), Vector3(0, UP, L))
	else:
		set_exit(Vector3(0, UP, L), 0.0)
	# the flight of stairs
	var run := STAIR1 - STAIR0
	var steps := 16
	for i in steps:
		var y := (i + 1) * UP / steps
		box(Vector3(X1 - X0, y, run / steps + 0.01), Vector3(0, y / 2, STAIR0 + (i + 0.5) * run / steps), Mats.get_mat("concrete_floor"), false)
	var ang := atan2(UP, run)
	var cs := CollisionShape3D.new()
	var sh := BoxShape3D.new()
	sh.size = Vector3(X1 - X0, 0.1, sqrt(run * run + UP * UP))
	cs.shape = sh
	cs.transform = Transform3D(Basis(Vector3.RIGHT, -ang), Vector3(0, UP / 2 - 0.05, STAIR0 + run / 2))
	_body.add_child(cs)
	# handrail and a yellow stripe along the wall
	box(Vector3(0.05, 0.05, run), Vector3(X0 + 0.12, UP / 2 + 0.95, STAIR0 + run / 2), Mats.get_mat("metal"), false).rotation.x = -ang
	box(Vector3(0.02, 0.12, L), Vector3(X0 + 0.01, UP + 1.1, L / 2), Mats.get_mat("caution"), false)
	# floor numbers painted on the wall, big
	var num := Label3D.new()
	num.text = str(number)
	num.font_size = 220
	num.pixel_size = 0.004
	num.modulate = Color(0.85, 0.75, 0.2)
	num.outline_size = 0
	num.position = Vector3(X0 + 0.02, UP + 1.6, L - 1.8)
	num.rotation.y = PI / 2
	add_child(num)
	var sign := Label3D.new()
	sign.text = "FLOOR %d" % number
	sign.font_size = 44
	sign.pixel_size = 0.004
	sign.modulate = Color(0.9, 0.9, 0.85)
	sign.outline_size = 0
	sign.position = Vector3(0, UP + DOOR_H + 0.25, L - 0.02)
	sign.rotation.y = PI
	if not home:
		add_child(sign)
	var apt := Label3D.new()
	apt.text = ("APT %dA  (HOME)" % number) if home else "APT %d%s" % [number, ["A", "B", "C", "D"][rng.randi() % 4]]
	apt.font_size = 36
	apt.pixel_size = 0.004
	apt.modulate = Color(0.95, 0.85, 0.5)
	apt.outline_size = 0
	apt.position = Vector3(X1 - 0.02, UP + DOOR_H + 0.25, _door_z)
	apt.rotation.y = -PI / 2
	add_child(apt)
	ceiling_light(Vector3(0, UP + H, 8.4))
	ceiling_light(Vector3(0, UP + H, 3.5))


# ---------- gate control ----------

func _gate_control() -> void:
	var p := Vector3(X0 + 0.08, UP + 1.3, 9.2)
	box(Vector3(0.1, 0.5, 0.4), p, Mats.get_mat("metal"), false)
	_lamp = box(Vector3(0.12, 0.08, 0.08), p + Vector3(0.02, 0.18, 0), Mats.get_mat("led_green" if gate_open else "led_red"), false)
	var l := Label3D.new()
	l.text = "GATE CONTROL"
	l.font_size = 28
	l.pixel_size = 0.004
	l.modulate = Color(0.95, 0.95, 0.9)
	l.outline_size = 0
	l.position = p + Vector3(0.07, 0.36, 0)
	l.rotation.y = PI / 2
	add_child(l)
	var it := Interactable.make(Vector3(0.5, 0.6, 0.6), "press gate control")
	it.position = p + Vector3(0.3, 0, 0)
	it.used.connect(func(_pl): press_gate())
	add_child(it)


func _lock_gate() -> void:
	if exit_door and not gate_open:
		exit_door.code_lock(_gate_shut, "locked", false)


func _gate_shut() -> void:
	if Game.player:
		Game.player.notify("the gate is shut. press the gate control.")


func press_gate(from_net := false) -> void:
	if gate_open:
		return
	if anomaly_here and not (fire and fire.used):
		Game.play_ui("deny")
		var a := AudioStreamPlayer3D.new()
		a.stream = Assets.sound("gate_beep")
		a.bus = "SFX"
		add_child(a)
		a.position = Vector3(X0 + 0.3, UP + 1.4, 9.2)
		a.play()
		if Game.player and not from_net:
			Game.player.notify("BEEP. something is wrong in the apartment.")
		return
	open_gate()
	if not from_net:
		Net.send_event(self, "gate")


func open_gate() -> void:
	gate_open = true
	if _lamp:
		_lamp.material_override = Mats.get_mat("led_green")
	if exit_door and exit_door.locked:
		exit_door.unlock()
	Game.play_ui("power_on")


func net_event(what: String, _arg) -> void:
	if what == "gate":
		press_gate(true)


## the fire went through: the anomaly is out cold and the gate opens
func on_fire() -> void:
	if anomaly:
		anomaly.stun(420.0)
	open_gate()


# ---------- the apartments ----------

## the outer box of an apartment: x from X1 to X1 + w, z from z0 to z1, at the landing height
func _apt_shell(w: float, z0: float, z1: float, floor_mat: String, wall_mat := "wall") -> void:
	var wm := Mats.get_mat(wall_mat)
	var ax0 := X1
	var ax1 := X1 + w
	var d := z1 - z0
	box(Vector3(w, 0.2, d), Vector3(ax0 + w / 2, UP - 0.1, z0 + d / 2), Mats.get_mat(floor_mat))
	box(Vector3(w, 0.2, d), Vector3(ax0 + w / 2, UP + APT_H + 0.1, z0 + d / 2), Mats.get_mat("ceiling"), false)
	wall(Vector3(ax0, UP, z0), Vector3(ax1, UP, z0), APT_H, [], wm)
	wall(Vector3(ax1, UP, z1), Vector3(ax0, UP, z1), APT_H, [], wm)
	wall(Vector3(ax1, UP, z0), Vector3(ax1, UP, z1), APT_H, [], wm)
	# the stairwell side, except the bit that's the stairwell wall itself
	if z0 < 0.0:
		wall(Vector3(ax0 + 0.01, UP, z0), Vector3(ax0 + 0.01, UP, 0.0), APT_H, [], wm)
	if z1 > L:
		wall(Vector3(ax0 + 0.01, UP, L), Vector3(ax0 + 0.01, UP, z1), APT_H, [], wm)
	apt_bounds = AABB(Vector3(ax0, UP, z0), Vector3(w, APT_H, d))
	for i in maxi(1, int(w / 5.0)):
		for j in maxi(1, int(d / 5.0)):
			ceiling_light(Vector3(ax0 + w * (i + 0.5) / maxi(1, int(w / 5.0)), UP + APT_H, z0 + d * (j + 0.5) / maxi(1, int(d / 5.0))))


## puts the fire escape box on a wall far from the front door, and the anomaly somewhere
func _finish_apt(spots: Array) -> void:
	var door := Vector3(X1, UP, _door_z)
	spots.sort_custom(func(a, b): return a[0].distance_to(door) > b[0].distance_to(door))
	var pick: Array = spots[rng.randi() % mini(2, spots.size())]
	fire = FireEscape.new()
	fire.position = pick[0]
	fire.rotation.y = pick[1]
	fire.room = self
	add_child(fire)
	safe_spot = pick[0] + Basis(Vector3.UP, pick[1]) * Vector3(0, 0, 0.9)
	if anomaly_here:
		anomaly = Anomaly.new()
		anomaly.kind = ["chaser", "stalker", "lurker"][rng.randi() % 3]
		anomaly.apt_theme = apt_theme
		anomaly.room = self
		var c := apt_bounds.get_center()
		anomaly.position = Vector3(c.x + rng.randf_range(-1, 1), UP, c.z + rng.randf_range(-1, 1))
		add_child(anomaly)


func _apartment() -> void:
	var w := rng.randf_range(8.0, 12.0)
	var z0 := rng.randf_range(-2.0, 2.0)
	var z1 := rng.randf_range(12.0, 16.0)
	_apt_shell(w, z0, z1, "carpet_blue" if rng.randf() < 0.5 else "wood")
	var mid := X1 + w * 0.5
	# a wall splitting the flat in two, with two doorways
	wall(Vector3(mid, UP, z0), Vector3(mid, UP, z1), APT_H, [[(z1 - z0) * 0.25, DOOR_W], [(z1 - z0) * 0.75, DOOR_W]])
	# living room by the door: couch, tv, table
	Props.couch(self, Vector3(X1 + 1.0, UP, _door_z - 3.0), PI / 2)
	box(Vector3(0.1, 0.7, 1.4), Vector3(mid - 0.15, UP + 1.2, _door_z - 3.0), Mats.get_mat("dark"))
	Props.table(self, Vector3(X1 + 2.3, UP, _door_z + 2.5), 0.0, Vector3(1.2, Props.TABLE_H, 0.8))
	Props.plant(self, Vector3(X1 + 0.5, UP, z1 - 0.6))
	# kitchen and bedroom on the far side
	Props.counter(self, Vector3(X1 + w - 0.35, UP, z0 + (z1 - z0) * 0.3), -PI / 2, 2.5)
	Props.fridge(self, Vector3(X1 + w - 0.45, UP, z0 + 0.6), -PI / 2)
	box(Vector3(1.6, 0.5, 2.1), Vector3(X1 + w - 1.2, UP + 0.25, z1 - 1.5), Mats.get_mat("fabric"))
	box(Vector3(1.6, 0.9, 0.12), Vector3(X1 + w - 1.2, UP + 0.45, z1 - 0.5), Mats.get_mat("wood_dark"))
	Props.shelf(self, Vector3(mid + 0.4, UP, z1 - 0.4), PI)
	_finish_apt([
		[Vector3(X1 + w - 0.15, UP, z1 - 3.4), -PI / 2],
		[Vector3(X1 + w * 0.75, UP, z0 + 0.15), 0.0],
		[Vector3(X1 + w * 0.3, UP, z1 - 0.15), PI],
	])


func _hotel() -> void:
	var w := 5.0
	var z0 := rng.randf_range(-12.0, -6.0)
	var z1 := rng.randf_range(18.0, 26.0)
	_apt_shell(w, z0, z1, "carpet_red")
	# a long hotel corridor: room doors on the far wall with brass numbers
	var z := z0 + 2.0
	var k := 1
	while z < z1 - 1.5:
		box(Vector3(0.08, DOOR_H, 1.0), Vector3(X1 + w - 0.05, UP + DOOR_H / 2, z), Mats.get_mat("door"), false)
		var l := Label3D.new()
		l.text = "%d%02d" % [number + 1, k]
		l.font_size = 30
		l.pixel_size = 0.004
		l.modulate = Color(0.9, 0.75, 0.3)
		l.outline_size = 0
		l.position = Vector3(X1 + w - 0.1, UP + DOOR_H + 0.15, z)
		l.rotation.y = -PI / 2
		add_child(l)
		if k % 3 == 0:
			Props.plant(self, Vector3(X1 + 0.5, UP, z + 1.0))
		z += 3.0
		k += 1
	# the reception desk at the far end
	box(Vector3(3.0, 1.05, 0.7), Vector3(X1 + w / 2, UP + 0.525, z1 - 1.2), Mats.get_mat("wood_dark"))
	_finish_apt([
		[Vector3(X1 + w / 2, UP, z1 - 0.15), PI],
		[Vector3(X1 + w / 2, UP, z0 + 0.15), 0.0],
		[Vector3(X1 + 0.15, UP, z0 + 3.0), PI / 2],
	])


func _hospital() -> void:
	var w := rng.randf_range(10.0, 14.0)
	var z0 := rng.randf_range(-8.0, -4.0)
	var z1 := rng.randf_range(16.0, 22.0)
	_apt_shell(w, z0, z1, "fridge_white")
	var mid := X1 + 3.0
	# a corridor along the stairwell side, wards behind a wall with openings
	wall(Vector3(mid, UP, z0), Vector3(mid, UP, z1), APT_H, [[2.0, 1.6], [(z1 - z0) * 0.5, 1.6], [(z1 - z0) - 2.5, 1.6]])
	var z := z0 + 1.6
	while z < z1 - 1.5:
		# a bed with a curtain
		box(Vector3(1.0, 0.55, 2.0), Vector3(X1 + w - 0.8, UP + 0.3, z), Mats.get_mat("fridge_white"))
		box(Vector3(0.9, 0.12, 0.5), Vector3(X1 + w - 0.8, UP + 0.65, z - 0.7), Mats.get_mat("fabric"))
		box(Vector3(0.04, 2.2, 2.2), Vector3(X1 + w - 1.6, UP + 1.25, z), Mats.get_mat("poster_green"), false)
		z += 3.0
	# the surgery table under a big lamp
	var sc := Vector3(mid + (w - 3.0) * 0.45, UP, z0 + (z1 - z0) * 0.5)
	box(Vector3(0.9, 0.9, 2.0), sc + Vector3(0, 0.45, 0), Mats.get_mat("metal"))
	box(Vector3(0.6, 0.1, 0.6), sc + Vector3(0, APT_H - 0.4, 0), Mats.get_mat("white_glow"), false)
	var sl := SpotLight3D.new()
	sl.position = sc + Vector3(0, APT_H - 0.5, 0)
	sl.rotation.x = -PI / 2
	sl.light_energy = 3.0
	sl.spot_range = 4.0
	add_child(sl)
	_finish_apt([
		[Vector3(X1 + w - 0.15, UP, z0 + 1.0), -PI / 2],
		[Vector3(mid + 1.5, UP, z1 - 0.15), PI],
		[Vector3(X1 + 1.5, UP, z1 - 0.15), PI],
	])


## floor 150: home. your flat, your bed. lie down
func _home() -> void:
	var w := 9.0
	_apt_shell(w, 2.0, 14.0, "wood")
	Props.couch(self, Vector3(X1 + 1.0, UP, 5.0), PI / 2)
	Props.table(self, Vector3(X1 + 2.4, UP, 5.0), 0.0, Vector3(1.0, Props.TABLE_H, 0.7))
	Props.plant(self, Vector3(X1 + 0.5, UP, 2.6))
	Props.fridge(self, Vector3(X1 + w - 0.45, UP, 2.6), -PI / 2)
	Props.shelf(self, Vector3(X1 + w - 0.3, UP, 6.5), -PI / 2)
	# the bed, warm light over it
	var bed := Vector3(X1 + w - 1.3, UP, 12.2)
	box(Vector3(1.8, 0.5, 2.2), bed + Vector3(0, 0.25, 0), Mats.get_mat("fabric"))
	box(Vector3(1.8, 1.0, 0.12), bed + Vector3(0, 0.5, 1.1), Mats.get_mat("wood_dark"))
	box(Vector3(0.7, 0.14, 0.4), bed + Vector3(0, 0.57, 0.75), Mats.get_mat("fridge_white"))
	var lamp := OmniLight3D.new()
	lamp.light_color = Color(1.0, 0.8, 0.55)
	lamp.light_energy = 1.6
	lamp.omni_range = 5.0
	lamp.position = bed + Vector3(-1.2, 1.4, 0.6)
	add_child(lamp)
	var it := Interactable.make(Vector3(1.8, 0.8, 2.2), "go to sleep")
	it.position = bed + Vector3(0, 0.5, 0)
	it.used.connect(func(_p): Game.run_finished.emit("complex"))
	add_child(it)
	var note := Label3D.new()
	note.text = "home."
	note.font_size = 64
	note.pixel_size = 0.004
	note.modulate = Color(0.95, 0.85, 0.6)
	note.outline_size = 0
	note.position = Vector3(X1 + w - 0.05, UP + 1.8, 9.0)
	note.rotation.y = -PI / 2
	add_child(note)
