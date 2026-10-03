class_name CuriousLight
extends CanvasLayer
## the curious light: after you die, a warm yellow light shows up and tells you what
## went wrong, a line at a time. tap / click / any key skips to the next line.
## emits done when it's finished talking.

signal done

const COLOR := Color(1.0, 0.86, 0.45)
const LINE_TIME := 3.2  # each line stays this long unless you skip it

const NAMES := {
	"a60": "a-60", "a60b": "a-60b", "a90": "a-90", "a90b": "a-90b", "a120": "a-120", "a200": "a-200",
	"rush": "Rush", "ambush": "Ambush", "eyes": "Eyes", "screech": "Screech", "figure": "Figure", "seek": "Seek",
}
## cause -> detail -> lines. "" is the fallback for a cause.
const HINTS := {
	"a60": {
		"": ["it comes from behind, right after you walk into a room with lockers.", "when the lights flicker and you hear crackling, get into a locker right away."],
		"left_early": ["you got out of the locker too early.", "wait until the hissing is completely gone before you come out."],
	},
	"a120": {
		"": ["it comes from the rooms ahead of you. listen for the clanging.", "hide as soon as you hear it, and don't come out until it's silent."],
		"left_early": ["you came out before it was gone. it likes to come back.", "wait until the clanging has stopped completely."],
	},
	"a200": {
		"": ["it was glowing white, and you weren't hiding.", "white means hide in a locker."],
		"in_locker": ["it turned purple, and you stayed in your locker.", "purple means get out of the locker. right away."],
		"left_early": ["you came out while it was still white.", "white means stay hidden, purple means get out."],
	},
	"a60b": {
		"": ["it doesn't get tired, and it doesn't stop.", "keep running and hide when it gets close. it leaves after 20 more doors."],
	},
	"a90": {
		"": ["you moved while it was watching.", "when you hear the knock, let go of everything until the sign is gone."],
		"touch": ["you had a finger on the screen while it was watching.", "when you hear the knock, take your hands off the screen."],
		"move": ["you kept walking while it was watching.", "when you hear the knock, stop walking right away."],
		"look": ["you looked around while it was watching.", "when you hear the knock, don't even move the camera."],
		"button": ["you pressed something while it was watching.", "when you hear the knock, don't touch anything."],
	},
	"rush": {
		"": ["the lights flicker right before it comes.", "when they do, get into a locker right away."],
		"left_early": ["you came out before it was gone.", "wait until the room is quiet again."],
	},
	"ambush": {
		"": ["the lights flicker, and it comes, and it comes back. again and again.", "stay in the locker until it's gone for good."],
		"left_early": ["it came back for you. it always comes back a few times.", "wait in the locker until it's quiet for a while."],
	},
	"eyes": {
		"": ["you looked at it for too long.", "look at the floor and walk past it."],
	},
	"screech": {
		"": ["it whispered to you, and you didn't look.", "when you hear psst, turn around and find it."],
	},
	"figure": {
		"": ["it can't see you, but it can hear everything.", "crouch and walk slowly when it's close, or hide."],
		"sprinting": ["you ran while it was listening.", "it hears running from far away. crouch-walk past it."],
	},
	"seek": {
		"": ["it caught up with you.", "when the eyes appear on the walls, get ready to run. don't stop until it's gone."],
		"hands": ["the hands got you.", "stay on the side of the room without hands, and crouch under the beams."],
	},
	"a90b": {
		"": ["you didn't do what it told you.", "halt means stand still, proceed means keep walking."],
		"halt": ["it said halt, and you kept walking.", "when you see the stop sign, let go and stand still."],
		"proceed": ["it said proceed, and you stood still.", "when you see the green arrow, keep walking until the next order."],
	},
}
const ENDINGS := [
	"i'll be here if it happens again.",
	"try again. you're getting further.",
	"keep walking, worker.",
	"it's okay. everyone gets lost in here.",
]

