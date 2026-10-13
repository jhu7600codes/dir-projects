class_name A25
extends Entity
## a-25, warp (from rooms: revisited): a portal opens somewhere in the room you just walked
## into. while you keep an eye on it, it can't do anything. look away for too long and it
## starts pulling you in. it closes again after a while.

const RULES := {
	"trigger": "door", "floor": ["offices"], "min_door": 26, "chance": 0.08,
	"cooldown": 60.0, "group": "warp", "blocked_by": ["a60b", "seek"],
}
const LIFE := 14.0
const GRACE := 1.2   # seconds you can look away before it pulls
const PULL := 3.2    # a bit slower than walking: you can fight it, barely

var _room_n := 0
var _t := 0.0
var _unseen := 0.0
var sprite: Sprite3D
var _hum: AudioStreamPlayer3D


func begin() -> void:
	id = "a25"
	_room_n = Game.door
	var r: RoomBase = generator.room(_room_n)
	if r == null:
		finish()
		return
	var pts := r.path_global()
	var p: Vector3 = pts[clampi(pts.size() / 2, 0, pts.size() - 1)]
	global_position = p + Vector3(randf_range(-1.0, 1.0), -0.2, randf_range(-1.0, 1.0))
	sprite = make_sprite("a25_face", 2.0)
	var l := OmniLight3D.new()
	l.light_color = Color(0.55, 0.3, 1.0)
	l.light_energy = 2.5
	l.omni_range = 7.0
	add_child(l)
	_hum = make_3d_audio("a25_hum", 8.0, 40.0)
	_hum.play()


func _physics_process(delta: float) -> void:
	if _done or is_frozen() or player == null or player.dead:
		return
	_t += delta
	sprite.rotation.z += delta * 2.0
	sprite.offset = Vector2(sin(_t * 9.0), cos(_t * 7.0)) * 2.0
	if _t > LIFE or Game.door >= _room_n + 2:
		finish()
		return
	if player.hidden:
		_unseen = 0.0
		return
	if _looked_at():
		_unseen = 0.0
		_hum.pitch_scale = 1.0
		return
	_unseen += delta
	if _unseen < GRACE:
		return
	_hum.pitch_scale = 1.4
	var to: Vector3 = global_position - player.global_position
	to.y = 0.0
	if to.length() < 0.9:
		hit_player = true
		Game.death_details["a25"] = "pulled"
		player.take_damage(999, "a25")
		return
	player.move_and_collide(to.normalized() * PULL * delta)


func _looked_at() -> bool:
	var cam: Camera3D = player.camera
	var to := global_position - cam.global_position
	return (-cam.global_transform.basis.z).dot(to.normalized()) > 0.7 and sees_player()
