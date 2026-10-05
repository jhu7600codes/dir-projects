class_name WiresRoom
extends RoomBase
## shared bits for the wires rooms: concrete, pipes along the ceiling, cables on the walls,
## crates and w-15 raw wires hanging around. no lockers down here.

func _init() -> void:
	theme = "wires"


## a few pipes running along one wall near the ceiling
func pipes_along(x: float, length: float, height: float) -> void:
	var n := rng.randi_range(2, 3)
	for i in n:
		var y := height - 0.25 - i * 0.22
		var mat := Mats.get_mat("rust" if rng.randf() < 0.3 else "pipe")
		box(Vector3(0.16, 0.16, length), Vector3(x - signf(x) * (0.15 + i * 0.05), y, length / 2.0), mat, false)
		# brackets
		var z := 1.0
		while z < length:
			box(Vector3(0.3, 0.04, 0.06), Vector3(x - signf(x) * 0.15, y + 0.1, z), Mats.get_mat("metal"), false)
			z += 2.5


## black cables stapled along a wall
func wall_cables(x: float, length: float) -> void:
	for i in rng.randi_range(1, 3):
		var y := rng.randf_range(1.6, 2.6)
		box(Vector3(0.04, 0.04, length - 0.4), Vector3(x - signf(x) * 0.12, y, length / 2.0), Mats.get_mat("wire"), false)


func crate(pos: Vector3, s := 0.8) -> void:
	box(Vector3(s, s, s), pos + Vector3(0, s / 2.0, 0), Mats.get_mat("wood_old"), true, rng.randf_range(-0.3, 0.3))


## w-15: live wires hanging from the ceiling at these spots
func raw_wires(spots: Array, height: float) -> void:
	for p in spots:
		var w := RawWire.new()
		w.height = height
		w.position = p
		add_child(w)


## a sign painted on the wall
func stencil(text: String, pos: Vector3, yaw: float, size := 48) -> void:
	label(text, pos, yaw, size, Color(0.85, 0.75, 0.2))
