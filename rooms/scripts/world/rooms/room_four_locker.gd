extends RoomBase
## empty room with a locker in every corner. nothing spawns when you enter it.


func build() -> void:
	room_type = "four_locker"
	var length := 8.0
	var w := 6.4
	shell(-w / 2, w / 2, length, 3.0, "front", 0.0)
	add_locker(Vector3(w / 2 - 0.4, 0, 1.0), -PI / 2)
	add_locker(Vector3(-w / 2 + 0.4, 0, 1.0), PI / 2)
	add_locker(Vector3(w / 2 - 0.4, 0, length - 1.0), -PI / 2)
	add_locker(Vector3(-w / 2 + 0.4, 0, length - 1.0), PI / 2)
	make_path([Vector3(0, 0, length * 0.5)])
