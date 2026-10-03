class_name Eyes
extends Entity
## eyes (from doors, "got any more doors?" modifier): a ball of eyes floats in the room you
## just walked into, with a purple glow. looking at it hurts, a lot. look at the floor and
## walk past. it stays behind once you're two doors further.

const RULES := {
	"trigger": "door", "min_door": 5, "chance": 0.1, "cooldown": 30.0,
	"group": "eyes", "blocked_by": ["seek"], "needs_mod": "more_doors",
}
const DAMAGE := 8.0
const TICK := 0.25

var _room_n := 0
var _tick := 0.0
var sprite: Sprite3D


func begin() -> void:
	id = "eyes"
	_room_n = Game.door
	var r: RoomBase = generator.room(_room_n)
	if r == null:
		finish()
		return
	var pts := r.path_global()
	var mid: Vector3 = pts[pts.size() / 2]
	global_position = mid + Vector3(0, 0.6, 0)
	sprite = make_sprite("eyes_face", 1.3)
	var l := OmniLight3D.new()
	l.light_color = Color(0.6, 0.3, 1.0)
	l.light_energy = 2.5
	l.omni_range = 8.0
	add_child(l)
	var hum := make_3d_audio("eyes_ambience", 6.0, 30.0)
	hum.play()


func _process(delta: float) -> void:
	if _done or is_frozen() or player == null or player.dead:
		return
	if Game.door >= _room_n + 2 or generator.room(_room_n) == null:
		finish()
		return
	sprite.offset.y = sin(Time.get_ticks_msec() / 400.0) * 6.0
	_tick -= delta
	if _tick > 0.0 or player.hidden or not looking_at_me():
		return
	_tick = TICK
	hit_player = true
	Game.death_details["eyes"] = "looked"
	Game.play_ui("hurt")
	var a := make_3d_audio("eyes_hurt", 10.0, 40.0)
	a.play()
	a.finished.connect(a.queue_free)
	player.take_damage(DAMAGE, "eyes")


func looking_at_me() -> bool:
	var cam: Camera3D = player.camera
	var to := global_position - cam.global_position
	if to.length() > 30.0:
		return false
	var fwd := -cam.global_transform.basis.z
	return fwd.dot(to.normalized()) > 0.82 and sees_player()
