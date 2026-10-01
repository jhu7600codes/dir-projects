class_name HUD
extends CanvasLayer
## in-game hud: door counter, health / stamina, items, interact prompt, toasts,
## subtitles, damage flashes, the locker slit view, a-200's screen tint and
## the a-60b star marker.

const TINT_SHADER := """
shader_type canvas_item;
uniform vec4 tint : source_color = vec4(0.0);
void fragment() {
	float v = smoothstep(0.3, 0.75, length(UV - 0.5) * 1.35);
	COLOR = vec4(tint.rgb, tint.a * v);
}
"""
const SLIT_SHADER := """
shader_type canvas_item;
void fragment() {
	// dark locker door with a few horizontal slits in the upper middle
	float band = step(0.5, fract(UV.y * 22.0)) * step(0.36, UV.y) * step(UV.y, 0.62);
	float mid = step(0.22, UV.x) * step(UV.x, 0.78);
	float open = band * mid;
	COLOR = vec4(0.0, 0.0, 0.0, mix(0.97, 0.0, open));
}
"""

var player: Player
var _door: Label
var _health: ProgressBar
var _stamina: ProgressBar
var _items: Label
var _prompt: Label
var _hold: ProgressBar
var _toast: Label
var _subtitle: Label
var _flash: ColorRect
var _tint: ColorRect
var _slits: ColorRect
var _star: TextureRect
var _toast_tw: Tween
var _sub_tw: Tween


func _ready() -> void:
	layer = 10
	var root := Control.new()
	root.set_anchors_preset(Control.PRESET_FULL_RECT)
	root.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(root)

	_slits = _rect(root, Color.BLACK)
	var sm := ShaderMaterial.new()
	sm.shader = _shader(SLIT_SHADER)
	_slits.material = sm
	_slits.visible = false

	_tint = _rect(root, Color.WHITE)
	var tm := ShaderMaterial.new()
	tm.shader = _shader(TINT_SHADER)
	_tint.material = tm

	_flash = _rect(root, Color(0, 0, 0, 0))

	_door = UIKit.label("a-000", 34, Color(0.95, 0.85, 0.4))
	_door.set_anchors_preset(Control.PRESET_CENTER_TOP)
	_door.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	_door.offset_left = -100
	_door.offset_right = 100
	_door.offset_top = 10
	root.add_child(_door)

	var bars := UIKit.vbox(4)
	bars.position = Vector2(16, 14)
	root.add_child(bars)
	_health = _bar(bars, Color(0.8, 0.15, 0.15))
	_stamina = _bar(bars, Color(0.35, 0.7, 1.0))

	_items = UIKit.label("", 15, Color(0.85, 0.85, 0.85))
	_items.set_anchors_preset(Control.PRESET_BOTTOM_WIDE)
	_items.offset_top = -34
	_items.offset_bottom = -10
	_items.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	root.add_child(_items)

	var dot := ColorRect.new()
	dot.color = Color(1, 1, 1, 0.5)
	dot.size = Vector2(4, 4)
	dot.set_anchors_preset(Control.PRESET_CENTER)
	dot.position -= Vector2(2, 2)
	dot.mouse_filter = Control.MOUSE_FILTER_IGNORE
	root.add_child(dot)

	_prompt = _center_label(root, 18, 70)
	_hold = ProgressBar.new()
	_hold.set_anchors_preset(Control.PRESET_CENTER)
	_hold.custom_minimum_size = Vector2(160, 8)
	_hold.position = Vector2(-80, 100)
	_hold.show_percentage = false
	_hold.visible = false
	root.add_child(_hold)
	_toast = _center_label(root, 17, 130)
	_toast.modulate.a = 0.0
	_subtitle = _center_label(root, 22, 190)
	_subtitle.modulate.a = 0.0

	_star = TextureRect.new()
	_star.texture = Assets.texture("star_marker")
	_star.size = Vector2(40, 40)
	_star.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
	_star.modulate = Color(0.5, 0.75, 1.0)
	_star.visible = false
	_star.mouse_filter = Control.MOUSE_FILTER_IGNORE
	root.add_child(_star)

	Game.door_changed.connect(func(n): _door.text = Game.door_label(n))
	Game.subtitle.connect(show_subtitle)
	Game.screen_tint.connect(func(c): _tint.material.set_shader_parameter("tint", c))


