class_name ArcadeMachine
extends Node3D
## an old arcade cabinet in the break room / cafeteria: "OUTLET DODGE". play a round (see
## ArcadeScreen), the first round on each machine pays out gold for how long you lasted.
## local space like the atm: the screen faces +z, the wall is behind it (-z).

var paid := false


func _init() -> void:
	set_meta("no_merge", true)


func _ready() -> void:
	_box(Vector3(0.7, 1.75, 0.6), Vector3(0, 0.875, 0), "car_blue")
	_box(Vector3(0.56, 0.45, 0.03), Vector3(0, 1.32, 0.31), "screen")
	_box(Vector3(0.66, 0.12, 0.3), Vector3(0, 0.98, 0.42), "dark")  # the control panel
	var l := Label3D.new()
	l.text = "OUTLET DODGE"
	l.font_size = 40
	l.pixel_size = 0.0035
	l.modulate = Color(1.0, 0.85, 0.3)
	l.outline_size = 0
	l.position = Vector3(0, 1.66, 0.31)
	add_child(l)
	var glow := OmniLight3D.new()
	glow.light_color = Color(0.3, 1.0, 0.5)
	glow.omni_range = 1.6
	glow.light_energy = 0.6
	glow.position = Vector3(0, 1.3, 0.6)
	add_child(glow)
	var it := Interactable.make(Vector3(0.8, 1.2, 0.6), "play arcade")
	it.position = Vector3(0, 1.0, 0.55)
	it.used.connect(func(_p):
		var s := ArcadeScreen.new()
		s.machine = self
		get_tree().root.add_child(s))
	add_child(it)
	var body := StaticBody3D.new()
	var cs := CollisionShape3D.new()
	var sh := BoxShape3D.new()
	sh.size = Vector3(0.7, 1.75, 0.6)
	cs.shape = sh
	cs.position.y = 0.875
	body.add_child(cs)
	add_child(body)


func _box(size: Vector3, pos: Vector3, mat: String) -> void:
	var mi := MeshInstance3D.new()
	var bm := BoxMesh.new()
	bm.size = size
	mi.mesh = bm
	mi.material_override = Mats.get_mat(mat)
	mi.position = pos
	add_child(mi)
