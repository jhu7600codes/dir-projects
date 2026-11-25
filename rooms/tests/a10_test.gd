extends Node
## a-10 test: godot --headless --path . res://tests/a10_test.tscn
## hide too long -> a-10 comes, warns, kicks you out with damage. get out in time -> it leaves.

var failures := 0


func _ready() -> void:
	get_tree().current_scene = null
	await _frames(2)
	Game.start_run(true, false, "offices", {"sightings": "office"})
	await _frames(30)
	var p: Player = Game.player
	var lk: Locker = null
	for n in range(20, 80):
		Game.generator.jump_to(n)
		await _frames(5)
		var r = Game.generator.room(n)
		if not r.lockers.is_empty():
			lk = r.lockers[0]
			break
	_check(lk != null, "found a locker")
	Game.entities.clear_all()
	# 1: stay hidden -> kicked out
	p.enter_locker(lk)
	await _secs(0.6)
	_check(p.hidden, "hiding")
	var hp := p.health
	await _secs(26.0)
	_check(Game.entities.active.has("a10") or not p.hidden, "a-10 came after hiding a while")
	await _secs(8.0)
	_check(not p.hidden, "kicked out of the locker")
	_check(p.health < hp, "it hurt (%.0f -> %.0f)" % [hp, p.health])
	await _secs(1.0)
	_check(not Game.entities.active.has("a10"), "a-10 left after the kick")
	# 2: get out when it says so -> no damage
	p.health = 100.0
	await _secs(31.0)  # cooldown
	p.enter_locker(lk)
	await _secs(0.6)
	var t := 0.0
	while not Game.entities.active.has("a10") and t < 30.0:
		await _secs(0.5)
		t += 0.5
	_check(Game.entities.active.has("a10"), "a-10 came again")
	await _secs(2.0)
	p.exit_locker()
	await _secs(1.5)
	_check(not Game.entities.active.has("a10"), "a-10 left when you got out")
	_check(p.health >= 99.0, "no damage when you got out in time (%.0f)" % p.health)
	print("a10 test %s (%d failures)" % ["passed" if failures == 0 else "FAILED", failures])
	get_tree().quit()


func _check(ok: bool, what: String) -> void:
	print(("ok   " if ok else "FAIL ") + what)
	if not ok:
		failures += 1


func _secs(t: float) -> void:
	await get_tree().create_timer(t).timeout


func _frames(k: int) -> void:
	for i in k:
		await get_tree().process_frame
