class_name Figure
extends Entity
## figure (from doors, "got any more doors?" modifier): it's blind and walks around the
## room you just entered, listening. sprinting near it, or walking close to it, and it hears
## you and charges. crouch-walk past it, or hide in a locker until you can sneak out.
## it stays in its room.

const RULES := {
	"trigger": "door", "min_door": 20, "chance": 0.07, "cooldown": 90.0,
	"group": "figure", "blocked_by": ["seek", "a60b"], "needs_mod": "more_doors",
}
const WALK_SPEED := 1.6
const CHASE_SPEED := 6.2
const HEAR_SPRINT := 14.0
const HEAR_WALK := 5.0
const HEAR_CROUCH := 1.6

var _room_n := 0
var _target := Vector3.ZERO
var _chasing := false
var _step_t := 0.0
var sprite: Sprite3D
var _steps: AudioStreamPlayer3D


func begin() -> void:
	id = "figure"
	_room_n = Game.door
	var r: RoomBase = generator.room(_room_n)
	if r == null:
		finish()
		return
	var pts := r.path_global()
	# start away from the entrance so you get a moment to see it
	global_position = _floor(pts[pts.size() - 1])
	_pick_target()
	sprite = make_sprite("figure_body", 2.7)
	set_sprite_texture(sprite, Assets.texture("figure_body"))  # it's a cut-out render, no glow edges
	(sprite.material_override as StandardMaterial3D).billboard_mode = BaseMaterial3D.BILLBOARD_FIXED_Y
	sprite.position.y = 1.35
	_steps = make_3d_audio("figure_step", 6.0, 40.0)
	var g := make_3d_audio("figure_growl", 8.0, 50.0)
	g.play()


func _floor(p: Vector3) -> Vector3:
	return Vector3(p.x, p.y - 1.6, p.z)


func _pick_target() -> void:
	var r: RoomBase = generator.room(_room_n)
	if r == null:
		return
	var pts := r.path_global()
	_target = _floor(pts[randi() % pts.size()])


func _process(delta: float) -> void:
	if _done or is_frozen() or player == null or player.dead:
		return
	if Game.door > _room_n or generator.room(_room_n) == null:
		finish()  # you got through its door
		return
	var me := global_position
	var p: Vector3 = player.global_position
	var flat_d := Vector2(me.x - p.x, me.z - p.z).length()
	if not _chasing and not player.hidden and _hears(flat_d):
		_chasing = true
		var g := make_3d_audio("figure_growl", 10.0, 60.0)
		g.pitch_scale = 0.8
		g.play()
	if _chasing and player.hidden:
		_chasing = false  # lost you
		_pick_target()
	var goal := Vector3(p.x, me.y, p.z) if _chasing else _target
	var speed := CHASE_SPEED if _chasing else WALK_SPEED
	var to := goal - me
	to.y = 0.0
	if to.length() < 0.3:
		_pick_target()
	else:
		global_position += to.normalized() * minf(speed * delta, to.length())
	_step_t -= delta
	if _step_t <= 0.0:
		_step_t = 0.35 if _chasing else 0.8
		_steps.play()
	if _chasing and flat_d < 1.2:
		_kill()


func _hears(d: float) -> bool:
	if not player.is_walking():
		return false
	if player.sprinting:
		return d < HEAR_SPRINT
	if player.crouching:
		return d < HEAR_CROUCH
	return d < HEAR_WALK


func _kill() -> void:
	hit_player = true
	Game.death_details["figure"] = "sprinting" if player.sprinting else "heard"
	var a := AudioStreamPlayer.new()
	a.stream = Assets.sound("figure_kill")
	a.bus = "Alert"
	get_parent().add_child(a)
	a.play()
	player.take_damage(999, "figure")
	finish()
