class_name Cutscene
extends Node3D
## the cutscenes. each one builds a small stage out of the game's own rooms and props, moves
## a camera around it, shows a few lines of text and then hands over (Game.cutscene_then).
##   intro    - your first day: you walk into the miles tower, the lobby door locks behind you
##   elevator - the ride up out of the wires, past every floor from w to a, coworkers at their desks
##   outside  - the a-1000 door opens onto the street in front of the miles tower
##   bus      - you get on the bus home, it drives off, credits
## once you've seen one, tap / any key skips it.

const R := "res://scripts/world/rooms/"

var id := "intro"
var _cam: Camera3D
var _env: Environment
var _fade: ColorRect
var _text: Label
var _skip_hint: Label
var _stage: Node3D
var _done := false
var _can_skip := false


func _ready() -> void:
	id = Game.cutscene_id
	_can_skip = Array(Save.data.get("seen_cutscenes", [])).has(id)
	_cam = Camera3D.new()
	_cam.fov = 70.0
	add_child(_cam)
	_cam.make_current()
	_env = Environment.new()
	_env.background_mode = Environment.BG_COLOR
	_env.background_color = Color.BLACK
	_env.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	_env.ambient_light_color = Color(0.9, 0.9, 0.95)
	_env.ambient_light_energy = 0.35
	_env.tonemap_mode = Environment.TONE_MAPPER_ACES
	_env.glow_enabled = int(Settings.data.quality) >= 1
	var we := WorldEnvironment.new()
	we.environment = _env
	add_child(we)
	_ui()
	Input.mouse_mode = Input.MOUSE_MODE_HIDDEN
	match id:
		"intro": _intro()
		"elevator": _elevator()
		"outside": _outside()
		"bus": _bus()
		_: _finish()


func _ui() -> void:
	var cl := CanvasLayer.new()
	cl.layer = 50
	add_child(cl)
	for top in [true, false]:
		var bar := ColorRect.new()
		bar.color = Color.BLACK
		bar.set_anchors_and_offsets_preset(Control.PRESET_TOP_WIDE if top else Control.PRESET_BOTTOM_WIDE)
		if top:
			bar.offset_bottom = 60
		else:
			bar.offset_top = -60
		bar.mouse_filter = Control.MOUSE_FILTER_IGNORE
		cl.add_child(bar)
	_text = UIKit.label("", 22, Color(0.95, 0.92, 0.85))
	_text.set_anchors_and_offsets_preset(Control.PRESET_BOTTOM_WIDE)
	_text.offset_top = -120
	_text.offset_bottom = -70
	_text.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	_text.add_theme_color_override("font_outline_color", Color.BLACK)
	_text.add_theme_constant_override("outline_size", 4)
	cl.add_child(_text)
	_skip_hint = UIKit.label("tap to skip" if Settings.use_touch() else "press any key to skip", 14, Color(1, 1, 1, 0.5))
	_skip_hint.set_anchors_and_offsets_preset(Control.PRESET_BOTTOM_RIGHT)
	_skip_hint.offset_left = -260
	_skip_hint.offset_top = -44
	_skip_hint.offset_right = -16
	_skip_hint.offset_bottom = -16
	_skip_hint.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
	_skip_hint.visible = _can_skip
	cl.add_child(_skip_hint)
	_fade = ColorRect.new()
	_fade.color = Color.BLACK
	_fade.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	_fade.mouse_filter = Control.MOUSE_FILTER_IGNORE
	cl.add_child(_fade)


func _input(event: InputEvent) -> void:
	if not _can_skip or _done:
		return
	if (event is InputEventKey or event is InputEventMouseButton or event is InputEventScreenTouch or event is InputEventJoypadButton) and event.is_pressed():
		get_viewport().set_input_as_handled()
		_finish()


# ---------- helpers ----------

func wait(t: float) -> bool:
	await get_tree().create_timer(t).timeout
	return not _done


func fade(to_black: bool, t := 0.8, col := Color.BLACK) -> void:
	_fade.color = Color(col, _fade.color.a)
	var tw := create_tween()
	tw.tween_property(_fade, "color:a", 1.0 if to_black else 0.0, t)


