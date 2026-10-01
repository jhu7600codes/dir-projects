class_name A60
extends Rusher
## a-60: charges through the rooms from behind, rush style. only comes when you open a door
## into a room with lockers. crackling static first, a high hiss when it's close.
## kills you unless you're in a locker (or it can't see you).

const RULES := {
	"trigger": "door", "min_door": 60, "chance": 0.3,
	"rare_min": 15, "rare_chance": 0.04,
	"needs_spawn_room": true, "cooldown": 40.0,
	"group": "rusher", "blocked_by": ["a60b"],
}

var slowed := false  # set when a-90 spawned together with it
var _faces: Array[Texture2D] = []
var _face_t := 0.0
var _lingered := false


func begin() -> void:
	id = "a60"
	far_key = "a60_far"
	near_key = "a60_near"
	speed = 18.0 if slowed else 32.0
	place(true)
	setup_look("a60_face_1", Color(1.0, 0.15, 0.1), 2.6)
	for k in ["a60_face_1", "a60_face_2", "a60_face_3"]:
		_faces.append(Assets.glow_texture(k))
	set_visible_body(false)
	var rumble := make_3d_audio("a60_rumble", 30.0, 200.0)
	rumble.play()
	# a few seconds of warning, then it goes
	after(5.0, func():
		set_visible_body(true)
		moving = true)


func _process(delta: float) -> void:
	if is_frozen() or sprite == null:
		return
	_face_t -= delta
	if _face_t <= 0.0:
		_face_t = 0.08
		set_sprite_texture(sprite, _faces[randi() % _faces.size()])
		sprite.offset = Vector2(randf_range(-6, 6), randf_range(-6, 6))


## at the closed door it sometimes lingers: rushes back a bit to re-check, then leaves
func on_path_end() -> void:
	if not _lingered and randf() < 0.5:
		_lingered = true
		reverse()
		after(randf_range(0.6, 1.2), reverse)
		return
	finish()
