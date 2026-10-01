extends Node
## renders a few screenshots for checking how things look:
## xvfb-run godot --path . --rendering-method gl_compatibility res://tests/screenshots.tscn -- <out dir>

var out := "user://shots"


func _ready() -> void:
	var args := OS.get_cmdline_user_args()
	if args.size() > 0:
		out = args[0]
	DirAccess.make_dir_recursive_absolute(out)
	get_tree().current_scene = null
	get_window().size = Vector2i(1280, 720)
	Game.start_run(true)
	await _frames(20)
	Game.admin_flags.god = true
	var p: Player = Game.player
	await _shot("01_lobby")
	p.rotate_y(PI * 0.7)
	await _shot("02_lobby_shop")
	p.rotate_y(-PI * 0.7)
	for i in 6:
		await _open_next()
	await _shot("03_room_a006")
	for n in [12, 40, 75]:
		Game.generator.jump_to(n)
		await _frames(10)
		await _shot("04_room_a%03d" % n)
	Game.generator.jump_to(99)
	await _frames(5)
	await _open_next()
	p.global_position += p.global_basis * Vector3(0, 0, -6)
	await _shot("05_corridor_100")
	p.global_position += p.global_basis * Vector3(0, 0, -30)
	await _shot("06_corridor_100_end")
	Game.generator.jump_to(140)
	Game.player.lights.toggle_flashlight()
	await _frames(10)
	await _shot("07_room_a140_dark")
	Game.player.lights.toggle_flashlight()
	Game.generator.jump_to(149)
	await _frames(5)
	await _open_next()
	await _shot("08_shop_150")
	# find an exit room
	for n in range(201, 400):
		Game.generator.jump_to(n)
		await _frames(2)
		if Game.generator.room(n).room_type == "exit_room":
			p.global_position += p.global_basis * Vector3(0, 0, -5)
			p.rotate_y(PI / 2.5)
			await _shot("09_exit_room")
			break
	Game.generator.jump_to(999)
	await _frames(5)
	await _open_next()
	p.global_position += p.global_basis * Vector3(0, 0, -6)
	await _shot("10_a1000_bridge")
	Game.generator.jump_to(30)
	await _frames(5)
	Game.entities.spawn("a90")
	await _secs(1.6)
	await _shot("11_a90")
	Game.entities.clear_all()
	Game.entities.spawn("a90b")
	await _secs(2.6)
	await _shot("12_a90b")
	Game.entities.clear_all()
	Game.player.lights.toggle_flashlight()
	Game.entities.spawn("a200")
	await _secs(6.5)
	await _shot("13_a200")
	Game.entities.clear_all()
	Game.entities.spawn("a60")
	await _secs(5.3)
	await _shot("14_a60")
	Game.entities.clear_all()
	get_tree().quit()


func _open_next() -> void:
	var r = Game.generator.room(Game.door)
	if r and r.exit_door and not r.exit_door.is_open:
		r.exit_door.open()
		Game.player.teleport(r.global_exit() * Transform3D(Basis(Vector3.UP, PI), Vector3(0, 0.1, 1.0)))
	await _frames(4)


func _secs(t: float) -> void:
	await get_tree().create_timer(t).timeout


func _frames(n: int) -> void:
	for i in n:
		await get_tree().process_frame


func _shot(name: String) -> void:
	await _frames(3)
	await RenderingServer.frame_post_draw
	get_viewport().get_texture().get_image().save_png(out.path_join(name + ".png"))
	print("shot ", name)
