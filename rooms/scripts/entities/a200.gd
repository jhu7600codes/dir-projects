class_name A200
extends Rusher
## a-200, the happy scribble (nico's original name). she comes from the front, then runs
## back and forth about three times ("rebounds"). each rebound she may switch mode:
##   white  - kills players outside lockers that she can see
##   purple - ignores players outside, checks the lockers instead
## the screen edges glow in her current color.

const RULES := {
	"floor": ["offices", "city"], "city_min": 10,
	"trigger": "timer", "min_door": 100, "timer": [250.0, 325.0], "chance": 0.6,
	"cooldown": 60.0, "group": "rusher",
	"blocked_by": ["a60", "a60b", "a120"], "pause_timer_while": ["a60", "a60b"],
}

const WHITE := Color(1, 1, 1)
const PURPLE := Color(0.7, 0.25, 1.0)
## her own lines (written for this game) when she turns around
const LINES := [
	"round and round we go~",
	"forgot to check behind you!",
	"oh, you're still here?",
	"one more lap, just for you.",
	"peekaboo...",
]

var purple := false
var rebounds := 0
var max_rebounds := 3


func begin() -> void:
	id = "a200"
	far_key = "a200_far"
	near_key = "a200_near"
	speed = 15.0
	stop_at_closed_door = false
	place(false)
	setup_look("a200_face", WHITE, 2.6)
	set_visible_body(false)
	make_3d_audio("a200_spawn", 30.0, 200.0).play()
	after(5.0, func():
		set_visible_body(true)
		_set_mode(false)
		moving = true)


func _set_mode(p: bool) -> void:
	purple = p
	var c := PURPLE if purple else WHITE
	if sprite:
		set_sprite_color(sprite, c)
	if light:
		light.light_color = c.lerp(Color(0.5, 0.7, 1.0), 0.3)
	Game.screen_tint.emit(Color(c, 0.35))


func on_path_end() -> void:
	if rebounds >= max_rebounds:
		finish()
		return
	rebounds += 1
	if randf() < 0.5:
		_set_mode(not purple)
	if randf() < 0.7:
		Game.subtitle.emit(LINES[randi() % LINES.size()], PURPLE if purple else WHITE)
		var v := make_3d_audio("a200_voice", 20.0, 150.0)
		v.play()
	reverse()


func should_kill(dist: float) -> bool:
	if purple:
		return player.hidden and dist < 6.0
	return not player.hidden and sees_player()


func _exit_tree() -> void:
	Game.screen_tint.emit(Color(0, 0, 0, 0))
