class_name RemotePlayer
extends Node3D
## another player in a multiplayer run: a coworker avatar with their name over their head
## and their flashlight. moves smoothly toward the latest position they sent.

var peer := 0
var player_name := "worker"
var dead := false
var door := 0
var _target := Vector3.ZERO
var _yaw := 0.0
var _avatar: Worker
var _tag: Label3D
var _light: SpotLight3D
var _first := true
var _it: Interactable
var _hit_t := 0.0
var _seeker_look: Sprite3D


func _ready() -> void:
	_avatar = Worker.new()
	_avatar.seated = false
	_avatar.can_overreact = false  # your friends don't overreact when you look at them
	add_child(_avatar)
	_tag = Label3D.new()
	_tag.text = player_name
	_tag.font_size = 40
	_tag.pixel_size = 0.004
	_tag.billboard = BaseMaterial3D.BILLBOARD_ENABLED
	_tag.no_depth_test = true
	_tag.modulate = Color(0.95, 0.85, 0.4)
	_tag.outline_size = 6
	_tag.position = Vector3(0, 2.25, 0)
	add_child(_tag)
	_light = SpotLight3D.new()
	_light.position = Vector3(0.25, 1.4, -0.3)
	_light.spot_range = 14.0
	_light.spot_angle = 28.0
	_light.light_energy = 2.0
	_light.light_color = Color(1.0, 0.95, 0.82)
	add_child(_light)
	# battle: you can hit your friends. hide and seek: the seeker can catch you
	var mine := ""
	if Game.sightings == "battle":
		mine = Loc.t("hit ") + player_name
	elif Game.sightings == "hide_seek" and Game.seeker:
		mine = Loc.t("catch ") + player_name
	if mine != "":
		_it = Interactable.make(Vector3(0.8, 1.8, 0.8), mine)
		_it.position = Vector3(0, 0.9, 0)
		_it.used.connect(_on_hit)
		add_child(_it)
	# hide and seek: the seeker looks like an entity to everyone else
	if Game.sightings == "hide_seek" and peer == Net.seeker_id:
		_avatar.visible = false
		_tag.modulate = Color(1, 0.3, 0.25)
		_seeker_look = Sprite3D.new()
		_seeker_look.texture = Assets.glow_texture("a60_face_1")
		_seeker_look.pixel_size = 2.2 / maxf(1.0, _seeker_look.texture.get_height())
		_seeker_look.billboard = BaseMaterial3D.BILLBOARD_ENABLED
		_seeker_look.shaded = false
		_seeker_look.position = Vector3(0, 1.2, 0)
		add_child(_seeker_look)
		_light.light_color = Color(1, 0.2, 0.15)


func _on_hit(_player) -> void:
	if dead or _hit_t > 0.0 or Game.player == null or Game.player.dead:
		return
	_hit_t = 0.6
	if Game.sightings == "hide_seek":
		Net.hit_player(peer, 999.0, "seeker")
	else:
		Net.hit_player(peer, 25.0, "friend")
	Game.play_ui("hurt")
	var tw := create_tween()
	tw.tween_property(_avatar, "position:z", 0.25, 0.06)
	tw.tween_property(_avatar, "position:z", 0.0, 0.15)


func apply(s: Array) -> void:
	_target = s[0]
	_yaw = s[1]
	var hidden: bool = s[3]
	var crouch: bool = s[4]
	dead = s[5]
	door = int(s[7])
	_light.visible = s[6] and not dead and not hidden
	_avatar.visible = not hidden and _seeker_look == null
	if _it:
		_it.enabled = not dead and not hidden
	_tag.visible = not hidden and not dead
	_avatar.scale.y = 0.7 if crouch else 1.0
	if dead and not _avatar.dead:
		_avatar.fall_over()
	if _first:
		_first = false
		global_position = _target
		rotation.y = _yaw


func _process(delta: float) -> void:
	_hit_t -= delta
	var k := minf(1.0, (6.0 if Net.online else 12.0) * delta)
	global_position = global_position.lerp(_target, k)
	rotation.y = lerp_angle(rotation.y, _yaw, k)


func head_position() -> Vector3:
	return global_position + Vector3(0, 1.6, 0)
