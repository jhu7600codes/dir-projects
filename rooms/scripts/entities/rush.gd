class_name Rush
extends Rusher
## rush (from doors, "got any more doors?" modifier): the lights flicker, a roar comes from
## behind, then it flies through the rooms once. hide in a locker.

const RULES := {
	"trigger": "door", "min_door": 5, "chance": 0.22,
	"needs_spawn_room": true, "cooldown": 35.0,
	"group": "rusher", "blocked_by": ["a60b", "seek"], "needs_mod": "more_doors",
}


func begin() -> void:
	id = "rush"
	far_key = "rush_far"
	speed = 34.0
	place(true)
	setup_look("rush_face", Color(0.3, 0.3, 0.45), 2.6)
	set_visible_body(false)
	_flicker_rooms(4.0)
	after(4.0, func():
		set_visible_body(true)
		moving = true)


func _flicker_rooms(t: float) -> void:
	for r in generator.rooms:
		r.flicker_burst(t)


func _process(_delta: float) -> void:
	if sprite and moving and not is_frozen():
		sprite.offset = Vector2(randf_range(-4, 4), randf_range(-4, 4))
