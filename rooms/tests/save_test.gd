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
		if Game.generator.room(n).lockers.size() > 0:
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
	var angle := rad_to_deg(moved.normalized().angle_to(look_dir.normalized()))
	_check(moved.length() > 0.5 and angle < 10.0, "walks where you look after leaving a locker (off by %.1f deg)" % angle)
	Game.player.take_damage(999, "a60")
	await _frames(5)
	_check(not Save.has_run(), "dying clears the saved run")
	print("save test %s (%d failures)" % ["passed" if failures == 0 else "FAILED", failures])
	get_tree().quit(1 if failures else 0)


func _frames(n: int) -> void:
	for i in n:
		await get_tree().physics_frame


func _check(ok: bool, what: String) -> void:
	if not ok:
		failures += 1
	print(("ok   " if ok else "FAIL ") + what)
