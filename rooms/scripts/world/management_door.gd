class_name ManagementDoor
extends Node3D
## the door at the end of the vine hallway behind a-240's cracked wall. it says MANAGEMENT
## and needs the management key (in one of a-240's drawers). opening it takes you down
## into the wires. local space: the door faces +z.

var _it: Interactable


func _ready() -> void:
	var w := RoomBase.DOOR_W
	var h := RoomBase.DOOR_H
	_box(Vector3(w, h, 0.08), Vector3(0, h / 2, 0), "door_metal")
	_box(Vector3(w + 0.2, 0.1, 0.12), Vector3(0, h + 0.05, 0), "rust")
	_box(Vector3(0.1, h, 0.12), Vector3(-w / 2 - 0.05, h / 2, 0), "rust")
	_box(Vector3(0.1, h, 0.12), Vector3(w / 2 + 0.05, h / 2, 0), "rust")
	_box(Vector3(0.75, 0.2, 0.02), Vector3(0, 1.75, 0.05), "sign")
	var l := Label3D.new()
	l.text = "MANAGEMENT"
	l.font_size = 40
	l.pixel_size = 0.0035
	l.modulate = Color(0.1, 0.1, 0.1)
	l.outline_size = 0
	l.position = Vector3(0, 1.75, 0.065)
	add_child(l)
	_box(Vector3(0.12, 0.16, 0.05), Vector3(w / 2 - 0.15, 1.05, 0.06), "metal")  # keyhole plate
	_it = Interactable.make(Vector3(w, h, 0.5), "locked")
	_it.position = Vector3(0, h / 2, 0.25)
	_it.used.connect(_use)
	add_child(_it)


func _process(_delta: float) -> void:
	_it.prompt = "unlock with the management key" if Game.has_key else "locked. it needs a key"


func _box(size: Vector3, pos: Vector3, mat: String) -> void:
	var mi := MeshInstance3D.new()
	var bm := BoxMesh.new()
	bm.size = size
	mi.mesh = bm
	mi.material_override = Mats.get_mat(mat)
	mi.position = pos
	add_child(mi)


func _use(player) -> void:
	if not Game.has_key:
		player.notify("it's locked. the key must be around here somewhere")
		Game.play_ui("deny")
		return
	_it.enabled = false
	Save.data.wires_found = true
	Save.write()
	Achievements.unlock("management")
	Game.play_ui("door_open")
	var carry := Game.carry_state()
	# down the stairs into the wires (keeping your health and items)
	Game.start_run(Game.admin, false, "wires", carry)