var _lines: Array = []
var _i := -1
var _t := 0.0
var _orb: Control
var _text: Label
var _bg: ColorRect
var _pulse := 0.0
var _finished := false


func setup(cause: String, detail: String) -> void:
	layer = 46
	process_mode = Node.PROCESS_MODE_ALWAYS
	var by_detail: Dictionary = HINTS.get(cause, {})
	if NAMES.has(cause):
		_lines.append("you died to what you call " + NAMES[cause] + "...")
	else:
		_lines.append("the office got you this time...")
	_lines.append_array(by_detail.get(detail, by_detail.get("", [])))
	_lines.append(ENDINGS[randi() % ENDINGS.size()])

	var root := Control.new()
	root.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	root.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(root)
	_bg = ColorRect.new()
	_bg.color = Color(0, 0, 0, 0)
	_bg.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	_bg.mouse_filter = Control.MOUSE_FILTER_IGNORE
	root.add_child(_bg)
	_orb = Control.new()
	_orb.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	_orb.mouse_filter = Control.MOUSE_FILTER_IGNORE
	_orb.draw.connect(_draw_orb)
	_orb.modulate.a = 0.0
	root.add_child(_orb)
	_text = UIKit.label("", 22, COLOR)
	_text.set_anchors_and_offsets_preset(Control.PRESET_CENTER)
	_text.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	_text.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	_text.offset_left = -330
	_text.offset_right = 330
	_text.offset_top = 70
	_text.offset_bottom = 170
	_text.add_theme_color_override("font_outline_color", Color(0.25, 0.15, 0.0))
	_text.add_theme_constant_override("outline_size", 3)
	root.add_child(_text)
	var tw := create_tween()
	tw.tween_property(_bg, "color:a", 0.92, 1.0)
	tw.parallel().tween_property(_orb, "modulate:a", 1.0, 1.4)
	tw.tween_callback(_next)
	Input.mouse_mode = Input.MOUSE_MODE_VISIBLE


func _process(delta: float) -> void:
	_pulse += delta
	_orb.queue_redraw()
	if _i < 0 or _finished:
		return
	_t -= delta
	if _t <= 0.0:
		_next()


func _input(event: InputEvent) -> void:
	if _i < 0 or _finished:
		return
	var pressed := (event is InputEventScreenTouch or event is InputEventMouseButton or event is InputEventKey or event is InputEventJoypadButton) and event.is_pressed()
	# a short delay so a tap that was already happening doesn't skip a line by accident
	if pressed and _t < LINE_TIME - 0.5:
		get_viewport().set_input_as_handled()
		_next()


func _next() -> void:
	_i += 1
	if _i >= _lines.size():
		_finish()
		return
	_t = LINE_TIME
	_text.text = UIKit.cap(_lines[_i])
	_text.visible_ratio = 0.0
	create_tween().tween_property(_text, "visible_ratio", 1.0, 0.6)


func _finish() -> void:
	_finished = true
	var tw := create_tween()
	tw.tween_property(_orb, "modulate:a", 0.0, 0.8)
	tw.parallel().tween_property(_text, "modulate:a", 0.0, 0.5)
	tw.tween_callback(func():
		done.emit()
		queue_free())


## a soft glowing ball: lots of faint circles on top of each other, gently pulsing
func _draw_orb() -> void:
	var vs := _orb.get_viewport_rect().size
	var c := vs / 2.0 + Vector2(0, -50 + sin(_pulse * 1.3) * 6.0)
	var r := 46.0 + sin(_pulse * 2.1) * 4.0
	for k in 28:
		var f := float(k) / 27.0
		_orb.draw_circle(c, r * (3.2 - f * 2.6), Color(COLOR.r, COLOR.g, COLOR.b, 0.018 + f * 0.025))
	_orb.draw_circle(c, r * 0.55, Color(1.0, 0.97, 0.85, 0.95))
