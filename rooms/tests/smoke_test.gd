extends Node
## headless smoke test: godot --headless --path . res://tests/smoke_test.tscn
## starts an admin run, opens a bunch of doors, visits the special rooms and spawns
## every entity. prints "smoke test passed" at the end, errors show up in the log.

var failures := 0


func _ready() -> void:
	get_tree().current_scene = null  # keep this node alive across scene changes
	Game.player_died.connect(func(c): print("debug: player died to ", c, " at ", Game.door_label(Game.door), " hidden=", Game.player.hidden, " active=", Game.entities.active.keys()))
	await _frames(2)
	Game.start_run(true)
	await _frames(10)
	_check(Game.generator != null and Game.player != null, "game scene set up")
	Game.admin_flags.god = true
	for i in 45:
		await _open_next()
	_check(Game.door == 45, "opened 45 doors (at %s)" % Game.door_label(Game.door))
	_check(Game.generator.rooms.size() <= 5, "old rooms are freed (%d loaded)" % Game.generator.rooms.size())
	for id in EntityManager.SCRIPTS:
		Game.entities.clear_all()
		await _frames(2)
		var e = Game.entities.spawn(id)
		_check(e != null, "spawned " + id)
		await _frames(60)
		# walk through a couple of doors while it's active
		await _open_next()
		await _frames(200)
		Game.entities.clear_all()
		await _frames(5)
	for n in [99, 149, 230, 505, 998]:
		Game.generator.jump_to(n)
		await _frames(5)
		await _open_next()
		await _open_next()
		_check(Game.door == n + 2, "jumped to %d and walked on" % n)
	Game.generator.jump_to(999)
	await _frames(3)
	await _open_next()
	_check(Game.generator.room(1000) != null, "a-1000 exists")
	_check(Game.generator.room(1000).exit_door == null, "a-1000 has no next door")
	# hide in a locker and let a-60 pass: should survive
	Game.admin_flags.god = false
	Game.entities.natural_spawns = false
	Game.entities.clear_all()
	var lk = await _room_with_locker()
	Game.player.enter_locker(lk)
	await _frames(40)
	_check(Game.player.hidden, "player hides in a locker")
	Game.entities.spawn("a60")
	var t := Time.get_ticks_msec()
	while Game.entities.active.has("a60") and Time.get_ticks_msec() - t < 30000:
		await _frames(10)
	_check(not Game.entities.active.has("a60"), "a-60 passed and left (%.1fs)" % ((Time.get_ticks_msec() - t) / 1000.0))
	_check(not Game.player.dead, "hidden player survived a-60")
	_check(Achievements.has("survive_a60") or Save.locked, "survive achievement path ran")
	Game.player.exit_locker()
	await _frames(40)
	_check(not Game.player.hidden, "player leaves the locker")
	# standing in the open: a-60 should kill
	Game.player.health = 100
	Game.entities.spawn("a60")
	t = Time.get_ticks_msec()
	while not Game.player.dead and Game.entities.active.has("a60") and Time.get_ticks_msec() - t < 30000:
		await _frames(10)
	_check(Game.player.dead, "a-60 kills a player in the open")
	# a-90: staying still is safe, any input costs 90 health
	Game.start_run(true)
	await _frames(10)
	Game.entities.natural_spawns = false
	Game.generator.jump_to(20)
	await _frames(5)
	Game.player.health = 100
	Game.entities.spawn("a90")
	await get_tree().create_timer(3.0).timeout
	_check(Game.player.health == 100, "a-90 does nothing if you don't move")
	Game.entities.clear_all()
	await _frames(5)
	Game.entities.spawn("a90")
	await get_tree().create_timer(1.6).timeout
	var key := InputEventKey.new()
	key.physical_keycode = KEY_W
	key.pressed = true
	Input.parse_input_event(key)
	await get_tree().create_timer(1.5).timeout
	var up := InputEventKey.new()
	up.physical_keycode = KEY_W
	Input.parse_input_event(up)
	_check(Game.player.health == 10, "a-90 hits for 90 when you press a key (hp %d)" % Game.player.health)
	print("smoke test %s (%d failures)" % ["passed" if failures == 0 else "FAILED", failures])
	get_tree().quit(1 if failures else 0)


func _room_with_locker() -> Locker:
	for n in range(10, 400, 7):
		Game.generator.jump_to(n)
		await _frames(3)
		var r = Game.generator.room(n)
		if r.lockers.size() > 0:
			return r.lockers[0]
	return null


func _open_next() -> void:
	var r = Game.generator.room(Game.door)
	if r and r.exit_door and not r.exit_door.is_open:
		r.exit_door.open()
		Game.player.teleport(r.global_exit() * Transform3D(Basis(Vector3.UP, PI), Vector3(0, 0.1, 1.0)))
	await _frames(4)


func _frames(n: int) -> void:
	for i in n:
		await get_tree().physics_frame


func _check(ok: bool, what: String) -> void:
	if not ok:
		failures += 1
	print(("ok   " if ok else "FAIL ") + what)
