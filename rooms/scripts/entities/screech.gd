class_name Screech
extends Entity
## screech (from doors, "got any more doors?" modifier): only in dark rooms. you hear a
## "psst", and it's hiding somewhere next to or behind you. turn and look at it within a
## couple of seconds and it leaves. don't, and it bites.

const RULES := {
	"trigger": "timer", "min_door": 10, "timer": [40.0, 120.0], "chance": 0.6, "cooldown": 30.0,
	"group": "screech", "blocked_by": ["a90", "a90b", "seek"], "needs_mod": "more_doors",
	"needs_dark": true,
}
const TIME_TO_LOOK := 2.6
const DAMAGE := 40.0

var _offset := Vector3.ZERO
var _t := TIME_TO_LOOK
var sprite: Sprite3D


func begin() -> void:
	id = "screech"
	if player.hidden:
		finish()
		return
	# somewhere around you, mostly behind
	var cam_yaw: float = player.camera.global_rotation.y
	var ang := cam_yaw + PI + randf_range(-1.6, 1.6)
	_offset = Vector3(sin(ang), 0, cos(ang)) * 1.6 + Vector3(0, randf_range(-0.3, 0.5), 0)
	global_position = player.head_position() + _offset
	sprite = make_sprite("screech_face", 0.9)
	var psst := make_3d_audio("screech_psst", 4.0, 20.0)
	psst.play()


func _process(delta: float) -> void:
	if _done or is_frozen() or player == null or player.dead:
		return
	if player.hidden:
		finish()
		return
	global_position = player.head_position() + _offset
	if _looked_at():
		var a := AudioStreamPlayer.new()
		a.stream = Assets.sound("screech_caught")
		a.bus = "SFX"
		get_parent().add_child(a)
		a.play()
		a.finished.connect(a.queue_free)
		finish()
		return
	_t -= delta
	if _t <= 0.0:
		_bite()


func _looked_at() -> bool:
	var cam: Camera3D = player.camera
	var to := (global_position - cam.global_position).normalized()
	return (-cam.global_transform.basis.z).dot(to) > 0.85


func _bite() -> void:
	hit_player = true
	Game.death_details["screech"] = "missed"
	var a := AudioStreamPlayer.new()
	a.stream = Assets.sound("screech_bite")
	a.bus = "Alert"
	get_parent().add_child(a)
	a.play()
	a.finished.connect(a.queue_free)
	_bite_flash()
	player.take_damage(DAMAGE, "screech")
	finish()


## its face jumps at the screen for a moment
func _bite_flash() -> void:
	var layer := CanvasLayer.new()
	layer.layer = 60
	get_parent().add_child(layer)
	var vs := get_viewport().get_visible_rect().size
	var t := TextureRect.new()
	t.texture = Assets.texture("screech_face")
	t.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
	t.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_CENTERED
	t.size = vs
	t.pivot_offset = vs / 2.0
	t.scale = Vector2(0.4, 0.4)
	t.mouse_filter = Control.MOUSE_FILTER_IGNORE
	layer.add_child(t)
	var tw := layer.create_tween()
	tw.tween_property(t, "scale", Vector2(1.6, 1.6), 0.18)
	tw.tween_interval(0.25)
	tw.tween_callback(layer.queue_free)
