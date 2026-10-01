class_name Viewmodel
extends Node3D
## the item you're holding, shown in the bottom right of the screen. it sways a little
## behind your mouse, dips when you use something and rattles when you shake the shakelight.

const REST := Vector3(0.21, -0.2, -0.4)

var player: Player
var _models := {}
var _shown := ""
var _sway := Vector2.ZERO
var _kick := 0.0


func _ready() -> void:
	position = REST
	for item in Player.ITEMS:
		var m := ItemModels.build(item)
		# point the models forward (they're built pointing up)
		m.rotation = Vector3(-PI / 2 + 0.08, 0.05, 0)
		m.position = Vector3(0, 0, 0.12)  # hold it by the handle, not the middle
		m.scale = Vector3.ONE * 0.75
		if item in ["bandage", "vitamins"]:
			m.rotation = Vector3(-0.3, 0.4, 0)
			m.position = Vector3.ZERO
			m.scale = Vector3.ONE * 1.1
		m.visible = false
		_set_no_shadows(m)
		add_child(m)
		_models[item] = m


func _set_no_shadows(n: Node) -> void:
	if n is GeometryInstance3D:
		n.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	for c in n.get_children():
		_set_no_shadows(c)


func add_sway(rel: Vector2) -> void:
	_sway = (_sway + rel * 0.0004).limit_length(0.03)


## small dip / jiggle when an item is used
func kick(strength := 1.0) -> void:
	_kick = 0.06 * strength


func _process(delta: float) -> void:
	if player == null:
		return
	var want: String = player.selected if player.owns(player.selected) and not player.hidden and not player.dead else ""
	if want != _shown:
		for k in _models:
			_models[k].visible = k == want
		_shown = want
	_sway = _sway.lerp(Vector2.ZERO, minf(1.0, 8.0 * delta))
	_kick = move_toward(_kick, 0.0, delta * 0.3)
	var shake := Vector3.ZERO
	if _kick > 0.0 and _shown == "shakelight":
		shake = Vector3(randf_range(-1, 1), randf_range(-1, 1), 0) * _kick * 0.3
	var target := REST + Vector3(-_sway.x, _sway.y, 0) + Vector3(0, -_kick, _kick * 0.5) + shake
	position = position.lerp(target, minf(1.0, 14.0 * delta))
