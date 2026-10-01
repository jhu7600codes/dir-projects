class_name Pickup
extends Node3D
## loose item on the floor or a table: gold, battery, bandage, vitamins

var item := "gold"
var amount := 1


func _ready() -> void:
	var m := ItemModels.build(item)
	# a little bigger than real life so you can spot them, like in doors
	m.scale = Vector3.ONE * (1.4 if item == "gold" else 1.7)
	if item == "battery":
		m.rotation = Vector3(0, randf() * TAU, PI / 2)
		m.position.y = 0.03
	else:
		m.rotation.y = randf() * TAU
	add_child(m)
	var it := Interactable.make(Vector3(0.5, 0.4, 0.5), "take " + _name())
	it.position.y = 0.15
	it.used.connect(_take)
	add_child(it)


func _name() -> String:
	if item == "gold":
		return "%d gold" % amount
	return item


func _take(player) -> void:
	player.inventory.add(item, amount)
	queue_free()
