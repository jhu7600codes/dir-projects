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

## the red handwritten line under the logo, a different one every time
const TAGLINES := [
	"keep walking.",
	"don't stop.",
	"the lights hum louder when you stop.",
	"it's only a few more rooms.",
	"hide when you hear it.",
	"your badge just says worker.",
	"freeze when it knocks.",
	"same door. same sign. same door.",
	"the exit is warm.",
	"someone wrote on the walls.",
	"did you hear that?",
	"don't look back.",
	"white means hide.",
	"there is no a-1001.",
	"clock in.",
	"nobody else came to work today.",
	"your feet hurt.",
	"is that crackling getting closer?",
]

var _admin := false
var _music: AudioStreamPlayer
var _cam: Camera3D
var _cam_start := Transform3D.IDENTITY
var _t := 0.0
var _title: Control
var _flicker := 0.0


func _ready() -> void:
	set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	Input.mouse_mode = Input.MOUSE_MODE_VISIBLE
	# the admin toggle picks which progress is shown and used
	_admin = Game.menu_admin
	Save.use_profile("admin" if _admin else "main")
	Prewarm.run()
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


## shots for the background: real places from the game, each shown for a few seconds
## with a slow camera move, then a fade to the next one.
##   door: which room, z: how far in the camera starts, dolly: meters it moves forward,
##   dark: 0 lit .. 1 pitch black, light: camera carries a flashlight, a60: a-60 glimpse
const SHOTS := [
	{"door": 118, "z": 0.8, "dolly": 8.0, "dark": 0.7, "light": true},
	{"door": 0, "z": 1.0, "dolly": 5.0, "dark": 0.0, "light": false},
	{"door": 100, "z": 4.0, "dolly": 16.0, "dark": 0.45, "light": false},
	{"door": 47, "z": 0.8, "dolly": 3.0, "dark": 0.15, "light": false, "a60": true},
	{"door": -1, "z": 1.0, "dolly": 3.5, "dark": 0.6, "light": false},  # -1 = an exit room
	{"door": 165, "z": 0.8, "dolly": 7.0, "dark": 1.0, "light": true},
	{"door": 1000, "z": 3.0, "dolly": 22.0, "dark": 1.0, "light": false},
]
const SHOT_TIME := 9.0
const SEED := 77031

var _gen: RoomGenerator
var _env: Environment
var _cam_light: SpotLight3D
var _fade: ColorRect
var _place: Label
var _shot := -1
var _shot_t := 0.0
var _dolly := 0.0
var _extra: Node3D  # per-shot things like the a-60 glimpse
var _fading := false


func _make_backdrop() -> void:
	var box := SubViewportContainer.new()
	box.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	box.stretch = true
	# low quality (phones): render the background at half resolution
	box.stretch_shrink = 2 if int(Settings.data.quality) == 0 else 1
	box.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(box)
	var vp := SubViewport.new()
	vp.own_world_3d = true
	vp.msaa_3d = Viewport.MSAA_DISABLED if int(Settings.data.quality) == 0 else Viewport.MSAA_2X
	box.add_child(vp)
	_env = Environment.new()
	_env.background_mode = Environment.BG_COLOR
	_env.background_color = Color.BLACK
	_env.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	_env.ambient_light_color = Color(0.8, 0.85, 1.0)
	_env.fog_enabled = true
	_env.fog_light_color = Color(0.02, 0.02, 0.025)
	_env.tonemap_mode = Environment.TONE_MAPPER_ACES
	_env.glow_enabled = true
	_env.glow_intensity = 0.4
	var we := WorldEnvironment.new()
	we.environment = _env
	vp.add_child(we)
	_gen = RoomGenerator.new()
	_gen.run_seed = SEED
	vp.add_child(_gen)
	_cam = Camera3D.new()
	_cam.fov = 62
	vp.add_child(_cam)
	_cam.current = true
	_cam_light = SpotLight3D.new()
	_cam_light.spot_angle = 30
	_cam_light.spot_range = 18
	_cam_light.light_color = Color(1, 0.95, 0.85)
	_cam.add_child(_cam_light)
	# black overlay for the fades between shots
	_fade = ColorRect.new()
	_fade.color = Color.BLACK
	_fade.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	_fade.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(_fade)
	# where we are, written small in the corner
	_place = Label.new()
	_place.add_theme_font_override("font", Assets.font("handwriting"))
	_place.add_theme_font_size_override("font_size", 28)
	_place.add_theme_color_override("font_color", Color(0.95, 0.85, 0.4, 0.8))
	_place.set_anchors_and_offsets_preset(Control.PRESET_BOTTOM_RIGHT)
	_place.offset_left = -160
	_place.offset_top = -56
	_place.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(_place)
	_next_shot()


