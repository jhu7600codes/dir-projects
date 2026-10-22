extends Node
## multiplayer battle test, host side. run together with mp_battle_client.tscn:
## godot --headless --path . res://tests/mp_battle_host.tscn & godot --headless --path . res://tests/mp_battle_client.tscn
var failures := 0

func _ready() -> void:
	get_tree().current_scene = null
	_check(Net.host("host") == "", "hosting works")
	var t := 0.0
	while Net.players.size() < 2 and t < 25.0:
		await get_tree().create_timer(0.2).timeout
		t += 0.2
	_check(Net.players.size() == 2, "the client joined")
	Net.host_start("offices", false, "", "battle")
	await get_tree().create_timer(2.0).timeout
	Game.entities.natural_spawns = false
	_check(Game.sightings == "battle", "battle started")
	# the client hits us until we're dead
	var t2 := 0.0
	while t2 < 30.0 and not Game.player.dead:
		await get_tree().create_timer(0.2).timeout
		t2 += 0.2
	_check(Game.player.dead and Game.death_details.get("friend", "") == "client", "the client killed us")
	t2 = 0.0
	while t2 < 30.0 and _result() == null:
		await get_tree().create_timer(0.5).timeout
		t2 += 0.5
	_check(_result() != null, "one left standing: battle over")
	_check(int(Net.points.get("client", 0)) == 1, "the client got the kill (%s)" % Net.points)
	print("mp battle host %s (%d failures)" % ["passed" if failures == 0 else "FAILED", failures])
	await get_tree().create_timer(4.0).timeout
	Net.stop("")
	get_tree().quit(1 if failures else 0)


func _result():
	var r := Game.player.get_parent().find_children("*", "MinigameResult", true, false)
	return r[0] if r.size() > 0 else null


func _check(ok: bool, what: String) -> void:
	if not ok:
		failures += 1
	print(("ok   " if ok else "FAIL ") + "host: " + what)
