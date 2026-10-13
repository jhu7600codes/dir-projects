extends Node
## multiplayer test, client side (see mp_host.gd)
var failures := 0

func _ready() -> void:
	get_tree().current_scene = null
	var online := OS.get_cmdline_user_args().has("--online")
	await get_tree().create_timer(3.0 if online else 1.5).timeout
	if online:
		var code := FileAccess.get_file_as_string("user://mp_test_code.txt")
		_check(Net.join_online(code, "client") == "", "joining online works (code %s)" % code)
	else:
		_check(Net.join("127.0.0.1", "client") == "", "joining works")
	var t := 0.0
	while (Game.player == null or Game.generator == null) and t < 25.0:
		await get_tree().create_timer(0.2).timeout
		t += 0.2
	_check(Game.player != null, "the host started the run for us")
	await get_tree().create_timer(4.5).timeout
	_check(Game.generator.run_seed == Game.net_seed and Game.net_seed != 0, "got the host's seed")
	_check(Game.door == 1, "the host opened door 1 for us too (door %d)" % Game.door)
	_check(Game.entities.active.has("a90") or Game.player.dead or Game.player.health < 100, "the host's a-90 came to us too")
	_check(Net.remotes.size() == 1, "the host shows up as an avatar")
	# wait for a-90 to leave before trying to hide
	var t3 := 0.0
	while Game.entities.active.has("a90") and t3 < 6.0:
		await get_tree().create_timer(0.2).timeout
		t3 += 0.2
	var lk = null
	for r in Game.generator.rooms:
		for l in r.find_children("*", "Locker", true, false):
			if lk == null and not l.broken and not l.mimic:
				lk = l
	if lk:
		Game.player.enter_locker(lk)
		await get_tree().create_timer(2.5).timeout
		_check(Game.player.hidden, "hid in a locker")
		Game.player.exit_locker()
	await get_tree().create_timer(1.5).timeout
	Game.player.take_damage(999, "a60")
	var world = Game.player.get_parent()
	var t2 := 0.0
	while t2 < 21.0 and world.find_children("*", "Spectator", true, false).is_empty():
		await get_tree().create_timer(0.5).timeout
		t2 += 0.5
	_check(world.find_children("*", "Spectator", true, false).size() > 0, "dead: spectating the host")
	t2 = 0.0
	while t2 < 25.0 and world.find_children("*", "EndScreen", true, false).is_empty():
		await get_tree().create_timer(0.5).timeout
		t2 += 0.5
	_check(world.find_children("*", "EndScreen", true, false).size() > 0, "everyone's dead: the run is over here too")
	print("mp client %s (%d failures)" % ["passed" if failures == 0 else "FAILED", failures])
	Net.stop("")
	get_tree().quit(1 if failures else 0)


func _check(ok: bool, what: String) -> void:
	if not ok:
		failures += 1
	print(("ok   " if ok else "FAIL ") + "client: " + what)
