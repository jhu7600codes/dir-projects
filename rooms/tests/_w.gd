extends Node
func _ready() -> void:
	get_tree().current_scene = null
	get_window().size = Vector2i(1280, 720)
	Game.start_run(true, false, "wires")
	for i in 30:
		await get_tree().process_frame
	Game.admin_flags.god = true
	Game.entities.natural_spawns = false
	var out: String = OS.get_cmdline_user_args()[0]
	var p = Game.player
	for n in [0, 3, 7, 10, 50]:
		if n > 0:
			Game.generator.jump_to(n)
		for i in 10:
			await get_tree().process_frame
		var r = Game.generator.room(n)
		var pts: Array = r.path_global()
		p.teleport(r.global_transform * Transform3D(Basis(Vector3.UP, PI), Vector3(0, 0.1, 1.0)))
		p.head.rotation.x = 0.05
		for i in 20:
			await get_tree().process_frame
		get_viewport().get_texture().get_image().save_png(out + "/w%02d.png" % n)
		print("room ", n, " ", r.room_type, " door=", r.exit_door.number if r.exit_door else -1, " locked=", r.exit_door.locked if r.exit_door else false)
	get_tree().quit()
