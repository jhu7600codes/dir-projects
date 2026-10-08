extends RoomBase
## a corridor lined with closed office doors on both sides. they're all locked, someone's
## name plate on each. a bench to sit on, nothing else.


func build() -> void:
	room_type = "side_offices"
	var length := rng.randf_range(13.0, 19.0)
	var w := 3.4
	shell(-w / 2, w / 2, length, 3.0, "front", rng.randf_range(-0.5, 0.5), "carpet_red" if rng.randf() < 0.5 else "carpet_blue")
	var z := 2.6
	var k := 0
	while z < length - 2.0:
		for side in [-1.0, 1.0]:
			if rng.randf() < 0.75:
				_side_door(Vector3(side * (w / 2 - 0.1), 0, z), side, k)
				k += 1
		z += rng.randf_range(3.2, 4.2)
	if rng.randf() < 0.5:
		box(Vector3(0.45, 0.45, 1.6), Vector3(-w / 2 + 0.3, 0.225, length * 0.5 + 1.6), Mats.get_mat("wood_dark"), true)
	scatter_loot([Vector3(w / 2 - 0.35, 0, length * 0.6)])
	make_path([Vector3(0, 0, length * 0.5)])


## a closed, locked office door set into the wall (no gap behind it)
func _side_door(at: Vector3, side: float, k: int) -> void:
	var inward := Vector3(-side, 0, 0)
	var dw := 1.0
	var dh := 2.15
	decor_block.append(AABB(at - Vector3(0.6, 0, 0.9), Vector3(1.2, 2.6, 1.8)))
	box(Vector3(0.06, dh, dw), at + inward * 0.03 + Vector3.UP * dh / 2, Mats.get_mat("door"), false)
	box(Vector3(0.07, dh + 0.08, 0.06), at + inward * 0.035 + Vector3(0, dh / 2 + 0.02, -dw / 2 - 0.03), Mats.get_mat("frame"), false)
	box(Vector3(0.07, dh + 0.08, 0.06), at + inward * 0.035 + Vector3(0, dh / 2 + 0.02, dw / 2 + 0.03), Mats.get_mat("frame"), false)
	box(Vector3(0.07, 0.06, dw + 0.12), at + inward * 0.035 + Vector3(0, dh + 0.03, 0), Mats.get_mat("frame"), false)
	box(Vector3(0.08, 0.04, 0.14), at + inward * 0.08 + Vector3(0, 1.0, dw / 2 - 0.15), Mats.get_mat("metal"), false)
	var plate := Label3D.new()
	plate.text = "%s-%s" % [Game.door_label(number), "ABCDEFGH"[k % 8]]
	plate.font_size = 28
	plate.pixel_size = 0.0035
	plate.modulate = Color(0.15, 0.15, 0.15)
	plate.outline_size = 0
	plate.position = at + inward * 0.065 + Vector3.UP * 1.6
	plate.rotation.y = PI / 2 * -side
	add_child(plate)
	box(Vector3(0.05, 0.12, 0.42), at + inward * 0.04 + Vector3.UP * 1.6, Mats.get_mat("sign"), false)
	var it := Interactable.make(Vector3(0.4, dh, dw), "locked")
	it.position = at + inward * 0.25 + Vector3.UP * dh / 2
	it.used.connect(func(p): p.notify(["locked.", "nobody's in.", "it won't budge.", "you hear something behind it. it stops."][randi() % 4]))
	add_child(it)
