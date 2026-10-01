class_name TouchControls
extends CanvasLayer
## android controls: a virtual stick on one side, drag anywhere else to look, and
## buttons for interact / sprint / crouch / lights / items. layout (left handed, size,
## opacity) comes from the settings. everything is drawn by hand so multitouch works.
## Game.touch_count tells a-90 if any finger is on the screen.

const STICK_R := 80.0

var _pad: Control
var _stick_finger := -1
var _stick_center := Vector2.ZERO
var _stick_pos := Vector2.ZERO
var _look_finger := -1
var _look_last := Vector2.ZERO
var _fingers := {}
var _buttons := []  # [name, action, center, radius, toggle]
var _toggled := {}


func _ready() -> void:
	layer = 20
	_pad = Control.new()
	_pad.set_anchors_preset(Control.PRESET_FULL_RECT)
	_pad.mouse_filter = Control.MOUSE_FILTER_IGNORE
	_pad.draw.connect(_draw_pad)
	add_child(_pad)
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
		["use", "interact", Vector2(bx, by), 58.0 * s, false],
		["run", "sprint", Vector2(bx + side * 140 * s, by + 30 * s), 42.0 * s, true],
		["duck", "crouch", Vector2(bx + side * 30 * s, by - 140 * s), 40.0 * s, true],
		["light", "flashlight", Vector2(bx + side * 130 * s, by - 100 * s), 36.0 * s, false],
		["shake", "slot_shakelight", Vector2(bx + side * 230 * s, by - 40 * s), 32.0 * s, false],
		["heal", "use_bandage", Vector2(bx + side * 230 * s, by - 130 * s), 30.0 * s, false],
		["vit", "use_vitamins", Vector2(bx + side * 150 * s, by - 200 * s), 30.0 * s, false],
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
				_press_button(b)
			elif _stick_finger == -1 and _stick_side_hit(event.position):
				_stick_finger = event.index
				_stick_center = event.position
				_stick_pos = event.position
			elif _look_finger == -1:
				_look_finger = event.index
				_look_last = event.position
		else:
			_fingers.erase(event.index)
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


func _press_button(b: Array) -> void:
	var action: String = b[1]
	if b[4]:
		# toggles: sprint and crouch stay on until tapped again
		var on: bool = not _toggled.get(action, false)
		_toggled[action] = on
		if on:
			Input.action_press(action)
		else:
			Input.action_release(action)
		return
	var ev := InputEventAction.new()
	ev.action = action
	ev.pressed = true
	Input.parse_input_event(ev)
	var up := InputEventAction.new()
	up.action = action
	up.pressed = false
	Input.parse_input_event.call_deferred(up)


func _draw_pad() -> void:
	var a := float(Settings.data.touch_opacity)
	var font := ThemeDB.fallback_font
	for b in _buttons:
		var on: bool = _toggled.get(b[1], false)
		_pad.draw_circle(b[2], b[3], Color(1, 1, 1, a * 0.45) if on else Color(0, 0, 0, a * 0.45))
		_pad.draw_arc(b[2], b[3], 0, TAU, 32, Color(1, 1, 1, a), 2.0)
		var fs := int(b[3] * 0.42)
		var w := font.get_string_size(b[0], HORIZONTAL_ALIGNMENT_LEFT, -1, fs).x
		_pad.draw_string(font, b[2] + Vector2(-w / 2, fs * 0.35), b[0], HORIZONTAL_ALIGNMENT_LEFT, -1, fs, Color(1, 1, 1, a + 0.2))
	if _stick_finger != -1:
		_pad.draw_circle(_stick_center, STICK_R, Color(1, 1, 1, a * 0.15))
		_pad.draw_circle(_stick_center + (_stick_pos - _stick_center).limit_length(STICK_R), 32, Color(1, 1, 1, a * 0.6))
	else:
		var vs := get_viewport().get_visible_rect().size
		var hint := Vector2(150, vs.y - 150) if not Settings.data.touch_left_handed else Vector2(vs.x - 150, vs.y - 150)
		_pad.draw_arc(hint, STICK_R, 0, TAU, 32, Color(1, 1, 1, a * 0.4), 2.0)
