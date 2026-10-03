class_name Keypad
extends CanvasLayer
## the code lock on figure's door: type the 4 digits from the paper. you can't walk or look
## around while it's open, but figure keeps listening... close it with back / esc.

signal solved

var code := ""
var note := ""  # what the paper said, if you found it
var _entry := ""
var _display: Label


func _ready() -> void:
	layer = 40
	process_mode = Node.PROCESS_MODE_ALWAYS
	Game.ui_open = true
	Input.mouse_mode = Input.MOUSE_MODE_VISIBLE
	var root := Control.new()
	root.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	add_child(root)
	UIKit.full_screen_bg(root, Color(0, 0, 0, 0.35))
	var p := UIKit.panel(Vector2(300, 0))
	UIKit.centered(root, p)
	var v := UIKit.vbox(8)
	p.add_child(v)
	v.add_child(UIKit.label("code lock", 20, UIKit.ACCENT))
	_display = UIKit.label("_ _ _ _", 34)
	_display.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	v.add_child(_display)
	var grid := GridContainer.new()
	grid.columns = 3
	grid.add_theme_constant_override("h_separation", 6)
	grid.add_theme_constant_override("v_separation", 6)
	v.add_child(grid)
	for k in ["1", "2", "3", "4", "5", "6", "7", "8", "9", "clear", "0", "ok"]:
		var b := UIKit.button(k, _press.bind(k), 22)
		b.custom_minimum_size = Vector2(88, 56)
		grid.add_child(b)
	var hint := UIKit.label("your note: " + note if note != "" else "the code is written on a paper somewhere in this room.", 14, Color(0.7, 0.7, 0.7))
	hint.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	hint.custom_minimum_size = Vector2(280, 0)
	v.add_child(hint)
	v.add_child(UIKit.button("back", close, 18))


func _process(_delta: float) -> void:
	if Game.player == null or Game.player.dead:
		close()


func _press(k: String) -> void:
	match k:
		"clear":
			_entry = ""
		"ok":
			_check()
			return
		_:
			if _entry.length() < 4:
				_entry += k
	_show()


func _show() -> void:
	var s := ""
	for i in 4:
		s += (_entry[i] if i < _entry.length() else "_") + " "
	_display.text = s.strip_edges()


func _check() -> void:
	if _entry == code:
		Game.play_ui("buy")
		solved.emit()
		close()
	else:
		Game.play_ui("deny")
		_entry = ""
		_display.text = "wrong"


func _unhandled_input(event: InputEvent) -> void:
	if event.is_action_pressed("pause"):
		get_viewport().set_input_as_handled()
		close()
	elif event is InputEventKey and event.pressed and not event.echo:
		var c := char(event.unicode)
		if c >= "0" and c <= "9":
			get_viewport().set_input_as_handled()  # don't also switch items with 1-4
			_press(c)
		elif event.keycode == KEY_ENTER or event.keycode == KEY_KP_ENTER:
			_press("ok")
		elif event.keycode == KEY_BACKSPACE:
			_press("clear")


func close() -> void:
	Game.ui_open = false
	if not Settings.use_touch():
		Input.mouse_mode = Input.MOUSE_MODE_CAPTURED
	queue_free()


func _exit_tree() -> void:
	Game.ui_open = false
