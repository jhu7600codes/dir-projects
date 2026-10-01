class_name Viewmodel
extends Node3D
## the item you're holding, shown in the bottom right of the screen, held by a blocky
## roblox arm. it sways a little behind your mouse, dips when you use something and
## rattles when you shake the shakelight.

const REST := Vector3(0.21, -0.2, -0.4)
const LAYER := 2
const SKIN := Color(0.87, 0.69, 0.55)
const SHIRT := Color(0.93, 0.94, 0.95)
const CUFF := Color(0.72, 0.8, 0.9)
## where the hand grabs each item (viewmodel space) and where the arm goes off screen
const GRIP := {
	"flashlight": [Vector3(0.0, -0.01, 0.115), Vector3(0.06, -0.09, 0.6)],
	"shakelight": [Vector3(0.0, -0.01, 0.115), Vector3(0.06, -0.09, 0.6)],
	"bandage": [Vector3(0.01, -0.07, 0.03), Vector3(0.1, -0.3, 0.45)],
	"vitamins": [Vector3(0.01, -0.06, 0.03), Vector3(0.1, -0.3, 0.45)],
}

var player: Player
var _models := {}
var _shown := ""
var _sway := Vector2.ZERO
var _kick := 0.0
var _arm: Node3D


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
		_set_layer(m)
		add_child(m)
		_models[item] = m
	# the arm: blocky like a roblox arm. skin colored hand, white office shirt sleeve
	# with a pale blue cuff. local +z runs from the hand back along the arm.
	_arm = Node3D.new()
	_arm.visible = false
	add_child(_arm)
	_arm_part(Vector3(0.068, 0.068, 0.075), Vector3(0, 0, 0.0375), SKIN, 0.7)
	_arm_part(Vector3(0.086, 0.086, 0.03), Vector3(0, 0, 0.09), CUFF, 0.85)
	_arm_part(Vector3(0.083, 0.083, 0.5), Vector3(0, 0, 0.35), SHIRT, 0.9)
	# a little cuff button
	_arm_part(Vector3(0.012, 0.012, 0.012), Vector3(0.044, 0.0, 0.09), Color(0.92, 0.92, 0.9), 0.4)


func _set_no_shadows(n: Node) -> void:
	if n is GeometryInstance3D:
		n.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	for c in n.get_children():
		_set_no_shadows(c)


## the hand + item sit on their own render layer so your own flashlight doesn't blow them out
func _set_layer(n: Node) -> void:
	if n is VisualInstance3D:
		n.layers = LAYER
	for c in n.get_children():
		_set_layer(c)


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
		_place_arm(want)
	_sway = _sway.lerp(Vector2.ZERO, minf(1.0, 8.0 * delta))
	_kick = move_toward(_kick, 0.0, delta * 0.3)
	var shake := Vector3.ZERO
	if _kick > 0.0 and _shown == "shakelight":
		shake = Vector3(randf_range(-1, 1), randf_range(-1, 1), 0) * _kick * 0.3
	var target := REST + Vector3(-_sway.x, _sway.y, 0) + Vector3(0, -_kick, _kick * 0.5) + shake
	position = position.lerp(target, minf(1.0, 14.0 * delta))


func _place_arm(item: String) -> void:
	_arm.visible = GRIP.has(item)
	if not _arm.visible:
		return
	var grip: Vector3 = GRIP[item][0]
	var back: Vector3 = GRIP[item][1]
	var dir := (back - grip).normalized()
	var basis := Basis.looking_at(-dir, Vector3.UP)
	_arm.transform = Transform3D(basis.rotated(dir, 0.2), grip - dir * 0.03)


func _arm_part(size: Vector3, pos: Vector3, color: Color, rough: float) -> void:
	var mi := MeshInstance3D.new()
	var bm := BoxMesh.new()
	bm.size = size
	mi.mesh = bm
	var mat := StandardMaterial3D.new()
	mat.albedo_color = color
	mat.roughness = rough
	mi.material_override = mat
	mi.position = pos
	mi.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	mi.layers = LAYER
	_arm.add_child(mi)
