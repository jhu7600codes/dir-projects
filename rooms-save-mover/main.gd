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
var _log: Label  # what root actually saw, so a failed read can be figured out from a screenshot


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
	_log = _label("", 20, Color(0.55, 0.55, 0.6))
	v.add_child(_log)


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
	_log.text = ""
	if OS.get_name() != "Android" and OS.get_environment("SAVE_MOVER_TEST") == "":
		_say("this only works on a rooted android phone.", true)
		return
	var r := _run_search()
	if not r.root:
		_say("no root. allow it in your root app (magisk etc) when it asks, then try again.", true)
		_log.text = "details (send this to jhu):\n" + r.log
		return
	var all := {"v": 1}
	for p in FILES:
		var parsed = JSON.parse_string(r.files.get(p, "")) if r.files.get(p, "") != "" else null
		if parsed is Dictionary:
			all[p] = parsed
	if not (all.get("main") is Dictionary):
		_say("no old save found. is the old The ROOMS still installed? (uninstalling deletes it)", true)
		_log.text = "details (send this to jhu):\n" + r.log
		return
	var raw := JSON.stringify(all).to_utf8_buffer()
	_out.text = CODE_PREFIX + Marshalls.raw_to_base64(raw.compress(FileAccess.COMPRESSION_GZIP))
	_say("got it: best door %d, %d gold. copy the code." % [int(all.main.get("best_door", 0)), int(all.main.get("gold", 0))], false)


## the search runs as a little script file, so nothing in it gets mangled on the way to su.
## it prints "@@main" / "@@admin" followed by each file, and "#" lines about what it saw
const SCRIPT := """
for d in /data/data/PKG /data/user/0/PKG /data_mirror/data_ce/null/0/PKG /data/media/0/Android/data/PKG /sdcard/Android/data/PKG /storage/emulated/0/Android/data/PKG; do
	[ -d "$d" ] && echo "# found $d"
done
for n in save.json save_admin.json; do
	for d in /data/data/PKG /data/user/0/PKG /data_mirror/data_ce/null/0/PKG /data/media/0/Android/data/PKG /sdcard/Android/data/PKG /storage/emulated/0/Android/data/PKG; do
		f=$(find "$d" -name "$n" 2>/dev/null | head -n 1)
		if [ -n "$f" ]; then
			echo "# file $f"
			echo "@@$n"
			cat "$f"
			echo ""
			echo "@@end"
			break
		fi
	done
done
echo "# done"
"""


func _run_search() -> Dictionary:
	var test_dir := OS.get_environment("SAVE_MOVER_TEST")
	var text := ""
	var code := 0
	if test_dir != "":
		for n in ["save.json", "save_admin.json"]:
			var path := test_dir.path_join(n)
			if FileAccess.file_exists(path):
				text += "@@%s\n%s\n@@end\n" % [n, FileAccess.get_file_as_string(path)]
	else:
		var script_path := OS.get_user_data_dir().path_join("search.sh")
		var f := FileAccess.open(script_path, FileAccess.WRITE)
		f.store_string(SCRIPT.replace("PKG", PKG))
		f.close()
		# magisk's --mount-master runs outside this app's sandbox, where every app's folder is visible
		for args in [["--mount-master", "-c", "sh " + script_path], ["-c", "sh " + script_path]]:
			var out := []
			code = OS.execute("su", args, out, true)
			text = "" if out.is_empty() else str(out[0])
			if code == 0 and text.contains("# done"):
				break
	var files := {}
	var log := []
	var current := ""
	var buf := []
	for line in text.split("\n"):
		if line.begins_with("@@end"):
			files[current] = "\n".join(buf)
			current = ""
			buf.clear()
		elif line.begins_with("@@"):
			current = line.substr(2).strip_edges()
		elif current != "":
			buf.append(line)
		else:
			log.append(line)
	var by_profile := {}
	for p in FILES:
		by_profile[p] = files.get(FILES[p], "")
	return {"root": code == 0, "files": by_profile, "log": "exit %d\n%s" % [code, "\n".join(log).left(1500)]}