func say(line: String, t := 3.0) -> void:
	_text.text = line
	_text.modulate.a = 0.0
	var tw := create_tween()
	tw.tween_property(_text, "modulate:a", 1.0, 0.4)
	tw.tween_interval(maxf(0.2, t - 0.8))
	tw.tween_property(_text, "modulate:a", 0.0, 0.4)


func shot(pos: Vector3, look: Vector3) -> void:
	_cam.global_position = pos
	_cam.look_at(look, Vector3.UP)


## move the camera to pos while looking at look, over t seconds
func dolly(pos: Vector3, look: Vector3, t: float) -> void:
	var start_look := _cam.global_position - _cam.global_basis.z
	var from := _cam.global_position
	var tw := create_tween()
	tw.tween_method(func(k: float):
		_cam.global_position = from.lerp(pos, k)
		_cam.look_at(start_look.lerp(look, k), Vector3.UP), 0.0, 1.0, t).set_trans(Tween.TRANS_SINE).set_ease(Tween.EASE_IN_OUT)


func new_stage() -> Node3D:
	if _stage:
		_stage.queue_free()
	_stage = Node3D.new()
	add_child(_stage)
	return _stage


## one of the game's rooms, built just like in a run
func room(script_name: String, n: int, seed_value := 7) -> RoomBase:
	var r: RoomBase = load(R + script_name).new()
	r.setup(n, seed_value)
	_stage.add_child(r)
	return r


## you: a blocky worker, walking
func avatar(pos: Vector3) -> Worker:
	var w := Worker.new()
	w.seated = false
	w.set_physics_process(false)
	w.position = pos
	_stage.add_child(w)
	return w


func walk(w: Worker, to: Vector3, t: float) -> void:
	var d := to - w.position
	w.rotation.y = atan2(-d.x, -d.z)
	var tw := create_tween()
	tw.tween_property(w, "position", to, t)
	var bob := create_tween().set_loops(int(t / 0.3))
	bob.tween_property(w, "position:y", to.y + 0.05, 0.15)
	bob.tween_property(w, "position:y", to.y, 0.15)


func sound(key: String, pos: Vector3, vol := 0.0, pitch := 1.0) -> AudioStreamPlayer3D:
	var a := AudioStreamPlayer3D.new()
	a.stream = Assets.sound(key)
	a.volume_db = vol
	a.pitch_scale = pitch
	a.unit_size = 10.0
	a.position = pos
	_stage.add_child(a)
	a.play()
	return a


func light(pos: Vector3, col: Color, energy: float, rng: float) -> OmniLight3D:
	var l := OmniLight3D.new()
	l.position = pos
	l.light_color = col
	l.light_energy = energy
	l.omni_range = rng
	_stage.add_child(l)
	return l


func sky(top: Color, horizon: Color, sun_col: Color, sun_angle: float) -> void:
	var sm := ProceduralSkyMaterial.new()
	sm.sky_top_color = top
	sm.sky_horizon_color = horizon
	sm.ground_horizon_color = horizon.darkened(0.5)
	sm.ground_bottom_color = Color(0.08, 0.08, 0.1)
	var s := Sky.new()
	s.sky_material = sm
	_env.background_mode = Environment.BG_SKY
	_env.sky = s
	_env.ambient_light_source = Environment.AMBIENT_SOURCE_SKY
	_env.ambient_light_energy = 0.8
	_env.fog_enabled = true
	_env.fog_density = 0.006
	_env.fog_light_color = horizon.lerp(Color(0.6, 0.6, 0.7), 0.5)
	var sun := DirectionalLight3D.new()
	sun.light_color = sun_col
	sun.rotation = Vector3(deg_to_rad(sun_angle), deg_to_rad(150), 0)
	sun.shadow_enabled = int(Settings.data.quality) > 0
	_stage.add_child(sun)


func indoor() -> void:
	_env.background_mode = Environment.BG_COLOR
	_env.sky = null
	_env.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	_env.ambient_light_energy = 0.35
	_env.fog_enabled = false