func _next_shot() -> void:
	_shot = (_shot + 1) % SHOTS.size()
	var sh: Dictionary = SHOTS[_shot]
	var n: int = sh.door
	if n == -1:
		n = 260
		for k in range(201, 600):
			if _gen._is_exit_room(k):
				n = k
				break
	_gen.start(SEED, n)
	var r := _gen.room(n)
	_cam_start = r.global_transform * Transform3D(Basis(Vector3.UP, PI), Vector3(0, 1.55, float(sh.z)))
	if sh.door == -1:
		# look toward the hole in the wall where the exit door glows
		_cam_start = _cam_start.rotated_local(Vector3.UP, -0.5)
	_dolly = float(sh.dolly)
	var d: float = sh.dark
	_env.ambient_light_energy = lerpf(0.3, 0.02, d)
	_env.fog_density = lerpf(0.01, 0.07, d)
	_cam_light.visible = sh.light
	_cam_light.light_energy = 1.2
	if _extra:
		_extra.queue_free()
		_extra = null
	if sh.get("a60", false):
		_extra = _a60_glimpse(r)
		_gen.add_child(_extra)
	_place.text = Game.door_label(n)
	_shot_t = 0.0
	_cam.transform = _cam_start
	var tw := create_tween()
	tw.tween_property(_fade, "color:a", 0.0, 1.0)


## a red glow with a face at the far end of the room, rushing past slowly
func _a60_glimpse(r: RoomBase) -> Node3D:
	var root := Node3D.new()
	var pts := r.path_global()
	root.position = pts[pts.size() - 1] + Vector3(0, 0.2, 0)
	var s := Sprite3D.new()
	s.texture = Assets.glow_texture("a60_face_1")
	s.pixel_size = 2.4 / maxf(1.0, s.texture.get_height())
	var m := StandardMaterial3D.new()
	m.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
	m.blend_mode = BaseMaterial3D.BLEND_MODE_ADD
	m.billboard_mode = BaseMaterial3D.BILLBOARD_ENABLED
	m.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	m.albedo_texture = s.texture
	s.material_override = m
	root.add_child(s)
	var l := OmniLight3D.new()
	l.light_color = Color(1, 0.15, 0.1)
	l.light_energy = 3.0
	l.omni_range = 9.0
	root.add_child(l)
	return root


func _make_menu() -> void:
	var col := VBoxContainer.new()
	col.add_theme_constant_override("separation", 4)
	col.position = Vector2(64, 0)
	col.custom_minimum_size = Vector2(360, 0)
	col.set_anchors_preset(Control.PRESET_LEFT_WIDE)
	col.offset_left = 64
	col.offset_right = 64 + 380
	col.alignment = BoxContainer.ALIGNMENT_CENTER
	add_child(col)
	# the logo (assets/branding/logo.png)
	var logo := TextureRect.new()
	logo.texture = load("res://assets/branding/logo.png")
	logo.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
	logo.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT
	logo.custom_minimum_size = Vector2(400, 400.0 * 811.0 / 1892.0)
	col.add_child(logo)
	_title = logo
	var tag := Label.new()
	tag.text = TAGLINES[randi() % TAGLINES.size()]
	tag.add_theme_font_override("font", Assets.font("handwriting"))
	tag.add_theme_font_size_override("font_size", 30)
	tag.add_theme_color_override("font_color", Color(0.75, 0.2, 0.2))
	col.add_child(tag)
	var stats := UIKit.label(("admin progress   ·   " if _admin else "") + "best %s   ·   %d gold   ·   %d deaths" % [Game.a_label(int(Save.data.best_door)), int(Save.data.gold), int(Save.data.deaths)], 15, Color(1, 0.6, 0.3) if _admin else UIKit.ACCENT)
	col.add_child(stats)
	var gap := Control.new()
	gap.custom_minimum_size = Vector2(0, 14)
	col.add_child(gap)
	if Save.has_run():
		var rd := int(Save.data.run.get("door", 0))
		var where := ("W-%02d" % rd) if str(Save.data.run.get("floor", "offices")) == "wires" else Game.a_label(rd)
		col.add_child(_menu_button("continue  (%s)" % where, Game.continue_run, 26))
		col.add_child(_menu_button("new run", _pick_floor))
	else:
		col.add_child(_menu_button("play", _pick_floor, 26))
	col.add_child(_menu_button("journal", func(): add_child(JournalScreen.new())))
	var mods := Array(Save.data.get("modifiers", []))
	var mod_text := "modifiers" if Game.modifiers_unlocked() else "modifiers  (locked)"
	if Game.modifiers_unlocked() and not mods.is_empty():
		mod_text += "  (%d on)" % mods.size()
	col.add_child(_menu_button(mod_text, func(): add_child(ModifiersScreen.new())))
	col.add_child(_menu_button("achievements", func(): add_child(AchievementsScreen.new())))
	col.add_child(_menu_button("settings", func(): add_child(SettingsMenu.new())))
	col.add_child(_menu_button("credits", func(): add_child(CreditsScreen.new())))
	if not OS.has_feature("mobile") and not OS.has_feature("web"):
		col.add_child(_menu_button("quit", func(): get_tree().quit()))
	var gap2 := Control.new()
	gap2.custom_minimum_size = Vector2(0, 6)
	col.add_child(gap2)
	col.add_child(UIKit.check("admin mode (separate admin progress)", _admin, _toggle_admin))
	var foot := UIKit.label("unofficial fan game - not affiliated with doors, rooms or roblox", 12, Color(0.45, 0.45, 0.45))
	foot.set_anchors_preset(Control.PRESET_BOTTOM_LEFT)
	foot.offset_left = 16
	foot.offset_top = -28
	add_child(foot)
	_fit_menu.call_deferred(col)


