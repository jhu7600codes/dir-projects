class_name A150
extends ScreenEntity
## a-150, "star" (nico's unreleased monster from rooms, the yellow face with star eyes).
## it shows up out of nowhere in the dark rooms and watches. while it's here, any light is
## fatal: turn your flashlight (or shakelight) off and wait until it's gone.
## you get a moment to switch it off after it appears.

const RULES := {
	"floor": ["offices"], "trigger": "timer", "timer": [150.0, 260.0], "chance": 0.55,
	"min_door": 150, "needs_dark": true, "cooldown": 90.0,
	"group": "star", "blocked_by": ["a90", "a90b", "a60b"],
}
const GRACE := 1.4

var _face: TextureRect
var _tint: ColorRect
var _hum: AudioStreamPlayer
var _t := 0.0
var _stay := 14.0
var _leaving := false


func begin() -> void:
	id = "a150"
	_stay = randf_range(12.0, 17.0)
	make_overlay()
	_tint = full_rect(Color(1.0, 0.85, 0.2, 0.0))
	_face = TextureRect.new()
	_face.texture = Assets.texture("a150_face")
	_face.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
	_face.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_CENTERED
	_face.mouse_filter = Control.MOUSE_FILTER_IGNORE
	_face.modulate.a = 0.0
	root.add_child(_face)
	_place_face()
	_hum = AudioStreamPlayer.new()
	_hum.stream = Assets.sound("a150_hum")
	_hum.bus = "SFX"
	_hum.volume_db = -6.0
	add_child(_hum)
	_hum.play()
	var tw := create_tween()
	tw.tween_property(_face, "modulate:a", 0.85, 0.6)
	tw.parallel().tween_property(_tint, "color:a", 0.07, 0.6)


func _place_face() -> void:
	var vs := root.get_viewport_rect().size
	var h := vs.y * 0.24
	_face.size = Vector2(h, h)
	_face.position = Vector2((vs.x - h) / 2.0, vs.y * 0.06)
	_face.pivot_offset = _face.size / 2.0


func _process(delta: float) -> void:
	if _done or is_frozen() or player == null or player.dead:
		return
	_t += delta
	_face.scale = Vector2.ONE * (1.0 + sin(_t * 3.0) * 0.04)
	if _leaving:
		return
	if _t > GRACE and player.lights.is_lit():
		_catch()
		return
	if _t > _stay:
		_leaving = true
		var tw := create_tween()
		tw.tween_property(_face, "modulate:a", 0.0, 0.8)
		tw.parallel().tween_property(_tint, "color:a", 0.0, 0.8)
		tw.parallel().tween_property(_hum, "volume_db", -40.0, 0.8)
		tw.tween_callback(finish)


func _catch() -> void:
	hit_player = true
	Game.death_details["a150"] = "light_on"
	play_alert("a150_kill")
	var tw := create_tween()
	tw.tween_property(_face, "scale", Vector2(3.5, 3.5), 0.25)
	tw.parallel().tween_property(_face, "modulate:a", 1.0, 0.1)
	player.take_damage(999, "a150")
