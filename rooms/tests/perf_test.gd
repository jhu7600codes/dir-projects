extends Node
## cpu cost check: godot --headless --path . res://tests/perf_test.tscn [-- office|plain]
## headless frames run as fast as they can, so wall time per frame = cpu cost of a frame.
## measures standing in a few rooms, then with each entity active.

func _ready() -> void:
	get_tree().current_scene = null
	Engine.max_fps = 0
	await _frames(2)
	var mode := OS.get_cmdline_user_args()[0] if OS.get_cmdline_user_args().size() > 0 else "office"
	Game.start_run(true, false, "offices", {"sightings": mode} if mode != "plain" else {})
	await _frames(20)
	Game.admin_flags.god = true
	Game.entities.natural_spawns = false
	Engine.max_fps = 0
	OS.low_processor_usage_mode = false
	DisplayServer.window_set_vsync_mode(DisplayServer.VSYNC_DISABLED)
	print("idle lobby: %.3f ms" % await _cost(200))
	for n in [12, 40, 75, 130]:
		Game.generator.jump_to(n)
		await _frames(10)
		print("room A-%03d (%s): %.3f ms" % [n, Game.generator.room(n).room_type, await _cost(200)])
	Game.generator.jump_to(60)
	await _frames(10)
	for j in 4:
		if j < 2:
			Game.generator.jump_to(60 + j * 7)
		elif j == 2:
			print("  (no jump, just a hud notify)")
			Game.player.hud.notify("test text here")
		else:
			print("  (no jump, a light pulse)")
			Game.generator.room(Game.door).light_pulse(Color.RED, 2.0)
		for i in 6:
			var pr := 0.0
			var ph := 0.0
			for f in 50:
				await get_tree().process_frame
				pr += Performance.get_monitor(Performance.TIME_PROCESS)
				ph += Performance.get_monitor(Performance.TIME_PHYSICS_PROCESS)
			print("  jump %d chunk %d: process %.3f  physics %.3f ms  nav %d  objs %d" % [j, i, pr * 20.0, ph * 20.0, Performance.get_monitor(Performance.PHYSICS_3D_ACTIVE_OBJECTS), Performance.get_monitor(Performance.OBJECT_COUNT)])
	var base := await _cost(150)
	print("--- a-060 %.3f ms. per node, process + physics switched off:" % base)
	var nodes: Array = []
	_collect(get_tree().root, nodes)
	for n in nodes:
		if not is_instance_valid(n):
			continue
		var pp: bool = n.is_processing()
		var ph: bool = n.is_physics_processing()
		n.set_process(false)
		n.set_physics_process(false)
		var m := await _cost(80)
		n.set_process(pp)
		n.set_physics_process(ph)
		if base - m > 0.3:
			print("  %7.3f ms  %s (%s)" % [base - m, n.get_script().resource_path.get_file() if n.get_script() else n.get_class(), n.name])
	for id in EntityManager.SCRIPTS:
		Game.entities.clear_all()
		await _frames(3)
		Game.entities.spawn(id)
		await _frames(5)
		print("  %-8s %.3f ms" % [id, await _cost(120)])
	Game.entities.clear_all()
	# opening doors: the frame you open one (room build) and the frame after (mesh merge)
	var worst := 0.0
	var sum := 0.0
	for i in 15:
		var r = Game.generator.room(Game.door)
		var t := Time.get_ticks_usec()
		r.exit_door.open()
		await get_tree().process_frame
		var a := (Time.get_ticks_usec() - t) / 1000.0
		t = Time.get_ticks_usec()
		await get_tree().process_frame
		var b := (Time.get_ticks_usec() - t) / 1000.0
		Game.player.teleport(r.global_exit() * Transform3D(Basis(Vector3.UP, PI), Vector3(0, 0.1, 1.0)))
		await _frames(5)
		worst = maxf(worst, maxf(a, b))
		sum += a + b
	print("door open: avg %.1f ms over 2 frames, worst frame %.1f ms" % [sum / 15.0, worst])
	get_tree().quit()


func _collect(n: Node, out: Array) -> void:
	if (n.is_processing() or n.is_physics_processing()) and n != self:
		out.append(n)
	for c in n.get_children():
		_collect(c, out)


func _cost(k: int) -> float:
	await get_tree().process_frame
	var a: Array[float] = []
	for i in k:
		await get_tree().process_frame
		a.append((Performance.get_monitor(Performance.TIME_PROCESS) + Performance.get_monitor(Performance.TIME_PHYSICS_PROCESS)) * 1000.0)
	a.sort()
	return a[k / 2]


func _frames(k: int) -> void:
	for i in k:
		await get_tree().process_frame
