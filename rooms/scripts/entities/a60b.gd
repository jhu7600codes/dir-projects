class_name A60B
extends Rusher
## a-60b, the multi bounder: a blue, much more persistent a-60. it shows up once, bounces
## between the oldest and newest loaded rooms over and over, and only leaves after you've
## gone through 20 more doors. while it's around you're forced to sprint at double speed,
## a star marker shows where it is, and it has its own boss music.
## in rooms: revisited it comes after a-1005; this game ends at a-1000, so it comes at AT_DOOR.

const AT_DOOR := 505
const ROOMS_TO_SURVIVE := 20
const RULES := {
	"trigger": "door_number", "at_door": AT_DOOR,
	"group": "boss", "blocked_by": [],
}

var _start_door := 0
var _faces: Array[Texture2D] = []
var _face_t := 0.0
var _music: AudioStreamPlayer


func begin() -> void:
	id = "a60b"
	far_key = "a60b_far"
	near_key = "a60b_near"
	speed = 30.0
	stop_at_closed_door = false
	_start_door = Game.door
	place(true)
	setup_look("a60b_face", Color(0.2, 0.45, 1.0), 2.6)
	for k in ["a60b_face", "a60_face_1", "a60_face_2", "a60_face_3"]:
		_faces.append(Assets.glow_texture(k))
	set_visible_body(false)
	_music = AudioStreamPlayer.new()
	_music.stream = Assets.sound("a60b_theme")
	_music.bus = "Music"
	add_child(_music)
	_music.play()
	Game.forced_sprint = true
	Game.tracked_entity = self
	Game.subtitle.emit("something blue is coming...", Color(0.4, 0.6, 1.0))
	after(10.0, func():
		set_visible_body(true)
		moving = true)


func _process(delta: float) -> void:
	if Game.door - _start_door >= ROOMS_TO_SURVIVE:
		finish()
		return
	if sprite == null or is_frozen():
		return
	_face_t -= delta
	if _face_t <= 0.0:
		_face_t = 0.08
		set_sprite_texture(sprite, _faces[randi() % _faces.size()])
		# the a-60 faces get tinted blue, its own face stays as is
		set_sprite_color(sprite, Color.WHITE if sprite.texture == _faces[0] else Color(0.4, 0.6, 1.0))


## disappears for about a second at each end, then comes back the other way
func on_path_end() -> void:
	moving = false
	set_visible_body(false)
	after(1.0, func():
		set_visible_body(true)
		reverse()
		moving = true)


func _exit_tree() -> void:
	Game.forced_sprint = false
	if Game.tracked_entity == self:
		Game.tracked_entity = null
