class_name TouchControls
extends CanvasLayer
## android controls: a virtual stick on one side, drag anywhere else to look, and
## four buttons: use, item (tap = use it, hold = next item), run and crouch.
## layout (left handed, size, opacity) comes from the settings. everything is
## drawn by hand so multitouch works.
## Game.touch_count tells a-90 if any finger is on the screen.

const STICK_R := 80.0
const ITEM_HOLD := 0.4  # holding "item" this long switches to the next item instead of using it
const ICONS := {
	"use": "res://assets/branding/touch_use.png",
	"item": "res://assets/branding/touch_item.png",
	"run": "res://assets/branding/touch_run.png",
	"crouch": "res://assets/branding/touch_crouch.png",
}

var _pad: Control
var _stick_finger := -1
var _stick_center := Vector2.ZERO
var _stick_pos := Vector2.ZERO
var _look_finger := -1
var _look_last := Vector2.ZERO
var _fingers := {}
var _buttons := []  # [name, action, center, radius, toggle]
var _toggled := {}
var _held := {}  # finger index -> action held by that finger (so "use" can be held for exit doors)
var _item_finger := -1
var _item_time := 0.0
var _item_swapped := false
var _icons := {}


func _ready() -> void:
	layer = 20
	_pad = Control.new()
	_pad.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	_pad.mouse_filter = Control.MOUSE_FILTER_IGNORE
	_pad.draw.connect(_draw_pad)
	add_child(_pad)
	for k in ICONS:
		if ResourceLoader.exists(ICONS[k]):
			_icons[k] = load(ICONS[k])
	Settings.changed.connect(_layout)
	get_viewport().size_changed.connect(_layout)
	_layout()


func _layout() -> void:
	visible = Settings.use_touch()
	var vs := get_viewport().get_visible_rect().size
	var s := float(Settings.data.touch_scale)
	var left: bool = Settings.data.touch_left_handed
	var bx := vs.x - 110.0 * s if not left else 110.0 * s
	var side := -1.0 if not left else 1.0
	var by := vs.y - 120.0 * s
	_buttons = [
		["use", "interact", Vector2(bx, by), 60.0 * s, false],
		["item", "use_item", Vector2(bx + side * 120 * s, by - 95 * s), 44.0 * s, false],
		["run", "sprint", Vector2(bx + side * 150 * s, by + 35 * s), 40.0 * s, true],
		["crouch", "crouch", Vector2(bx + side * 5 * s, by - 150 * s), 40.0 * s, true],
		["||", "pause", Vector2(vs.x / 2 + 150, 34), 26.0, false],
	]
	if Game.admin:
		_buttons.append(["adm", "admin_panel", Vector2(vs.x / 2 - 150, 34), 26.0, false])
	_pad.queue_redraw()


func _stick_side_hit(p: Vector2) -> bool:
	var vs := get_viewport().get_visible_rect().size
	if Settings.data.touch_left_handed:
		return p.x > vs.x * 0.55
	return p.x < vs.x * 0.45


func _input(event: InputEvent) -> void:
	if not visible:
		return
	if event is InputEventScreenTouch:
		if event.pressed:
			_fingers[event.index] = true
			var b = _button_at(event.position)
			if b != null:
				_press_button(b, event.index)
			elif _stick_finger == -1 and _stick_side_hit(event.position):
				_stick_finger = event.index
				_stick_center = event.position
				_stick_pos = event.position
			elif _look_finger == -1:
				_look_finger = event.index
				_look_last = event.position
		else:
			_fingers.erase(event.index)
			if event.index == _item_finger:
				_item_finger = -1
				if not _item_swapped:
					_tap("use_item")
			if _held.has(event.index):
				var up := InputEventAction.new()
				up.action = _held[event.index]
				up.pressed = false
				Input.parse_input_event(up)
				_held.erase(event.index)
			if event.index == _stick_finger:
				_stick_finger = -1
				Game.touch_move = Vector2.ZERO
			elif event.index == _look_finger:
				_look_finger = -1
		Game.touch_count = _fingers.size()
		_pad.queue_redraw()
	elif event is InputEventScreenDrag:
		if event.index == _stick_finger:
			_stick_pos = event.position
			var d: Vector2 = (_stick_pos - _stick_center) / STICK_R
			Game.touch_move = d.limit_length(1.0)
			_pad.queue_redraw()
		elif event.index == _look_finger:
			Game.touch_look += event.position - _look_last
			_look_last = event.position


func _button_at(p: Vector2):
	for b in _buttons:
		if p.distance_to(b[2]) <= b[3]:
			return b
	return null


func _process(_delta: float) -> void:
	if _item_finger != -1 and not _item_swapped and Time.get_ticks_msec() / 1000.0 - _item_time >= ITEM_HOLD:
		_item_swapped = true
		_tap("item_next")


func _tap(action: String) -> void:
	for pressed in [true, false]:
		var ev := InputEventAction.new()
		ev.action = action
		ev.pressed = pressed
		Input.parse_input_event(ev)


func _press_button(b: Array, finger: int) -> void:
	var action: String = b[1]
	if b[0] == "item":
		# decided when the finger lifts (tap) or after ITEM_HOLD (switch item)
		_item_finger = finger
		_item_time = Time.get_ticks_msec() / 1000.0
		_item_swapped = false
		return
	if b[4]:
		# toggles: sprint and crouch stay on until tapped again
		var on: bool = not _toggled.get(action, false)
		_toggled[action] = on
		if on:
			Input.action_press(action)
		else:
			Input.action_release(action)
		return
	# pressed while the finger stays down, released when it lifts (holding works)
	var ev := InputEventAction.new()
	ev.action = action
	ev.pressed = true
	Input.parse_input_event(ev)
	_held[finger] = action


func _draw_pad() -> void:
	var a := float(Settings.data.touch_opacity)
	var font := ThemeDB.fallback_font
	for b in _buttons:
		var on: bool = _toggled.get(b[1], false)
		_pad.draw_circle(b[2], b[3], Color(1, 1, 1, a * 0.45) if on else Color(0, 0, 0, a * 0.45))
		_pad.draw_arc(b[2], b[3], 0, TAU, 32, Color(1, 1, 1, a), 2.0)
		if _icons.has(b[0]):
			var sz: float = b[3] * 1.4
			_pad.draw_texture_rect(_icons[b[0]], Rect2(b[2] - Vector2(sz, sz) / 2, Vector2(sz, sz)), false, Color(0, 0, 0, a + 0.3) if on else Color(1, 1, 1, a + 0.3))
			continue
		var fs := int(b[3] * 0.42)
		var w := font.get_string_size(UIKit.cap(b[0]), HORIZONTAL_ALIGNMENT_LEFT, -1, fs).x
		_pad.draw_string(font, b[2] + Vector2(-w / 2, fs * 0.35), UIKit.cap(b[0]), HORIZONTAL_ALIGNMENT_LEFT, -1, fs, Color(1, 1, 1, a + 0.2))
	if _stick_finger != -1:
		_pad.draw_circle(_stick_center, STICK_R, Color(1, 1, 1, a * 0.15))
		_pad.draw_circle(_stick_center + (_stick_pos - _stick_center).limit_length(STICK_R), 32, Color(1, 1, 1, a * 0.6))
	else:
		var vs := get_viewport().get_visible_rect().size
		var hint := Vector2(150, vs.y - 150) if not Settings.data.touch_left_handed else Vector2(vs.x - 150, vs.y - 150)
		_pad.draw_arc(hint, STICK_R, 0, TAU, 32, Color(1, 1, 1, a * 0.4), 2.0)
