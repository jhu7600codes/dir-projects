class_name UIKit
## small helpers so every menu looks the same. all text is lowercase.

const BG := Color(0.03, 0.03, 0.035, 0.94)
const ACCENT := Color(0.95, 0.85, 0.4)


static func panel(min_size := Vector2(420, 0)) -> PanelContainer:
	var p := PanelContainer.new()
	p.custom_minimum_size = min_size
	var sb := StyleBoxFlat.new()
	sb.bg_color = BG
	sb.border_color = Color(0.3, 0.3, 0.32)
	sb.set_border_width_all(1)
	sb.set_corner_radius_all(6)
	sb.set_content_margin_all(18)
	p.add_theme_stylebox_override("panel", sb)
	return p


static func centered(parent: Control, child: Control) -> CenterContainer:
	var c := CenterContainer.new()
	c.set_anchors_preset(Control.PRESET_FULL_RECT)
	parent.add_child(c)
	c.add_child(child)
	return c


static func label(text: String, size := 18, color := Color(0.88, 0.88, 0.88)) -> Label:
	var l := Label.new()
	l.text = text.to_lower()
	l.add_theme_font_size_override("font_size", size)
	l.add_theme_color_override("font_color", color)
	return l


static func button(text: String, on_press: Callable, size := 20) -> Button:
	var b := Button.new()
	b.text = text.to_lower()
	b.add_theme_font_size_override("font_size", size)
	b.custom_minimum_size = Vector2(0, 44)
	b.focus_mode = Control.FOCUS_ALL
	b.pressed.connect(on_press)
	return b


static func slider(text: String, value: float, lo: float, hi: float, on_change: Callable, step := 0.05) -> HBoxContainer:
	var row := HBoxContainer.new()
	var l := label(text, 16)
	l.custom_minimum_size = Vector2(170, 0)
	row.add_child(l)
	var s := HSlider.new()
	s.min_value = lo
	s.max_value = hi
	s.step = step
	s.value = value
	s.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	s.custom_minimum_size = Vector2(180, 32)
	s.value_changed.connect(on_change)
	row.add_child(s)
	return row


static func check(text: String, on: bool, on_toggle: Callable) -> CheckBox:
	var c := CheckBox.new()
	c.text = text.to_lower()
	c.button_pressed = on
	c.add_theme_font_size_override("font_size", 16)
	c.toggled.connect(on_toggle)
	return c


static func options(text: String, items: Array, selected: int, on_select: Callable) -> HBoxContainer:
	var row := HBoxContainer.new()
	var l := label(text, 16)
	l.custom_minimum_size = Vector2(170, 0)
	row.add_child(l)
	var o := OptionButton.new()
	for it in items:
		o.add_item(str(it).to_lower())
	o.selected = selected
	o.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	o.item_selected.connect(on_select)
	row.add_child(o)
	return row


static func vbox(sep := 10) -> VBoxContainer:
	var v := VBoxContainer.new()
	v.add_theme_constant_override("separation", sep)
	return v


static func full_screen_bg(parent: Control, color := Color(0, 0, 0, 0.75)) -> ColorRect:
	var c := ColorRect.new()
	c.color = color
	c.set_anchors_preset(Control.PRESET_FULL_RECT)
	parent.add_child(c)
	return c
