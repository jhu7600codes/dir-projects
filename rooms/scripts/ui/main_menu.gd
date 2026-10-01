extends Control
## title screen: a live view of a dark office hallway behind the menu, the worker's
## journal, play / continue (with the admin toggle), achievements, settings, credits

const VIGNETTE := """
shader_type canvas_item;
void fragment() {
	vec2 uv = UV - vec2(0.5);
	float v = smoothstep(0.25, 0.85, length(uv * vec2(1.3, 1.0)));
	// darker on the left where the menu is
	float side = smoothstep(0.55, 0.0, UV.x) * 0.55;
	COLOR = vec4(0.0, 0.0, 0.0, clamp(v * 0.9 + side, 0.0, 0.95));
}
"""

var _admin := false
var _music: AudioStreamPlayer
var _cam: Camera3D
var _cam_start := Transform3D.IDENTITY
var _t := 0.0
var _title: Label
var _flicker := 0.0


func _ready() -> void:
	set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	Input.mouse_mode = Input.MOUSE_MODE_VISIBLE
	_make_backdrop()
	var vig := ColorRect.new()
	vig.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	vig.mouse_filter = Control.MOUSE_FILTER_IGNORE
	var sm := ShaderMaterial.new()
	sm.shader = Shader.new()
	sm.shader.code = VIGNETTE
	vig.material = sm
	add_child(vig)
	_make_menu()
	_music = AudioStreamPlayer.new()
	_music.stream = Assets.sound("menu_music")
	_music.bus = "Music"
	add_child(_music)
	_music.play()
	CreditsScreen.check_all()


## a real hallway from the game, rendered behind the menu with a slow drifting camera
func _make_backdrop() -> void:
	var box := SubViewportContainer.new()
	box.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	box.stretch = true
	box.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(box)
	var vp := SubViewport.new()
	vp.own_world_3d = true
	vp.msaa_3d = Viewport.MSAA_2X
	box.add_child(vp)
	var env := Environment.new()
	env.background_mode = Environment.BG_COLOR
	env.background_color = Color.BLACK
	env.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	env.ambient_light_color = Color(0.8, 0.85, 1.0)
	env.ambient_light_energy = 0.12
	env.fog_enabled = true
	env.fog_light_color = Color(0.02, 0.02, 0.025)
	env.fog_density = 0.06
	env.tonemap_mode = Environment.TONE_MAPPER_ACES
	env.glow_enabled = true
	env.glow_intensity = 0.4
	var we := WorldEnvironment.new()
	we.environment = env
	vp.add_child(we)
	var gen := RoomGenerator.new()
	vp.add_child(gen)
	# a long locker-ish stretch somewhere past a-100, dim and foggy
	gen.start(77031, 118)
	_cam = Camera3D.new()
	_cam.fov = 62
	_cam_start = gen.room(118).global_transform * Transform3D(Basis(Vector3.UP, PI), Vector3(0, 1.55, 0.8))
	_cam.transform = _cam_start
	vp.add_child(_cam)
	_cam.current = true
	# a weak flashlight-like glow so the hallway reads
	var l := SpotLight3D.new()
	l.spot_angle = 30
	l.spot_range = 18
	l.light_energy = 1.0
	l.light_color = Color(1, 0.95, 0.85)
	_cam.add_child(l)


