class_name Atm
extends Node3D
## an atm in the powered office. you can't rob the place anymore, but you can put your gold
## in the bank. local space: the screen faces +z, standing against a wall behind it (-z).

func _init() -> void:
	set_meta("no_merge", true)


func _ready() -> void:
	_box(Vector3(0.75, 1.6, 0.5), Vector3(0, 0.8, 0), "metal")
	_box(Vector3(0.5, 0.35, 0.03), Vector3(0, 1.3, 0.26), "screen")
	_box(Vector3(0.4, 0.06, 0.15), Vector3(0, 0.95, 0.3), "dark")
	var l := Label3D.new()
	l.text = "ATM"
	l.font_size = 48
	l.pixel_size = 0.004
	l.modulate = Color(0.3, 1.0, 0.5)
	l.outline_size = 0
	l.position = Vector3(0, 1.72, 0.26)
	add_child(l)
	var it := Interactable.make(Vector3(0.8, 1.2, 0.5), "use the atm")
	it.position = Vector3(0, 1.0, 0.45)
	it.used.connect(func(_p): get_tree().root.add_child(AtmScreen.new()))
	add_child(it)
	var body := StaticBody3D.new()
	var cs := CollisionShape3D.new()
	var sh := BoxShape3D.new()
	sh.size = Vector3(0.75, 1.6, 0.5)
	cs.shape = sh
	cs.position.y = 0.8
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
