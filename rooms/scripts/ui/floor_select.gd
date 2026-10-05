class_name FloorSelect
extends Control
## shown when you press play: pick a floor. each card shows the floor's first door.
##   the offices - the endless office, a-000 to a-1000
##   the wires   - the maintenance floor under it (50 doors, no timer). finish it and the
##                 elevator takes you up into the fixed office for the rest of that run

signal closed

const FLOORS := [
	["offices", "the offices", "the endless office. a-000 to a-1000. hide, run, survive.", "res://assets/branding/floor_offices.png"],
	["wires", "the wires", "the maintenance floor under the office. 50 doors, no timer. flip every breaker, power the elevator and ride it up.", "res://assets/branding/floor_wires.png"],
]

var admin := false


func _ready() -> void:
	set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	UIKit.full_screen_bg(self, Color(0, 0, 0, 0.88))
	var col := UIKit.vbox(16)
	col.alignment = BoxContainer.ALIGNMENT_CENTER
	col.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	add_child(col)
	var title := UIKit.label("choose a floor", 30, UIKit.ACCENT)
	title.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	col.add_child(title)
	var row := HBoxContainer.new()
	row.alignment = BoxContainer.ALIGNMENT_CENTER
	row.add_theme_constant_override("separation", 24)
	col.add_child(row)
	var vs := get_viewport_rect().size
	var card_w := minf(480.0, (vs.x - 96.0) / 2.0)
	for f in FLOORS:
		row.add_child(_card(f, card_w))
	var back := UIKit.button("back", func():
		closed.emit()
		queue_free(), 18)
	back.custom_minimum_size = Vector2(200, 44)
	back.size_flags_horizontal = Control.SIZE_SHRINK_CENTER
	col.add_child(back)


func _card(f: Array, w: float) -> Control:
	var p := UIKit.panel(Vector2(w, 0))
	var v := UIKit.vbox(8)
	p.add_child(v)
	var img := TextureRect.new()
	img.texture = load(f[3])
	img.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
	img.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_COVERED
	img.custom_minimum_size = Vector2(w - 24, (w - 24) * 9.0 / 16.0)
	v.add_child(img)
	v.add_child(UIKit.label(f[1], 24, UIKit.ACCENT))
	var d := UIKit.label(f[2], 14, Color(0.75, 0.75, 0.75))
	d.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	d.custom_minimum_size = Vector2(w - 24, 0)
	v.add_child(d)
	if f[0] == "wires" and int(Save.data.get("wires_best", 0)) > 0:
		v.add_child(UIKit.label("best: W-%02d" % int(Save.data.wires_best), 13, Color(0.6, 0.6, 0.6)))
	var id: String = f[0]
	v.add_child(UIKit.button("play", func(): Game.start_run(admin, false, id), 20))
	return p
