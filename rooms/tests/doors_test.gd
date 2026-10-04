extends Node
## checks the doors entities from the "got any more doors?" modifier: eyes hurt when you
## look at them, screech leaves when you look at it and bites when you don't, seek chases
## you and gives up after a few doors. runs in the admin profile.
## godot --headless --path . res://tests/doors_test.tscn

var failures := 0


func _ready() -> void:
	get_tree().current_scene = null
	Save.use_profile("admin")
	Save.data.achievements["long_walk"] = 1
	Save.data.modifiers = ["more_doors"]
	Game.start_run(true)
	await _frames(10)
	Game.entities.natural_spawns = false
	var p: Player = Game.player
	var gen = Game.generator
	gen.jump_to(40)
	await _frames(5)
	Game.door = 40

	# eyes: looking at it hurts, looking away doesn't
	var eyes = Game.entities.spawn("eyes")
	await _frames(2)
	_look_from(p, eyes.global_position, 5.0)
	p.health = 100.0
	await _secs(1.0)
	_check(p.health < 90.0, "eyes hurt when you look at them (%d)" % p.health)
	p.health = 100.0
	_look_away(p, eyes.global_position)
	await _secs(1.0)
	_check(p.health == 100.0, "eyes don't hurt when you look away (%d)" % p.health)
	Game.entities.clear_all()
	await _frames(3)

	# screech: look at it in time and it leaves
	p.health = 100.0
	var sc = Game.entities.spawn("screech")
	if is_instance_valid(sc):
		_point_at(p, sc.global_position)
	await _secs(0.5)
	_check(not Game.entities.active.has("screech") and p.health == 100.0, "screech leaves when you look at it")
	# ...and bites when you don't
	sc = Game.entities.spawn("screech")
	_look_away(p, sc.global_position)
	await _secs(3.2)
	_check(p.health <= 60.0, "screech bites when you ignore it (%d)" % p.health)
	Game.entities.clear_all()
	await _frames(3)

	# figure: the door is locked with a code, the paper has it, the keypad opens the door
	p.health = 100.0
	Game.admin_flags.god = true
	var fig = Game.entities.spawn("figure")
	await _frames(2)
	var fdoor = Game.generator.room(Game.door).exit_door
	_check(fdoor.locked and fig.code.length() == 4, "figure's door has a code lock")
	_check(is_instance_valid(fig._paper) and fig._paper.is_inside_tree(), "the paper with the code is in the room")
	fig._paper.get_children().filter(func(c): return c is Interactable)[0].interact(p)
	_check(fig.note == fig.code, "reading the paper remembers the code")
	fdoor._on_used(p)
	await _frames(2)
	var pad: Keypad = null
	for c in fig.get_parent().get_children():
		if c is Keypad:
			pad = c
	_check(pad != null and Game.ui_open, "trying the door opens the keypad")
	var wrong := "0000" if fig.code != "0000" else "1111"
	for ch in wrong:
		pad._press(ch)
	pad._press("ok")
	_check(fdoor.locked, "a wrong code keeps it locked")
	for ch in fig.code:
		pad._press(ch)
	pad._press("ok")
	await _frames(3)
	_check(not fdoor.locked and fdoor.is_open and not Game.ui_open, "the right code opens the door")
	Game.entities.clear_all()
	await _frames(3)
	Game.admin_flags.god = false

	# seek: eyes on the walls, then a chase through the next door, then it gives up
	p.health = 100.0
	Game.admin_flags.god = true
	var sk = Game.entities.spawn("seek")
	await _frames(2)
	_check(sk._eye_nodes.size() > 0, "seek's eyes show up on the walls (%d)" % sk._eye_nodes.size())
	await _open_next()
	await _frames(10)
	_check(Game.chase and sk._chasing, "seek's chase starts through the next door")
	await _frames(3)
	_check(sk._obstacles.size() > 0, "the chase rooms have obstacles (%d)" % sk._obstacles.size())
	for i in Seek.CHASE_DOORS:
		await _open_next()
	var last_door = Game.generator.room(Game.door - 1).exit_door
	await _frames(5)
	# walk on into the room so the door can close behind you
	Game.player.teleport(Game.player.global_transform * Transform3D(Basis(), Vector3(0, 0, -3.0)))
	await _secs(1.5)
	_check(not last_door.is_open, "the curious light slams the last door shut")
	_check(not Game.entities.active.has("seek") and not Game.chase, "seek gives up after %d doors" % Seek.CHASE_DOORS)
	Game.admin_flags.god = false

	# seek's beams: they block you standing up, you get under them crouching
	# (in an empty hallway, so furniture can't get in the way of the test)
	var hn := Game.door + 2
	while Game.generator.room(hn) == null or Game.generator.room(hn).room_type != "hallway":
		hn += 1
		Game.generator.jump_to(hn)
		await _frames(1)
	Game.door = hn
	var hr = Game.generator.room(hn)
	var hp: Array = hr.path_global()
	# a seek that only builds the beam, it never starts a chase
	var sk2 := Seek.new()
	add_child(sk2)
	var mid: Vector3 = hp[hp.size() / 2]
	var bf: Vector3 = Vector3(hp[-1].x - hp[0].x, 0, hp[-1].z - hp[0].z).normalized()
	sk2._beam(hr, mid, bf)
	var beam = sk2._obstacles[-1]
	for crouch in [false, true]:
		p.teleport(Transform3D(Basis(), mid - bf * 1.5 - Vector3(0, 1.6, 0)))
		p.rotation.y = atan2(-bf.x, -bf.z)
		p.head.rotation = Vector3.ZERO
		if crouch:
			Input.action_press("crouch")
		Game.touch_move = Vector2(0, -1)
		await _secs(1.5)
		Game.touch_move = Vector2.ZERO
		Input.action_release("crouch")
		var passed: float = (p.global_position - mid).dot(bf)
		if crouch:
			_check(passed > 0.5, "crouching gets you under a beam (%.1f)" % passed)
		else:
			_check(passed < 0.0, "standing up, a beam blocks you (%.1f)" % passed)
	sk2.queue_free()

	# no ghost kills: during a-60's warning it's invisible and can't hurt you, even up close
	p.health = 100.0
	var a60 = Game.entities.spawn("a60")
	p.teleport(Transform3D(Basis(), a60.global_position + Vector3(0, -1.6, 1.5)))
	await _secs(3.0)
	_check(not p.dead and p.health == 100.0, "a-60 can't kill you while it's still warning (invisible)")
	Game.entities.clear_all()
	await _frames(3)

	Save.data.modifiers = []
	Save.write()
	Game.to_menu()
	await _frames(10)
	Save.use_profile("main")
	print("doors test %s (%d failures)" % ["passed" if failures == 0 else "FAILED", failures])
	get_tree().quit(1 if failures else 0)


