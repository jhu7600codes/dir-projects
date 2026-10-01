extends RoomBase
## meeting room: big table, fallen chairs, a whiteboard and two lockers


func build() -> void:
	room_type = "meeting"
	entity_spawn_ok = true
	var length := rng.randf_range(10.0, 12.0)
	var w := 9.0
	shell(-3.0, w - 3.0, length, 3.2, "front", w - 4.5)
	var c := Vector3(1.5, 0, length * 0.5)
	Props.table(self, c, PI / 2, Vector3(3.6, Props.TABLE_H, 1.4))
	for i in 6:
		var side := -1.0 if i % 2 == 0 else 1.0
		var p := c + Vector3(side * 1.1, 0, -1.4 + (i / 2) * 1.4)
		Props.chair(self, p, side * PI / 2 + rng.randf_range(-0.6, 0.6), rng.randf() < 0.6)
	Props.whiteboard(self, Vector3(w - 3.0 - 0.15, 1.6, length * 0.5), PI / 2)
	add_locker(Vector3(1.5, 0, 0.45), 0.0)
	add_locker(Vector3(w - 3.0 - 0.4, 0, length - 2.3), -PI / 2)
	scatter_loot([c + Vector3(0, Props.TABLE_H + 0.02, 0.5), Vector3(-2.5, 0, length - 1.0)])
	make_path([Vector3(0, 0, 2.5), Vector3(-1.5, 0, length * 0.5), Vector3(w - 4.5, 0, length - 1.5)])
