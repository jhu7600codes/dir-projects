class_name UIKit
## small helpers so every menu looks the same. text gets normal capitalization.

const BG := Color(0.03, 0.03, 0.035, 0.94)
const ACCENT := Color(0.95, 0.85, 0.4)


## first letter uppercase ("open door" -> "Open door"), the rest stays as written
static func cap(text: String) -> String:
	if text.is_empty():
		return text
	text = Loc.t(text)
	return text.substr(0, 1).to_upper() + text.substr(1)


static func panel(min_size := Vector2(420, 0)) -> PanelContainer:
	# the look comes from UITheme (dark glass, rounded, a soft shadow)
	var p := PanelContainer.new()
	p.custom_minimum_size = min_size
	return p


static func centered(parent: Control, child: Control) -> CenterContainer:
	var c := CenterContainer.new()
	c.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	parent.add_child(c)
	c.add_child(child)
	return c


static func label(text: String, size := 18, color := Color(0.88, 0.88, 0.88)) -> Label:
	var l := Label.new()
	l.text = cap(text)
	if size >= 24:
		UITheme.fonts()
		l.add_theme_font_override("font", UITheme.title_font)
	l.add_theme_font_size_override("font_size", size)
	l.add_theme_color_override("font_color", color)
	return l


static func button(text: String, on_press: Callable, size := 20) -> Button:
	var b := Button.new()
	b.text = cap(text)
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
	s.custom_minimum_size = Vector2(160, 32)
	s.value_changed.connect(on_change)
	row.add_child(s)
	# the value, so you know where it's at
	var val := label("", 14, Color(0.6, 0.6, 0.62))
	val.custom_minimum_size = Vector2(60, 0)
	val.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
	var fmt := func(x: float) -> String:
		return str(int(x)) if step >= 1.0 else ("%d%%" % int(round(x * 100.0)) if hi <= 1.0 else "%.1f" % x)
	val.text = fmt.call(value)
	s.value_changed.connect(func(x): val.text = fmt.call(x))
	row.add_child(val)
	return row


## a section title in a menu: small, yellow, with a thin line under it
static func section(parent: Control, text: String) -> void:
	var gap := Control.new()
	gap.custom_minimum_size = Vector2(0, 6)
	parent.add_child(gap)
	parent.add_child(label(Loc.t(text).to_upper(), 13, ACCENT.darkened(0.15)))
	var line := ColorRect.new()
	line.color = Color(1, 1, 1, 0.08)
	line.custom_minimum_size = Vector2(0, 1)
	parent.add_child(line)


static func check(text: String, on: bool, on_toggle: Callable) -> CheckBox:
	var c := CheckBox.new()
	c.text = cap(text)
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
		o.add_item(cap(str(it)))
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
	c.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	parent.add_child(c)
	return c
