extends RoomBase
## a cramped supply closet: shelves of boxes on both sides, a mop bucket, one locker.


func build() -> void:
	room_type = "supply_closet"
	entity_spawn_ok = true
	var length := rng.randf_range(6.0, 7.5)
	var w := 4.2
	shell(-w / 2, w / 2, length, 2.8, "front", 0.0, "carpet_blue")
	Props.shelf(self, Vector3(w / 2 - 0.3, 0, 2.2), -PI / 2)
	Props.shelf(self, Vector3(-w / 2 + 0.3, 0, 3.6), PI / 2)
	add_locker(Vector3(w / 2 - 0.4, 0, 4.4), -PI / 2)
	box(Vector3(0.4, 0.35, 0.4), Vector3(-w / 2 + 0.45, 0.175, 1.6), Mats.get_mat("poster_yellow"), true)
	box(Vector3(0.03, 1.3, 0.03), Vector3(-w / 2 + 0.45, 0.85, 1.6), Mats.get_mat("wood_old"), false, 0.0)
	scatter_loot([Vector3(w / 2 - 0.3, 0.75, 2.2)])
	make_path([Vector3(0, 0, length * 0.5)])
