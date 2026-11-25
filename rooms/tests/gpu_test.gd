extends Node
## rough render cost check (run with a window, any renderer):
## xvfb-run godot --path . --rendering-method mobile res://tests/gpu_test.tscn
## stands in a few rooms on low quality / 0.6 scale like a phone and prints the frame time
## with things switched off one at a time, so you can see what the gpu spends time on.

func _ready() -> void:
	get_tree().current_scene = null
	get_window().size = Vector2i(1110, 540)
	await _frames(2)
	Settings.data.quality = 0
	Settings.data.render_scale = 0.6
	Settings.apply()
	Game.start_run(true, false, "offices", {"sightings": "office"})
	await _frames(30)
	Engine.max_fps = 0
	DisplayServer.window_set_vsync_mode(DisplayServer.VSYNC_DISABLED)
	Game.admin_flags.god = true
	Game.entities.natural_spawns = false
	var vp := get_viewport().get_viewport_rid()
	RenderingServer.viewport_set_measure_render_time(vp, true)
	var world = Game.generator.get_parent()
	var env: Environment = world.env
	for n in [14, 45, 120]:
		Game.generator.jump_to(n)
		await _frames(20)
		var lights := get_tree().root.find_children("*", "Light3D", true, false)
		print("room %d (%s), %d lights" % [n, Game.generator.room(n).room_type, lights.size()])
		print("  baseline      %.2f ms" % await _cost())
		for l in lights:
			l.visible = false
		print("  no lights     %.2f ms" % await _cost())
		for l in lights:
			l.visible = true
		env.fog_enabled = false
		print("  no fog        %.2f ms" % await _cost())
		env.fog_enabled = true
		env.tonemap_mode = Environment.TONE_MAPPER_LINEAR
		env.adjustment_enabled = false
		print("  no tonemap/adj %.2f ms" % await _cost())
		env.tonemap_mode = Environment.TONE_MAPPER_ACES
		env.adjustment_enabled = true
		get_viewport().scaling_3d_scale = 0.4
		print("  scale 0.4     %.2f ms" % await _cost())
		get_viewport().scaling_3d_scale = 0.6
		Game.player.hud.visible = false
		print("  no hud        %.2f ms" % await _cost())
		Game.player.hud.visible = true
	get_tree().quit()


func _cost() -> float:
	await _frames(5)
	var t := Time.get_ticks_usec()
	for i in 20:
		await RenderingServer.frame_post_draw
	return (Time.get_ticks_usec() - t) / 1000.0 / 20.0


func _frames(k: int) -> void:
	for i in k:
		await get_tree().process_frame
