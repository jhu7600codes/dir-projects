class_name Locker
extends Node3D
## grey office locker. interact to hide; inside you look out through the slits.
## local space: the locker stands on the floor at the origin, its door faces +z.

var occupied := false
var _door: Node3D
var _audio: AudioStreamPlayer3D


func _ready() -> void:
	var body := StaticBody3D.new()
	body.collision_layer = 1
	body.collision_mask = 0
	add_child(body)
	var cs := CollisionShape3D.new()
	var sh := BoxShape3D.new()
	sh.size = Vector3(0.9, 2.1, 0.7)
	cs.shape = sh
	cs.position = Vector3(0, 1.05, 0)
	body.add_child(cs)
	var m := Assets.model("locker")
	if m:
		add_child(m)
	else:
		_placeholder()
	var it := Interactable.make(Vector3(0.9, 2.0, 0.4), "hide")
	it.position = Vector3(0, 1.0, 0.45)
	it.used.connect(func(player): player.enter_locker(self))
	add_child(it)
	_audio = AudioStreamPlayer3D.new()
	_audio.bus = "SFX"
	_audio.position = Vector3(0, 1.2, 0.3)
	add_child(_audio)


func _placeholder() -> void:
	_mesh(Vector3(0.9, 2.1, 0.62), Vector3(0, 1.05, -0.04), Mats.get_mat("locker"), self)
	_door = Node3D.new()
	_door.position = Vector3(-0.43, 0, 0.3)
	add_child(_door)
	var mi := MeshInstance3D.new()
	mi.mesh = _door_mesh()
	_door.add_child(mi)


## the door + its vent slits as one shared mesh (2 surfaces), built once for all lockers
static var _shared_door: ArrayMesh = null


static func _door_mesh() -> ArrayMesh:
	if _shared_door:
		return _shared_door
	var mesh := ArrayMesh.new()
	var st := SurfaceTool.new()
	st.begin(Mesh.PRIMITIVE_TRIANGLES)
	var door := BoxMesh.new()
	door.size = Vector3(0.86, 2.0, 0.04)
	st.append_from(door, 0, Transform3D(Basis(), Vector3(0.43, 1.05, 0.0)))
	st.commit(mesh)
	mesh.surface_set_material(0, Mats.get_mat("locker"))
	st = SurfaceTool.new()
	st.begin(Mesh.PRIMITIVE_TRIANGLES)
	var slit := BoxMesh.new()
	slit.size = Vector3(0.5, 0.025, 0.02)
	for i in 5:
		st.append_from(slit, 0, Transform3D(Basis(), Vector3(0.43, 1.55 + i * 0.06, 0.025)))
	st.commit(mesh)
	mesh.surface_set_material(1, Mats.get_mat("dark"))
	_shared_door = mesh
	return mesh


func _mesh(size: Vector3, pos: Vector3, mat: Material, parent: Node3D) -> void:
	var mi := MeshInstance3D.new()
	var bm := BoxMesh.new()
	bm.size = size
	mi.mesh = bm
	mi.material_override = mat
	mi.position = pos
	parent.add_child(mi)


## where the player stands while hiding / after leaving, in global space
func inside_transform() -> Transform3D:
	# player faces -z by default, so turn around to look out of the door (+z)
	return global_transform * Transform3D(Basis(Vector3.UP, PI), Vector3(0, 0.02, -0.02))


func outside_transform() -> Transform3D:
	return global_transform * Transform3D(Basis(Vector3.UP, PI), Vector3(0, 0.02, 1.0))


func play_door() -> void:
	_audio.stream = Assets.sound("locker")
	_audio.play()
	if _door:
		var tw := create_tween()
		tw.tween_property(_door, "rotation:y", -1.2, 0.12)
		tw.tween_property(_door, "rotation:y", 0.0, 0.18)


## hide the door mesh while the player is inside, the hud draws the slits instead
func set_inside(on: bool) -> void:
	if _door:
		_door.visible = not on
