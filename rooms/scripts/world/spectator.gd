class_name Spectator
extends Node3D
## multiplayer, after you die: the camera floats behind one of the players still alive.
## tap / click / any key switches to the next one.

var _cam: Camera3D
var _i := 0
var _label: Label


func _ready() -> void:
	_cam = Camera3D.new()
	_cam.near = 0.08
	add_child(_cam)
	_cam.make_current()
	var cl := CanvasLayer.new()
	cl.layer = 20
	add_child(cl)
	_label = UIKit.label("", 18, Color(0.95, 0.85, 0.4))
	_label.set_anchors_and_offsets_preset(Control.PRESET_CENTER_BOTTOM)
	_label.offset_top = -100
	_label.offset_bottom = -70
	_label.offset_left = -300
	_label.offset_right = 300
	_label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	cl.add_child(_label)
	Input.mouse_mode = Input.MOUSE_MODE_VISIBLE


func _unhandled_input(event: InputEvent) -> void:
	if (event is InputEventMouseButton or event is InputEventScreenTouch or event is InputEventKey) and event.is_pressed():
		_i += 1


func _process(delta: float) -> void:
	var alive := Net.alive_remotes()
	if alive.is_empty():
		_label.text = "everyone's down..."
		return
	var r: RemotePlayer = alive[_i % alive.size()]
	_label.text = Loc.t("spectating %s  -  %s to switch") % [r.player_name, Loc.t("tap") if Settings.use_touch() else Loc.t("click")]
	var behind: Vector3 = r.global_position + r.global_basis.z * 2.6 + Vector3(0, 2.2, 0)
	_cam.global_position = _cam.global_position.lerp(behind, minf(1.0, 6.0 * delta))
	_cam.look_at(r.head_position(), Vector3.UP)