func _shader(code: String) -> Shader:
	var s := Shader.new()
	s.code = code
	return s


func _rect(parent: Control, c: Color) -> ColorRect:
	var r := ColorRect.new()
	r.color = c
	r.set_anchors_preset(Control.PRESET_FULL_RECT)
	r.mouse_filter = Control.MOUSE_FILTER_IGNORE
	parent.add_child(r)
	return r


func _bar(parent: Control, c: Color) -> ProgressBar:
	var b := ProgressBar.new()
	b.custom_minimum_size = Vector2(200, 10)
	b.show_percentage = false
	b.max_value = 100
	var fill := StyleBoxFlat.new()
	fill.bg_color = c
	var bg := StyleBoxFlat.new()
	bg.bg_color = Color(0, 0, 0, 0.5)
	b.add_theme_stylebox_override("fill", fill)
	b.add_theme_stylebox_override("background", bg)
	parent.add_child(b)
	return b


func _center_label(parent: Control, size: int, y: float) -> Label:
	var l := UIKit.label("", size)
	l.set_anchors_preset(Control.PRESET_CENTER)
	l.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	l.offset_left = -400
	l.offset_right = 400
	l.offset_top = y
	l.offset_bottom = y + 30
	l.add_theme_color_override("font_outline_color", Color.BLACK)
	l.add_theme_constant_override("outline_size", 4)
	parent.add_child(l)
	return l


func _process(_delta: float) -> void:
	if player == null:
		return
	_health.value = player.health
	_stamina.value = player.stamina
	_stamina.visible = player.stamina < 99.0 or player.sprinting
	var inv := player.inventory
	var parts := []
	if inv.has_flashlight:
		parts.append("[1] flashlight %d%%" % int(inv.battery) + (" <" if player.lights.equipped == "flashlight" else ""))
	if inv.has_shakelight:
		parts.append("[2] shakelight %d%%" % int(inv.shake_charge) + (" <" if player.lights.equipped == "shakelight" else ""))
	parts.append("batteries %d" % inv.batteries)
	parts.append("[h] bandages %d" % inv.bandages)
	parts.append("[v] vitamins %d" % inv.vitamins)
	parts.append("gold %d" % int(Save.data.gold))
	_items.text = "   ".join(parts)
	_update_star()


func _update_star() -> void:
	var e: Node3D = Game.tracked_entity
	if e == null or not is_instance_valid(e):
		_star.visible = false
		return
	_star.visible = true
	var cam := player.camera
	var vs := get_viewport().get_visible_rect().size
	var p := cam.unproject_position(e.global_position)
	if cam.is_position_behind(e.global_position):
		p = vs - p  # flip so it points the right way when behind you
		p.y = vs.y - 30
	p.x = clampf(p.x, 24, vs.x - 24)
	p.y = clampf(p.y, 24, vs.y - 24)
	_star.position = p - _star.size / 2.0


func set_prompt(text: String, hold_progress := -1.0) -> void:
	if text == "":
		_prompt.text = ""
	elif Settings.use_touch():
		_prompt.text = text
	else:
		_prompt.text = "[e] " + text
	_hold.visible = hold_progress >= 0.0
	if _hold.visible:
		_hold.value = hold_progress * 100.0


func notify(text: String) -> void:
	_toast.text = text
	if _toast_tw:
		_toast_tw.kill()
	_toast.modulate.a = 1.0
	_toast_tw = create_tween()
	_toast_tw.tween_interval(1.8)
	_toast_tw.tween_property(_toast, "modulate:a", 0.0, 0.5)


func show_subtitle(text: String, color := Color.WHITE) -> void:
	_subtitle.text = text.to_lower()
	_subtitle.add_theme_color_override("font_color", color)
	if _sub_tw:
		_sub_tw.kill()
	_subtitle.modulate.a = 1.0
	_sub_tw = create_tween()
	_sub_tw.tween_interval(2.8)
	_sub_tw.tween_property(_subtitle, "modulate:a", 0.0, 0.6)


func flash(color: Color, seconds: float) -> void:
	_flash.color = color
	var tw := create_tween()
	tw.tween_property(_flash, "color:a", 0.0, seconds)


func set_locker_view(on: bool) -> void:
	_slits.visible = on
