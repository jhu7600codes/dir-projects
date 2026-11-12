extends Control
## ROOMS save mover (root only). the old The ROOMS (signed with the lost key) keeps its save
## in its private folder. with root we can read it and turn it into the same code as the
## game's settings -> move your save, so it can go into the new version.
## it has to run BEFORE the old game is uninstalled: uninstalling deletes the files.

const PKG := "com.jhu.roomsthehallway"
const FILES := {"main": "save.json", "admin": "save_admin.json"}
const CODE_PREFIX := "ROOMS1:"

var _out: TextEdit
var _status: Label


func _ready() -> void:
	var bg := ColorRect.new()
	bg.color = Color(0.05, 0.047, 0.055)
	bg.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	add_child(bg)
	var m := MarginContainer.new()
	m.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	for side in ["left", "right", "top", "bottom"]:
		m.add_theme_constant_override("margin_" + side, 36)
	add_child(m)
	var v := VBoxContainer.new()
	v.add_theme_constant_override("separation", 22)
	m.add_child(v)
	v.add_child(_label("ROOMS save mover", 56, Color(0.85, 0.7, 0.35)))
	v.add_child(_label("for rooted phones. reads the save of the OLD The ROOMS (still installed) and gives you a code. then uninstall the old game, install the new one, and paste the code in settings -> move your save.", 30, Color(0.75, 0.75, 0.75)))
	v.add_child(_button("read the old save (asks for root)", _read))
	_out = TextEdit.new()
	_out.custom_minimum_size = Vector2(0, 360)
	_out.wrap_mode = TextEdit.LINE_WRAPPING_BOUNDARY
	_out.editable = false
	_out.add_theme_font_size_override("font_size", 26)
	v.add_child(_out)
	v.add_child(_button("copy the code", func():
		if _out.text != "":
			DisplayServer.clipboard_set(_out.text)
			_say("copied. now paste it into the new The ROOMS.", false)))
	_status = _label("", 30, Color(0.85, 0.7, 0.35))
	v.add_child(_status)


func _label(text: String, size: int, color: Color) -> Label:
	var l := Label.new()
	l.text = text
	l.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	l.add_theme_font_size_override("font_size", size)
	l.add_theme_color_override("font_color", color)
	return l


func _button(text: String, on_press: Callable) -> Button:
	var b := Button.new()
	b.text = text
	b.custom_minimum_size = Vector2(0, 110)
	b.add_theme_font_size_override("font_size", 34)
	b.pressed.connect(on_press)
	return b


func _say(text: String, bad: bool) -> void:
	_status.text = text
	_status.add_theme_color_override("font_color", Color(1, 0.45, 0.4) if bad else Color(0.85, 0.7, 0.35))


func _read() -> void:
	_out.text = ""
	if OS.get_name() != "Android" and OS.get_environment("SAVE_MOVER_TEST") == "":
		_say("this only works on a rooted android phone.", true)
		return
	var all := {"v": 1}
	var root_ok := false
	for p in FILES:
		var r := _read_file(FILES[p])
		if r.root:
			root_ok = true
		var parsed = JSON.parse_string(r.text) if r.text != "" else null
		if parsed is Dictionary:
			all[p] = parsed
	if not root_ok:
		_say("no root. allow it in your root app (magisk etc) when it asks, then try again.", true)
		return
	if not (all.get("main") is Dictionary):
		_say("no old save found. is the old The ROOMS still installed? (uninstalling deletes it)", true)
		return
	var raw := JSON.stringify(all).to_utf8_buffer()
	_out.text = CODE_PREFIX + Marshalls.raw_to_base64(raw.compress(FileAccess.COMPRESSION_GZIP))
	_say("got it: best door %d, %d gold. copy the code." % [int(all.main.get("best_door", 0)), int(all.main.get("gold", 0))], false)


## reads one of the game's save files with su. finds it wherever the app keeps its files
func _read_file(name: String) -> Dictionary:
	var test_dir := OS.get_environment("SAVE_MOVER_TEST")
	if test_dir != "":
		var path := test_dir.path_join(name)
		return {"root": true, "text": FileAccess.get_file_as_string(path) if FileAccess.file_exists(path) else ""}
	var out := []
	var cmd := "for d in /data/user/0/%s /data/data/%s; do f=$(find \"$d\" -name '%s' 2>/dev/null | head -n 1); if [ -n \"$f\" ]; then cat \"$f\"; exit 0; fi; done; exit 3" % [PKG, PKG, name]
	var code := OS.execute("su", ["-c", cmd], out, true)
	var text: String = "" if out.is_empty() else str(out[0])
	# exit 3: root worked but the file isn't there. anything else non-zero: no root
	return {"root": code == 0 or code == 3, "text": text.strip_edges() if code == 0 else ""}
