class_name Dispenser
extends Node3D
## the a-000 dispenser: a vending machine with four buttons (bandage, vitamins, battery,
## shakelight). the items sit behind the glass next to their button. buy one and it rolls
## down the chute into the slot at the bottom, where you take it with interact (or leave it).
## local space: the front faces +z.

const OFFERS := [["bandage", 5], ["vitamins", 8], ["battery", 4], ["shakelight", 10]]
const W := 1.0
const H := 2.0
const D := 0.7

var _slot_items := 0
var _drops := 0


func _ready() -> void:
	var body := StaticBody3D.new()
	body.collision_mask = 0
	add_child(body)
	var cs := CollisionShape3D.new()
	var sh := BoxShape3D.new()
	sh.size = Vector3(W, H, D)
	cs.shape = sh
	cs.position = Vector3(0, H / 2, 0)
	body.add_child(cs)
	_box(Vector3(W, H, D), Vector3(0, H / 2, 0), Mats.get_mat("car_red"))
	# the glass window with the goods, the dark inside behind it
	_box(Vector3(0.62, 1.0, 0.3), Vector3(-0.12, 1.35, D / 2 - 0.15), Mats.get_mat("cubicle"))
	var glow := OmniLight3D.new()
	glow.position = Vector3(-0.12, 1.75, D / 2 + 0.15)
	glow.light_energy = 0.6
	glow.omni_range = 1.4
	add_child(glow)
	_box(Vector3(0.64, 1.02, 0.02), Vector3(-0.12, 1.35, D / 2 + 0.11), Mats.get_mat("glass"))
	for x in [-0.45, 0.21]:
		_box(Vector3(0.04, 1.02, 0.12), Vector3(x, 1.35, D / 2 + 0.06), Mats.get_mat("car_red"))
	# button panel
	_box(Vector3(0.22, 1.0, 0.02), Vector3(0.36, 1.35, D / 2 + 0.01), Mats.get_mat("metal"))
	# the dispense slot with its flap
	_box(Vector3(0.6, 0.22, 0.03), Vector3(-0.05, 0.38, D / 2 + 0.005), Mats.get_mat("dark"))
	_box(Vector3(0.6, 0.03, 0.12), Vector3(-0.05, 0.27, D / 2 + 0.05), Mats.get_mat("metal"))
	for i in OFFERS.size():
		var item: String = OFFERS[i][0]
		var price: int = OFFERS[i][1] * (2 if Game.mod("inflation") else 1)
		var y := 1.72 - i * 0.24
		# a shelf with the item on it
		_box(Vector3(0.58, 0.02, 0.2), Vector3(-0.12, y - 0.07, D / 2 + 0.005), Mats.get_mat("metal"))
		var m := ItemModels.build(item)
		m.scale = Vector3.ONE * 1.9
		if item in ["battery", "shakelight"]:
			m.rotation.z = PI / 2
			m.position = Vector3(-0.12 + (0.08 if item == "battery" else 0.24), y - 0.03, D / 2 + 0.005)
		else:
			m.position = Vector3(-0.12, y - 0.06, D / 2 + 0.005)
		add_child(m)
		# the button
		var b := _box(Vector3(0.1, 0.1, 0.04), Vector3(0.36, y, D / 2 + 0.03), Mats.get_mat(["poster_blue", "poster_yellow", "poster_green", "poster_red"][i]))
		var it := Interactable.make(Vector3(0.2, 0.18, 0.3), "%d gold" % price)
		it.position = Vector3(0.36, y, D / 2 + 0.12)
		it.used.connect(_press.bind(item, price, b))
		add_child(it)


func _box(size: Vector3, pos: Vector3, mat: Material) -> MeshInstance3D:
	var mi := MeshInstance3D.new()
	var bm := BoxMesh.new()
	bm.size = size
	mi.mesh = bm
	mi.material_override = mat
	mi.position = pos
	add_child(mi)
	return mi


func _press(player, item: String, price: int, button: MeshInstance3D) -> void:
	var tw := create_tween()
	tw.tween_property(button, "position:z", D / 2 + 0.01, 0.06)
	tw.tween_property(button, "position:z", D / 2 + 0.03, 0.1)
	if not ShopStand.can_buy(player, item):
		return
	if not Save.spend_gold(price):
		player.notify("not enough gold")
		Game.play_ui("deny")
		return
	Game.play_ui("buy")
	if item == "shakelight":
		Achievements.unlock("shake_it")
	_drop(item)
	Net.send_event(self, "drop", item)


## multiplayer: someone else bought something, it rolls out here too
func net_event(what: String, arg) -> void:
	if what == "drop":
		_drop(str(arg))


## the item falls off its shelf, rolls down the chute inside and lands in the slot
func _drop(item: String) -> void:
	var m := ItemModels.build(item)
	m.scale = Vector3.ONE * 1.6
	m.position = Vector3(-0.12, 0.95, D / 2 + 0.02)
	add_child(m)
	var a := AudioStreamPlayer3D.new()
	a.stream = Assets.sound("locker")
	a.pitch_scale = 1.8
	a.volume_db = -6.0
	a.position = Vector3(0, 0.5, D / 2)
	add_child(a)
	_drops += 1
	var slot_x := -0.2 + (_slot_items % 3) * 0.15
	_slot_items += 1
	var tw := create_tween()
	tw.tween_property(m, "position", Vector3(0.05, 0.62, D / 2 - 0.2), 0.35).set_ease(Tween.EASE_IN)
	tw.parallel().tween_property(m, "rotation:z", TAU, 0.35)
	tw.tween_property(m, "position", Vector3(slot_x, 0.3, D / 2 - 0.05), 0.45).set_ease(Tween.EASE_IN)
	tw.parallel().tween_property(m, "rotation:z", TAU * 2.2, 0.45)
	tw.tween_callback(func():
		a.play()
		m.queue_free()
		var p := Pickup.new()
		p.item = item
		p.name = "drop_%d" % _drops
		p.position = Vector3(slot_x, 0.29, D / 2 + 0.02)
		add_child(p)
		p.tree_exited.connect(func(): _slot_items = maxi(0, _slot_items - 1)))