func _finish() -> void:
	if _done:
		return
	_done = true
	var seen: Array = Array(Save.data.get("seen_cutscenes", []))
	if not seen.has(id):
		seen.append(id)
		Save.data.seen_cutscenes = seen
		Save.write()
	var then: Callable = Game.cutscene_then
	Game.cutscene_then = Callable()
	var tw := create_tween()
	tw.tween_property(_fade, "color:a", 1.0, 0.4)
	tw.tween_callback(func():
		if then.is_valid():
			then.call()
		else:
			Game.to_menu())


# ---------- the cutscenes ----------

func _intro() -> void:
	# outside, morning. you walk up to the miles tower
	new_stage()
	sky(Color(0.35, 0.55, 0.85), Color(0.95, 0.8, 0.6), Color(1.0, 0.9, 0.75), -25.0)
	var city := room("room_city.gd", 0)
	var me := avatar(Vector3(0.4, 0, 16.0))
	shot(Vector3(2.5, 1.6, 24.0), Vector3(0, 24.0, -2.0))
	fade(false, 1.2)
	say("monday, 8:59 am.", 3.0)
	dolly(Vector3(1.2, 1.7, 21.0), Vector3(0, 9.0, 0), 3.0)
	if not await wait(2.6): return
	say("first day at miles.", 3.2)
	walk(me, Vector3(0.0, 0, 0.6), 5.0)
	dolly(Vector3(0.9, 1.8, 8.0), Vector3(0, 1.4, 0), 4.6)
	if not await wait(4.6): return
	fade(true, 0.6)
	if not await wait(0.7): return
	# inside: the lobby. the door shuts behind you, and locks
	new_stage()
	indoor()
	city = null
	room("room_lobby.gd", 0)
	me = avatar(Vector3(0, 0, 0.5))
	shot(Vector3(1.6, 1.9, 7.5), Vector3(0, 1.2, 0.5))
	fade(false, 0.6)
	walk(me, Vector3(0.2, 0, 3.6), 2.6)
	if not await wait(2.4): return
	sound("door_locked", Vector3(0, 1.2, 0), 6.0, 0.7)
	say("the door locks behind you.", 2.6)
	if not await wait(2.6): return
	me.rotation.y = 0.0
	dolly(Vector3(0.6, 1.7, 5.2), Vector3(0, 1.4, 0), 2.0)
	say("nobody's at the front desk.", 2.8)
	if not await wait(2.6): return
	say("you'll have to find your own way up.", 3.0)
	if not await wait(3.0): return
	_finish()


