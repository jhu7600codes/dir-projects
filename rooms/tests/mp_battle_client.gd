extends Node
## multiplayer battle test, client side (see mp_battle_host.gd)
var failures := 0

func _ready() -> void:
	get_tree().current_scene = null
	await get_tree().create_timer(1.5).timeout
	_check(Net.join("127.0.0.1", "client") == "", "joining works")
	var t := 0.0
	while (Game.player == null or Game.generator == null) and t < 25.0:
		await get_tree().create_timer(0.2).timeout
		t += 0.2
	await get_tree().create_timer(3.0).timeout
	_check(Game.sightings == "battle", "battle started here too")
	_check(Net.remotes.size() == 1 and Net.remotes.values()[0]._it != null, "the host can be hit")
	for i in 4:
		Net.remotes.values()[0]._on_hit(Game.player)
		await get_tree().create_timer(0.8).timeout
	var t2 := 0.0
	while t2 < 30.0 and Game.player.get_parent().find_children("*", "MinigameResult", true, false).is_empty():
		await get_tree().create_timer(0.5).timeout
		t2 += 0.5
	_check(not Game.player.get_parent().find_children("*", "MinigameResult", true, false).is_empty(), "battle over here too")
	_check(not Game.player.dead, "we survived")
	print("mp battle client %s (%d failures)" % ["passed" if failures == 0 else "FAILED", failures])
	Net.stop("")
	get_tree().quit(1 if failures else 0)


func _check(ok: bool, what: String) -> void:
	if not ok:
		failures += 1
	print(("ok   " if ok else "FAIL ") + "client: " + what)
