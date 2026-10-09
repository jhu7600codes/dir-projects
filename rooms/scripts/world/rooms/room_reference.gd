extends RoomBase
## reference room (from doors' rooms): two tables and a cubicle. the desk has a monitor, a
## cup of hot chocolate, a purple keycard and a nameplate. the table by the entrance has two
## plants, the one by the exit one plant and some colored boxes. nothing spawns here.


func build() -> void:
	room_type = "reference"
	var length := rng.randf_range(9.0, 11.0)
	var w := 7.5
	var flip := 1.0 if rng.randf() < 0.5 else -1.0
	shell(-w / 2, w / 2, length, 3.0, "front", -flip * 2.2, "carpet_red")
	# table by the entrance, two plants
	var t1 := Vector3(flip * 2.2, 0, 1.4)
	Props.table(self, t1, 0.0, Vector3(1.6, Props.TABLE_H, 0.8))
	Props.plant(self, t1 + Vector3(-0.45, Props.TABLE_H + 0.02, 0))
	Props.plant(self, t1 + Vector3(0.45, Props.TABLE_H + 0.02, 0))
	# the cubicle against the side wall
	var cp := Vector3(flip * (w / 2 - 1.1), 0, length * 0.55)
	var yaw := flip * PI / 2
	Props.cubicle(self, cp, yaw)
	var b := Basis(Vector3.UP, yaw)
	var top := cp + b * Vector3(0, Props.TABLE_H + 0.02, -0.75)
	box(Vector3(0.55, 0.36, 0.04), top + b * Vector3(0, 0.3, -0.05) , Mats.get_mat("screen"), false, yaw)
	box(Vector3(0.08, 0.12, 0.08), top + b * Vector3(0, 0.06, -0.05), Mats.get_mat("dark"), false, yaw)
	box(Vector3(0.08, 0.1, 0.08), top + b * Vector3(0.45, 0.05, 0.1), Mats.get_mat("fridge_white"), false)
	box(Vector3(0.065, 0.01, 0.065), top + b * Vector3(0.45, 0.1, 0.1), Mats.get_mat("cocoa"), false)
	box(Vector3(0.09, 0.006, 0.055), top + b * Vector3(-0.4, 0.003, 0.15), Mats.get_mat("keycard"), false, yaw + 0.3)
	# table by the exit, one plant and colored boxes
	var t2 := Vector3(flip * 0.6, 0, length - 1.3)
	Props.table(self, t2, 0.0, Vector3(1.6, Props.TABLE_H, 0.8))
	Props.plant(self, t2 + Vector3(0.5, Props.TABLE_H + 0.02, 0))
	var cols := ["poster_red", "poster_blue", "poster_yellow", "poster_green"]
	for i in 3:
		box(Vector3(0.25, 0.2, 0.25), t2 + Vector3(-0.55 + i * 0.3, Props.TABLE_H + 0.1, rng.randf_range(-0.15, 0.15)), Mats.get_mat(cols[rng.randi() % cols.size()]), false, rng.randf_range(-0.4, 0.4))
	scatter_loot([t1 + Vector3(0, Props.TABLE_H + 0.02, 0.2), Vector3(-flip * (w / 2 - 0.4), 0, length * 0.5)])
	make_path([Vector3(0, 0, length * 0.35), Vector3(-flip * 1.2, 0, length * 0.6), Vector3(-flip * 2.2, 0, length - 1.0)])
