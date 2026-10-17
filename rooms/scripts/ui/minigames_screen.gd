class_name MinigamesScreen
extends Control
## the minigames, from the title screen: quick modes in a locked room, play until you lose,
## beat your best


func _ready() -> void:
	set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	UIKit.full_screen_bg(self, Color(0, 0, 0, 0.9))
	var p := UIKit.panel(Vector2(500, 0))
	UIKit.centered(self, p)
	var v := UIKit.vbox(12)
	p.add_child(v)
	v.add_child(UIKit.label("minigames", 28, UIKit.ACCENT))
	for id in Game.MINIGAMES:
		var info: Array = Game.MINIGAMES[id]
		var box := UIKit.vbox(4)
		box.add_child(UIKit.label(info[0], 20))
		var d := UIKit.label(info[1], 14, Color(0.7, 0.7, 0.7))
		d.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
		d.custom_minimum_size = Vector2(440, 0)
		box.add_child(d)
		var row := HBoxContainer.new()
		row.add_theme_constant_override("separation", 12)
		row.add_child(UIKit.button("play", func(): Game.start_run(false, false, "offices", {"minigame": id}), 18))
		var best := int(Save.data.get("mg_best_" + id, 0))
		row.add_child(UIKit.label(Loc.t("best: ") + MinigameResult.score_text(id, best), 15, Color(0.6, 0.6, 0.6)))
		box.add_child(row)
		v.add_child(box)
	v.add_child(UIKit.label("play them with friends: multiplayer, host a game, then pick a party game.", 13, Color(0.55, 0.55, 0.55)))
	v.add_child(UIKit.button("back", queue_free, 16))
