extends Node
## settings: volume, look sensitivity, graphics quality and touch layout.
## also sets up the input map and audio buses, so it is the first autoload.
## saved to user://settings.json

signal changed

const PATH := "user://settings.json"

var data := {
	"master_volume": 0.9,
	"music_volume": 0.7,
	"sfx_volume": 1.0,
	"sensitivity": 1.0,
	"fov": 75.0,
	"quality": 1,               # 0 low, 1 medium, 2 high
	"fullscreen": true,         # desktop only, f11 toggles it
	"auto_doors": true,         # doors open by themselves when you walk up to them
	"touch_controls": "auto",   # auto, on, off
	"touch_left_handed": false, # swaps the stick and the buttons
	"touch_scale": 1.0,
	"touch_opacity": 0.55,
}


func _ready() -> void:
	if OS.has_feature("mobile"):
		Engine.max_fps = 60  # no point drawing faster than the screen, saves battery and heat
	_setup_input()
	_setup_buses()
	load_settings()
	apply()


func load_settings() -> void:
	if not FileAccess.file_exists(PATH):
		# first launch: phones start on low quality
		if OS.has_feature("mobile"):
			data.quality = 0
		return
	var parsed = JSON.parse_string(FileAccess.get_file_as_string(PATH))
	if parsed is Dictionary:
		for k in parsed:
			if data.has(k):
				data[k] = parsed[k]


func save_settings() -> void:
	var f := FileAccess.open(PATH, FileAccess.WRITE)
	if f:
		f.store_string(JSON.stringify(data, "\t"))


func set_value(key: String, value) -> void:
	data[key] = value
	apply()
	save_settings()


func apply() -> void:
	_set_volume("Master", data.master_volume)
	_set_volume("Music", data.music_volume)
	for b in ["SFX", "Ambience", "Alert"]:
		_set_volume(b, data.sfx_volume)
	var vp := get_viewport()
	var q := int(data.quality)
	vp.msaa_3d = [Viewport.MSAA_DISABLED, Viewport.MSAA_2X, Viewport.MSAA_4X][q]
	vp.scaling_3d_scale = [0.7, 0.85, 1.0][q]
	vp.positional_shadow_atlas_size = [1024, 2048, 4096][q]
	_apply_window()
	changed.emit()


func _apply_window() -> void:
	if OS.has_feature("mobile") or DisplayServer.get_name() == "headless":
		return
	var want := DisplayServer.WINDOW_MODE_FULLSCREEN if data.fullscreen else DisplayServer.WINDOW_MODE_WINDOWED
	if DisplayServer.window_get_mode() != want:
		DisplayServer.window_set_mode(want)


func _input(event: InputEvent) -> void:
	if event.is_action_pressed("fullscreen"):
		set_value("fullscreen", not data.fullscreen)
		get_viewport().set_input_as_handled()


func use_touch() -> bool:
	match str(data.touch_controls):
		"on":
			return true
		"off":
			return false
	return OS.has_feature("mobile") or OS.has_feature("web_android") or OS.has_feature("web_ios")


func _set_volume(bus: String, linear: float) -> void:
	var i := AudioServer.get_bus_index(bus)
	if i >= 0:
		AudioServer.set_bus_volume_db(i, linear_to_db(maxf(linear, 0.0001)))


# audio buses: a-90 mutes everything except "Alert" while it is on screen
func _setup_buses() -> void:
	for b in ["Music", "SFX", "Ambience", "Alert"]:
		if AudioServer.get_bus_index(b) == -1:
			AudioServer.add_bus()
			var i := AudioServer.bus_count - 1
			AudioServer.set_bus_name(i, b)
			AudioServer.set_bus_send(i, "Master")


func set_world_audio_muted(muted: bool) -> void:
	for b in ["Music", "SFX", "Ambience"]:
		AudioServer.set_bus_mute(AudioServer.get_bus_index(b), muted)


# ---- input map ----------------------------------------------------------

func _setup_input() -> void:
	_add("move_forward", [_key(KEY_W), _key(KEY_UP), _axis(JOY_AXIS_LEFT_Y, -1)])
	_add("move_back", [_key(KEY_S), _key(KEY_DOWN), _axis(JOY_AXIS_LEFT_Y, 1)])
	_add("move_left", [_key(KEY_A), _key(KEY_LEFT), _axis(JOY_AXIS_LEFT_X, -1)])
	_add("move_right", [_key(KEY_D), _key(KEY_RIGHT), _axis(JOY_AXIS_LEFT_X, 1)])
	_add("look_left", [_axis(JOY_AXIS_RIGHT_X, -1)])
	_add("look_right", [_axis(JOY_AXIS_RIGHT_X, 1)])
	_add("look_up", [_axis(JOY_AXIS_RIGHT_Y, -1)])
	_add("look_down", [_axis(JOY_AXIS_RIGHT_Y, 1)])
	_add("sprint", [_key(KEY_SHIFT), _btn(JOY_BUTTON_LEFT_STICK)])
	_add("crouch", [_key(KEY_C), _key(KEY_CTRL), _btn(JOY_BUTTON_B)])
	_add("jump", [_key(KEY_SPACE), _btn(JOY_BUTTON_A)])
	_add("interact", [_key(KEY_E), _btn(JOY_BUTTON_X)])
	# every item is used with left click; 1-4 or the scroll wheel picks which one you hold
	_add("use_item", [_mouse(MOUSE_BUTTON_LEFT), _btn(JOY_BUTTON_Y), _btn(JOY_BUTTON_RIGHT_SHOULDER)])
	_add("slot_1", [_key(KEY_1)])
	_add("slot_2", [_key(KEY_2)])
	_add("slot_3", [_key(KEY_3)])
	_add("slot_4", [_key(KEY_4)])
	_add("item_next", [_mouse(MOUSE_BUTTON_WHEEL_DOWN), _btn(JOY_BUTTON_DPAD_RIGHT)])
	_add("item_prev", [_mouse(MOUSE_BUTTON_WHEEL_UP), _btn(JOY_BUTTON_DPAD_LEFT)])
	_add("pause", [_key(KEY_ESCAPE), _key(KEY_P), _btn(JOY_BUTTON_START)])
	_add("fullscreen", [_key(KEY_F11)])
	_add("admin_panel", [_key(KEY_F1), _key(KEY_QUOTELEFT), _btn(JOY_BUTTON_BACK)])


func _add(action: String, events: Array) -> void:
	if not InputMap.has_action(action):
		InputMap.add_action(action, 0.25)
	for e in events:
		InputMap.action_add_event(action, e)


func _key(k: Key) -> InputEventKey:
	var e := InputEventKey.new()
	e.physical_keycode = k
	return e


func _mouse(b: MouseButton) -> InputEventMouseButton:
	var e := InputEventMouseButton.new()
	e.button_index = b
	return e


func _btn(b: JoyButton) -> InputEventJoypadButton:
	var e := InputEventJoypadButton.new()
	e.button_index = b
	return e


func _axis(a: JoyAxis, v: float) -> InputEventJoypadMotion:
	var e := InputEventJoypadMotion.new()
	e.axis = a
	e.axis_value = v
	return e
