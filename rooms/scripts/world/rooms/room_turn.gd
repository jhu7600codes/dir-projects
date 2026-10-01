extends RoomBase
## l-shaped room that turns left or right. the generator sets `turn` (-1 right, 1 left)
## so the hallway never folds back into itself.


func build() -> void:
	room_type = "turn"
	if turn == 0:
		turn = 1 if rng.randf() < 0.5 else -1
	var length := 9.0
	var s := float(turn)
	# room spans from the corridor side to the turn side
	var x_near := -1.5 * s
	var x_far := 6.5 * s
	shell(minf(x_near, x_far), maxf(x_near, x_far), length, 3.0, "left" if turn > 0 else "right", 7.0)
	# solid block that fills the inside corner and makes the "l"
	box(Vector3(4.8, 3.0, 5.5), Vector3(4.1 * s, 1.5, 2.75), Mats.get_mat("wall"))
	Props.table(self, Vector3(0, 0, length - 0.7), 0.0, Vector3(1.4, Props.TABLE_H, 0.6))
	Props.plant(self, Vector3(0.3, Props.TABLE_H + 0.02, length - 0.7))
	scatter_loot([Vector3(-0.3, Props.TABLE_H + 0.02, length - 0.7)])
	make_path([Vector3(0, 0, 7.0), Vector3(3.0 * s, 0, 7.0)])
