class_name W30
extends Entity
## w-30, "the steam": in the wires the pipes along the ceiling start to hiss and rattle.
## a couple of seconds later they blast a sheet of steam across the room at head height.
## crouch under it (or hide). three blasts, then the pipes calm down.

const RULES := {
	"floor": ["wires"], "trigger": "door", "min_door": 12, "chance": 0.22,
	"cooldown": 35.0, "group": "steam",
}
const DAMAGE := 30.0

var _room_n := 0
var _blasts := 3
var _hiss: AudioStreamPlayer3D
var _sheet: MeshInstance3D
var _overlay: ColorRect


func begin() -> void:
	id = "w30"
	_room_n = Game.door
	var r: RoomBase = generator.room(_room_n)
	if r == null:
		finish()
		return
	var pts := r.path_global()
	global_position = pts[pts.size() / 2]
	_hiss = make_3d_audio("w30_hiss", 10.0, 50.0)
	# the steam: a big pale slab across the room above crouch height
	_sheet = MeshInstance3D.new()
	var bm := BoxMesh.new()
	bm.size = Vector3(30, 1.1, 30)
	_sheet.mesh = bm
	var m := StandardMaterial3D.new()
	m.albedo_color = Color(0.95, 0.95, 0.95, 0.0)
	m.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	m.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
	m.cull_mode = BaseMaterial3D.CULL_DISABLED
	_sheet.material_override = m
	add_child(_sheet)
	_sheet.global_position = Vector3(global_position.x, pts[0].y - 1.6 + 1.85, global_position.z)
	var cl := CanvasLayer.new()
	cl.layer = 20
	add_child(cl)
	_overlay = ColorRect.new()
	_overlay.color = Color(1, 1, 1, 0)
	_overlay.set_anchors_preset(Control.PRESET_FULL_RECT)
	_overlay.mouse_filter = Control.MOUSE_FILTER_IGNORE
	cl.add_child(_overlay)
	player.notify("the pipes are hissing...")
	_warn()


func _warn() -> void:
	if _done:
		return
	if Game.door != _room_n or _blasts <= 0:
		finish()
		return
	_hiss.play()
	after(2.2, _blast)


func _blast() -> void:
	_blasts -= 1
	var m := _sheet.material_override as StandardMaterial3D
	var tw := create_tween()
	tw.tween_property(m, "albedo_color:a", 0.7, 0.15)
	tw.tween_interval(1.0)
	tw.tween_property(m, "albedo_color:a", 0.0, 0.6)
	make_3d_audio("w30_blast", 12.0, 60.0).play()
	# you're in it unless you're crouched down or in a locker
	if Game.door == _room_n and not player.crouching and not player.hidden and not player.dead:
		hit_player = true
		Game.death_details["w30"] = "standing"
		player.take_damage(DAMAGE, "w30")
		_overlay.color.a = 0.85
	elif Game.door == _room_n and not player.hidden:
		_overlay.color.a = 0.25  # it's right above you
	create_tween().tween_property(_overlay, "color:a", 0.0, 1.4)
	after(randf_range(3.5, 5.5), _warn)
