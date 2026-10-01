extends RoomBase
## a-150: a shop in the dark. spend the gold from the drawers here.


func build() -> void:
	room_type = "shop_150"
	is_special = true
	darkness = 0.55  # a few working lights so you can see the counter
	var length := 10.0
	var w := 8.0
	shell(-w / 2, w / 2, length, 3.0, "front", 0.0)
	var shop := ShopStand.new()
	shop.title = "a-150 shop"
	shop.offers = [["battery", 4], ["bandage", 6], ["vitamins", 10], ["flashlight", 20], ["shakelight", 12]]
	shop.position = Vector3(w / 2 - 0.45, 0, length * 0.5)
	shop.rotation.y = -PI / 2
	add_child(shop)
	var l := OmniLight3D.new()
	l.position = Vector3(w / 2 - 1.5, 2.4, length * 0.5)
	l.light_color = Color(1.0, 0.85, 0.55)
	l.omni_range = 5.0
	add_child(l)
	Props.couch(self, Vector3(-w / 2 + 0.5, 0, length * 0.5), PI / 2)
	add_locker(Vector3(-w / 2 + 0.4, 0, length - 1.0), PI / 2)
	make_path([Vector3(-0.5, 0, length * 0.5)])
