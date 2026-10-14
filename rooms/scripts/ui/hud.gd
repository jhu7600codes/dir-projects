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
## a soft dark vignette around the edges (no grain)
const FILM_SHADER := """
shader_type canvas_item;
void fragment() {
	float v = smoothstep(0.42, 0.95, length(UV - 0.5) * 1.25);
	COLOR = vec4(0.0, 0.0, 0.0, 0.55 * v);
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
var vitals: Vitals
var slots: ItemSlots
var _fps: Label
var _prompt: Label
var _hold: ProgressBar
var _toast: Label
var _subtitle: Label
var _flash: ColorRect
var _tint: ColorRect
var _slits: ColorRect
var _star: TextureRect
var _toast_tw: Tween
var _film: ColorRect
var _star_banner: Label
var _sub_tw: Tween


func _ready() -> void:
	layer = 10
	var root := Control.new()
	root.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	root.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(root)

	var film := _rect(root, Color.WHITE)
	var fm := ShaderMaterial.new()
	fm.shader = _shader(FILM_SHADER)
	film.material = fm
	film.visible = true
	_film = film
	Settings.changed.connect(func(): _film.visible = int(Settings.data.quality) >= 1)
	_film.visible = int(Settings.data.quality) >= 1

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

	_door = UIKit.label("a-000", 36, Color(0.95, 0.85, 0.4))
	_door.add_theme_color_override("font_shadow_color", Color(0, 0, 0, 0.7))
	_door.add_theme_constant_override("shadow_offset_y", 2)
	_door.set_anchors_preset(Control.PRESET_CENTER_TOP)
	_door.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	_door.offset_left = -100
	_door.offset_right = 100
	_door.offset_top = 10
	root.add_child(_door)

	vitals = Vitals.new()
	root.add_child(vitals)

	slots = ItemSlots.new()
	root.add_child(slots)

	_fps = UIKit.label("", 14, Color(0.6, 1.0, 0.6))
	_fps.set_anchors_preset(Control.PRESET_TOP_RIGHT)
	_fps.offset_left = -330
	_fps.offset_right = -12
	_fps.offset_top = 10
	_fps.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
	root.add_child(_fps)

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
	_star_banner = UIKit.label("", 24, Color(0.6, 0.8, 1.0))
	UITheme.fonts()
	_star_banner.add_theme_font_override("font", UITheme.title_font)
	_star_banner.set_anchors_and_offsets_preset(Control.PRESET_CENTER_TOP)
	_star_banner.offset_left = -400
	_star_banner.offset_right = 400
	_star_banner.offset_top = 56
	_star_banner.offset_bottom = 90
	_star_banner.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	_star_banner.add_theme_color_override("font_outline_color", Color(0, 0, 0.1))
	_star_banner.add_theme_constant_override("outline_size", 6)
	_star_banner.modulate.a = 0.0
	root.add_child(_star_banner)
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
	r.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
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
	UITheme.fonts()
	l.add_theme_font_override("font", UITheme.title_font)
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
	_fps.visible = Settings.data.show_fps
	if _fps.visible:
		# where the frame time goes: game scripts + physics, the renderer on the cpu, the gpu
		var vp := get_viewport().get_viewport_rid()
		RenderingServer.viewport_set_measure_render_time(vp, true)
		var game_ms := (Performance.get_monitor(Performance.TIME_PROCESS) + Performance.get_monitor(Performance.TIME_PHYSICS_PROCESS)) * 1000.0
		_fps.text = "%d FPS\ngame %.1f  render %.1f  gpu %.1f ms" % [Engine.get_frames_per_second(), game_ms,
				RenderingServer.viewport_get_measured_render_time_cpu(vp), RenderingServer.viewport_get_measured_render_time_gpu(vp)]
	if player == null:
		return
	if vitals.player == null:
		vitals.player = player
	if slots.player == null:
		slots.player = player
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
		_prompt.text = UIKit.cap(text)
	else:
		_prompt.text = "[E] " + UIKit.cap(text)
	_hold.visible = hold_progress >= 0.0
	if _hold.visible:
		_hold.value = hold_progress * 100.0


## stardevice / starhelp: a blue banner at the top when an entity shows up
func star_warning(who: String) -> void:
	_star_banner.text = "★  %s is coming  ★" % who.to_upper()
	_star_banner.modulate.a = 1.0
	var tw := create_tween()
	tw.tween_interval(2.5)
	tw.tween_property(_star_banner, "modulate:a", 0.0, 0.8)
	var a := AudioStreamPlayer.new()
	a.stream = Assets.sound("switch_flip")
	a.pitch_scale = 1.8
	a.bus = "SFX"
	add_child(a)
	a.play()
	a.finished.connect(a.queue_free)


func notify(text: String) -> void:
	_toast.text = UIKit.cap(text)
	if _toast_tw:
		_toast_tw.kill()
	_toast.modulate.a = 1.0
	_toast_tw = create_tween()
	_toast_tw.tween_interval(1.8)
	_toast_tw.tween_property(_toast, "modulate:a", 0.0, 0.5)


func show_subtitle(text: String, color := Color.WHITE) -> void:
	_subtitle.text = UIKit.cap(text)
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
