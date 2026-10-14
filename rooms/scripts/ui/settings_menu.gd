class_name SettingsMenu
extends Control
## settings screen, used from the main menu and the pause menu

signal closed


func _ready() -> void:
	set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	UIKit.full_screen_bg(self, Color(0, 0, 0, 0.8))
	var p := UIKit.panel(Vector2(600, 0))
	UIKit.centered(self, p)
	# the title and the back button stay put, only the settings in between scroll
	var outer := UIKit.vbox(10)
	p.add_child(outer)
	outer.add_child(UIKit.label("settings", 28, UIKit.ACCENT))
	var scroll := ScrollContainer.new()
	scroll.custom_minimum_size = Vector2(550, clampf(get_viewport_rect().size.y - 230, 200, 520))
	scroll.horizontal_scroll_mode = ScrollContainer.SCROLL_MODE_DISABLED
	outer.add_child(scroll)
	# room on the right so the scroll bar never covers the values
	var pad := MarginContainer.new()
	pad.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	pad.add_theme_constant_override("margin_right", 18)
	scroll.add_child(pad)
	var v := UIKit.vbox(10)
	v.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	pad.add_child(v)
	var d := Settings.data
	UIKit.section(v, "audio")
	v.add_child(UIKit.slider("master volume", d.master_volume, 0, 1, func(x): Settings.set_value("master_volume", x)))
	v.add_child(UIKit.slider("music volume", d.music_volume, 0, 1, func(x): Settings.set_value("music_volume", x)))
	v.add_child(UIKit.slider("sound volume", d.sfx_volume, 0, 1, func(x): Settings.set_value("sfx_volume", x)))
	UIKit.section(v, "controls")
	v.add_child(UIKit.slider("sensitivity", d.sensitivity, 0.2, 3.0, func(x): Settings.set_value("sensitivity", x)))
	v.add_child(UIKit.check("open doors automatically", d.auto_doors, func(on): Settings.set_value("auto_doors", on)))
	v.add_child(UIKit.slider("field of view", d.fov, 60, 100, func(x): Settings.set_value("fov", x), 1.0))
	UIKit.section(v, "graphics")
	if not OS.has_feature("mobile"):
		v.add_child(UIKit.check("fullscreen (F11)", d.fullscreen, func(on): Settings.set_value("fullscreen", on)))
	else:
		v.add_child(UIKit.check("uncap fps", d.uncap_fps, func(on): Settings.set_value("uncap_fps", on)))
	v.add_child(UIKit.options("quality", ["low", "medium", "high"], int(d.quality), func(i): Settings.set_value("quality", i)))
	v.add_child(UIKit.slider("resolution", d.render_scale, 0.4, 1.0, func(x): Settings.set_value("render_scale", x)))
	v.add_child(UIKit.check("show fps", d.show_fps, func(on): Settings.set_value("show_fps", on)))
	UIKit.section(v, "touch controls")
	var modes := ["auto", "on", "off"]
	v.add_child(UIKit.options("touch controls", modes, maxi(0, modes.find(d.touch_controls)), func(i): Settings.set_value("touch_controls", modes[i])))
	v.add_child(UIKit.check("left handed layout", d.touch_left_handed, func(on): Settings.set_value("touch_left_handed", on)))
	v.add_child(UIKit.slider("button size", d.touch_scale, 0.6, 1.6, func(x): Settings.set_value("touch_scale", x)))
	v.add_child(UIKit.slider("button opacity", d.touch_opacity, 0.15, 1.0, func(x): Settings.set_value("touch_opacity", x)))
	outer.add_child(UIKit.button("back", func():
		closed.emit()
		queue_free()))
