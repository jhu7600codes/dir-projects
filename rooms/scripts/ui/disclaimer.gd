class_name Disclaimer
extends CanvasLayer
## shown once every time the game starts, before the title screen

var _done := false


func _ready() -> void:
	layer = 90
	var root := Control.new()
	root.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	add_child(root)
	var bg := UIKit.full_screen_bg(root, Color(0, 0, 0, 1))
	bg.mouse_filter = Control.MOUSE_FILTER_STOP
	var v := UIKit.vbox(18)
	v.alignment = BoxContainer.ALIGNMENT_CENTER
	v.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	root.add_child(v)
	var big := Label.new()
	big.text = "THIS USES DOORS AND ROOMS REVISITED ASSETS!"
	big.add_theme_font_size_override("font_size", 34)
	big.add_theme_color_override("font_color", Color(1.0, 0.3, 0.25))
	big.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	big.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	v.add_child(big)
	var small := UIKit.label("unofficial fan game. not affiliated with doors, rooms, rooms: revisited or roblox.\nall credit for those assets goes to their creators.", 16, Color(0.75, 0.75, 0.75))
	small.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	small.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	v.add_child(small)
	var hint := UIKit.label("tap to continue" if Settings.use_touch() else "press any key to continue", 14, Color(0.5, 0.5, 0.5))
	hint.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	v.add_child(hint)
	var tw := create_tween().set_loops()
	tw.tween_property(hint, "modulate:a", 0.3, 0.9)
	tw.tween_property(hint, "modulate:a", 1.0, 0.9)


func _input(event: InputEvent) -> void:
	if _done:
		return
	if (event is InputEventKey or event is InputEventMouseButton or event is InputEventScreenTouch or event is InputEventJoypadButton) and event.is_pressed():
		get_viewport().set_input_as_handled()
		_done = true
		var tw := create_tween()
		for c in get_children():
			tw.parallel().tween_property(c, "modulate:a", 0.0, 0.4)
		tw.tween_callback(queue_free)
