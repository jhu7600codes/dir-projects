class_name A50
extends ScreenEntity
## a-50, "the darkness" (from rooms & doors): stay hidden too long while nothing's coming
## and the dark comes for you instead. it creeps in from the edges, whispers, then hurts.
## turn your flashlight on to push it back (once per hiding spot), or just get out.
## spawned by the game when you've hidden too long (see game_world.gd).

const RULES := {"trigger": "manual", "floor": ["offices", "city"], "group": "darkness"}
const DPS := 7.0

var _dark: ColorRect
var _level := 0.0
var _pushed := false
var _whisper: AudioStreamPlayer
var _hold := 0.0


func begin() -> void:
	id = "a50"
	make_overlay()
	_dark = full_rect(Color(0, 0, 0, 0))
	_whisper = AudioStreamPlayer.new()
	_whisper.stream = Assets.sound("a50_whisper")
	_whisper.bus = "SFX"
	_whisper.volume_db = -20.0
	add_child(_whisper)
	_whisper.play()
	player.notify("it's getting dark in here...")


func _process(delta: float) -> void:
	if _done or player == null or player.dead:
		return
	if not player.hidden:
		finish()
		return
	# the flashlight scares it off, but only once per hiding spot
	if not _pushed and player.lights.flashlight_on:
		_pushed = true
		_hold = 10.0
		_level = 0.0
		player.notify("the dark backs off. for now.")
	if _hold > 0.0:
		_hold -= delta
	else:
		_level = minf(1.0, _level + delta / 4.0)
	_dark.color.a = _level * 0.92
	_whisper.volume_db = lerpf(-20.0, 0.0, _level)
	if _level >= 0.75:
		hit_player = true
		Game.death_details["a50"] = "light_used" if _pushed else "no_light"
		player.take_damage(DPS * delta, "a50")
