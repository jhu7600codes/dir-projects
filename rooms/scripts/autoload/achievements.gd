extends CanvasLayer
## achievement list + unlock popup. unlocked ones are stored in Save.data.achievements

const LIST := {
	# the two you asked for
	"long_walk": ["welp, thats been a long walk.", "leave through an exit door"],
	"a1000": ["a-1000", "my legs hurt..."],
	"going_home": ["going home", "get on the bus outside Morse"],
	# one per entity
	"survive_a60": ["static on the line", "hide from a-60 and live"],
	"survive_a60b": ["ping, pong, gone", "outlast a-60b for twenty rooms"],
	"survive_a90": ["statue mode", "don't move a muscle when a-90 shows up"],
	"survive_a90b": ["simon says", "do everything a-90b tells you"],
	"survive_a120": ["wait for the quiet", "sit out a-120, rebounds and all"],
	"survive_a200": ["reverse uno", "survive the happy scribble until she leaves"],
	# extra ones, inspired by the rooms: revisited badge list
	"first_run": ["clocking in", "start your first run"],
	"first_death": ["the first of many", "die for the first time"],
	"deaths_10": ["used to it by now", "die 10 times"],
	"deaths_50": ["regular customer", "die 50 times"],
	"rich": ["office savings", "have 500 gold saved up"],
	"no_lockers": ["who needs lockers", "reach an exit without ever hiding"],
	"read_walls": ["someone was here", "walk the whole a-100 corridor"],
	"lights_out": ["lights out", "reach a-150"],
	"shake_it": ["shake it", "buy a shakelight"],
	"early_bird": ["you're early", "meet a-90 in a-000"],
	"management": ["management access", "find the management key and go down into the wires"],
	"wired_up": ["wired up", "power the whole office and ride the elevator back up"],
}

var _queue: Array = []  # [header, title, description, color]
var _showing := false
var _card: Control
var _icon: Control
var _small: Label
var _title: Label
var _desc: Label
var _bar: ColorRect
var _kind := "achievement"
var _color := Color(0.95, 0.85, 0.4)

const CARD_W := 400.0
const CARD_H := 108.0


## the popup card: slides in from the right edge, an icon on the left (a star badge for
## achievements, a torn page for journal pages), a colored edge, and a thin line along the
## bottom that runs out while it's on screen
func _ready() -> void:
	layer = 100
	process_mode = Node.PROCESS_MODE_ALWAYS
	UITheme.fonts()
	_card = Control.new()
	_card.anchor_left = 1.0
	_card.anchor_right = 1.0
	_card.offset_top = 20
	_card.offset_bottom = 20 + CARD_H
	_card.offset_left = 40
	_card.offset_right = 40 + CARD_W
	_card.mouse_filter = Control.MOUSE_FILTER_IGNORE
	_card.draw.connect(_draw_card)
	add_child(_card)
	_icon = Control.new()
	_icon.position = Vector2(14, 14)
	_icon.size = Vector2(64, 64)
	_icon.mouse_filter = Control.MOUSE_FILTER_IGNORE
	_icon.draw.connect(_draw_icon)
	_card.add_child(_icon)
	var box := VBoxContainer.new()
	box.position = Vector2(92, 12)
	box.size = Vector2(CARD_W - 104, CARD_H - 20)
	box.add_theme_constant_override("separation", 0)
	box.mouse_filter = Control.MOUSE_FILTER_IGNORE
	_card.add_child(box)
	_small = Label.new()
	_small.add_theme_font_override("font", UITheme.title_font)
	_small.add_theme_font_size_override("font_size", 13)
	box.add_child(_small)
	_title = Label.new()
	_title.add_theme_font_override("font", UITheme.title_font)
	_title.add_theme_font_size_override("font_size", 22)
	_title.text_overrun_behavior = TextServer.OVERRUN_TRIM_ELLIPSIS
	box.add_child(_title)
	_desc = Label.new()
	_desc.add_theme_font_size_override("font_size", 14)
	_desc.add_theme_color_override("font_color", Color(0.72, 0.72, 0.74))
	_desc.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	_desc.max_lines_visible = 2
	box.add_child(_desc)
	_bar = ColorRect.new()
	_bar.position = Vector2(0, CARD_H - 3)
	_bar.size = Vector2(CARD_W, 3)
	_bar.mouse_filter = Control.MOUSE_FILTER_IGNORE
	_card.add_child(_bar)
	_card.visible = false