## if the menu is still taller than the screen (more buttons, a short window), shrink it
## to fit instead of letting the logo and the admin toggle fall off the edges
func _fit_menu(col: Control) -> void:
	var h := get_viewport_rect().size.y
	var need := col.get_combined_minimum_size().y
	if need > h - 24.0:
		var k := (h - 24.0) / need
		col.scale = Vector2(k, k)
		col.pivot_offset = Vector2(0, h / 2.0)


func _pick_floor() -> void:
	var fs := FloorSelect.new()
	fs.admin = _admin
	add_child(fs)


func _toggle_admin(on: bool) -> void:
	Game.menu_admin = on
	# rebuild the title screen with the other progress (best, gold, continue, journal)
	get_tree().reload_current_scene.call_deferred()


## text-only menu buttons that light up when hovered, like a lot of horror games
func _menu_button(text: String, on_press: Callable, size := 22) -> Button:
	var b := Button.new()
	text = UIKit.cap(text)
	b.text = text
	b.flat = true
	b.alignment = HORIZONTAL_ALIGNMENT_LEFT
	b.add_theme_font_size_override("font_size", size)
	b.add_theme_color_override("font_color", Color(0.72, 0.72, 0.72))
	b.add_theme_color_override("font_hover_color", Color(1, 1, 1))
	b.add_theme_color_override("font_focus_color", Color(1, 0.92, 0.6))
	b.add_theme_color_override("font_pressed_color", UIKit.ACCENT)
	b.custom_minimum_size = Vector2(0, size + 8)
	b.pressed.connect(on_press)
	b.mouse_entered.connect(func(): b.text = "› " + text)
	b.mouse_exited.connect(func(): b.text = text)
	return b


func _process(delta: float) -> void:
	_t += delta
	if _cam:
		_shot_t += delta
		var k := _shot_t / SHOT_TIME
		var sway := Vector3(sin(_t * 0.4) * 0.12, sin(_t * 0.7) * 0.035, 0)
		_cam.transform = _cam_start.translated_local(Vector3(0, 0, -k * _dolly) + sway)
		_cam.rotate_object_local(Vector3.UP, sin(_t * 0.23) * 0.06)
		if _extra:
			# the a-60 glimpse slides across the far doorway
			_extra.get_child(0).modulate.a = 0.6 + randf() * 0.4
		# fade out near the end of the shot, then cut to the next one
		if _shot_t > SHOT_TIME - 1.0 and _fade.color.a < 0.01 and not _fading:
			_fading = true
			var tw := create_tween()
			tw.tween_property(_fade, "color:a", 1.0, 0.9)
			tw.tween_callback(func():
				_fading = false
				_next_shot())
	# the title flickers now and then like a dying light
	_flicker -= delta
	if _flicker <= 0.0:
		_flicker = randf_range(0.05, 3.0)
		_title.modulate.a = 0.35 if randf() < 0.3 else 1.0
