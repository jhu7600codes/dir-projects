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
	box(Vector3(3.5, 0.06, 4.0), Vector3(0, 3.57, length * 0.5), Mats.get_mat("skylight"), false)
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
	var shop := ShopStand.new()
	shop.title = "shakelight dispenser"
	shop.offers = [["shakelight", 10], ["bandage", 5], ["battery", 4]]
	shop.position = Vector3(w / 2 - 0.45, 0, length * 0.5 - 2.6)
	shop.rotation.y = -PI / 2
	add_child(shop)
	make_path([Vector3(0, 0, length * 0.5)])
