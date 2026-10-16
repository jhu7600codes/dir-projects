class_name ShopStand
extends Node3D
## a counter with the items you can buy standing on it, slowly turning. no signs: walk up
## to an item and the price shows up. offers: [[item, price], ...]
## local space: the counter faces +z (that's where you stand).

var offers: Array = [["shakelight", 10]]
var title := "shop"
var _models: Array[Node3D] = []


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
	for i in offers.size():
		var item: String = offers[i][0]
		var price: int = offers[i][1] * (2 if Game.mod("inflation") else 1)
		var x := -w / 2 + 0.65 + i * 0.9
		# a little stand with the item on it
		var pad := MeshInstance3D.new()
		var pm := CylinderMesh.new()
		pm.top_radius = 0.16
		pm.bottom_radius = 0.18
		pm.height = 0.04
		pad.mesh = pm
		pad.material_override = Mats.get_mat("dark")
		pad.position = Vector3(x, 1.02, 0.02)
		add_child(pad)
		var m := ItemModels.build(item)
		m.scale = Vector3.ONE * 2.2
		if item == "battery":
			m.scale = Vector3.ONE * 2.6
		m.position = Vector3(x, 1.05, 0.02)
		add_child(m)
		_models.append(m)
		var it := Interactable.make(Vector3(0.8, 0.8, 0.6), Loc.t("%d gold") % price)
		it.position = Vector3(x, 1.2, 0.2)
		it.used.connect(_buy.bind(item, price))
		add_child(it)


func _process(delta: float) -> void:
	for m in _models:
		m.rotation.y += delta * 0.8


func _buy(player, item: String, price: int) -> void:
	if not ShopStand.can_buy(player, item):
		return
	if not Save.spend_gold(price):
		player.notify("not enough gold")
		Game.play_ui("deny")
		return
	player.inventory.add(item, 1)
	Game.play_ui("buy")
	if item == "shakelight":
		Achievements.unlock("shake_it")


static func can_buy(player, item: String) -> bool:
	if (item == "shakelight" and player.inventory.has_shakelight) or (item == "flashlight" and player.inventory.has_flashlight):
		player.notify("you already have one")
		Game.play_ui("deny")
		return false
	return true
