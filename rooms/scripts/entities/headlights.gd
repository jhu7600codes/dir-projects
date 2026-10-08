class_name Headlights
extends Rusher
## the great city: a horn from up the street, then two blinding headlights come down the
## avenue with nothing behind them. get in a phone booth.

const RULES := {
	"trigger": "door", "floor": ["city"], "city_min": 2, "chance": 0.25,
	"needs_spawn_room": true, "cooldown": 30.0, "group": "rusher",
}


func begin() -> void:
	id = "headlights"
	far_key = "headlights_far"
	speed = 38.0
	stop_at_closed_door = false
	place(false)
	setup_look("headlights_face", Color(1.0, 0.95, 0.75), 2.8)
	light.light_energy = 6.0
	light.omni_range = 16.0
	set_visible_body(false)
	var horn := make_3d_audio("car_horn", 40.0, 300.0)
	horn.play()
	after(1.2, func(): horn.play())
	after(4.0, func():
		set_visible_body(true)
		moving = true)


func _process(_delta: float) -> void:
	if sprite and moving and not is_frozen():
		sprite.offset = Vector2(randf_range(-2, 2), randf_range(-2, 2))
