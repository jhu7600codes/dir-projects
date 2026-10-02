extends CanvasLayer
## achievement list + unlock popup. unlocked ones are stored in Save.data.achievements

const LIST := {
	# the two you asked for
	"long_walk": ["welp, thats been a long walk.", "leave through an exit door"],
	"a1000": ["a-1000", "my legs hurt..."],
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
}

var _queue: Array[String] = []
var _showing := false
var _panel: PanelContainer
var _title: Label
var _desc: Label


func _ready() -> void:
	layer = 100
	process_mode = Node.PROCESS_MODE_ALWAYS
	_panel = PanelContainer.new()
	_panel.anchor_left = 1.0
	_panel.anchor_right = 1.0
	_panel.offset_left = -360
	_panel.offset_right = -16
	_panel.offset_top = 16
	_panel.modulate.a = 0.0
	_panel.mouse_filter = Control.MOUSE_FILTER_IGNORE
	var sb := StyleBoxFlat.new()
	sb.bg_color = Color(0.05, 0.05, 0.06, 0.9)
	sb.border_color = Color(0.95, 0.85, 0.4)
	sb.set_border_width_all(2)
	sb.set_content_margin_all(12)
	sb.set_corner_radius_all(6)
	_panel.add_theme_stylebox_override("panel", sb)
	var box := VBoxContainer.new()
	_panel.add_child(box)
	var small := Label.new()
	small.text = "Achievement unlocked"
	small.add_theme_font_size_override("font_size", 13)
	small.modulate = Color(0.95, 0.85, 0.4)
	box.add_child(small)
	_title = Label.new()
	_title.add_theme_font_size_override("font_size", 20)
	box.add_child(_title)
	_desc = Label.new()
	_desc.autowrap_mode = TextServer.AUTOWRAP_WORD
	_desc.modulate = Color(0.8, 0.8, 0.8)
	box.add_child(_desc)
	add_child(_panel)


func has(id: String) -> bool:
	return Save.data.achievements.has(id)


func unlock(id: String) -> void:
	if Save.locked or not LIST.has(id) or has(id):
		return
	Save.data.achievements[id] = int(Time.get_unix_time_from_system())
	Save.write()
	_queue.append(id)
	if not _showing:
		_show_next()


func _show_next() -> void:
	if _queue.is_empty():
		_showing = false
		return
	_showing = true
	var id: String = _queue.pop_front()
	_title.text = UIKit.cap(LIST[id][0])
	_desc.text = UIKit.cap(LIST[id][1])
	Game.play_ui("achievement")
	var tw := create_tween()
	tw.tween_property(_panel, "modulate:a", 1.0, 0.3)
	tw.tween_interval(3.5)
	tw.tween_property(_panel, "modulate:a", 0.0, 0.5)
	tw.tween_callback(_show_next)
