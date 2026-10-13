class_name A70
extends Entity
## a-70, the locker mimic (from rooms: revisited): it looks just like a locker, except for
## a faint face on the door. try to hide in it and it bursts out and chases you, hurting you
## on contact. run. it gives up after a while, or when you're two doors away.

const RULES := {"trigger": "manual", "floor": ["offices"], "group": "a70"}
const SPEED := 5.2       # slower than sprinting, faster than walking
const DAMAGE := 30.0
const LIFE := 14.0

var start := Vector3.ZERO
var _room_n := 0
var _hit_cd := 0.0
var _t := 0.0
var sprite: Sprite3D


func begin() -> void:
	id = "a70"
	_room_n = Game.door
	global_position = start + Vector3(0, 1.1, 0)
	sprite = make_sprite("a70_face", 1.8)
	var l := OmniLight3D.new()
	l.light_color = Color(0.6, 0.6, 0.65)
	l.light_energy = 1.5
	l.omni_range = 5.0
	add_child(l)
	make_3d_audio("a70_scream", 12.0, 60.0).play()
	Game.play_ui("hurt")


func _physics_process(delta: float) -> void:
	if _done or is_frozen() or player == null or player.dead:
		return
	_t += delta
	if _t > LIFE or Game.door >= _room_n + 2:
		finish()
		return
	var target: Vector3 = player.global_position + Vector3(0, 1.1, 0)
	var to := target - global_position
	to.y = 0.0
	if to.length() > 0.05:
		global_position += to.normalized() * minf(SPEED * delta, to.length())
	sprite.offset = Vector2(randf_range(-3, 3), randf_range(-3, 3))
	_hit_cd -= delta
	if _hit_cd <= 0.0 and global_position.distance_to(target) < 1.0:
		_hit_cd = 1.2
		hit_player = true
		Game.death_details["a70"] = "caught"
		player.take_damage(DAMAGE, "a70")
