class_name ShopStand
extends Node3D
## a counter that sells items for gold. a-000 has the shakelight dispenser,
## a-150 has a bigger shop. offers: [[item, price], ...]
## local space: the counter faces +z (that's where you stand).

var offers: Array = [["shakelight", 10]]
var title := "shop"


func _ready() -> void:
	var w := 0.9 * offers.size() + 0.4
	var body := StaticBody3D.new()
	body.collision_mask = 0
	add_child(body)
	var cs := CollisionShape3D.new()
	var sh := BoxShape3D.new()
	sh.size = Vector3(w, 1.0, 0.6)
	cs.shape = sh
	cs.position = Vector3(0, 0.5, 0)
	body.add_child(cs)
	var mi := MeshInstance3D.new()
	var bm := BoxMesh.new()
	bm.size = Vector3(w, 1.0, 0.6)
	mi.mesh = bm
	mi.material_override = Mats.get_mat("metal")
	mi.position = Vector3(0, 0.5, 0)
	add_child(mi)
	var t := Label3D.new()
	t.text = title
	t.font_size = 40
	t.pixel_size = 0.004
	t.position = Vector3(0, 1.45, 0)
	t.modulate = Color(0.95, 0.85, 0.4)
	add_child(t)
	for i in offers.size():
		var item: String = offers[i][0]
		var price: int = offers[i][1]
		var x := -w / 2 + 0.65 + i * 0.9
		var l := Label3D.new()
		l.text = "%s\n%d gold" % [item, price]
		l.font_size = 28
		l.pixel_size = 0.004
		l.position = Vector3(x, 1.12, 0.1)
		add_child(l)
		var it := Interactable.make(Vector3(0.8, 0.6, 0.5), "buy %s (%d gold)" % [item, price])
		it.position = Vector3(x, 1.0, 0.3)
		it.used.connect(_buy.bind(item, price))
		add_child(it)


func _buy(player, item: String, price: int) -> void:
	if item == "shakelight" and player.inventory.has_shakelight:
		player.notify("you already have one")
		return
	if item == "flashlight" and player.inventory.has_flashlight:
		player.notify("you already have one")
		return
	if not Save.spend_gold(price):
		player.notify("not enough gold")
		Game.play_ui("deny")
		return
	player.inventory.add(item, 1)
	Game.play_ui("buy")
	if item == "shakelight":
		Achievements.unlock("shake_it")