func _elevator() -> void:
	new_stage()
	indoor()
	_env.ambient_light_energy = 0.05
	# the office waiting above: a room full of coworkers (built as the powered office)
	var fl := Game.floor
	var fixed := Game.fixed_office
	Game.floor = "offices"
	Game.fixed_office = true
	var office := room("room_cubicles.gd", 12, 3)
	office.position = Vector3(0, 0, 1.3)
	Game.floor = fl
	Game.fixed_office = fixed
	office.visible = false
	# the cage
	var metal := Mats.get_mat("door_metal")
	var cage := Node3D.new()
	_stage.add_child(cage)
	for b in [[Vector3(2.4, 0.1, 2.6), Vector3(0, -0.05, 0)], [Vector3(2.4, 0.1, 2.6), Vector3(0, 2.75, 0)],
			[Vector3(0.1, 2.8, 2.6), Vector3(-1.2, 1.4, 0)], [Vector3(0.1, 2.8, 2.6), Vector3(1.2, 1.4, 0)],
			[Vector3(2.4, 2.8, 0.1), Vector3(0, 1.4, -1.3)], [Vector3(2.4, 0.6, 0.1), Vector3(0, 2.5, 1.3)]]:
		_cube(cage, b[0], b[1], metal)
	var doors: Array[MeshInstance3D] = []
	for s in [-1.0, 1.0]:
		doors.append(_cube(cage, Vector3(1.2, 2.2, 0.06), Vector3(s * 0.6, 1.1, 1.28), Mats.get_mat("metal")))
	var counter := Label3D.new()
	counter.text = "W"
	counter.font_size = 64
	counter.pixel_size = 0.004
	counter.modulate = Color(1.0, 0.4, 0.2)
	counter.position = Vector3(0, 2.45, 1.22)
	counter.rotation.y = PI
	cage.add_child(counter)
	var lamp := light(Vector3(0, 2.5, 0), Color(1.0, 0.85, 0.6), 0.3, 5.0)
	shot(Vector3(0.3, 1.6, -1.0), Vector3(0, 1.5, 1.3))
	var hum := sound("elevator", Vector3(0, 1, 0), -4.0)
	fade(false, 1.0)
	say("the elevator shudders, then starts to climb.", 3.2)
	# from the wires all the way up to the office, floor by floor: w, v, u ... a.
	# slow at first, then picking up speed, then slowing down for a
	var floors := "VUTSRQPONMLKJIHGFEDCBA"
	for i in floors.length():
		var k := float(i) / (floors.length() - 1)
		var step := lerpf(0.55, 0.16, sin(k * PI)) if i < floors.length() - 1 else 0.7
		if not await wait(step): return
		counter.text = floors[i]
		sound("flashlight_click", Vector3(0, 2.4, 1.2), -10.0, 0.6)
		lamp.light_energy = 0.3 + k * 1.4
		_env.ambient_light_energy = 0.05 + k * 0.28
		if i == 9:
			say("the lights come on, one floor at a time.", 3.0)
	if not await wait(0.4): return
	sound("power_on", Vector3(0, 2, 1), 0.0)
	if hum:
		hum.stop()
	office.visible = true
	_env.ambient_light_energy = 0.35
	for k in 2:
		var d := doors[k]
		create_tween().tween_property(d, "position:x", (-1.0 if k == 0 else 1.0) * 1.7, 1.2).set_trans(Tween.TRANS_SINE)
	sound("typing", Vector3(0, 1, 4), 0.0)
	say("keyboards. people talking. someone laughs.", 3.2)
	dolly(Vector3(0, 1.65, 1.6), Vector3(0, 1.3, 6.0), 3.2)
	if not await wait(3.4): return
	say("they work here now.", 2.6)
	if not await wait(2.6): return
	_finish()


func _outside() -> void:
	# the void bridge: the glowing door at the end swings open
	new_stage()
	indoor()
	_env.ambient_light_energy = 0.02
	var door_pos := Vector3(0, 0, 6)
	_cube(_stage, Vector3(1.6, 0.15, 7.0), Vector3(0, -0.075, 2.5), Mats.get_mat("wood_old"))
	_cube(_stage, Vector3(1.6, 2.5, 0.12), door_pos + Vector3(0, 1.25, 0.05), Mats.get_mat("white_glow"))
	var hinge := Node3D.new()
	hinge.position = door_pos + Vector3(-0.65, 0, -0.02)
	_stage.add_child(hinge)
	_cube(hinge, Vector3(1.3, 2.3, 0.07), Vector3(0.65, 1.15, 0), Mats.get_mat("void_door"))
	light(door_pos + Vector3(0, 1.4, -0.6), Color(1, 0.95, 0.85), 1.2, 6.0)
	shot(Vector3(0.4, 1.7, 0.5), door_pos + Vector3(0, 1.2, 0))
	fade(false, 1.0)
	say("a-1000.", 2.4)
	dolly(Vector3(0.1, 1.7, 4.4), door_pos + Vector3(0, 1.2, 0), 3.0)
	if not await wait(3.0): return
	sound("door_open", door_pos, 2.0, 0.8)
	create_tween().tween_property(hinge, "rotation:y", -1.6, 1.2).set_trans(Tween.TRANS_SINE)
	if not await wait(0.6): return
	fade(true, 1.0, Color(1, 0.97, 0.9))
	if not await wait(1.3): return
	# the street. you step out of the glass doors of the miles tower
	new_stage()
	sky(Color(0.12, 0.16, 0.34), Color(0.95, 0.55, 0.3), Color(1.0, 0.72, 0.5), -18.0)
	room("room_city.gd", 0)
	var me := avatar(Vector3(0, 0, 0.6))
	shot(Vector3(0.0, 1.7, 1.2), Vector3(0, 1.6, 30.0))
	fade(false, 1.6, Color(1, 0.97, 0.9))
	say("outside.", 2.6)
	walk(me, Vector3(0, 0, 9.0), 5.0)
	if not await wait(2.6): return
	say("the sun is going down.", 3.0)
	# turn around to see where you've been
	dolly(Vector3(3.5, 2.0, 17.0), Vector3(0, 14.0, 0), 4.0)
	if not await wait(3.2): return
	say("miles. the company you work for.", 3.0)
	if not await wait(3.0): return
	_finish()


