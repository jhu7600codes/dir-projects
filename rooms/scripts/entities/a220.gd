class_name A220
extends Entity
## a-220 (made for this game by jhu): a-200's husband. when he comes, she hides in one of
## the lockers in your room (her scribble glows on the door) and he marches through,
## stopping at every locker to look. he lets her be, but anyone hiding in her locker or the
## lockers right next to it is dead. sometimes she sneaks into YOUR locker just before he
## comes in: get out and find another one, quick. out in the open he kills you too.

const RULES := {
	"floor": ["offices"], "trigger": "door", "min_door": 220, "chance": 0.12,
	"cooldown": 120.0, "needs_spawn_room": true, "group": "rusher",
	"blocked_by": ["a60", "a60b", "a120", "a200", "seek"],
}
const WALK := 2.2
const NEAR := 1.9      # lockers this close to hers count as "with her"
const SNEAK_CHANCE := 0.3

var _room_n := 0
var _her: Locker = null
var _mark: Sprite3D
var _stops: Array = []   # [path position, locker]
var _i := 0
var _look := 0.0
var _walking := false
var _target := Vector3.ZERO
var _drum: AudioStreamPlayer3D
var _step_t := 0.0
var _sneaked := false
var sprite: Sprite3D


func begin() -> void:
	id = "a220"
	_room_n = Game.door
	var r: RoomBase = generator.room(_room_n)
	var lks: Array = r.lockers.filter(func(l): return is_instance_valid(l) and not l.broken and not l.mimic and l.style == "locker") if r else []
	if lks.size() < 2:
		finish()
		return
	var pts := r.path_global()
	# a couple of steps into the room, so he isn't standing inside the doorway
	var into: Vector3 = (pts[1] - pts[0]).normalized() if pts.size() > 1 else Vector3.ZERO
	global_position = _floor(pts[0]) + into * 1.5
	_her = lks[randi() % lks.size()]
	_mark_her()
	# he stops in front of every locker, in the order he walks past them
	for lk in lks:
		var best := 0
		for k in pts.size():
			if pts[k].distance_to(lk.global_position) < pts[best].distance_to(lk.global_position):
				best = k
		var stand: Vector3 = _floor(pts[best]).lerp(Vector3(lk.global_position.x, _floor(pts[best]).y, lk.global_position.z), 0.45)
		_stops.append([best, stand, lk])
	_stops.sort_custom(func(a, b): return a[0] < b[0])
	_stops.append([pts.size() - 1, _floor(pts[pts.size() - 1]), null])
	sprite = make_sprite("a220_face", 2.8)
	set_sprite_texture(sprite, Assets.texture("a220_face"))
	sprite.pixel_size = 2.8 / float(sprite.texture.get_height())
	(sprite.material_override as StandardMaterial3D).billboard_mode = BaseMaterial3D.BILLBOARD_FIXED_Y
	sprite.position.y = 1.5
	sprite.visible = false
	var l := OmniLight3D.new()
	l.light_color = Color(0.6, 0.05, 0.08)
	l.light_energy = 2.0
	l.omni_range = 7.0
	l.position.y = 1.6
	add_child(l)
	_drum = make_3d_audio("a220_march", 10.0, 80.0)
	make_3d_audio("a200_spawn", 8.0, 40.0).play()
	player.notify("someone giggles inside a locker. something is marching this way.")
	after(6.0, _enter)


func _floor(p: Vector3) -> Vector3:
	return Vector3(p.x, p.y - 1.6, p.z)


## her scribble on the locker door she's hiding behind
func _mark_her() -> void:
	if _mark:
		_mark.queue_free()
	_mark = Sprite3D.new()
	_mark.texture = Assets.texture("a200_face")
	_mark.pixel_size = 0.55 / float(_mark.texture.get_height())
	_mark.shaded = false
	_mark.modulate = Color(0.85, 0.6, 1.0)
	_her.add_child(_mark)
	_mark.global_transform = _her.outside_transform()
	_mark.global_position = _her.global_position.lerp(_mark.global_position, 0.35) + Vector3(0, 1.4, 0)
	var g := OmniLight3D.new()
	g.light_color = Color(0.7, 0.3, 1.0)
	g.omni_range = 1.4
	g.light_energy = 0.8
	_mark.add_child(g)


func _enter() -> void:
	# sometimes she slips into your locker right before he walks in
	if not _sneaked and player.hidden and player.current_locker and player.current_locker != _her \
			and player.current_locker in generator.room(_room_n).lockers and randf() < SNEAK_CHANCE:
		_sneaked = true
		_her = player.current_locker
		_mark_her()
		make_3d_audio("a200_spawn", 4.0, 20.0).play()
		player.notify("...she's in here with you.")
		after(2.5, _enter)
		return
	sprite.visible = true
	_drum.play()
	_walking = true
	_target = _stops[0][1]


func _process(delta: float) -> void:
	if _done or is_frozen() or player == null or player.dead or not _walking:
		return
	if Game.door != _room_n and not player.hidden:
		finish()  # you got out through the door ahead
		return
	# out in the open, in his room: he sees you
	if not player.hidden and Game.door == _room_n and global_position.distance_to(player.global_position) < 14.0 and _sees():
		_kill("seen")
		return
	if _look > 0.0:
		_look -= delta
		if _look <= 0.0:
			_check(_stops[_i][2])
			_i += 1
			if _i >= _stops.size():
				_leave()
				return
			_target = _stops[_i][1]
		return
	var to := _target - global_position
	to.y = 0.0
	var step := WALK * delta
	if to.length() <= step:
		global_position = Vector3(_target.x, global_position.y, _target.z)
		if _stops[_i][2] == null:
			_leave()
			return
		_look = 0.9
	else:
		global_position += to.normalized() * step
	_step_t -= delta
	if _step_t <= 0.0:
		_step_t = 0.5


func _sees() -> bool:
	var q := PhysicsRayQueryParameters3D.create(global_position + Vector3(0, 1.6, 0), player.head_position(), 1)
	return get_world_3d().direct_space_state.intersect_ray(q).is_empty()


## he looks at this locker. if you're in hers, or one right next to hers: that's it
func _check(lk: Locker) -> void:
	if lk == null or not player.hidden or player.current_locker != lk:
		return
	var d := lk.global_position.distance_to(_her.global_position)
	if lk == _her:
		_kill("her_locker")
	elif d < NEAR:
		_kill("next_to_her")


func _kill(detail: String) -> void:
	hit_player = true
	Game.death_details["a220"] = detail
	player.take_damage(999, "a220")
	_leave()


func _leave() -> void:
	_walking = false
	var tw := create_tween()
	if _mark:
		tw.tween_property(_mark, "modulate:a", 0.0, 0.8)
	if sprite:
		tw.parallel().tween_property(sprite, "modulate:a", 0.0, 0.8)
	tw.tween_callback(finish)


func _exit_tree() -> void:
	if is_instance_valid(_mark):
		_mark.queue_free()
