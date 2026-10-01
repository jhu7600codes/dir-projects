class_name Pickup
extends Node3D
## loose item on the floor or a table: gold, battery, bandage, vitamins

var item := "gold"
var amount := 1

const LOOK := {
	"gold": [Vector3(0.22, 0.06, 0.16), "gold"],
	"battery": [Vector3(0.08, 0.16, 0.08), "metal"],
	"bandage": [Vector3(0.18, 0.06, 0.12), "paper"],
	"vitamins": [Vector3(0.08, 0.14, 0.08), "pot"],
}


func _ready() -> void:
	var look: Array = LOOK.get(item, LOOK.gold)
	var mi := MeshInstance3D.new()
	var bm := BoxMesh.new()
	bm.size = look[0]
	mi.mesh = bm
	mi.material_override = Mats.get_mat(look[1])
	mi.position.y = look[0].y / 2
	add_child(mi)
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
