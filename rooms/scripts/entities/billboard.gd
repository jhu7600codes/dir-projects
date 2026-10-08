class_name Billboard
extends Entity
## the great city: a face lights up on a billboard over the street you just walked into.
## looking at it burns. keep your eyes on the road. it switches off two gates later.

const RULES := {
	"trigger": "door", "floor": ["city"], "city_min": 1, "chance": 0.2, "cooldown": 25.0,
	"group": "eyes",
}
const DAMAGE := 6.0
const TICK := 0.25

var _room_n := 0
var _tick := 0.0
var sprite: Sprite3D


func begin() -> void:
	id = "billboard"
	_room_n = Game.door
	var r: RoomBase = generator.room(_room_n)
	if r == null:
		finish()
		return
	var pts := r.path_global()
	var mid: Vector3 = pts[pts.size() / 2]
	var side := r.global_basis.x * (10.6 if randf() < 0.5 else -10.6)
	global_position = mid + side + Vector3(0, 5.5, 0)
	sprite = make_sprite("billboard_face", 4.0)
	sprite.billboard = BaseMaterial3D.BILLBOARD_DISABLED
	sprite.material_override.billboard_mode = BaseMaterial3D.BILLBOARD_DISABLED
	look_at(mid + Vector3(0, 5.5, 0), Vector3.UP)
	sprite.rotation.y = PI
	var l := OmniLight3D.new()
	l.light_color = Color(1.0, 0.25, 0.35)
	l.light_energy = 3.0
	l.omni_range = 12.0
	l.position = Vector3(0, 0, -1.5)
	add_child(l)
	var hum := make_3d_audio("billboard_hum", 8.0, 50.0)
	hum.play()


func _process(delta: float) -> void:
	if _done or is_frozen() or player == null or player.dead:
		return
	if Game.door >= _room_n + 2 or generator.room(_room_n) == null:
		finish()
		return
	_tick -= delta
	if _tick > 0.0 or player.hidden or not _looked_at():
		return
	_tick = TICK
	hit_player = true
	Game.death_details["billboard"] = "looked"
	Game.play_ui("hurt")
	player.take_damage(DAMAGE, "billboard")


func _looked_at() -> bool:
	var cam: Camera3D = player.camera
	var to := global_position - cam.global_position
	if to.length() > 35.0:
		return false
	return (-cam.global_transform.basis.z).dot(to.normalized()) > 0.85 and sees_player()
