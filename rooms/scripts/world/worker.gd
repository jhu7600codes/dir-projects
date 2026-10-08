class_name Worker
extends Node3D
## an office worker in the powered office (after the wires). blocky like a roblox character.
## seated ones type away at their desk. stare at one for too long and they yell, snap back
## to their spot and come through the rooms after you (see overreact.gd). hide.
## local space: faces -z (toward the desk when seated).

const SKINS := [Color(0.87, 0.69, 0.55), Color(0.62, 0.45, 0.32), Color(0.95, 0.8, 0.68), Color(0.42, 0.3, 0.22)]
const SHIRTS := [Color(0.92, 0.92, 0.9), Color(0.55, 0.68, 0.85), Color(0.3, 0.32, 0.38), Color(0.7, 0.3, 0.3), Color(0.35, 0.55, 0.4)]

var seated := true
var head_texture: Texture2D = null  # entity workers get an entity face for a head
var _arms: Array[Node3D] = []
var dead := false
var face_key := "a60_face_1"  # what their face looks like when they come after you
var look_time := 1.5          # how long you can stare before they snap
var look_range := 8.0
var _home := Transform3D.IDENTITY
var _looked := 0.0
var _check_t := 0.0
var _calm_t := 0.0
static var _mats := {}


func _init() -> void:
	set_meta("no_merge", true)


func _ready() -> void:
	_home = transform
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


func _physics_process(delta: float) -> void:
	if dead or Game.player == null or Game.player.dead or Game.player.hidden:
		return
	_calm_t -= delta
	_check_t -= delta
	if _check_t > 0.0:
		return
	_check_t = 0.1
	if _calm_t <= 0.0 and _seen():
		_looked += 0.1
		if _looked >= look_time:
			overreact()
	else:
		_looked = maxf(0.0, _looked - 0.1)


func _seen() -> bool:
	var cam: Camera3D = Game.player.camera
	var head := global_position + Vector3(0, 1.5 if seated else 1.75, 0)
	var to := head - cam.global_position
	if to.length() > look_range:
		return false
	if (-cam.global_transform.basis.z).dot(to.normalized()) < 0.94:
		return false
	var q := PhysicsRayQueryParameters3D.create(cam.global_position, head, 1)
	return get_world_3d().direct_space_state.intersect_ray(q).is_empty()


## "IM AN INTROVERT". back to their spot in a blink, and then they come for you
func overreact() -> void:
	_looked = 0.0
	_calm_t = 12.0
	var yell := Label3D.new()
	yell.text = "IM AN INTROVERT"
	yell.font_size = 72
	yell.pixel_size = 0.006
	yell.modulate = Color(1.0, 0.3, 0.25)
	yell.outline_size = 8
	yell.billboard = BaseMaterial3D.BILLBOARD_ENABLED
	yell.no_depth_test = true
	yell.position = global_position + Vector3(0, 2.3, 0)
	get_parent().add_child(yell)
	var tw := yell.create_tween()
	tw.tween_property(yell, "position:y", yell.position.y + 0.6, 1.6)
	tw.parallel().tween_property(yell, "modulate:a", 0.0, 1.6).set_delay(0.6)
	tw.tween_callback(yell.queue_free)
	var a := AudioStreamPlayer3D.new()
	a.stream = Assets.sound("introvert")
	a.bus = "SFX"
	a.position = yell.position
	get_parent().add_child(a)
	a.play()
	a.finished.connect(a.queue_free)
	transform = _home
	on_overreact()
	var em = Game.entities
	if em and not em.active.has("worker"):
		var fk := face_key
		get_tree().create_timer(0.5).timeout.connect(func():
			if Game.entities and Game.player and not Game.player.dead:
				Game.entities.spawn("worker", {"face_key": fk}))


## subclasses reset their walking here
func on_overreact() -> void:
	pass


## an entity worker went off: everyone in the room drops
func fall_over() -> void:
	if dead:
		return
	dead = true
	var tw := create_tween()
	tw.tween_property(self, "rotation:x", -PI / 2 if randf() < 0.5 else PI / 2, 0.4).set_trans(Tween.TRANS_BOUNCE).set_ease(Tween.EASE_OUT)
