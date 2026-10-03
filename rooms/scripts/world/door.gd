class_name Door
extends Node3D
## handleless office door with the yellow number plate. push (interact) to open.
## opening it tells the generator to build the next room.
## local space: the doorway is centered on x = 0, the door swings toward +z (into the next room).

signal opened(door: Door)

var number := 1
var is_open := false
var locked := false  # used for the decorative locked door in some rooms
var lock_handler := Callable()  # figure's code lock: called instead of the "locked" sound

var _hinge: Node3D
var _panel_shape: CollisionShape3D
var _it: Interactable
var _audio: AudioStreamPlayer3D


func _ready() -> void:
	var w := RoomBase.DOOR_W
	var h := RoomBase.DOOR_H
	# frame
	_frame_box(Vector3(0.1, h, 0.26), Vector3(-w / 2 - 0.05, h / 2, 0))
	_frame_box(Vector3(0.1, h, 0.26), Vector3(w / 2 + 0.05, h / 2, 0))
	_frame_box(Vector3(w + 0.2, 0.1, 0.26), Vector3(0, h + 0.05, 0))
	# panel on a hinge at the -x side
	_hinge = Node3D.new()
	_hinge.position = Vector3(-w / 2, 0, 0)
	add_child(_hinge)
	var body := AnimatableBody3D.new()
	body.collision_layer = 1
	body.collision_mask = 0
	body.sync_to_physics = false
	_hinge.add_child(body)
	var mesh := Assets.model("door")
	if mesh:
		mesh.position = Vector3(w / 2, 0, 0)
		_hinge.add_child(mesh)
	else:
		var mi := MeshInstance3D.new()
		var bm := BoxMesh.new()
		bm.size = Vector3(w - 0.04, h - 0.04, 0.07)
		mi.mesh = bm
		mi.material_override = Mats.get_mat("door")
		mi.position = Vector3(w / 2, h / 2, 0)
		_hinge.add_child(mi)
	_panel_shape = CollisionShape3D.new()
	var sh := BoxShape3D.new()
	sh.size = Vector3(w, h, 0.08)
	_panel_shape.shape = sh
	_panel_shape.position = Vector3(w / 2, h / 2, 0)
	body.add_child(_panel_shape)
	# number plate, readable from the room you're standing in (-z side)
	var plate := MeshInstance3D.new()
	var pm := BoxMesh.new()
	pm.size = Vector3(0.62, 0.22, 0.03)
	plate.mesh = pm
	plate.material_override = Mats.get_mat("sign")
	plate.position = Vector3(0, h + 0.28, -0.14)
	add_child(plate)
	var lbl := Label3D.new()
	lbl.text = Game.door_label(number)
	lbl.font_size = 48
	lbl.pixel_size = 0.0035
	lbl.modulate = Color(0.08, 0.08, 0.08)
	lbl.outline_size = 0
	lbl.position = Vector3(0, h + 0.28, -0.16)
	lbl.rotation.y = PI
	add_child(lbl)
	# interaction area on the room side
	_it = Interactable.make(Vector3(w, h, 0.5), "open door")
	_it.position = Vector3(0, h / 2, -0.15)
	_it.used.connect(_on_used)
	add_child(_it)
	_audio = AudioStreamPlayer3D.new()
	_audio.bus = "SFX"
	_audio.position = Vector3(0, 1.5, 0)
	add_child(_audio)
	if locked:
		_it.prompt = "locked"


func _frame_box(size: Vector3, pos: Vector3) -> void:
	var mi := MeshInstance3D.new()
	var bm := BoxMesh.new()
	bm.size = size
	mi.mesh = bm
	mi.material_override = Mats.get_mat("frame")
	mi.position = pos
	add_child(mi)


func _on_used(_player) -> void:
	if locked and lock_handler.is_valid():
		lock_handler.call()
		return
	if locked:
		_audio.stream = Assets.sound("door_locked")
		_audio.play()
		return
	open()


## lock it with a code padlock (figure's room). `handler` runs when you try the door
func code_lock(handler: Callable) -> void:
	locked = true
	lock_handler = handler
	_it.prompt = "enter the code"
	var pad := MeshInstance3D.new()
	var bm := BoxMesh.new()
	bm.size = Vector3(0.16, 0.22, 0.06)
	pad.mesh = bm
	pad.material_override = Mats.get_mat("metal")
	pad.position = Vector3(RoomBase.DOOR_W - 0.2, 1.05, -0.06)  # hinge-local: the handle side
	pad.name = "Padlock"
	_hinge.add_child(pad)


func unlock() -> void:
	locked = false
	lock_handler = Callable()
	if is_instance_valid(_it):
		_it.prompt = "open door"
	var pad := _hinge.get_node_or_null("Padlock")
	if pad:
		pad.queue_free()


func open() -> void:
	if is_open:
		return
	is_open = true
	_it.enabled = false
	_it.queue_free()
	_panel_shape.set_deferred("disabled", true)
	_audio.stream = Assets.sound("door_open")
	_audio.play()
	var tw := create_tween()
	tw.tween_property(_hinge, "rotation:y", -PI * 0.55, 0.45).set_trans(Tween.TRANS_QUAD).set_ease(Tween.EASE_OUT)
	opened.emit(self)


## the end of seek's chase: the curious light slams the door shut behind you
func slam_shut() -> void:
	if not is_open:
		return
	is_open = false
	_panel_shape.set_deferred("disabled", false)
	_audio.stream = Assets.sound("door_open")
	_audio.pitch_scale = 0.6
	_audio.volume_db = 6.0
	_audio.play()
	var tw := create_tween()
	tw.tween_property(_hinge, "rotation:y", 0.0, 0.12).set_trans(Tween.TRANS_QUAD).set_ease(Tween.EASE_IN)
	var glow := OmniLight3D.new()
	glow.light_color = Color(1.0, 0.86, 0.45)
	glow.light_energy = 4.0
	glow.omni_range = 7.0
	glow.position = Vector3(0, 1.4, -0.6)
	add_child(glow)
	var gt := create_tween()
	gt.tween_property(glow, "light_energy", 0.0, 1.6)
	gt.tween_callback(glow.queue_free)