## stand a few metres back along the room's path from target and look straight at it
func _look_from(p: Player, target: Vector3, _dist: float) -> void:
	var pts: Array = Game.generator.room(Game.door).path_global()
	var best: Vector3 = pts[0]
	for q in pts:
		var d: float = Vector2(q.x - target.x, q.z - target.z).length()
		if d > 2.0 and d < Vector2(best.x - target.x, best.z - target.z).length():
			best = q
	p.teleport(Transform3D(Basis(), best - Vector3(0, 1.6, 0)))
	_point_at(p, target)


func _point_at(p: Player, target: Vector3) -> void:
	var eye: Vector3 = p.head_position()
	var flat := Vector3(target.x - eye.x, 0, target.z - eye.z)
	p.rotation.y = atan2(-flat.x, -flat.z)
	p.head.rotation.y = 0.0
	p.head.rotation.x = atan2(target.y - eye.y, flat.length())
	p.camera.force_update_transform()


func _look_away(p: Player, target: Vector3) -> void:
	_point_at(p, target)
	p.rotation.y += PI
	p.head.rotation.x = 0.0


func _open_next() -> void:
	var r = Game.generator.room(Game.door)
	r.exit_door.open()
	Game.player.teleport(r.global_exit() * Transform3D(Basis(Vector3.UP, PI), Vector3(0, 0.1, 1.0)))
	await _frames(3)


func _frames(n: int) -> void:
	for i in n:
		await get_tree().process_frame


func _secs(t: float) -> void:
	await get_tree().create_timer(t).timeout


func _check(ok: bool, what: String) -> void:
	print(("ok   " if ok else "FAIL ") + what)
	if not ok:
		failures += 1
