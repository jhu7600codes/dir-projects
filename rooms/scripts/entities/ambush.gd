class_name Ambush
extends Rusher
## ambush (from doors, "got any more doors?" modifier): like rush but green, faster, and it
## rebounds back and forth a few times. stay in the locker until it's really gone.

const RULES := {
	"trigger": "door", "min_door": 15, "chance": 0.12,
	"needs_spawn_room": true, "cooldown": 60.0,
	"group": "rusher", "blocked_by": ["a60b", "seek"], "needs_mod": "more_doors",
}

var rebounds := 0
var max_rebounds := 3


func begin() -> void:
	id = "ambush"
	far_key = "ambush_far"
	speed = 42.0
	max_rebounds = randi_range(2, 5)
	place(true)
	setup_look("ambush_face", Color(0.3, 1.0, 0.4), 2.6)
	set_visible_body(false)
	for r in generator.rooms:
		r.flicker_burst(3.0)
	after(3.0, func():
		set_visible_body(true)
		moving = true)


func on_path_end() -> void:
	if rebounds < max_rebounds:
		rebounds += 1
		moving = false
		set_visible_body(false)
		after(randf_range(1.0, 2.5), func():
			set_visible_body(true)
			reverse()
			moving = true)
		return
	finish()


func _process(_delta: float) -> void:
	if sprite and moving and not is_frozen():
		sprite.offset = Vector2(randf_range(-5, 5), randf_range(-5, 5))
