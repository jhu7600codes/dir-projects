class_name SaveTransfer
extends Control
## settings -> move your save: turns both save profiles into one text code you can copy,
## and takes a code back in. for a new phone, the apk <-> web version, or a reinstall.

var _out: TextEdit
var _in: TextEdit
var _status: Label
var _import_btn: Button
var _armed := false


func _ready() -> void:
	set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	UIKit.full_screen_bg(self, Color(0, 0, 0, 0.88))
	var p := UIKit.panel(Vector2(600, 0))
	UIKit.centered(self, p)
	var v := UIKit.vbox(10)
	p.add_child(v)
	v.add_child(UIKit.label("move your save", 28, UIKit.ACCENT))
	var help := UIKit.label("copy the code, then paste it into The ROOMS on your other device (or after reinstalling).", 15, Color(0.7, 0.7, 0.7))
	help.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	help.custom_minimum_size = Vector2(560, 0)
	v.add_child(help)
	_out = _box(false)
	_out.text = Save.export_code()
	v.add_child(_out)
	v.add_child(UIKit.button("copy the code", func():
		DisplayServer.clipboard_set(_out.text)
		_say("copied.", false), 18))
	# importing mid-run would pull the save out from under the run
	if Game.player == null:
		v.add_child(UIKit.label("paste a code here:", 16))
		_in = _box(true)
		v.add_child(_in)
		var row := HBoxContainer.new()
		row.add_theme_constant_override("separation", 10)
		row.add_child(UIKit.button("paste", func():
			_in.text = DisplayServer.clipboard_get()
			_disarm(), 16))
		_import_btn = UIKit.button("load this save", _import, 16)
		row.add_child(_import_btn)
		v.add_child(row)
		_in.text_changed.connect(_disarm)
	_status = UIKit.label("", 15, UIKit.ACCENT)
	_status.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	_status.custom_minimum_size = Vector2(560, 0)
	v.add_child(_status)
	v.add_child(UIKit.button("back", queue_free, 16))


func _box(editable: bool) -> TextEdit:
	var t := TextEdit.new()
	t.custom_minimum_size = Vector2(560, 90)
	t.wrap_mode = TextEdit.LINE_WRAPPING_BOUNDARY
	t.editable = editable
	t.add_theme_font_size_override("font_size", 12)
	return t


func _say(text: String, bad: bool) -> void:
	_status.text = Loc.t(text)
	_status.add_theme_color_override("font_color", Color(1, 0.45, 0.4) if bad else UIKit.ACCENT)


func _disarm() -> void:
	_armed = false
	_import_btn.text = UIKit.cap("load this save")


## tap once to check the code, tap again to really replace your save
func _import() -> void:
	if _in.text.strip_edges() == "":
		_say("paste a code first.", true)
		return
	if not _armed:
		_armed = true
		_import_btn.text = UIKit.cap("tap again: this replaces your save")
		return
	var err := Save.import_code(_in.text)
	_disarm()
	if err != "":
		_say(err, true)
		return
	_say("save loaded.", false)
	# the title screen shows best door, gold and continue: rebuild it with the new save
	if get_tree().current_scene and get_tree().current_scene.scene_file_path.ends_with("main_menu.tscn"):
		get_tree().create_timer(0.8).timeout.connect(func(): get_tree().reload_current_scene())
