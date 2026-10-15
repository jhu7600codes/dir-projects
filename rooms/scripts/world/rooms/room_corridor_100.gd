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
		var ink := Color(0.12, 0.1, 0.1).lerp(Color(0.45, 0.05, 0.05), t)
		var y := rng.randf_range(1.2, 1.9)
		if i == n - 1:
			# the last one is written by hand on two lines, and the end of it is scribbled out
			_last_message(Vector3(x, 1.75, z), yaw, ink)
			continue
		var lbl := label(MESSAGES[i], Vector3(x, y, z), yaw, int(lerpf(46, 96, t)), ink, "handwriting")
		lbl.rotation.z = rng.randf_range(-0.04, 0.04) + rng.randf_range(-0.25, 0.25) * t
		lbl.pixel_size = 0.006
		lbl.width = 700
		lbl.autowrap_mode = TextServer.AUTOWRAP_WORD
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


func _last_message(pos: Vector3, yaw: float, ink: Color) -> void:
	var size := 90
	var line1 := label("please help i have no", pos, yaw, size, ink, "handwriting")
	line1.pixel_size = 0.006
	line1.rotation.z = -0.06
	var second := "signal and cant ca"
	var line2 := label(second, pos + Vector3(0, -0.6, 0), yaw, size, ink, "handwriting")
	line2.pixel_size = 0.006
	line2.rotation.z = 0.04
	# work out where "ca" sits on the second line, and scribble right over it
	var font: Font = line2.font if line2.font else ThemeDB.fallback_font
	var full := font.get_string_size(second, HORIZONTAL_ALIGNMENT_LEFT, -1, size).x
	var before := font.get_string_size("signal and cant ", HORIZONTAL_ALIGNMENT_LEFT, -1, size).x
	var x0 := (before - full / 2.0) * line2.pixel_size - 0.04
	var x1 := (full / 2.0) * line2.pixel_size + 0.18
	_scribble(line2, x0, x1, ink.darkened(0.35))


## a messy back-and-forth scribble, like someone angrily crossing words out
func _scribble(lbl: Label3D, x0: float, x1: float, col: Color) -> void:
	var mat := Mats.unshaded(col)
	var strokes := 9
	var prev := Vector2(x0, rng.randf_range(-0.12, 0.12))
	for k in strokes:
		var nx := x1 if k % 2 == 0 else x0 + rng.randf_range(0.0, 0.08)
		var nxt := Vector2(nx + rng.randf_range(-0.05, 0.05), rng.randf_range(-0.16, 0.16))
		var d := nxt - prev
		var stroke := MeshInstance3D.new()
		var bm := BoxMesh.new()
		bm.size = Vector3(d.length(), rng.randf_range(0.025, 0.04), 0.004)
		stroke.mesh = bm
		stroke.material_override = mat
		stroke.position = Vector3((prev.x + nxt.x) / 2.0, (prev.y + nxt.y) / 2.0, 0.006 + k * 0.0005)
		stroke.rotation.z = atan2(d.y, d.x)
		lbl.add_child(stroke)
		prev = nxt
