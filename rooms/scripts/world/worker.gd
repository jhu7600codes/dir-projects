class_name Worker
extends Node3D
## an office worker in the powered office (after the wires). blocky like a roblox character.
## seated ones type away at their desk. if an entity worker attacks, everyone in the room
## falls over. local space: faces -z (toward the desk when seated).

const SKINS := [Color(0.87, 0.69, 0.55), Color(0.62, 0.45, 0.32), Color(0.95, 0.8, 0.68), Color(0.42, 0.3, 0.22)]
const SHIRTS := [Color(0.92, 0.92, 0.9), Color(0.55, 0.68, 0.85), Color(0.3, 0.32, 0.38), Color(0.7, 0.3, 0.3), Color(0.35, 0.55, 0.4)]

var seated := true
var head_texture: Texture2D = null  # entity workers get an entity face for a head
var _arms: Array[Node3D] = []
var dead := false
static var _mats := {}


func _init() -> void:
	set_meta("no_merge", true)


func _ready() -> void:
	var skin: Color = SKINS[randi() % SKINS.size()]
	var shirt: Color = SHIRTS[randi() % SHIRTS.size()]
	var pants := Color(0.15, 0.16, 0.2)
	var hip_y := 0.68 if seated else 0.95  # seated: on top of the chair seat (Props.SEAT_H)
	# legs
	for s in [-1.0, 1.0]:
		if seated:
			_part(Vector3(0.19, 0.19, 0.48), Vector3(s * 0.11, hip_y, -0.2), pants)
			_part(Vector3(0.19, hip_y, 0.19), Vector3(s * 0.11, hip_y / 2.0, -0.42), pants)
		else:
			_part(Vector3(0.19, 0.95, 0.19), Vector3(s * 0.11, 0.475, 0), pants)
	# torso
	_part(Vector3(0.44, 0.6, 0.24), Vector3(0, hip_y + 0.3, 0), shirt)
	# arms (typing pose when seated: reaching forward)
	for s in [-1.0, 1.0]:
		var pivot := Node3D.new()
		pivot.position = Vector3(s * 0.31, hip_y + 0.55, 0)
		add_child(pivot)
		var arm := _part(Vector3(0.17, 0.55, 0.17), Vector3(0, -0.27, 0), shirt, pivot)
		_part(Vector3(0.16, 0.12, 0.16), Vector3(0, -0.6, 0), skin, pivot)
		if seated:
			pivot.rotation.x = 1.2
			_arms.append(pivot)
		arm.name = "arm"
	# head
	if head_texture:
		var s3 := Sprite3D.new()
		s3.texture = head_texture
		s3.pixel_size = 0.55 / maxf(1.0, head_texture.get_height())
		s3.shaded = false
		s3.transparent = true
		s3.billboard = BaseMaterial3D.BILLBOARD_FIXED_Y
		s3.position = Vector3(0, hip_y + 0.85, 0)
		add_child(s3)
	else:
		_part(Vector3(0.3, 0.3, 0.3), Vector3(0, hip_y + 0.78, 0), skin)
	if seated:
		var tw := create_tween().set_loops()
		var speed := randf_range(0.09, 0.16)
		for i in 2:
			tw.tween_callback(func():
				if _arms.size() == 2 and not dead:
					_arms[0].rotation.x = 1.2 + randf_range(-0.12, 0.12)
					_arms[1].rotation.x = 1.2 + randf_range(-0.12, 0.12))
			tw.tween_interval(speed)


func _part(size: Vector3, pos: Vector3, c: Color, parent: Node3D = null) -> MeshInstance3D:
	var mi := MeshInstance3D.new()
	var bm := BoxMesh.new()
	bm.size = size
	mi.mesh = bm
	var key := c.to_html()
	if not _mats.has(key):
		var m := StandardMaterial3D.new()
		m.albedo_color = c
		m.roughness = 0.8
		_mats[key] = m
	mi.material_override = _mats[key]
	mi.position = pos
	(parent if parent else self).add_child(mi)
	return mi


## an entity worker went off: everyone in the room drops
func fall_over() -> void:
	if dead:
		return
	dead = true
	var tw := create_tween()
	tw.tween_property(self, "rotation:x", -PI / 2 if randf() < 0.5 else PI / 2, 0.4).set_trans(Tween.TRANS_BOUNCE).set_ease(Tween.EASE_OUT)