func _bus() -> void:
	new_stage()
	sky(Color(0.06, 0.08, 0.2), Color(0.75, 0.38, 0.25), Color(1.0, 0.65, 0.45), -8.0)
	room("room_city.gd", 5, 11)
	# the night bus, at the stop
	var bus := Node3D.new()
	bus.position = Vector3(5.2, 0, 14.0)
	_stage.add_child(bus)
	_cube(bus, Vector3(2.6, 3.0, 11.0), Vector3(0, 1.8, 0), Mats.get_mat("car_white"))
	_cube(bus, Vector3(2.64, 0.9, 9.0), Vector3(0, 2.4, -0.5), Mats.get_mat("glass_dark"))
	_cube(bus, Vector3(2.66, 0.4, 11.0), Vector3(0, 0.6, 0), Mats.get_mat("car_blue"))
	var bus_door := _cube(bus, Vector3(0.06, 2.2, 1.2), Vector3(1.34, 1.4, 3.5), Mats.get_mat("glass_dark"))
	bus_door.position.z = 4.7
	for wz in [-3.5, 3.5]:
		for wx in [-1.3, 1.3]:
			_cube(bus, Vector3(0.3, 0.9, 0.9), Vector3(wx, 0.45, wz), Mats.get_mat("dark"))
	var inside := OmniLight3D.new()
	inside.position = Vector3(0, 2.2, 0)
	inside.light_color = Color(1.0, 0.95, 0.8)
	inside.light_energy = 1.5
	inside.omni_range = 7.0
	bus.add_child(inside)
	for s in [-0.8, 0.8]:
		var hl := SpotLight3D.new()
		hl.position = Vector3(s, 1.0, 5.6)
		hl.light_energy = 3.0
		hl.spot_range = 25.0
		hl.rotation.y = PI
		bus.add_child(hl)
	var me := avatar(Vector3(9.5, 0, 12.0))
	shot(Vector3(-2.5, 1.7, 24.0), Vector3(6.0, 1.4, 15.0))
	fade(false, 1.0)
	say("the night bus is waiting.", 3.0)
	walk(me, Vector3(7.4, 0, 18.7), 2.6)
	if not await wait(2.6): return
	walk(me, Vector3(5.6, 0, 18.7), 1.0)
	if not await wait(1.0): return
	me.visible = false
	sound("door_open", bus.position + Vector3(1.4, 1, 4.7), 0.0, 0.7)
	create_tween().tween_property(bus_door, "position:z", 3.5, 0.6)
	say("you sit down at the back.", 2.8)
	if not await wait(1.4): return
	var hum := sound("elevator", bus.position, 2.0, 0.6)
	if hum:
		hum.reparent(bus)
	var tw := create_tween()
	tw.tween_property(bus, "position:z", 70.0, 7.0).set_trans(Tween.TRANS_QUAD).set_ease(Tween.EASE_IN)
	dolly(Vector3(-1.5, 3.0, 12.0), Vector3(5.2, 1.5, 40.0), 6.0)
	if not await wait(2.4): return
	say("the city goes by. the office doesn't follow you.", 3.4)
	if not await wait(3.4): return
	fade(true, 1.2)
	if not await wait(1.4): return
	# credits
	var lines := [
		"going home",
		"The ROOMS",
		"a fan game by jhu",
		"based on rooms by nicorocks5555\nand the rooms in doors by lsplash",
		"entity art and sounds by the doors team\nand the rooms: revisited team",
		"thanks for playing.",
	]
	for l in lines:
		say(l, 2.6)
		if not await wait(2.7): return
	_finish()


func _cube(parent: Node3D, size: Vector3, pos: Vector3, mat: Material) -> MeshInstance3D:
	var mi := MeshInstance3D.new()
	var bm := BoxMesh.new()
	bm.size = size
	mi.mesh = bm
	mi.material_override = mat
	mi.position = pos
	parent.add_child(mi)
	return mi
