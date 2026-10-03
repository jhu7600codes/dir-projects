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
	await _frames(2)
	_point_at(p, sc.global_position)
	await _secs(0.5)
	_check(not Game.entities.active.has("screech") and p.health == 100.0, "screech leaves when you look at it")
	# ...and bites when you don't
	sc = Game.entities.spawn("screech")
	await _frames(2)
	_look_away(p, sc.global_position)
	await _secs(3.2)
	_check(p.health <= 60.0, "screech bites when you ignore it (%d)" % p.health)
	Game.entities.clear_all()
	await _frames(3)

	# seek: eyes on the walls, then a chase through the next door, then it gives up
	p.health = 100.0
	Game.admin_flags.god = true
	var sk = Game.entities.spawn("seek")
	await _frames(2)
	_check(sk._eye_nodes.size() > 0, "seek's eyes show up on the walls (%d)" % sk._eye_nodes.size())
	await _open_next()
	await _frames(10)
	_check(Game.chase and sk._chasing, "seek's chase starts through the next door")
	for i in Seek.CHASE_DOORS:
		await _open_next()
	await _frames(5)
	_check(not Game.entities.active.has("seek") and not Game.chase, "seek gives up after %d doors" % Seek.CHASE_DOORS)
	Game.admin_flags.god = false

	Save.data.modifiers = []
	Save.write()
	Game.to_menu()
	await _frames(10)
	Save.use_profile("main")
	print("doors test %s (%d failures)" % ["passed" if failures == 0 else "FAILED", failures])
	get_tree().quit(1 if failures else 0)


## stand `dist` metres away from target and look straight at it
func _look_from(p: Player, target: Vector3, dist: float) -> void:
	var r = Game.generator.room(Game.door)
	var pts: Array = r.path_global()
	var from: Vector3 = pts[0]
	from.y = target.y - 1.6
	if from.distance_to(Vector3(target.x, from.y, target.z)) < 1.0:
		from = target + Vector3(0, -1.6, dist)
	p.teleport(Transform3D(Basis(), from))
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
