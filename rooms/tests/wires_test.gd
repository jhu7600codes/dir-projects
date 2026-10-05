extends Node
## the wires subfloor: a-240's key + crack + management door, the breaker rooms with w-10,
## w-15 raw wires, w-50's elevator power + the shock + the elevator ride, and the powered
## office afterwards. runs in the admin profile.
## godot --headless --path . res://tests/wires_test.tscn

var failures := 0


func _ready() -> void:
	get_tree().current_scene = null
	Save.use_profile("admin")
	Save.data.modifiers = []
	Save.write()
	Game.start_run(true)
	await _frames(10)
	Game.entities.natural_spawns = false
	var p: Player = Game.player

	# a-240: one drawer has the key, the crack is walkable, the door wants the key
	Game.generator.jump_to(240)
	await _frames(5)
	var r = Game.generator.room(240)
	_check(r.room_type == "management", "a-240 is the management room")
	var keyed: Array = r.get_children().filter(func(c): return c is Drawer and c.gives_key)
	_check(keyed.size() == 1, "exactly one drawer has the management key")
	var md: ManagementDoor = r.find_children("*", "ManagementDoor", true, false)[0]
	# walk from the room through the crack into the vine hallway
	p.teleport(r.global_transform * Transform3D(Basis(), Vector3(-3.6, 0.05, 4.0)))
	var md_dir: Vector3 = (md.global_position - p.global_position)
	md_dir.y = 0.0
	p.rotation.y = atan2(-md_dir.x, -md_dir.z)
	Game.touch_move = Vector2(0, -1)
	await _secs(2.0)
	Game.touch_move = Vector2.ZERO
	var walked: float = p.global_position.distance_to(md.global_position)
	_check(walked < 2.5, "you can walk through the crack to the management door (%.1fm left)" % walked)
	md._use(p)
	await _frames(2)
	_check(Game.floor == "offices", "the management door stays shut without the key")
	keyed[0]._open(p, keyed[0].get_children().filter(func(c): return c is Interactable)[0])
	_check(Game.has_key, "the drawer gives you the management key")
	p.health = 77.0
	md._use(p)
	await _frames(10)
	p = Game.player
	_check(Game.floor == "wires" and Game.door == 0 and Save.data.wires_found, "the management door takes you down into the wires")
	_check(is_equal_approx(p.health, 77.0), "you keep your health going down (%.0f)" % p.health)
	_check(Game.door_label(5) == "W-05", "wires doors are labeled W-05")
	Game.entities.natural_spawns = false

	# w-15: a raw wire shocks you when you walk into it
	var wire: RawWire = null
	var early := 0
	for n in range(1, 15):
		Game.generator.jump_to(n)
		await _frames(1)
		early += Game.generator.room(n).find_children("*", "RawWire", true, false).size()
	_check(early == 0, "no raw wires before w-15 (%d)" % early)
	Game.generator.jump_to(15)
	await _frames(1)
	_check(Game.generator.room(15).find_children("*", "RawWire", true, false).size() > 0, "w-15 always has raw wires")
	for n in range(15, 30):
		Game.generator.jump_to(n)
		await _frames(2)
		var ws: Array = Game.generator.room(n).find_children("*", "RawWire", true, false)
		if not ws.is_empty():
			wire = ws[0]
			break
	p.health = 100.0
	p.teleport(Transform3D(Basis(), wire.global_position + Vector3(0, 0.05, 0)))
	await _secs(0.4)
	_check(p.health <= 85.0, "w-15: walking into a raw wire shocks you (%.0f)" % p.health)
	p.teleport(Transform3D(Basis(), wire.global_position + Vector3(0, 0.05, 3.0)))

	# w-10: breaker room, locked door, outlets, the switch
	Game.generator.jump_to(10)
	await _frames(5)
	var sr = Game.generator.room(10)
	_check(sr.exit_door.locked, "w-10's door has no power at first")
	await _frames(3)
	_check(Game.entities.active.has("w10"), "w-10 wakes up in the unpowered breaker room")
	p.health = 100.0
	p.teleport(sr.global_transform * Transform3D(Basis(), Vector3(0, 0.05, 6.0)))
	await _secs(9.0)
	_check(p.health < 100.0, "w-10's outlets lunge at you if you stand still (%.0f)" % p.health)
	sr.panel.flip()
	await _frames(3)
	_check(not sr.exit_door.locked and not Game.entities.active.has("w10") and Game.power[0], "the switch powers a-001..a-200, opens the door and stops w-10")

	# w-50: range switch + elevator power, the shock, the elevator ride up
	Game.admin_flags.god = true
	Game.generator.jump_to(50)
	await _frames(5)
	var er = Game.generator.room(50)
	er.elevator_panel.flip()
	_check(not Game.elevator_power, "the elevator needs a-801..a-1000 powered first")
	er.panel.flip()
	er.elevator_panel.flip()
	await _frames(3)
	_check(Game.entities.active.has("w50"), "w-50 the shock wakes up when the elevator gets power")
	await _secs(2.5)
	var finished := [""]
	Game.run_finished.connect(func(why): finished[0] = why)
	p.teleport(er.global_transform * Transform3D(Basis(), Vector3(0, 0.05, er.LENGTH + 1.2)))
	await _secs(2.0)
	_check(finished[0] == "wires" and Game.carry.get("fixed", false), "riding the elevator takes you up to the fixed office")
	Game.admin_flags.god = false

	# the powered office: people at desks, no robbing, atms, no entities... except coworkers
	get_tree().paused = false
	Game.start_run(true, false, "offices", Game.carry)
	await _frames(10)
	p = Game.player
	_check(Game.office_powered() and Game.darkness(300) == 0.0, "the ride up is the fixed office, all lit")
	Game.door = 70
	_check(not Game.entities.can_spawn("a60") and not Game.entities.can_spawn("a90"), "no entities come in the powered office")
	var cub = null
	for n in range(8, 120):
		Game.generator.jump_to(n)
		await _frames(1)
		if Game.generator.room(n).room_type == "cubicles":
			cub = Game.generator.room(n)
			break
	var workers: Array = cub.find_children("*", "Worker", true, false)
	_check(workers.size() >= 4, "workers sit at the desks (%d)" % workers.size())
	var dr = cub.get_children().filter(func(c): return c is Drawer)[0]
	var g0 := int(Save.data.gold)
	dr._open(p, dr.get_children().filter(func(c): return c is Interactable)[0])
	_check(not dr._opened and int(Save.data.gold) == g0, "you can't rob the drawers anymore")
	Save.data.gold = 120
	var atm := AtmScreen.new()
	add_child(atm)
	await _frames(1)
	var bank0 := int(Save.data.get("bank", 0))
	atm._deposit(50)
	_check(int(Save.data.gold) == 70 and int(Save.data.get("bank", 0)) == bank0 + 50, "the atm moves gold into the bank")
	atm.close()
	# look at a coworker and the room goes down with you
	var ew := EntityWorker.new()
	ew.entity_id = "a120"
	ew.room = cub
	cub.add_child(ew)
	ew.global_position = cub.path_global()[0] + Vector3(0, -1.6, 0)
	ew.set_process(true)
	ew._target = ew.global_position
	p.teleport(Transform3D(Basis(), ew.global_position + (cub.path_global()[1] - cub.path_global()[0]).normalized() * 4.0))
	var to: Vector3 = (ew.global_position + Vector3(0, 1.8, 0)) - p.head_position()
	p.rotation.y = atan2(-to.x, -to.z)
	p.head.rotation.x = atan2(to.y, Vector2(to.x, to.z).length())
	await _secs(1.2)
	_check(p.dead and workers[0].dead, "looking at a coworker kills everyone in the room")
	# just playing the offices floor is the normal office
	get_tree().paused = false
	Game.start_run(true)
	await _frames(10)
	_check(not Game.office_powered() and Game.darkness(300) > 0.0, "a normal offices run is the old dark office")
	Save.write()
	get_tree().paused = false
	Game.to_menu()
	await _frames(10)
	Save.use_profile("main")
	print("wires test %s (%d failures)" % ["passed" if failures == 0 else "FAILED", failures])
	get_tree().quit(1 if failures else 0)


func _frames(n: int) -> void:
	for i in n:
		await get_tree().process_frame


func _secs(t: float) -> void:
	await get_tree().create_timer(t, true).timeout


func _check(ok: bool, what: String) -> void:
	print(("ok   " if ok else "FAIL ") + what)
	if not ok:
		failures += 1
