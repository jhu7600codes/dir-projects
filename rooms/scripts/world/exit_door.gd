class_name ExitDoor
extends Node3D
## a door out of the office. hold interact to push it open.
## exit rooms use style "exit" (warm light + its own ambient sound you can hear from the
## previous room), a-1000 uses style "void" (the glowing door at the end of the bridge).
## local space like Door: doorway centered at the origin, you walk up to it from -z.

var style := "exit"


func _ready() -> void:
	var w := RoomBase.DOOR_W
	var h := RoomBase.DOOR_H
	var glow := Mats.get_mat("void_door" if style == "void" else "exit_glow")
	_mesh(Vector3(0.12, h, 0.3), Vector3(-w / 2 - 0.06, h / 2, 0), glow)
	_mesh(Vector3(0.12, h, 0.3), Vector3(w / 2 + 0.06, h / 2, 0), glow)
	_mesh(Vector3(w + 0.24, 0.12, 0.3), Vector3(0, h + 0.06, 0), glow)
	_mesh(Vector3(w, h, 0.08), Vector3(0, h / 2, 0), Mats.get_mat("door") if style == "exit" else Mats.get_mat("white_glow"))
	var body := StaticBody3D.new()
	body.collision_mask = 0
	add_child(body)
	var cs := CollisionShape3D.new()
	var sh := BoxShape3D.new()
	sh.size = Vector3(w, h, 0.1)
	cs.shape = sh
	cs.position = Vector3(0, h / 2, 0)
	body.add_child(cs)
	# soft light in front of the door
	var l := OmniLight3D.new()
	l.position = Vector3(0, 2.0, -1.0)
	l.light_color = Color(1.0, 0.88, 0.6) if style == "exit" else Color(1, 0.97, 0.85)
	l.light_energy = 1.4 if style == "exit" else 2.5
	l.omni_range = 6.0 if style == "exit" else 12.0
	add_child(l)
	# ambient sound, loud and far reaching so it's audible from the room before
	var a := AudioStreamPlayer3D.new()
	a.stream = Assets.sound("exit_ambience")
	a.bus = "Ambience"
	a.unit_size = 6.0
	a.max_distance = 45.0
	a.position = Vector3(0, 1.5, -0.5)
	a.autoplay = true
	add_child(a)
	var it := Interactable.make(Vector3(w, h, 0.6), "hold to leave" if style == "exit" else "hold to open")
	it.hold_time = 1.2
	it.position = Vector3(0, h / 2, -0.3)
	it.used.connect(_on_used)
	add_child(it)


func _mesh(size: Vector3, pos: Vector3, mat: Material) -> void:
	var mi := MeshInstance3D.new()
	var bm := BoxMesh.new()
	bm.size = size
	mi.mesh = bm
	mi.material_override = mat
	mi.position = pos
	add_child(mi)


func _on_used(_player) -> void:
	Game.run_finished.emit("exit" if style == "exit" else "a1000")
