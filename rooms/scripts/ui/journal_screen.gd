class_name JournalScreen
extends Control
## the worker's journal. a handwritten notebook with the backstory. the first pages are
## open from the start, the rest unlock as your best door gets further.

signal closed

## [title, text, best door needed to read it]
const PAGES := [
	["day 1", "new job. junior data clerk, floor a.\n\nthe elevator only had one button. the lady at the front desk said i'd \"get the hang of the layout\" and then she went back to typing. she didn't look up once.\n\nmy badge doesn't have my name on it. it just says worker. guess they'll fix that.", 0],
	["day 3", "i keep losing my way back to the lobby. every hallway has the same yellow number plates. a-014. a-015. a-016. i don't remember this many rooms on the tour.\n\nthere's a little machine in the lobby that sells shakelights. why would an office need those?\n\nnobody else has shown up to work yet.", 0],
	["day ?", "the clock in the break room stopped. or maybe i stopped checking it.\n\nsomething crackled behind me today. like a radio between stations, getting louder. i hid in a locker and it went past so fast the door rattled.\n\nit was a face. it was a lot of faces.", 50],
	["the walls", "i wrote on the walls back in that long hallway. i don't remember doing it. i went back to read it and the hallway wasn't there anymore. just more rooms.\n\nmy handwriting looked so scared. i don't think i'm that scared. i think i'm fine.", 100],
	["dark", "the lights stopped working a while ago. i keep the flashlight pointed at the floor so i don't have to see the corners.\n\nsometimes there's a knock and everything goes quiet. even my own breathing. i freeze. freezing is the only thing that works. don't move. don't even look around.", 150],
	["the warm door", "i found a door with light coming from behind it. warm light, like outside in the evening. i could hear something on the other side. it sounded nice.\n\ni didn't go through. what if it's another trick.\n\nwhat if it isn't, and i'm just scared of leaving now.", 200],
	["her", "there's a smiley face that someone drew. badly. it moves. it talks. it laughs.\n\nwhen it glows white you hide. when it glows purple you get OUT of the locker. i learned that the hard way. i'm still learning things the hard way.", 500],
	["a-1000", "there's a bridge. nothing around it, just dark. there's a door at the end and it's glowing.\n\nif you're reading this, i made it.\n\nor i didn't, and you're me, and you're about to start again at a-000 with a new badge that says worker.\n\nkeep walking.", 1000],
]

const PAPER := Color(0.93, 0.89, 0.78)
const INK := Color(0.12, 0.14, 0.28)
const LINE := Color(0.55, 0.65, 0.8, 0.35)

var _page := 0
var _paper: Panel
var _title: Label
var _text: Label
var _num: Label
var _prev: Button
var _next: Button


func _ready() -> void:
	set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	UIKit.full_screen_bg(self, Color(0, 0, 0, 0.75))
	var vs := get_viewport_rect().size
	var size := Vector2(minf(560, vs.x - 40), minf(600, vs.y - 110))
	var col := VBoxContainer.new()
	col.add_theme_constant_override("separation", 18)
	UIKit.centered(self, col)
	_paper = Panel.new()
	_paper.custom_minimum_size = size
	_paper.pivot_offset = size / 2.0
	_paper.rotation = -0.015
	var sb := StyleBoxFlat.new()
	sb.bg_color = PAPER
	sb.set_corner_radius_all(3)
	sb.shadow_color = Color(0, 0, 0, 0.5)
	sb.shadow_size = 18
	_paper.add_theme_stylebox_override("panel", sb)
	_paper.draw.connect(_draw_lines)
	col.add_child(_paper)
	var hand := Assets.font("handwriting")
	_title = _hand_label(hand, 40)
	_title.position = Vector2(70, 16)
	_title.size = Vector2(size.x - 100, 50)
	_paper.add_child(_title)
	_text = _hand_label(hand, 26)
	_text.position = Vector2(70, 80)
	_text.size = Vector2(size.x - 100, size.y - 120)
	_text.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	_paper.add_child(_text)
	_num = _hand_label(hand, 22)
	_num.position = Vector2(size.x - 90, size.y - 40)
	_paper.add_child(_num)
	var row := HBoxContainer.new()
	row.add_theme_constant_override("separation", 12)
	row.alignment = BoxContainer.ALIGNMENT_CENTER
	col.add_child(row)
	_prev = UIKit.button("< previous", func(): _turn(-1), 18)
	_next = UIKit.button("next >", func(): _turn(1), 18)
	row.add_child(_prev)
	row.add_child(_next)
	row.add_child(UIKit.button("close", _close, 18))
	_show()


func _close() -> void:
	closed.emit()
	queue_free()


func _hand_label(f: Font, size: int) -> Label:
	var l := Label.new()
	l.add_theme_font_override("font", f)
	l.add_theme_font_size_override("font_size", size)
	l.add_theme_color_override("font_color", INK)
	return l


## ruled notebook lines + the red margin
func _draw_lines() -> void:
	var s := _paper.size
	var y := 74.0
	while y < s.y - 20:
		_paper.draw_line(Vector2(10, y), Vector2(s.x - 10, y), LINE, 1.0)
		y += 33.0
	_paper.draw_line(Vector2(58, 0), Vector2(58, s.y), Color(0.8, 0.3, 0.3, 0.5), 1.5)
	for i in 3:
		_paper.draw_circle(Vector2(26, s.y * (0.2 + i * 0.3)), 9, Color(0.05, 0.05, 0.06))


static func unlocked(i: int) -> bool:
	return int(Save.data.best_door) >= int(PAGES[i][2]) or (i == PAGES.size() - 1 and Achievements.has("a1000"))


func _turn(d: int) -> void:
	_page = clampi(_page + d, 0, PAGES.size() - 1)
	Game.play_ui("pickup")
	_show()


func _show() -> void:
	var p: Array = PAGES[_page]
	if unlocked(_page):
		_title.text = p[0]
		_text.text = p[1]
		_text.modulate = Color.WHITE
	else:
		_title.text = "???"
		_text.text = "the next pages are stuck together.\n\n(get to %s to read this one)" % Game.a_label(int(p[2]))
		_text.modulate = Color(1, 1, 1, 0.55)
	_num.text = "%d / %d" % [_page + 1, PAGES.size()]
	_prev.disabled = _page == 0
	_next.disabled = _page == PAGES.size() - 1
