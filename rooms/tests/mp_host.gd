extends Node
## multiplayer test, host side. run together with mp_client.tscn:
## godot --headless --path . res://tests/mp_host.tscn & godot --headless --path . res://tests/mp_client.tscn
var failures := 0

func _ready() -> void:
	get_tree().current_scene = null
	_check(Net.host("host") == "", "hosting works")
	var t := 0.0
	while Net.players.size() < 2 and t < 20.0:
		await get_tree().create_timer(0.2).timeout
		t += 0.2
	_check(Net.players.size() == 2, "the client joined (%d players)" % Net.players.size())
	Net.host_start("offices")
	await get_tree().create_timer(2.0).timeout
	Game.entities.natural_spawns = false
	_check(Game.generator.run_seed == Game.net_seed, "same seed as sent")
	Game.generator.room(0).exit_door.open()
	await get_tree().create_timer(1.0).timeout
	Game.entities.spawn("a90")
	await get_tree().create_timer(3.0).timeout
	_check(Net.remotes.size() == 1, "the other player shows up as an avatar")
	var lk = null
	for n in range(1, 4):
		var r = Game.generator.room(n)
		if r and r.lockers.size() > 0:
			lk = r.lockers[0]
	# the client hides in the first locker of room 1, then dies
	var t2 := 0.0
	var occ := false
	while t2 < 8.0 and not occ:
		await get_tree().create_timer(0.2).timeout
		t2 += 0.2
		var r1 = Game.generator.room(1)
		for l in r1.find_children("*", "Locker", true, false):
			if l.occupied:
				occ = true
	_check(occ, "the client's locker shows as taken here")
	t2 = 0.0
	var client_dead := false
	while t2 < 8.0 and not client_dead:
		await get_tree().create_timer(0.2).timeout
		t2 += 0.2
		for id in Net.players:
			if int(id) != 1 and Net.players[id].dead:
				client_dead = true
	_check(client_dead, "the client died and we know about it")
	_check(not _has_end_screen(), "the run goes on while someone's alive")
	Game.player.take_damage(999, "a60")
	await get_tree().create_timer(9.0).timeout
	_check(_has_end_screen(), "everyone's dead: the run is over")
	print("mp host %s (%d failures)" % ["passed" if failures == 0 else "FAILED", failures])
	Net.stop("")
	get_tree().quit(1 if failures else 0)

func _has_end_screen() -> bool:
	return Game.player.get_parent().find_children("*", "EndScreen", true, false).size() > 0


func _check(ok: bool, what: String) -> void:
	if not ok:
		failures += 1
	print(("ok   " if ok else "FAIL ") + "host: " + what)
