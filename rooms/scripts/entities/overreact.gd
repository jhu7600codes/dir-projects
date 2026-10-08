class_name Overreact
extends Rusher
## a coworker you stared at. they yell, snap back to their spot, and then come through the
## rooms like a-60 with their face for a head. hide in a locker. they overreact.

const RULES := {"trigger": "manual", "floor": ["offices", "city"], "group": "rusher"}

var face_key := "a60_face_1"


func begin() -> void:
	id = "worker"
	far_key = "a60_far"
	near_key = "a60_near"
	speed = 30.0
	place(true)
	setup_look(face_key, Color(1.0, 0.85, 0.6), 2.4)
	set_visible_body(false)
	var rumble := make_3d_audio("a60_rumble", 30.0, 200.0)
	rumble.play()
	for r in generator.rooms:
		r.flicker_burst(3.5)
	after(3.5, func():
		set_visible_body(true)
		moving = true)


func _process(_delta: float) -> void:
	if sprite and moving and not is_frozen():
		sprite.offset = Vector2(randf_range(-5, 5), randf_range(-5, 5))
