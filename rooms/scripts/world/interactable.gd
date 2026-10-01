class_name Interactable
extends Area3D
## something the player can aim at and press interact on. the player's raycast hits this
## area (collision layer 3), shows `prompt` and calls interact().
## hold_time > 0 means the button has to be held that long (exit doors).

signal used(player)

var prompt := "interact"
var hold_time := 0.0
var enabled := true


static func make(size: Vector3, prompt_text: String) -> Interactable:
	var it := Interactable.new()
	it.prompt = prompt_text
	it.collision_layer = 4
	it.collision_mask = 0
	it.monitoring = false
	var cs := CollisionShape3D.new()
	var sh := BoxShape3D.new()
	sh.size = size
	cs.shape = sh
	it.add_child(cs)
	return it


func interact(player) -> void:
	if enabled:
		used.emit(player)
