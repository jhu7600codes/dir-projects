extends RoomBase
## straight room with a table of plants on one side


func build() -> void:
	room_type = "plant"
	var length := rng.randf_range(8.0, 11.0)
	shell(-2.0, 2.5, length, 3.0, "front", 0.0)
	Props.table(self, Vector3(1.9, 0, length * 0.5), PI / 2, Vector3(1.8, Props.TABLE_H, 0.7))
	Props.plant(self, Vector3(1.9, Props.TABLE_H + 0.02, length * 0.5 - 0.5))
	Props.plant(self, Vector3(1.9, Props.TABLE_H + 0.02, length * 0.5 + 0.5), rng.randf() < 0.3)
	if rng.randf() < 0.4:
		Props.plant(self, Vector3(-1.6, 0, length - 0.6))
	scatter_loot([Vector3(1.9, Props.TABLE_H + 0.02, length * 0.5), Vector3(-1.6, 0, 1.5)])
	make_path([Vector3(0, 0, length * 0.5)])
