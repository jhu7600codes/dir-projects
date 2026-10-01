class_name CreditsScreen
extends Control
## credits. reads assets/credits.json automatically, warns about licenses that don't
## allow reuse or need attribution, and thanks the people who made the original games.

signal closed

const PATH := "res://assets/credits.json"
## licenses that are fine to reuse without attribution
const FREE := ["cc0", "cc0-1.0", "public domain"]
## licenses that do NOT allow reuse in a game like this (or need permission)
const BLOCKED := ["all rights reserved", "standard", "editorial", "cc-by-nd", "cc-by-nc-nd", "unknown", "personal use only"]


static func load_credits() -> Dictionary:
	if not FileAccess.file_exists(PATH):
		return {}
	var d = JSON.parse_string(FileAccess.get_file_as_string(PATH))
	return d if d is Dictionary else {}


## returns a warning string for a model's license, or "" if it's fine
static func license_warning(license: String) -> String:
	var l := license.to_lower().strip_edges()
	for b in BLOCKED:
		if l.begins_with(b):
			return "warning: '%s' does not allow reuse, don't ship this model" % license
	if l.contains("nc"):
		return "warning: '%s' is non-commercial only" % license
	if l in FREE:
		return ""
	if l.begins_with("cc-by") or l.begins_with("cc by"):
		return "needs attribution (shown on this screen)"
	return "check this license manually: '%s'" % license


## prints license warnings to the console, called once at startup
static func check_all() -> void:
	for m in load_credits().get("models", []):
		var w := license_warning(str(m.get("license", "unknown")))
		if w != "":
			push_warning("credits: %s by %s - %s" % [m.get("title", "?"), m.get("author", "?"), w])


func _ready() -> void:
	set_anchors_preset(Control.PRESET_FULL_RECT)
	UIKit.full_screen_bg(self, Color(0, 0, 0, 0.9))
	var p := UIKit.panel(Vector2(620, 0))
	UIKit.centered(self, p)
	var outer := UIKit.vbox(10)
	p.add_child(outer)
	outer.add_child(UIKit.label("credits", 28, UIKit.ACCENT))
	var scroll := ScrollContainer.new()
	scroll.custom_minimum_size = Vector2(600, minf(480, get_viewport_rect().size.y - 160))
	scroll.horizontal_scroll_mode = ScrollContainer.SCROLL_MODE_DISABLED
	outer.add_child(scroll)
	var v := UIKit.vbox(6)
	v.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	scroll.add_child(v)
	var c := load_credits()
	_section(v, "made by")
	for line in c.get("made_by", ["jhulian"]):
		_line(v, str(line))
	_section(v, "inspired by (thank you!)")
	for t in c.get("inspiration", []):
		_line(v, "%s - %s" % [t.get("name", ""), t.get("for", "")])
	_section(v, "models")
	var models: Array = c.get("models", [])
	if models.is_empty():
		_line(v, "none yet - everything is placeholder boxes", Color(0.5, 0.5, 0.5))
	for m in models:
		_line(v, "%s by %s (%s)" % [m.get("title", "?"), m.get("author", "?"), m.get("license", "unknown")])
		_line(v, "  " + str(m.get("url", "")), Color(0.5, 0.5, 0.6), 13)
		var w := license_warning(str(m.get("license", "unknown")))
		if w != "":
			_line(v, "  " + w, Color(1, 0.6, 0.3) if w.begins_with("warning") else Color(0.6, 0.8, 0.6), 13)
	_section(v, "other")
	for line in c.get("other", []):
		_line(v, str(line))
	_line(v, "this is an unofficial fan project. not affiliated with roblox, lsplash or nicorocks5555.", Color(0.6, 0.6, 0.6), 13)
	outer.add_child(UIKit.button("back", func():
		closed.emit()
		queue_free()))


func _section(v: Control, text: String) -> void:
	var l := UIKit.label(text, 16, UIKit.ACCENT)
	v.add_child(l)


func _line(v: Control, text: String, color := Color(0.85, 0.85, 0.85), size := 15) -> void:
	var l := UIKit.label(text, size, color)
	l.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	l.custom_minimum_size = Vector2(560, 0)
	v.add_child(l)
