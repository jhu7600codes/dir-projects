extends Node
## checks saving + continuing a run, starter gold, and the locker exit fix.
## godot --headless --path . res://tests/save_test.tscn

var failures := 0


func _ready() -> void:
	get_tree().current_scene = null
	_check(int(Save.data.gold) >= 100 or Save.data.has("starter_gold_given"), "starter gold exists (%d)" % int(Save.data.gold))
	Game.start_run(false)
	await _frames(10)
	Game.entities.natural_spawns = false
	var gen = Game.generator
	for i in 12:
		var r = gen.room(Game.door)
		r.exit_door.open()
		Game.player.teleport(r.global_exit() * Transform3D(Basis(Vector3.UP, PI), Vector3(0, 0.1, 1.0)))
		await _frames(3)
	Game.player.inventory.bandages = 3
	Game.player.health = 42
	var seed_value: int = gen.run_seed
	var room_type: String = gen.room(12).room_type
	Game.to_menu()
	await _frames(10)
	_check(Save.has_run() and int(Save.data.run.door) == 12, "run saved at a-012")
	Game.continue_run()
	await _frames(10)
	_check(Game.door == 12, "continued at a-012 (got %s)" % Game.door_label(Game.door))
	_check(Game.generator.run_seed == seed_value, "same seed")
	_check(Game.generator.room(12).room_type == room_type, "same room (%s)" % room_type)
	_check(Game.player.health == 42 and Game.player.inventory.bandages == 3, "health and items back")
	# locker exit: peek sideways inside, leave, then walking forward must go where you look
	Game.entities.natural_spawns = false
	var lk = null
	for n in range(13, 300, 3):
		Game.generator.jump_to(n)
		await _frames(3)
		var rr = Game.generator.room(n)
		# a room with open floor in front of the lockers, so walking out doesn't hit furniture
		if rr.lockers.size() > 0 and rr.room_type in ["locker_room", "four_locker", "three_locker"]:
			lk = Game.generator.room(n).lockers[0]
			break
	var p: Player = Game.player
	p.enter_locker(lk)
	await get_tree().create_timer(0.6).timeout
	p.head.rotation.y = 0.45
	var look_dir := -p.camera.global_basis.z
	p.exit_locker()
	await get_tree().create_timer(0.6).timeout
	look_dir = -p.camera.global_basis.z
	look_dir.y = 0
	var before := p.global_position
	Input.action_press("move_forward")
	await get_tree().create_timer(0.5).timeout
	Input.action_release("move_forward")
	var moved := p.global_position - before
	moved.y = 0
	# "forward" for walking is the body's facing; it must match where the camera looks
	var body_fwd := -p.global_basis.z
	body_fwd.y = 0
	var angle := rad_to_deg(body_fwd.normalized().angle_to(look_dir.normalized()))
	_check(moved.length() > 0.5 and angle < 3.0, "walks where you look after leaving a locker (off by %.1f deg)" % angle)
	Game.player.take_damage(999, "a60")
	await _frames(5)
	_check(not Save.has_run(), "dying clears the saved run")
	# doors open by themselves when you walk up to them
	Game.player.dead = false
	Game.player.health = 100
	Game.generator.jump_to(20)
	await _frames(5)
	var r20 = Game.generator.room(20)
	Game.player.teleport(r20.global_exit() * Transform3D(Basis(), Vector3(0, 0.1, -1.0)))
	await _frames(10)
	_check(Game.door == 21, "door opens by itself when you walk up (now %s)" % Game.door_label(Game.door))
	# touch: holding the "use" button opens an exit door
	var finished := [false]
	Game.run_finished.connect(func(_r): finished[0] = true)
	var exit_n := -1
	for n in range(201, 600):
		if Game.generator._is_exit_room(n):
			exit_n = n
			break
	Game.generator.jump_to(exit_n)
	await _frames(5)
	var ed: ExitDoor = null
	for c in Game.generator.room(exit_n).get_children():
		if c is ExitDoor:
			ed = c
	# stand in front of the exit door, looking at it
	Game.player.teleport(ed.global_transform * Transform3D(Basis(Vector3.UP, PI), Vector3(0, 0.05, -1.2)))
	Game.player.head.rotation.x = -0.35
	Settings.data.touch_controls = "on"
	var tc: TouchControls = null
	for c in Game.player.get_parent().get_children():
		if c is TouchControls:
			tc = c
	tc._layout()
	var use_pos: Vector2 = tc._buttons[0][2]
	var down := InputEventScreenTouch.new()
	down.index = 3
	down.position = use_pos
	down.pressed = true
	# headless has no touchscreen, so hand the touch straight to the controls
	tc._input(down)
	await get_tree().create_timer(1.8).timeout
	var lift := InputEventScreenTouch.new()
	lift.index = 3
	lift.position = use_pos
	lift.pressed = false
	tc._input(lift)
	await _frames(3)
	Settings.data.touch_controls = "auto"
	_check(finished[0], "holding the touch use button opens an exit door")
	get_tree().paused = false
	# admin runs keep their own progress and don't touch the normal one
	Save.use_profile("main")
	var main_best := int(Save.data.best_door)
	var main_gold := int(Save.data.gold)
	Game.start_run(true)
	await _frames(10)
	Game.entities.natural_spawns = false
	Game.generator.jump_to(main_best + 40)
	await _frames(5)
	Save.data.gold = 777
	Game.to_menu()
	await _frames(10)
	_check(not Save.is_admin_profile(), "title screen goes back to normal progress")
	_check(int(Save.data.best_door) == main_best and int(Save.data.gold) == main_gold, "normal progress untouched by the admin run")
	Save.use_profile("admin")
	_check(int(Save.data.best_door) >= main_best + 40 and int(Save.data.gold) == 777, "admin progress saved (best %s)" % Game.door_label(int(Save.data.best_door)))
	_check(Save.has_run(), "admin run can be continued")
	Save.use_profile("main")
	print("save test %s (%d failures)" % ["passed" if failures == 0 else "FAILED", failures])
	get_tree().quit(1 if failures else 0)


func _frames(n: int) -> void:
	for i in n:
		await get_tree().physics_frame


func _check(ok: bool, what: String) -> void:
	if not ok:
		failures += 1
	print(("ok   " if ok else "FAIL ") + what)
