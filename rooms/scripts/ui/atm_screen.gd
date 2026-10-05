class_name AtmScreen
extends CanvasLayer
## the atm: move gold from your balance into the bank

var _info: Label


func _ready() -> void:
	layer = 40
	process_mode = Node.PROCESS_MODE_ALWAYS
	Game.ui_open = true
	Input.mouse_mode = Input.MOUSE_MODE_VISIBLE
	var root := Control.new()
	root.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	add_child(root)
	UIKit.full_screen_bg(root, Color(0, 0, 0, 0.4))
	var p := UIKit.panel(Vector2(320, 0))
	UIKit.centered(root, p)
	var v := UIKit.vbox(8)
	p.add_child(v)
	v.add_child(UIKit.label("atm", 22, Color(0.3, 1.0, 0.5)))
	_info = UIKit.label("", 17)
	v.add_child(_info)
	for amount in [10, 50]:
		v.add_child(UIKit.button("deposit %d gold" % amount, _deposit.bind(amount), 18))
	v.add_child(UIKit.button("deposit everything", _deposit.bind(-1), 18))
	v.add_child(UIKit.button("back", close, 18))
	_show()


func _show() -> void:
	_info.text = UIKit.cap("balance: %d gold\nbank: %d gold" % [int(Save.data.gold), int(Save.data.get("bank", 0))])


func _deposit(amount: int) -> void:
	var have := int(Save.data.gold)
	var n := have if amount < 0 else mini(amount, have)
	if n <= 0:
		Game.play_ui("deny")
		return
	Save.data.gold = have - n
	Save.data.bank = int(Save.data.get("bank", 0)) + n
	Save.write()
	Game.play_ui("buy")
	_show()


func _unhandled_input(event: InputEvent) -> void:
	if event.is_action_pressed("pause"):
		get_viewport().set_input_as_handled()
		close()


func close() -> void:
	Game.ui_open = false
	if not Settings.use_touch():
		Input.mouse_mode = Input.MOUSE_MODE_CAPTURED
	queue_free()
