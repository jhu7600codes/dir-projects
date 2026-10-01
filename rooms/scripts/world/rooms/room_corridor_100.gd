extends RoomBase
## a-100: a long corridor with handwriting on the walls. it gets messier the further
## you walk, and the last message is cut off and scribbled out.

const MESSAGES := [
	"i've been stuck here for 100 rooms already? i'm kinda dizzy...",
	"the lights hum louder when i stop walking",
	"my feet hurt",
	"i can't remember what the sun looks like",
	"same door. same yellow sign. same door. same yellow sign.",
	"something crackles behind me every time i open one",
	"dont stop dont stop dont stop dont stop",
	"please help i have no signal and cant ca",
]

var _reached := false


func _on_end_reached(_body_node) -> void:
	if not _reached:
		_reached = true
		Achievements.unlock("read_walls")


func build() -> void:
	room_type = "corridor_100"
	is_special = true
	var length := 46.0
	var w := 3.2
	shell(-w / 2, w / 2, length, 3.0, "front", 0.0)
	var n := MESSAGES.size()
	for i in n:
		var t := float(i) / (n - 1)  # 0 calm .. 1 panicked
		var z := 4.0 + i * (length - 8.0) / (n - 1)
		var on_left := i % 2 == 0
		var x := (w / 2 - 0.11) if on_left else (-w / 2 + 0.11)
		var yaw := -PI / 2 if on_left else PI / 2
		var lbl := label(MESSAGES[i], Vector3(x, rng.randf_range(1.2, 1.9), z), yaw, int(lerpf(46, 96, t)), Color(0.12, 0.1, 0.1).lerp(Color(0.45, 0.05, 0.05), t), "handwriting")
		lbl.rotation.z = rng.randf_range(-0.04, 0.04) + rng.randf_range(-0.25, 0.25) * t
		lbl.pixel_size = 0.006
		lbl.width = 700
		lbl.autowrap_mode = TextServer.AUTOWRAP_WORD
		if i == n - 1:
			_scribble_out(lbl)
	# lights flicker a lot in here
	for c in get_children():
		if c is OmniLight3D and rng.randf() < 0.5:
			_flicker.append(c)
	var trig := Area3D.new()
	trig.collision_layer = 0
	trig.collision_mask = 2
	var cs := CollisionShape3D.new()
	var sh := BoxShape3D.new()
	sh.size = Vector3(w, 3, 1.5)
	cs.shape = sh
	trig.add_child(cs)
	trig.position = Vector3(0, 1.5, length - 2.0)
	trig.body_entered.connect(_on_end_reached)
	add_child(trig)
	make_path([Vector3(0, 0, length * 0.33), Vector3(0, 0, length * 0.66)])


func _scribble_out(lbl: Label3D) -> void:
	# messy dark strokes over the end of the last line
	for k in 7:
		var stroke := MeshInstance3D.new()
		var bm := BoxMesh.new()
		bm.size = Vector3(rng.randf_range(0.5, 1.1), 0.035, 0.01)
		stroke.mesh = bm
		stroke.material_override = Mats.unshaded(Color(0.08, 0.02, 0.02))
		stroke.position = Vector3(rng.randf_range(0.2, 1.0), rng.randf_range(-0.12, 0.12), 0.01)
		stroke.rotation.z = rng.randf_range(-0.5, 0.5)
		lbl.add_child(stroke)
