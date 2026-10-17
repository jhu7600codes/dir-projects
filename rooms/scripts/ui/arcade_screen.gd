class_name ArcadeScreen
extends CanvasLayer
## OUTLET DODGE, the arcade game: you're the little yellow square. outlets blink at the edge
## of the screen, then shoot straight across at where you were. they come faster and more
## often. move with wasd / arrows / the stick, or drag your finger / the mouse.
## (single player pauses the office while you play, in multiplayer it keeps going)

const ARENA := Vector2(480, 360)
const SPEED := 230.0
const SIZE := 14.0

var machine: ArcadeMachine
var _area: Control
var _info: Label
var _over_box: VBoxContainer
var _me := ARENA / 2
var _outlets: Array = []  # [pos, vel, blink_time]
var _t := 0.0
var _next := 1.0
var _over := false
var _drag := false
var _drag_to := Vector2.ZERO
var _was_paused := false


func _ready() -> void:
	layer = 45
	process_mode = Node.PROCESS_MODE_ALWAYS
	Game.ui_open = true
	if not Net.active:
		_was_paused = get_tree().paused
		get_tree().paused = true
	Input.mouse_mode = Input.MOUSE_MODE_VISIBLE
	var root := Control.new()
	root.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	add_child(root)
	UIKit.full_screen_bg(root, Color(0, 0, 0, 0.85))
	var v := UIKit.vbox(8)
	v.alignment = BoxContainer.ALIGNMENT_CENTER
	var cc := CenterContainer.new()
	cc.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	root.add_child(cc)
	cc.add_child(v)
	_info = UIKit.label("", 20, Color(1.0, 0.85, 0.3))
	_info.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	v.add_child(_info)
	_area = Control.new()
	_area.custom_minimum_size = ARENA
	_area.clip_contents = true
	_area.draw.connect(_draw_game)
	_area.gui_input.connect(_on_area_input)
	v.add_child(_area)
	_over_box = UIKit.vbox(8)
	_over_box.visible = false
	v.add_child(_over_box)
	var leave := UIKit.button("leave", _close, 16)
	v.add_child(leave)
	# fit small phone screens
	var vs := root.get_viewport_rect().size
	var k := minf(1.0, minf(vs.x / (ARENA.x + 60), vs.y / (ARENA.y + 200)))
	v.scale = Vector2(k, k)


func _close() -> void:
	Game.ui_open = false
	if not Net.active:
		get_tree().paused = _was_paused
	if not Settings.use_touch() and Game.player:
		Input.mouse_mode = Input.MOUSE_MODE_CAPTURED
	queue_free()


func _unhandled_input(event: InputEvent) -> void:
	if event.is_action_pressed("ui_cancel") or event.is_action_pressed("pause"):
		get_viewport().set_input_as_handled()
		_close()


func _on_area_input(e: InputEvent) -> void:
	if e is InputEventScreenTouch or e is InputEventMouseButton:
		_drag = e.pressed
		_drag_to = e.position
	elif e is InputEventScreenDrag or (e is InputEventMouseMotion and _drag):
		_drag_to = e.position


func _process(delta: float) -> void:
	if _over:
		return
	_t += delta
	var mv := Input.get_vector("move_left", "move_right", "move_forward", "move_back") + Game.touch_move
	if mv.length() > 1.0:
		mv = mv.normalized()
	if _drag:
		var to := _drag_to - _me
		mv = to.normalized() if to.length() > 4.0 else Vector2.ZERO
	_me = (_me + mv * SPEED * delta).clamp(Vector2(SIZE, SIZE), ARENA - Vector2(SIZE, SIZE))
	_next -= delta
	if _next <= 0.0:
		_next = maxf(0.35, 1.3 - _t * 0.02) * randf_range(0.7, 1.2)
		_spawn()
	for o in _outlets.duplicate():
		if o[2] > 0.0:
			o[2] -= delta
			if o[2] <= 0.0:
				o[1] = (_me - o[0]).normalized() * (260.0 + _t * 6.0)
			continue
		o[0] += o[1] * delta
		if o[0].distance_to(_me) < SIZE:
			_game_over()
			return
		if o[0].x < -40 or o[0].y < -40 or o[0].x > ARENA.x + 40 or o[0].y > ARENA.y + 40:
			_outlets.erase(o)
	_info.text = "OUTLET DODGE   %.1f" % _t
	_area.queue_redraw()


func _spawn() -> void:
	var p: Vector2
	match randi() % 4:
		0: p = Vector2(randf() * ARENA.x, 8)
		1: p = Vector2(randf() * ARENA.x, ARENA.y - 8)
		2: p = Vector2(8, randf() * ARENA.y)
		_: p = Vector2(ARENA.x - 8, randf() * ARENA.y)
	_outlets.append([p, Vector2.ZERO, 0.6])


func _draw_game() -> void:
	_area.draw_rect(Rect2(Vector2.ZERO, ARENA), Color(0.04, 0.06, 0.05))
	for x in range(0, int(ARENA.x), 40):
		_area.draw_line(Vector2(x, 0), Vector2(x, ARENA.y), Color(0.1, 0.25, 0.15, 0.4))
	for y in range(0, int(ARENA.y), 40):
		_area.draw_line(Vector2(0, y), Vector2(ARENA.x, y), Color(0.1, 0.25, 0.15, 0.4))
	for o in _outlets:
		var blink: bool = o[2] > 0.0 and int(o[2] * 12) % 2 == 0
		var c := Color(0.9, 0.88, 0.8) if not blink else Color(0.4, 1, 1)
		_area.draw_rect(Rect2(o[0] - Vector2(9, 11), Vector2(18, 22)), c)
		_area.draw_rect(Rect2(o[0] + Vector2(-5, -6), Vector2(2, 5)), Color.BLACK)
		_area.draw_rect(Rect2(o[0] + Vector2(3, -6), Vector2(2, 5)), Color.BLACK)
		_area.draw_circle(o[0] + Vector2(0, 4), 2.5, Color.BLACK)
	_area.draw_rect(Rect2(_me - Vector2(SIZE, SIZE) / 2, Vector2(SIZE, SIZE)), Color(1.0, 0.85, 0.3))
	_area.draw_rect(Rect2(Vector2.ZERO, ARENA), Color(0.3, 1.0, 0.5, 0.6), false, 2.0)


func _game_over() -> void:
	_over = true
	_area.queue_redraw()
	var secs := int(_t)
	var best := int(Save.data.get("arcade_best", 0))
	if secs > best:
		Save.data.arcade_best = secs
		Save.write()
	for c in _over_box.get_children():
		c.queue_free()
	_over_box.add_child(UIKit.label(Loc.t("zapped! you lasted %d seconds") % secs, 18))
	_over_box.add_child(UIKit.label(Loc.t("best: ") + "%d" % maxi(best, secs), 14, Color(0.6, 0.6, 0.6)))
	# the first round on each machine pays out
	if machine and is_instance_valid(machine) and not machine.paid and Game.player:
		machine.paid = true
		var gold := clampi(secs / 3, 1, 25)
		Game.player.inventory.add("gold", gold)
		_over_box.add_child(UIKit.label(Loc.t("+%d gold") % gold, 16, UIKit.ACCENT))
	_over_box.add_child(UIKit.button("play again", _restart, 16))
	_over_box.visible = true


func _restart() -> void:
	_over = false
	_over_box.visible = false
	_outlets.clear()
	_t = 0.0
	_next = 1.0
	_me = ARENA / 2