func _draw_card() -> void:
	var r := Rect2(Vector2.ZERO, Vector2(CARD_W, CARD_H))
	# soft shadow, the dark glass card, a colored edge on the left
	for i in 6:
		_card.draw_rect(r.grow(float(6 - i) * 2.0), Color(0, 0, 0, 0.06))
	_card.draw_rect(r, Color(0.04, 0.04, 0.05, 0.94))
	_card.draw_rect(Rect2(0, 0, 4, CARD_H), _color)
	_card.draw_rect(r, Color(1, 1, 1, 0.06), false, 1.0)


func _draw_icon() -> void:
	var c := Vector2(32, 32)
	_icon.draw_circle(c, 31, Color(_color, 0.12))
	_icon.draw_arc(c, 30, 0, TAU, 40, Color(_color, 0.6), 2.0)
	if _kind == "journal":
		# a torn notebook page with handwriting lines
		var page := PackedVector2Array([c + Vector2(-14, -18), c + Vector2(14, -18), c + Vector2(14, 14), c + Vector2(9, 18), c + Vector2(3, 15), c + Vector2(-3, 18), c + Vector2(-9, 15), c + Vector2(-14, 18)])
		_icon.draw_colored_polygon(page, Color(0.92, 0.9, 0.82))
		for i in 4:
			_icon.draw_line(c + Vector2(-9, -10 + i * 6), c + Vector2(9 - (i % 2) * 5, -10 + i * 6), Color(0.3, 0.35, 0.6, 0.8), 1.5)
	else:
		# a star badge
		var pts := PackedVector2Array()
		for i in 10:
			var rr := 20.0 if i % 2 == 0 else 8.5
			var a := -PI / 2 + i * PI / 5
			pts.append(c + Vector2(cos(a), sin(a)) * rr)
		_icon.draw_colored_polygon(pts, _color)


func has(id: String) -> bool:
	return Save.data.achievements.has(id)


func unlock(id: String) -> void:
	if Save.locked or not LIST.has(id) or has(id):
		return
	Save.data.achievements[id] = int(Time.get_unix_time_from_system())
	Save.write()
	popup("achievement unlocked", LIST[id][0], LIST[id][1], Color(0.95, 0.85, 0.4))


## a popup in the corner (achievements, journal pages). they queue up one after another
func popup(header: String, title: String, desc: String, color: Color) -> void:
	_queue.append([header, title, desc, color])
	if not _showing:
		_show_next()


func _show_next() -> void:
	if _queue.is_empty():
		_showing = false
		_card.visible = false
		return
	_showing = true
	var p: Array = _queue.pop_front()
	_kind = "journal" if str(p[0]).begins_with("journal") else "achievement"
	_color = p[3]
	_small.text = str(p[0]).to_upper()
	_small.add_theme_color_override("font_color", _color)
	_title.text = UIKit.cap(p[1])
	_desc.text = UIKit.cap(p[2])
	_bar.color = Color(_color, 0.8)
	_bar.size.x = CARD_W
	_card.queue_redraw()
	_icon.queue_redraw()
	_card.visible = true
	_card.offset_left = 40
	_card.offset_right = 40 + CARD_W
	_icon.scale = Vector2(0.6, 0.6)
	_icon.pivot_offset = Vector2(32, 32)
	Game.play_ui("achievement")
	var tw := create_tween()
	tw.tween_property(_card, "offset_left", -CARD_W - 20, 0.45).set_trans(Tween.TRANS_BACK).set_ease(Tween.EASE_OUT)
	tw.parallel().tween_property(_card, "offset_right", -20.0, 0.45).set_trans(Tween.TRANS_BACK).set_ease(Tween.EASE_OUT)
	tw.parallel().tween_property(_icon, "scale", Vector2.ONE, 0.5).set_trans(Tween.TRANS_ELASTIC).set_ease(Tween.EASE_OUT).set_delay(0.15)
	tw.tween_property(_bar, "size:x", 0.0, 4.0)
	tw.tween_property(_card, "offset_left", 40.0, 0.35).set_trans(Tween.TRANS_QUAD).set_ease(Tween.EASE_IN)
	tw.parallel().tween_property(_card, "offset_right", 40.0 + CARD_W, 0.35).set_trans(Tween.TRANS_QUAD).set_ease(Tween.EASE_IN)
	tw.tween_callback(_show_next)
