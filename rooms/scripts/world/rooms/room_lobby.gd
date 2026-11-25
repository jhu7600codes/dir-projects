extends RoomBase
## a-000: the waiting room. red carpet, couches, a skylight, the shakelight dispenser
## and the elevator you came from behind you.


func build() -> void:
	room_type = "lobby"
	is_special = true
	darkness = 0.0
	var length := 11.0
	var w := 9.0
	shell(-w / 2, w / 2, length, 3.6, "front", 0.0)
	# elevator doors fill the entry gap
	box(Vector3(DOOR_W + 0.3, DOOR_H + 0.1, 0.15), Vector3(0, DOOR_H / 2, 0.02), Mats.get_mat("metal"))
	box(Vector3(0.02, DOOR_H, 0.16), Vector3(0, DOOR_H / 2, 0.03), Mats.get_mat("dark"), false)
	# skylight: bright panel + light pouring down
	var sky_c := Vector3(0, 3.59, length * 0.5)
	Screens.quad(self, Vector2(3.5, 4.0), sky_c, "sky", Basis(Vector3.RIGHT, PI / 2))
	var fr := Mats.get_mat("frame")
	for sx in [-1.0, 1.0]:
		box(Vector3(0.12, 0.08, 4.12), sky_c + Vector3(sx * 1.81, -0.03, 0), fr, false)
		box(Vector3(3.74, 0.08, 0.12), sky_c + Vector3(0, -0.03, sx * 2.06), fr, false)
	box(Vector3(0.05, 0.05, 4.0), sky_c + Vector3(0, -0.03, 0), fr, false)
	for k in [-1.0, 0.0, 1.0]:
		box(Vector3(3.5, 0.05, 0.05), sky_c + Vector3(0, -0.03, k * 1.0), fr, false)
	var sun := SpotLight3D.new()
	sun.position = Vector3(0, 3.5, length * 0.5)
	sun.rotation.x = -PI / 2
	sun.spot_angle = 60.0
	sun.spot_range = 8.0
	sun.light_energy = 1.8
	sun.light_color = Color(0.9, 0.95, 1.0)
	sun.shadow_enabled = int(Settings.data.quality) > 0
	add_child(sun)
	Props.couch(self, Vector3(w / 2 - 0.5, 0, length * 0.5), -PI / 2)
	Props.couch(self, Vector3(-w / 2 + 0.5, 0, length * 0.5), PI / 2)
	Props.plant(self, Vector3(w / 2 - 0.5, 0, 0.6))
	Props.plant(self, Vector3(-w / 2 + 0.5, 0, length - 0.6))
	var disp := Dispenser.new()
	disp.position = Vector3(w / 2 - 0.4, 0, length * 0.5 - 2.6)
	disp.rotation.y = -PI / 2
	add_child(disp)
	add_atm(Vector3(-w / 2 + 0.3, 0, 2.2), PI / 2)
	make_path([Vector3(0, 0, length * 0.5)])
