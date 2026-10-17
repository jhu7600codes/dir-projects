extends RoomBase
## break room / kitchen: counter, fridge, a table with fallen chairs


func build() -> void:
	room_type = "break_room"
	var length := rng.randf_range(8.0, 10.0)
	var w := 7.0
	shell(-w / 2, w / 2, length, 3.0, "front", -1.5)
	Props.counter(self, Vector3(w / 2 - 0.35, 0, length * 0.5), PI / 2, 4.0)
	# the white fridge: you can hide in it
	add_locker(Vector3(w / 2 - 0.4, 0, 1.2), -PI / 2, "fridge")
	var c := Vector3(-1.0, 0, length * 0.6)
	Props.table(self, c, 0.0, Vector3(1.2, Props.TABLE_H, 1.2))
	for i in 3:
		var a := TAU * i / 3.0
		Props.chair(self, c + Vector3(cos(a), 0, sin(a)) * 1.0, -a, true)
	add_cabinet(Vector3(-w / 2 + 0.27, 0, length * 0.75), PI / 2)
	scatter_loot([Vector3(w / 2 - 0.35, Props.COUNTER_H + 0.02, length * 0.4), c + Vector3(0, Props.TABLE_H + 0.02, 0)])
	add_atm(Vector3(-w / 2 + 0.3, 0, 1.5), PI / 2)
	if rng.randf() < 0.5:
		add_arcade(Vector3(-w / 2 + 0.35, 0, length * 0.42), PI / 2)
	make_path([Vector3(0.6, 0, length * 0.4), Vector3(-1.5, 0, length - 1.2)])
