class_name A10
extends Rusher
## a-10, "the void" (from rooms & doors): a hole in the world creeps after you from the rooms
## behind. it's slow, but it never stops and it doesn't care about lockers: anything it
## reaches is gone. keep walking. it gives up once you're 6 doors further.

const RULES := {
	"floor": ["offices"], "trigger": "door", "min_door": 10, "chance": 0.05,
	"cooldown": 200.0, "group": "void", "blocked_by": ["a60b", "seek"],
}

var start_door := 0


func begin() -> void:
	id = "a10"
	far_key = "a10_far"
	near_key = "a10_near"
	speed = 3.4
	kill_range = 2.4
	near_dist = 14.0
	place(true)
	setup_look("a10_face", Color(0.5, 0.1, 0.8), 4.2)
	set_sprite_texture(sprite, Faces.void_tex())
	sprite.pixel_size = 4.2 / 128.0
	light.light_energy = 1.2
	start_door = Game.door
	player.notify("the rooms behind you are disappearing. keep moving.")
	after(4.0, func(): moving = true)


func _process(_delta: float) -> void:
	if _done or player == null:
		return
	if Game.door >= start_door + 6:
		finish()
		return
	# far behind it hurries up a bit, so it stays a threat. close up it's slow, so you can walk away
	var d := global_position.distance_to(player.global_position)
	speed = 3.4 if d < 30.0 else 6.0
	if sprite:
		sprite.rotation.z += _delta * 0.4


## waiting at the door you haven't opened yet: it just sits there and waits for you
func on_path_end() -> void:
	pass


## lockers don't help. if it reaches you, you're gone
func should_kill(d: float) -> bool:
	return d < kill_range


func kill() -> void:
	hit_player = true
	Game.death_details[id] = "hid" if player.hidden else "caught"
	player.take_damage(999, id)
