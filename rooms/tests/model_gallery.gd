extends Node
## renders the furniture models one by one for comparing looks:
## xvfb-run godot --path . --rendering-method gl_compatibility res://tests/model_gallery.tscn -- <out dir> [prefix]

var out := "user://gallery"
var prefix := ""


func _ready() -> void:
	get_tree().current_scene = null
	var args := OS.get_cmdline_user_args()
	if args.size() > 0:
		out = args[0]
	if args.size() > 1:
		prefix = args[1]
	DirAccess.make_dir_recursive_absolute(out)
	get_window().size = Vector2i(640, 640)
	Game.start_run(true, false, "offices", {"sightings": "office"})
	for i in 300:
		await get_tree().process_frame
		if Game.entities and Game.player and i > 20:
			break
	Game.entities.natural_spawns = false
	Game.player.visible = false
	Game.player.set_physics_process(false)
	Game.player.set_process(false)
	if Game.player.hud:
		Game.player.hud.visible = false
	# a bare stage far away from the rooms
	var stage := RoomBase.new()
	stage.theme = "office"
	stage.position = Vector3(500, 0, 500)
	add_child(stage)
	stage._body = StaticBody3D.new()
	stage.add_child(stage._body)
	stage.rng.seed = 5
	var cam := Camera3D.new()
	cam.fov = 45
	add_child(cam)
	cam.current = true
	var sun := DirectionalLight3D.new()
	sun.rotation = Vector3(-0.9, 0.6, 0)
	sun.light_energy = 1.2
	stage.add_child(sun)
	var fill := OmniLight3D.new()
	fill.position = Vector3(1.5, 2.5, 3)
	fill.omni_range = 10
	stage.add_child(fill)
	stage.box(Vector3(8, 0.2, 8), Vector3(0, -0.1, 0), Mats.get_mat("carpet_blue"), false)
	stage.box(Vector3(8, 4, 0.2), Vector3(0, 2, -1.2), Mats.get_mat("wall"), false)
	var items := {
		"locker": func(s): s.add_locker(Vector3.ZERO, 0.0),
		"locker_broken": func(s):
			var lk = s.add_locker(Vector3.ZERO, 0.0)
			lk.ready.connect(func(): lk.break_open(true), CONNECT_ONE_SHOT),
		"desk": func(s): s.add_drawer_desk(Vector3.ZERO, 0.0),
		"table": func(s): Props.table(s, Vector3.ZERO, 0.0),
		"chair": func(s): Props.chair(s, Vector3.ZERO, 0.4),
		"chair_fallen": func(s): Props.chair(s, Vector3.ZERO, 0.4, true),
		"plant": func(s): Props.plant(s, Vector3.ZERO),
		"couch": func(s): Props.couch(s, Vector3.ZERO, 0.0),
		"shelf": func(s): Props.shelf(s, Vector3(0, 0, -0.6), 0.0),
		"fridge": func(s): Props.fridge(s, Vector3(0, 0, -0.6), 0.0),
		"dresser": func(s): s.add_cabinet(Vector3(0, 0, -0.8), 0.0),
		"water": func(s): Props.water_dispenser(s, Vector3(0, 0, -0.6)),
		"door": func(s):
			var d := Door.new()
			d.number = 42
			d.position = Vector3(0, 0, -1.1)
			s.add_child(d),
		"filing": func(s): Props.filing_cabinet(s, Vector3(0, 0, -0.8), 0.0),
		"desk2": func(s):
			var d = s.add_drawer_desk(Vector3.ZERO, 0.0)
			d.loot_seed = 0,
		"light": func(s): s.ceiling_light(Vector3(0, 2.2, 0)),
		"atm": func(s):
			var a := Atm.new()
			a.position = Vector3(0, 0, -0.6)
			s.add_child(a),
		"arcade": func(s): s.add_arcade(Vector3(0, 0, -0.6), 0.0),
	}
	var screens := ["desktop", "sheet", "email", "error", "saver", "nosignal", "off"]
	for key in items:
		for c in stage.get_children():
			if c != stage._body and not c is DirectionalLight3D and not c is OmniLight3D and c.get_index() > 3:
				c.queue_free()
		await get_tree().process_frame
		items[key].call(stage)
		await _frames(4)
		var look := Vector3(500, 0.9, 500)
		cam.global_position = look + Vector3(1.8, 1.1, 2.6)
		if key.begins_with("desk"):
			cam.global_position = look + Vector3(0.9, 0.9, 1.6)
			look.y = 1.1
		if key == "light":
			cam.global_position = look + Vector3(1.2, -0.4, 2.0)
			look.y = 2.2
		cam.look_at(look)
		await _frames(4)
		await RenderingServer.frame_post_draw
		get_viewport().get_texture().get_image().save_png(out.path_join(prefix + key + ".png"))
	# the screen designs side by side
	for c in stage.get_children():
		if c != stage._body and not c is DirectionalLight3D and not c is OmniLight3D and c.get_index() > 3:
			c.queue_free()
	await get_tree().process_frame
	for i in screens.size():
		Screens.quad(stage, Vector2(0.52, 0.3), Vector3(-1.6 + (i % 4) * 0.62, 1.4 - (i / 4) * 0.4, 0), screens[i])
	cam.global_position = Vector3(500 - 0.67, 1.2, 500 + 1.9)
	cam.look_at(Vector3(500 - 0.67, 1.2, 500))
	await _frames(4)
	await RenderingServer.frame_post_draw
	get_viewport().get_texture().get_image().save_png(out.path_join(prefix + "screens.png"))
	# the lobby skylight from below
	Game.generator.jump_to(0)
	await _frames(10)
	var lob = Game.generator.room(0)
	cam.global_position = lob.global_transform * Vector3(2.5, 1.6, 2.5)
	cam.look_at(lob.global_transform * Vector3(0, 3.6, 5.5))
	cam.fov = 70
	await _frames(6)
	await RenderingServer.frame_post_draw
	get_viewport().get_texture().get_image().save_png(out.path_join(prefix + "skylight.png"))
	get_tree().quit()


func _frames(k: int) -> void:
	for i in k:
		await get_tree().process_frame