func _make_menu() -> void:
	var col := VBoxContainer.new()
	col.add_theme_constant_override("separation", 10)
	col.position = Vector2(64, 0)
	col.custom_minimum_size = Vector2(360, 0)
	col.set_anchors_preset(Control.PRESET_LEFT_WIDE)
	col.offset_left = 64
	col.offset_right = 64 + 380
	col.alignment = BoxContainer.ALIGNMENT_CENTER
	add_child(col)
	_title = UIKit.label("rooms:", 30, Color(0.85, 0.85, 0.85))
	col.add_child(_title)
	var big := UIKit.label("the hallway", 58, Color(0.97, 0.97, 0.95))
	big.add_theme_color_override("font_outline_color", Color(0, 0, 0))
	big.add_theme_constant_override("outline_size", 6)
	col.add_child(big)
	var tag := Label.new()
	tag.text = "keep walking."
	tag.add_theme_font_override("font", Assets.font("handwriting"))
	tag.add_theme_font_size_override("font_size", 30)
	tag.add_theme_color_override("font_color", Color(0.75, 0.2, 0.2))
	col.add_child(tag)
	var stats := UIKit.label("best %s   ·   %d gold   ·   %d deaths" % [Game.door_label(int(Save.data.best_door)), int(Save.data.gold), int(Save.data.deaths)], 15, UIKit.ACCENT)
	col.add_child(stats)
	var gap := Control.new()
	gap.custom_minimum_size = Vector2(0, 14)
	col.add_child(gap)
	if Save.has_run():
		col.add_child(_menu_button("continue  (%s)" % Game.door_label(int(Save.data.run.get("door", 0))), Game.continue_run, 26))
		col.add_child(_menu_button("new run", func(): Game.start_run(_admin)))
	else:
		col.add_child(_menu_button("play", func(): Game.start_run(_admin), 26))
	col.add_child(_menu_button("journal", func(): add_child(JournalScreen.new())))
	col.add_child(_menu_button("achievements", func(): add_child(AchievementsScreen.new())))
	col.add_child(_menu_button("settings", func(): add_child(SettingsMenu.new())))
	col.add_child(_menu_button("credits", func(): add_child(CreditsScreen.new())))
	if not OS.has_feature("mobile") and not OS.has_feature("web"):
		col.add_child(_menu_button("quit", func(): get_tree().quit()))
	var gap2 := Control.new()
	gap2.custom_minimum_size = Vector2(0, 6)
	col.add_child(gap2)
	col.add_child(UIKit.check("admin panel (progress won't be saved)", false, func(on): _admin = on))
	var foot := UIKit.label("unofficial fan game - not affiliated with doors, rooms or roblox", 12, Color(0.45, 0.45, 0.45))
	foot.set_anchors_preset(Control.PRESET_BOTTOM_LEFT)
	foot.offset_left = 16
	foot.offset_top = -28
	add_child(foot)


## text-only menu buttons that light up when hovered, like a lot of horror games
func _menu_button(text: String, on_press: Callable, size := 22) -> Button:
	var b := Button.new()
	b.text = text
	b.flat = true
	b.alignment = HORIZONTAL_ALIGNMENT_LEFT
	b.add_theme_font_size_override("font_size", size)
	b.add_theme_color_override("font_color", Color(0.72, 0.72, 0.72))
	b.add_theme_color_override("font_hover_color", Color(1, 1, 1))
	b.add_theme_color_override("font_focus_color", Color(1, 0.92, 0.6))
	b.add_theme_color_override("font_pressed_color", UIKit.ACCENT)
	b.custom_minimum_size = Vector2(0, size + 14)
	b.pressed.connect(on_press)
	b.mouse_entered.connect(func(): b.text = "› " + text)
	b.mouse_exited.connect(func(): b.text = text)
	return b


func _process(delta: float) -> void:
	_t += delta
	if _cam:
		# slow drift down the hallway with a little sway, loops every 40 seconds
		var k := fmod(_t, 40.0) / 40.0
		var sway := Vector3(sin(_t * 0.4) * 0.15, sin(_t * 0.7) * 0.04, 0)
		_cam.transform = _cam_start.translated_local(Vector3(0, 0, -k * 9.0) + sway)
		_cam.rotate_object_local(Vector3.UP, sin(_t * 0.23) * 0.08)
	# the title flickers now and then like a dying light
	_flicker -= delta
	if _flicker <= 0.0:
		_flicker = randf_range(0.05, 3.0)
		_title.modulate.a = 0.35 if randf() < 0.3 else 1.0
