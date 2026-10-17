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


func apply(s: Array) -> void:
	_target = s[0]
	_yaw = s[1]
	var hidden: bool = s[3]
	var crouch: bool = s[4]
	dead = s[5]
	door = int(s[7])
	_light.visible = s[6] and not dead and not hidden
	_avatar.visible = not hidden
	_tag.visible = not hidden and not dead
	_avatar.scale.y = 0.7 if crouch else 1.0
	if dead and not _avatar.dead:
		_avatar.fall_over()
	if _first:
		_first = false
		global_position = _target
		rotation.y = _yaw


func _process(delta: float) -> void:
	var k := minf(1.0, (6.0 if Net.online else 12.0) * delta)
	global_position = global_position.lerp(_target, k)
	rotation.y = lerp_angle(rotation.y, _yaw, k)


func head_position() -> Vector3:
	return global_position + Vector3(0, 1.6, 0)
