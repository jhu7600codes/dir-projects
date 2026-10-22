class_name MinigameResult
extends CanvasLayer
## the end of a minigame: your score (and your best), or in a party everyone's scores,
## best first. play again or back to the title screen.


func setup(id: String, scores: Dictionary, best := -1) -> void:
	layer = 50
	process_mode = Node.PROCESS_MODE_ALWAYS
	Input.mouse_mode = Input.MOUSE_MODE_VISIBLE
	var root := Control.new()
	root.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	add_child(root)
	UIKit.full_screen_bg(root, Color(0, 0, 0, 0.8))
	var p := UIKit.panel(Vector2(420, 0))
	UIKit.centered(root, p)
	var v := UIKit.vbox(10)
	p.add_child(v)
	v.add_child(UIKit.label(Game.MINIGAMES[id][0], 30, UIKit.ACCENT))
	var names := scores.keys()
	names.sort_custom(func(a, b): return int(scores[a]) > int(scores[b]))
	for i in names.size():
		var n: String = names[i]
		var line := score_text(id, int(scores[n]))
		if names.size() > 1:
			line = "%d.  %s  -  %s" % [i + 1, n, line]
		v.add_child(UIKit.label(line, 22 if i == 0 else 18, Color(1, 1, 1) if i == 0 else Color(0.75, 0.75, 0.75)))
	if best >= 0:
		var mine := int(scores.values()[0]) if not scores.is_empty() else 0
		if mine > best:
			v.add_child(UIKit.label("new best!", 18, Color(0.5, 1.0, 0.6)))
		else:
			v.add_child(UIKit.label(Loc.t("best: ") + score_text(id, best), 16, Color(0.6, 0.6, 0.6)))
	if not Net.active:
		v.add_child(UIKit.button("play again", func(): Game.start_run(false, false, "offices", {"minigame": id}), 20))
	elif Net.is_host:
		v.add_child(UIKit.button("play again (everyone)", func(): Net.host_start("offices", false, id), 20))
	else:
		v.add_child(UIKit.label("waiting for the host to start again...", 15, Color(0.6, 0.6, 0.6)))
	v.add_child(UIKit.button("title screen", func():
		Net.stop("")
		Game.to_menu(), 18))


static func score_text(id: String, score: int) -> String:
	if id == "w10":
		return "%d:%02d" % [score / 60, score % 60]
	return Loc.t("%d a-90s") % score


## multiplayer sightings: battle (everyone's kills) or hide and seek (who won)
func setup_sightings(reason: String, kills: Dictionary) -> void:
	layer = 50
	process_mode = Node.PROCESS_MODE_ALWAYS
	Input.mouse_mode = Input.MOUSE_MODE_VISIBLE
	var root := Control.new()
	root.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	add_child(root)
	UIKit.full_screen_bg(root, Color(0, 0, 0, 0.8))
	var p := UIKit.panel(Vector2(420, 0))
	UIKit.centered(root, p)
	var v := UIKit.vbox(10)
	p.add_child(v)
	var sid := "battle" if reason == "battle_over" else "hide_seek"
	v.add_child(UIKit.label(Game.SIGHTINGS[sid][0], 30, UIKit.ACCENT))
	if reason == "seeker_win":
		v.add_child(UIKit.label("the entity caught everyone.", 20))
	elif reason == "hiders_win":
		v.add_child(UIKit.label("someone made it to the door. the hiders win.", 20))
	else:
		var names := kills.keys()
		names.sort_custom(func(a, b): return int(kills[a]) > int(kills[b]))
		if names.is_empty():
			v.add_child(UIKit.label("nobody got anyone.", 18))
		for i in names.size():
			v.add_child(UIKit.label("%d.  %s  -  %s" % [i + 1, names[i], Loc.t("%d kills") % int(kills[names[i]])], 22 if i == 0 else 18))
	if Net.is_host:
		v.add_child(UIKit.button("play again (everyone)", func(): Net.host_start("offices", false, "", sid), 20))
	else:
		v.add_child(UIKit.label("waiting for the host to start again...", 15, Color(0.6, 0.6, 0.6)))
	v.add_child(UIKit.button("title screen", func():
		Net.stop("")
		Game.to_menu(), 18))
