class_name Anomaly
extends Node3D
## the thing living in a complex apartment when the gate control beeps. it never leaves its
## apartment. three kinds, and it looks like wherever it lives:
##   chaser  - wanders, and runs you down once it sees you
##   stalker - only moves while you're not looking at it
##   lurker  - waits hidden until you walk right up to it, then bursts out
## in a hospital it's the surgeon, in a hotel the bellboy, in a flat the neighbor.
## the fire escape's fire knocks it out for ~7 minutes.

const NAMES := {"hospital": "the surgeon", "hotel": "the bellboy", "apartment": "the neighbor"}

var kind := "chaser"
var apt_theme := "apartment"
var room: Node3D
var stunned := 0.0
var _sprite: Sprite3D
var _goal := Vector3.ZERO
var _chasing := 0.0
var _hidden := false
var _breath: AudioStreamPlayer3D
var _wake_warned := false


func _ready() -> void:
	_sprite = Sprite3D.new()
	_sprite.texture = Faces.anomaly_tex(apt_theme)
	_sprite.pixel_size = 2.3 / float(_sprite.texture.get_height())
	_sprite.billboard = BaseMaterial3D.BILLBOARD_FIXED_Y
	_sprite.shaded = false
	_sprite.alpha_cut = SpriteBase3D.ALPHA_CUT_DISCARD
	_sprite.position.y = 1.15
	add_child(_sprite)
	_breath = AudioStreamPlayer3D.new()
	_breath.stream = Assets.sound("anomaly_breath")
	_breath.bus = "SFX"
	_breath.unit_size = 3.0
	_breath.max_distance = 18.0
	add_child(_breath)
	_breath.play()
	if kind == "lurker":
		_hide()
	_pick_goal()


func display_name() -> String:
	return NAMES.get(apt_theme, "the tenant")


func stun(seconds: float) -> void:
	stunned = seconds
	_chasing = 0.0
	_wake_warned = false
	_sprite.visible = true
	_sprite.modulate = Color(0.25, 0.22, 0.2)
	_breath.stop()


func _hide() -> void:
	_hidden = true
	_sprite.visible = false


func _bounds() -> AABB:
	return room.apt_bounds


func _pick_goal() -> void:
	var b := _bounds()
	_goal = Vector3(randf_range(b.position.x + 0.6, b.end.x - 0.6), b.position.y, randf_range(b.position.z + 0.6, b.end.z - 0.6))


func _player_inside(p: Player) -> bool:
	return _bounds().grow(0.2).has_point(room.to_local(p.global_position) + Vector3(0, 0.5, 0))


func _process(delta: float) -> void:
	var p: Player = Game.player
	if p == null or p.dead or not is_instance_valid(room):
		return
	if stunned > 0.0:
		stunned -= delta
		if stunned < 8.0 and not _wake_warned and _player_inside(p):
			_wake_warned = true
			p.notify("it's waking up. get to the stairs.")
		if stunned <= 0.0:
			_sprite.modulate = Color.WHITE
			_breath.play()
			if kind == "lurker":
				_hide()
		return
	var inside := _player_inside(p)
	var me := position
	var you := room.to_local(p.global_position)
	you.y = me.y
	var d := me.distance_to(you)
	var speed := 1.2
	var target := _goal
	match kind:
		"chaser":
			if inside and not p.hidden and d < 14.0 and _sees(p):
				_chasing = 4.0
			if _chasing > 0.0:
				_chasing -= delta
				target = you
				speed = 4.4
		"stalker":
			if inside and not p.hidden:
				target = you
				speed = 0.0 if _watched(p) else 6.0
		"lurker":
			if _hidden:
				if inside and not p.hidden and d < 3.2 and not p.crouching:
					_hidden = false
					_sprite.visible = true
					_chasing = 7.0
					var s := AudioStreamPlayer3D.new()
					s.stream = Assets.sound("anomaly_scream")
					s.bus = "SFX"
					add_child(s)
					s.play()
				return
			if _chasing > 0.0 and inside:
				_chasing -= delta
				target = you
				speed = 5.0
			else:
				_pick_goal()
				position = _goal
				_hide()
				return
	var to := target - me
	to.y = 0.0
	if to.length() < 0.3:
		if target == _goal:
			_pick_goal()
	elif speed > 0.0:
		position += to.normalized() * minf(speed * delta, to.length())
	# never outside its apartment
	var b := _bounds()
	position.x = clampf(position.x, b.position.x + 0.3, b.end.x - 0.3)
	position.z = clampf(position.z, b.position.z + 0.3, b.end.z - 0.3)
	if inside and not p.hidden and d < 0.9 and _sprite.visible:
		Game.death_details["anomaly"] = kind
		Game.death_details["anomaly_name"] = display_name()
		p.take_damage(999, "anomaly")


func _sees(p: Player) -> bool:
	var q := PhysicsRayQueryParameters3D.create(global_position + Vector3(0, 1.5, 0), p.head_position(), 1)
	return get_world_3d().direct_space_state.intersect_ray(q).is_empty()


## the stalker freezes while it's on your screen (and you can actually see it)
func _watched(p: Player) -> bool:
	var cam: Camera3D = p.camera
	var to := (global_position + Vector3(0, 1.2, 0)) - cam.global_position
	if (-cam.global_basis.z).dot(to.normalized()) < 0.55:
		return false
	var q := PhysicsRayQueryParameters3D.create(cam.global_position, global_position + Vector3(0, 1.2, 0), 1)
	return get_world_3d().direct_space_state.intersect_ray(q).is_empty()
