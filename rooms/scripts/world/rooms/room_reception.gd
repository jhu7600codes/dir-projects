extends RoomBase
## a reception area: a long front desk with a computer, a row of waiting chairs, a sign,
## a water cooler. the receptionist isn't here.


func build() -> void:
	room_type = "reception"
	var length := rng.randf_range(9.0, 11.0)
	var w := 9.0
	shell(-w / 2, w / 2, length, 3.4, "front", rng.randf_range(-2.5, 2.5), "carpet_red")
	var dx := 2.2 if rng.randf() < 0.5 else -2.2
	Props.counter(self, Vector3(dx, 0, length * 0.55), 0.0, 3.2)
	box(Vector3(0.5, 0.35, 0.05), Vector3(dx, Props.COUNTER_H + 0.25, length * 0.55 + 0.1), Mats.get_mat("dark"), false)
	box(Vector3(0.45, 0.02, 0.18), Vector3(dx, Props.COUNTER_H + 0.01, length * 0.55 - 0.15), Mats.get_mat("dark"), false)
	var sign := Label3D.new()
	sign.text = "RECEPTION"
	sign.font_size = 64
	sign.pixel_size = 0.005
	sign.modulate = Color(0.2, 0.2, 0.22)
	sign.outline_size = 0
	sign.position = Vector3(dx, 2.6, length - 0.12)
	sign.rotation.y = PI
	add_child(sign)
	for i in 4:
		Props.chair(self, Vector3(-dx * 1.6, 0, 2.2 + i * 0.75), PI / 2 if dx > 0 else -PI / 2)
	Props.plant(self, Vector3(-dx * 1.9, 0, 1.0))
	scatter_loot([Vector3(dx, Props.COUNTER_H + 0.02, length * 0.55)])
	make_path([Vector3(0, 0, length * 0.3), Vector3(0, 0, length * 0.75)])
