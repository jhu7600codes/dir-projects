class_name SightingsScreen
extends Control
## the sightings tab on the title screen: ghlicht's versions of the office. the first
## time you open it, ghlicht introduces himself.

const INTRO := [
	"me is Ghlicht.",
	"me is exist today.",
	"me kick player from void. me not like that.",
	"me want to make player hallucination.",
	"me make Sightings. player like.",
	"me and player is friend.",
	"me still don't like player fall into void.",
]

var _board: VBoxContainer
var _board_title: Label


func _ready() -> void:
	set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	UIKit.full_screen_bg(self, Color(0, 0.02, 0.04, 0.93))
	if not Save.data.get("ghlicht_intro", false):
		_intro()
	else:
		_menu()


## ghlicht talks, one line at a time. tap / click to go faster
func _intro() -> void:
	var l := UIKit.label("", 28, Color(0.6, 0.9, 1.0))
	l.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	l.set_anchors_and_offsets_preset(Control.PRESET_CENTER)
	l.grow_horizontal = Control.GROW_DIRECTION_BOTH
	l.grow_vertical = Control.GROW_DIRECTION_BOTH
	add_child(l)
	Game.play_ui("glitch")
	for line in INTRO:
		l.text = Loc.t(line)
		l.modulate.a = 0.0
		var tw := create_tween()
		tw.tween_property(l, "modulate:a", 1.0, 0.3)
		await get_tree().create_timer(2.4).timeout
		if randf() < 0.4:
			l.position.x += randf_range(-8, 8)
	Save.data.ghlicht_intro = true
	Save.write()
	l.queue_free()
	_menu()


func _menu() -> void:
	var scroll := ScrollContainer.new()
	scroll.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	scroll.horizontal_scroll_mode = ScrollContainer.SCROLL_MODE_DISABLED
	add_child(scroll)
	var center := CenterContainer.new()
	center.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	scroll.add_child(center)
	var p := UIKit.panel(Vector2(560, 0))
	center.add_child(p)
	var v := UIKit.vbox(10)
	p.add_child(v)
	v.add_child(UIKit.label("sightings", 30, Color(0.6, 0.9, 1.0)))
	v.add_child(UIKit.label(Loc.t("made by ghlicht. ghlicht parts found: %d") % int(Save.data.get("ghlicht_parts", 0)), 14, Color(0.55, 0.7, 0.8)))
	for id in Game.SIGHTINGS:
		var info: Array = Game.SIGHTINGS[id]
		var box := UIKit.vbox(4)
		box.add_child(UIKit.label(info[0], 20))
		var d := UIKit.label(info[1], 14, Color(0.7, 0.7, 0.7))
		d.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
		d.custom_minimum_size = Vector2(500, 0)
		box.add_child(d)
		var row := HBoxContainer.new()
		row.add_theme_constant_override("separation", 12)
		if id in ["hide_seek"]:
			row.add_child(UIKit.label("play it from multiplayer.", 14, Color(0.55, 0.55, 0.55)))
		elif id == "daily" and str(Save.data.get("daily_date", "")) == Sightings.today():
			row.add_child(UIKit.label(Loc.t("done for today: ") + Game.a_label(int(Save.data.get("daily_best", 0))), 15, Color(0.6, 0.6, 0.6)))
		else:
			row.add_child(UIKit.button("play", _play.bind(id), 18))
			if id == "battle":
				row.add_child(UIKit.label("(with friends: multiplayer)", 13, Color(0.55, 0.55, 0.55)))
		var best := int(Save.data.get("s_best_" + id, 0))
		if best > 0 and id != "hide_seek":
			row.add_child(UIKit.label(Loc.t("best: ") + Game.a_label(best), 15, Color(0.6, 0.6, 0.6)))
		if id in ["daily", "endless", "battle"]:
			row.add_child(UIKit.button("board", _show_board.bind(id), 15))
		box.add_child(row)
		v.add_child(box)
	_board_title = UIKit.label("", 18, UIKit.ACCENT)
	v.add_child(_board_title)
	_board = UIKit.vbox(2)
	v.add_child(_board)
	var nr := HBoxContainer.new()
	nr.add_child(UIKit.label("name on the boards:  ", 14, Color(0.6, 0.6, 0.6)))
	var name_edit := LineEdit.new()
	name_edit.text = Leaderboard.player_name()
	name_edit.max_length = 16
	name_edit.custom_minimum_size = Vector2(220, 34)
	name_edit.text_changed.connect(func(t):
		Save.data.mp_name = t
		Save.write())
	nr.add_child(name_edit)
	v.add_child(nr)
	v.add_child(UIKit.button("back", queue_free, 16))


func _play(id: String) -> void:
	Game.start_run(false, false, "offices", {"sightings": id})


func _show_board(id: String) -> void:
	_board_title.text = Loc.t(Game.SIGHTINGS[id][0]) + Loc.t(" - top 10") + (Loc.t(" today") if id == "daily" else "")
	for c in _board.get_children():
		c.queue_free()
	_board.add_child(UIKit.label("loading...", 14, Color(0.6, 0.6, 0.6)))
	Leaderboard.top(get_tree(), id, Sightings.today() if id == "daily" else "", func(ok: bool, rows: Array):
		if not is_instance_valid(_board):
			return
		for c in _board.get_children():
			c.queue_free()
		if not ok:
			_board.add_child(UIKit.label("can't reach the boards right now.", 14, Color(0.8, 0.5, 0.4)))
			return
		if rows.is_empty():
			_board.add_child(UIKit.label("nobody yet. be the first.", 14, Color(0.6, 0.6, 0.6)))
		for i in rows.size():
			_board.add_child(UIKit.label("%d.  %s  -  %s" % [i + 1, rows[i][0], Game.a_label(int(rows[i][1]))], 16)))
