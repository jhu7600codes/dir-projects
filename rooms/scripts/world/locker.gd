class_name Locker
extends Node3D
## grey office locker. interact to hide; inside you look out through the slits.
## local space: the locker stands on the floor at the origin, its door faces +z.

var style := "locker"
var mimic := false  # a-70 in disguise  # "booth": a phone booth out in the city
var occupied := false
var broken := false  # a-120 tore it open: can't hide in it anymore
var _door: Node3D
var _audio: AudioStreamPlayer3D
var _it: Interactable


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
	var m: Node3D = null if style == "booth" else Assets.model("locker")
	if style == "booth":
		_booth()
	elif style == "fridge":
		_fridge()
	elif m:
		add_child(m)
	else:
		_placeholder()
	_it = Interactable.make(Vector3(0.9, 2.0, 0.4), "hide")
	_it.position = Vector3(0, 1.0, 0.45)
	_it.used.connect(func(player): player.enter_locker(self))
	add_child(_it)
	if mimic:
		_mimic_look()
	_audio = AudioStreamPlayer3D.new()
	_audio.bus = "SFX"
	_audio.position = Vector3(0, 1.2, 0.3)
	add_child(_audio)


## the break room fridge: you can hide in it, like in doors' rooms
func _fridge() -> void:
	var white := Mats.get_mat("fridge_white")
	_mesh(Vector3(0.9, 2.0, 0.62), Vector3(0, 1.0, -0.04), white, self)
	_door = Node3D.new()
	_door.position = Vector3(-0.43, 0, 0.3)
	add_child(_door)
	_mesh(Vector3(0.86, 1.25, 0.05), Vector3(0.43, 1.35, 0), white, _door)
	_mesh(Vector3(0.86, 0.68, 0.05), Vector3(0.43, 0.36, 0), white, _door)
	_mesh(Vector3(0.04, 0.5, 0.05), Vector3(0.78, 1.2, 0.05), Mats.get_mat("metal"), _door)


func _booth() -> void:
	var frame := Mats.get_mat("car_red")
	for x in [-0.44, 0.44]:
		for z in [-0.33, 0.3]:
			_mesh(Vector3(0.06, 2.2, 0.06), Vector3(x, 1.1, z), frame, self)
	_mesh(Vector3(0.96, 0.12, 0.72), Vector3(0, 2.26, -0.02), frame, self)
	_mesh(Vector3(0.9, 2.1, 0.03), Vector3(0, 1.05, -0.34), Mats.get_mat("glass"), self)
	for x in [-0.44, 0.44]:
		_mesh(Vector3(0.03, 2.1, 0.62), Vector3(x, 1.05, -0.02), Mats.get_mat("glass"), self)
	_mesh(Vector3(0.3, 0.4, 0.12), Vector3(0, 1.4, -0.28), Mats.get_mat("dark"), self)
	var l := Label3D.new()
	l.text = "PHONE"
	l.font_size = 40
	l.pixel_size = 0.005
	l.outline_size = 0
	l.position = Vector3(0, 2.26, 0.35)
	add_child(l)
	_door = Node3D.new()
	_door.position = Vector3(-0.43, 0, 0.3)
	add_child(_door)
	_mesh(Vector3(0.86, 2.0, 0.03), Vector3(0.43, 1.05, 0), Mats.get_mat("glass"), _door)


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


## a broken locker: squished down, dented all over, door jammed shut. can't hide in it.
## (a-120's warning breaks a few, and some locker rooms just have busted ones)
func break_open(silent := false) -> void:
	if broken or occupied:
		return
	broken = true
	_it.enabled = false
	_it.prompt = "broken"
	var squish := Vector3(randf_range(1.02, 1.06), randf_range(0.82, 0.9), randf_range(0.9, 0.96))
	var tilt := randf_range(-0.05, 0.05)
	var dents := Node3D.new()
	add_child(dents)
	for c in get_children():
		if c is StaticBody3D or c is Interactable or c is AudioStreamPlayer3D:
			continue
		if silent:
			c.scale = squish
			c.rotation.z = tilt
		else:
			var tw := create_tween()
			tw.tween_property(c, "scale", squish, 0.12).set_trans(Tween.TRANS_BOUNCE)
			tw.parallel().tween_property(c, "rotation:z", tilt, 0.12)
	# dents: dark dimples on the door and the sides
	var dark := Mats.get_mat("dark")
	for i in randi_range(4, 7):
		var size := randf_range(0.08, 0.2)
		_mesh(Vector3(size, size * randf_range(0.5, 1.2), 0.012), Vector3(randf_range(-0.32, 0.32), randf_range(0.3, 1.7) * squish.y, 0.33), dark, dents)
	for i in randi_range(2, 4):
		var sx := 0.465 if randf() < 0.5 else -0.465
		_mesh(Vector3(0.012, randf_range(0.1, 0.3), randf_range(0.08, 0.2)), Vector3(sx, randf_range(0.4, 1.6) * squish.y, randf_range(-0.2, 0.2)), dark, dents)
	if silent:
		return
	_audio.stream = Assets.sound("locker")
	_audio.pitch_scale = randf_range(0.5, 0.7)
	_audio.volume_db = 4.0
	_audio.play()


## a-70: a fake locker. the door has a faint face texture over it that's very easy to miss
func _mimic_look() -> void:
	var q := MeshInstance3D.new()
	var qm := QuadMesh.new()
	qm.size = Vector2(0.8, 1.9)
	q.mesh = qm
	var m := StandardMaterial3D.new()
	m.albedo_texture = Assets.texture("a70_face")
	m.albedo_color = Color(0.2, 0.2, 0.22, 0.07)
	m.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	m.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
	q.material_override = m
	q.position = Vector3(0, 1.05, 0.33)
	add_child(q)


## someone tried to hide in a-70. it bursts out and the locker is wrecked
func trigger_mimic() -> void:
	mimic = false
	broken = true
	_it.enabled = false
	if _door:
		var tw := create_tween()
		tw.tween_property(_door, "rotation:y", -2.4, 0.1)
	if Game.entities:
		Game.entities.spawn("a70", {"start": global_position + global_basis.z * 0.6})


## hide the door mesh while the player is inside, the hud draws the slits instead
func set_inside(on: bool) -> void:
	if _door:
		_door.visible = not on
