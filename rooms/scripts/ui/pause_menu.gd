class_name PauseMenu
extends CanvasLayer
## escape menu: resume, settings, back to the title screen

var _root: Control


func _ready() -> void:
	layer = 40
	process_mode = Node.PROCESS_MODE_ALWAYS
	_root = Control.new()
	_root.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	add_child(_root)
	UIKit.full_screen_bg(_root, Color(0, 0, 0, 0.6))
	var p := UIKit.panel(Vector2(320, 0))
	UIKit.centered(_root, p)
	var v := UIKit.vbox(10)
	p.add_child(v)
	v.add_child(UIKit.label("paused", 28, UIKit.ACCENT))
	v.add_child(UIKit.label(Game.door_label(Game.door), 16))
	v.add_child(UIKit.button("resume", toggle))
	v.add_child(UIKit.button("settings", func(): _root.add_child(SettingsMenu.new())))
	v.add_child(UIKit.button("quit to title", Game.to_menu))
	visible = false


func _unhandled_input(event: InputEvent) -> void:
	if event.is_action_pressed("pause"):
		if Game.player and Game.player.dead:
			return
		toggle()
		get_viewport().set_input_as_handled()


func toggle() -> void:
	visible = not visible
	get_tree().paused = visible
	if not Settings.use_touch():
		Input.mouse_mode = Input.MOUSE_MODE_VISIBLE if visible else Input.MOUSE_MODE_CAPTURED
