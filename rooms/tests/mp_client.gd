extends Node
## multiplayer test, client side (see mp_host.gd)
var failures := 0

func _ready() -> void:
	get_tree().current_scene = null
	await get_tree().create_timer(1.5).timeout
	_check(Net.join("127.0.0.1", "client") == "", "joining works")
	var t := 0.0
	while (Game.player == null or Game.generator == null) and t < 20.0:
		await get_tree().create_timer(0.2).timeout
		t += 0.2
	_check(Game.player != null, "the host started the run for us")
	await get_tree().create_timer(4.0).timeout
	_check(Game.generator.run_seed == Game.net_seed and Game.net_seed != 0, "got the host's seed")
	_check(Game.door == 1, "the host opened door 1 for us too (door %d)" % Game.door)
	_check(Game.entities.active.has("a90") or Game.player.dead or Game.player.health < 100, "the host's a-90 came to us too")
	_check(Net.remotes.size() == 1, "the host shows up as an avatar")
	var lk = null
	for l in Game.generator.room(1).find_children("*", "Locker", true, false):
		if not l.broken:
			lk = l
			break
	Game.player.enter_locker(lk)
	await get_tree().create_timer(2.0).timeout
	Game.player.exit_locker()
	await get_tree().create_timer(1.0).timeout
	Game.player.take_damage(999, "a60")
	await get_tree().create_timer(8.0).timeout
	_check(Game.player.get_parent().find_children("*", "Spectator", true, false).size() > 0 or Game.player.get_parent().find_children("*", "EndScreen", true, false).size() > 0, "dead: spectating or the run ended")
	await get_tree().create_timer(4.0).timeout
	_check(Game.player.get_parent().find_children("*", "EndScreen", true, false).size() > 0, "everyone's dead: the run is over here too")
	print("mp client %s (%d failures)" % ["passed" if failures == 0 else "FAILED", failures])
	Net.stop("")
	get_tree().quit(1 if failures else 0)

func _check(ok: bool, what: String) -> void:
	if not ok:
		failures += 1
	print(("ok   " if ok else "FAIL ") + "client: " + what)
