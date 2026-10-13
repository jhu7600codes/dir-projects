extends Node
## multiplayer test, host side. run together with mp_client.tscn (add -- --online to both
## to go through supabase instead of the lan):
## godot --headless --path . res://tests/mp_host.tscn & godot --headless --path . res://tests/mp_client.tscn
var failures := 0

func _ready() -> void:
	get_tree().current_scene = null
	var online := OS.get_cmdline_user_args().has("--online")
	if online:
		_check(Net.host_online("host") == "", "hosting online works (code %s)" % Net.code)
		var f := FileAccess.open("user://mp_test_code.txt", FileAccess.WRITE)
		f.store_string(Net.code)
		f.close()
	else:
		_check(Net.host("host") == "", "hosting works")
	var t := 0.0
	while Net.players.size() < 2 and t < 25.0:
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
	# the client hides in a locker in room 1, comes out, then dies
	var t2 := 0.0
	var occ := false
	while t2 < 16.0 and not occ:
		await get_tree().create_timer(0.2).timeout
		t2 += 0.2
		for r in Game.generator.rooms:
			for l in r.find_children("*", "Locker", true, false):
				if l.occupied:
					occ = true
	_check(occ, "the client's locker shows as taken here")
	t2 = 0.0
	var client_dead := false
	while t2 < 12.0 and not client_dead:
		await get_tree().create_timer(0.2).timeout
		t2 += 0.2
		for id in Net.players:
			if int(id) != 1 and Net.players[id].dead:
				client_dead = true
	_check(client_dead, "the client died and we know about it")
	_check(not _has_end_screen(), "the run goes on while someone's alive")
	# give the client time to read the curious light and start spectating us
	await get_tree().create_timer(22.0).timeout
	Game.player.take_damage(999, "a60")
	var t3 := 0.0
	while t3 < 25.0 and not _has_end_screen():
		await get_tree().create_timer(0.5).timeout
		t3 += 0.5
	_check(_has_end_screen(), "everyone's dead: the run is over")
	print("mp host %s (%d failures)" % ["passed" if failures == 0 else "FAILED", failures])
	await get_tree().create_timer(8.0).timeout
	Net.stop("")
	get_tree().quit(1 if failures else 0)


func _has_end_screen() -> bool:
	return Game.player.get_parent().find_children("*", "EndScreen", true, false).size() > 0


func _check(ok: bool, what: String) -> void:
	if not ok:
		failures += 1
	print(("ok   " if ok else "FAIL ") + "host: " + what)
