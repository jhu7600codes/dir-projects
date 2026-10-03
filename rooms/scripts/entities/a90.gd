class_name A90
extends ScreenEntity
## a-90: a knock, all other audio pauses, a face flashes somewhere on screen, then the red
## stop sign. any input at all while the sign is up (moving, looking, touching the screen,
## pressing a button, getting into a locker) costs 90 health. it freezes all other entities.
## on touch screens: no finger on the screen = safe.

const RULES := {
	"trigger": "timer", "min_door": 90, "timer": [100.0, 400.0], "chance": 1.0,
	"rare_min": 10, "rare_chance": 0.2, "cooldown": 20.0,
	"group": "screen", "blocked_by": ["a90b"],
}
const DAMAGE := 90
const WATCH_TIME := 1.3

var _watching := false
var _caught := false
var _static: ColorRect
var _face: TextureRect
var _sign: TextureRect
var _flick_t := 0.0


func begin() -> void:
	id = "a90"
	manager.frozen = true
	make_overlay()
	play_alert("a90_knock")
	Settings.set_world_audio_muted(true)
	after(0.6, _flash_face)
	after(1.3, _show_sign)
	after(1.3 + WATCH_TIME, _end_watch)


func _flash_face() -> void:
	var vs := root.get_viewport_rect().size
	_face = image("a90_face", vs.y * 0.35)
	_face.position = Vector2(randf_range(0, vs.x - _face.size.x), randf_range(0, vs.y - _face.size.y))
	play_alert("a90_spawn")
	after(0.3, func(): _face.visible = false)


func _show_sign() -> void:
	var vs := root.get_viewport_rect().size
	_static = full_rect(Color(0.6, 0.0, 0.0, 0.45))
	_face.visible = true
	_face.size = Vector2(vs.y * 0.55, vs.y * 0.55)
	center(_face)
	_sign = image("a90_stop", vs.y * 0.5)
	center(_sign)
	_watching = true


func _process(delta: float) -> void:
	if _static:
		_flick_t -= delta
		if _flick_t <= 0.0:
			_flick_t = 0.05
			_static.color.a = randf_range(0.3, 0.55)
	if not _watching:
		return
	# held inputs count too: walking, a finger on the screen, a stick pushed
	if Game.touch_count > 0:
		_trigger("touch")
	elif player and player.move_input().length() > 0.1:
		_trigger("move")
	elif player and player.look_input().length() > 0.1:
		_trigger("look")


func _input(event: InputEvent) -> void:
	if not _watching:
		return
	var moved := false
	var why := "button"
	if event is InputEventMouseMotion:
		why = "look"
		moved = event.relative.length() > 2.0
	elif event is InputEventKey or event is InputEventMouseButton or event is InputEventJoypadButton:
		moved = event.is_pressed()
	elif event is InputEventScreenTouch or event is InputEventScreenDrag:
		moved = true
		why = "touch"
	elif event is InputEventJoypadMotion:
		moved = absf(event.axis_value) > 0.4
	if moved:
		_trigger(why)


func _trigger(why := "button") -> void:
	if _caught:
		return
	Game.death_details["a90"] = why
	_caught = true
	_watching = false
	hit_player = true
	if _sign:
		_sign.visible = false
	if _face:
		_face.visible = false
	jumpscare("a90_jumpscare", "a90_jumpscare_sound", func():
		player.take_damage(DAMAGE, "a90")
		_cleanup())


func _end_watch() -> void:
	if _caught:
		return
	_watching = false
	play_alert("a90_despawn")
	_cleanup()


func _cleanup() -> void:
	Settings.set_world_audio_muted(false)
	manager.frozen = false
	finish()


func _exit_tree() -> void:
	if manager:
		manager.frozen = false
	Settings.set_world_audio_muted(false)
