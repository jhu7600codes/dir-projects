class_name UITheme
## the look of every menu: dark glassy panels, rounded buttons with the yellow accent on
## hover. keeps the game's normal font. set once on the root window (Game._ready).

const ACCENT := Color(0.95, 0.85, 0.4)
static var title_font: Font = null
static var body_font: Font = null


static func fonts() -> void:
	# the game's normal font everywhere (the custom fonts didn't fit)
	if title_font == null:
		title_font = ThemeDB.fallback_font
		body_font = ThemeDB.fallback_font


static func make() -> Theme:
	fonts()
	var t := Theme.new()
	t.default_font_size = 18
	# buttons
	for type in ["Button", "OptionButton", "MenuButton"]:
		t.set_font_size("font_size", type, 20)
		t.set_stylebox("normal", type, _box(Color(0.07, 0.07, 0.08, 0.88), Color(0.22, 0.22, 0.24), 1, 2))
		t.set_stylebox("hover", type, _box(Color(0.14, 0.13, 0.1, 0.95), ACCENT, 1, 2, true))
		t.set_stylebox("pressed", type, _box(Color(0.3, 0.26, 0.1, 0.95), ACCENT, 1, 2, true))
		t.set_stylebox("focus", type, _box(Color(0, 0, 0, 0), ACCENT.darkened(0.3), 1, 0))
		t.set_stylebox("disabled", type, _box(Color(0.05, 0.05, 0.05, 0.6), Color(0.15, 0.15, 0.15), 1, 0))
		t.set_color("font_color", type, Color(0.9, 0.9, 0.9))
		t.set_color("font_hover_color", type, ACCENT)
		t.set_color("font_pressed_color", type, Color.WHITE)
		t.set_color("font_focus_color", type, Color(0.95, 0.95, 0.95))
	# panels
	var p := _box(Color(0.07, 0.07, 0.08, 0.97), Color(0.3, 0.3, 0.33), 1, 0)
	p.set_corner_radius_all(10)
	p.shadow_color = Color(0, 0, 0, 0.55)
	p.shadow_size = 18
	p.set_content_margin_all(22)
	t.set_stylebox("panel", "PanelContainer", p)
	t.set_stylebox("panel", "Panel", p)
	# text fields
	var le := _box(Color(0.02, 0.02, 0.025, 0.95), Color(0.25, 0.25, 0.27), 1, 0)
	le.set_content_margin_all(8)
	t.set_stylebox("normal", "LineEdit", le)
	var lef := le.duplicate()
	lef.border_color = ACCENT
	t.set_stylebox("focus", "LineEdit", lef)
	t.set_color("caret_color", "LineEdit", ACCENT)
	t.set_color("font_placeholder_color", "LineEdit", Color(0.45, 0.45, 0.48))
	# sliders and bars
	var track := StyleBoxFlat.new()
	track.bg_color = Color(0.15, 0.15, 0.17)
	track.set_corner_radius_all(3)
	track.content_margin_top = 3
	track.content_margin_bottom = 3
	t.set_stylebox("slider", "HSlider", track)
	var filled := track.duplicate()
	filled.bg_color = ACCENT.darkened(0.15)
	t.set_stylebox("grabber_area", "HSlider", filled)
	t.set_stylebox("grabber_area_highlight", "HSlider", filled)
	var pb_bg := StyleBoxFlat.new()
	pb_bg.bg_color = Color(0, 0, 0, 0.55)
	pb_bg.set_corner_radius_all(3)
	t.set_stylebox("background", "ProgressBar", pb_bg)
	var pb_fill := pb_bg.duplicate()
	pb_fill.bg_color = ACCENT
	t.set_stylebox("fill", "ProgressBar", pb_fill)
	# check boxes
	t.set_color("font_color", "CheckBox", Color(0.85, 0.85, 0.85))
	t.set_color("font_hover_color", "CheckBox", ACCENT)
	t.set_font_size("font_size", "CheckBox", 16)
	# scroll bars: thin
	var sg := StyleBoxFlat.new()
	sg.bg_color = Color(0.35, 0.35, 0.38)
	sg.set_corner_radius_all(3)
	t.set_stylebox("grabber", "VScrollBar", sg)
	var sgh := sg.duplicate()
	sgh.bg_color = ACCENT
	t.set_stylebox("grabber_highlight", "VScrollBar", sgh)
	t.set_stylebox("scroll", "VScrollBar", StyleBoxEmpty.new())
	return t


static func _box(bg: Color, border: Color, bw: int, bottom: int, left_bar := false) -> StyleBoxFlat:
	var s := StyleBoxFlat.new()
	s.bg_color = bg
	s.border_color = border
	s.set_border_width_all(bw)
	s.border_width_bottom = bw + bottom
	if left_bar:
		s.border_width_left = 4
	s.set_corner_radius_all(5)
	s.content_margin_left = 16
	s.content_margin_right = 16
	s.content_margin_top = 8
	s.content_margin_bottom = 8
	return s
