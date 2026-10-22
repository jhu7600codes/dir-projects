class_name A10
extends Rusher
## a-10 "the locker and table kicker" (the sightings): heavy kicks bang closer and closer,
## then it storms through the rooms and kicks every locker and table it passes.
## if you're hiding in one when it gets kicked, you're dead. stay out in the open.

const RULES := {
	"trigger": "door", "min_door": 10, "chance": 0.16,
	"rare_min": 4, "rare_chance": 0.03,
	"cooldown": 45.0, "group": "rusher", "blocked_by": ["a60b"],
	"needs_sightings": true,
}
const KICK_RANGE := 5.0

var glitch_k := 1.0
var _kicked := {}
var _bang_t := 0.0


func begin() -> void:
	id = "a10"
	speed = 22.0
	place(true)
	setup_look("a10_face", Color(0.9, 0.6, 0.3), 2.4)
	set_sprite_texture(sprite, Faces.a10_tex())
	sprite.pixel_size = 2.4 / maxf(1.0, sprite.texture.get_height())
	set_visible_body(false)
	_bang_t = 0.2
	after(4.0, func():
		set_visible_body(true)
		moving = true)


func _process(delta: float) -> void:
	if is_frozen():
		return
	# the warning: kicks banging somewhere behind you, faster and louder
	if not moving:
		_bang_t -= delta
		if _bang_t <= 0.0:
			_bang_t = randf_range(0.35, 0.7)
			_bang(global_position, -16.0)
		return
	if sprite:
		sprite.offset = Vector2(randf_range(-3, 3), randf_range(-2, 2))
	if remote:
		return
	var r: RoomBase = generator.room(room_n)
	if r == null:
		return
	for lk in r.lockers + r.tables:
		if not is_instance_valid(lk) or _kicked.has(lk):
			continue
		var d := Vector2(lk.global_position.x - global_position.x, lk.global_position.z - global_position.z).length()
		if d < KICK_RANGE:
			_kicked[lk] = true
			_kick(lk)


func _kick(lk: Locker) -> void:
	_bang(lk.global_position + Vector3.UP, -2.0)
	# the hiding spot jumps from the kick
	var tw := lk.create_tween()
	tw.tween_property(lk, "position", lk.position + lk.basis.z * -0.06, 0.05)
	tw.tween_property(lk, "position", lk.position, 0.12)
	lk.play_door()
	if player and not player.dead and player.hidden and player.current_locker == lk:
		hit_player = true
		Game.death_details[id] = "table" if lk.style == "table" else "locker"
		player.take_damage(999, id)


func _bang(at: Vector3, db: float) -> void:
	var a := AudioStreamPlayer3D.new()
	a.stream = Assets.sound("a10_kick")
	a.bus = "SFX"
	a.unit_size = 12.0
	a.max_distance = 120.0
	a.volume_db = db
	a.pitch_scale = randf_range(0.85, 1.1)
	get_parent().add_child(a)
	a.global_position = at
	a.play()
	a.finished.connect(a.queue_free)


## out in the open you're fine. it only cares about lockers and tables
func should_kill(_dist: float) -> bool:
	return false
