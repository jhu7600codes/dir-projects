class_name A10
extends Entity
## a-10 (the sightings): it doesn't like you hiding. stay in a locker or under a table for
## too long and it comes for you: the room goes dark, it stares in at you and tells you to
## get out, louder and louder, banging on the door. get out in time and it leaves. don't,
## and it kicks you out itself, and that hurts.
## the manager starts it after you've been hidden a while (not while something's hunting
## you, that wouldn't be fair). each player has their own.

const RULES := {
	"trigger": "hiding", "min_door": 10,
	"cooldown": 30.0, "group": "hide",
	"needs_sightings": true,
	"hide_after": [16.0, 24.0],  # seconds hidden before it comes
}
const WARN := 6.0      # seconds from "get out" to the kick
const DAMAGE := 40.0
const LINES := [[0.4, "get out."], [2.4, "get out!"], [4.4, "GET OUT."]]

var sprite: Sprite3D
var _t := -1.0         # -1: waiting for you to hide (training spawns it any time)
var _wait := 0.0
var _said := 0
var _bang_t := 0.0
var _locker: Locker
var _leaving := false


func begin() -> void:
	id = "a10"
	sprite = make_sprite("a10_face", 2.0)
	set_sprite_texture(sprite, Faces.a10_tex())
	sprite.pixel_size = 2.0 / maxf(1.0, sprite.texture.get_height())
	sprite.modulate.a = 0.0
	if player and player.hidden:
		_start()


func _start() -> void:
	_t = 0.0
	_locker = player.current_locker
	# right outside the hiding spot, looking in through the slits
	global_transform = _locker.global_transform * Transform3D(Basis(), Vector3(0, 1.3 if _locker.style != "table" else 0.6, 1.0))
	Game.screen_tint.emit(Color(0.12, 0.02, 0.18, 0.5))


func _process(delta: float) -> void:
	if is_frozen() or player == null or _done or _leaving:
		return
	if player.dead:
		_leave()
		return
	if _t < 0.0:
		_wait += delta
		if player.hidden and player.current_locker:
			_start()
		elif _wait > 15.0:
			finish()
		return
	if not player.hidden:
		_leave()  # you got out. it lets you go
		return
	_t += delta
	sprite.modulate.a = minf(0.9, _t * 0.5)
	sprite.offset = Vector2(randf_range(-2, 2), randf_range(-2, 2)) * (1.0 + _t)
	while _said < LINES.size() and _t >= LINES[_said][0]:
		Game.subtitle.emit(Loc.t(LINES[_said][1]), Color(0.85, 0.55, 1.0))
		_said += 1
	# it bangs on the door, faster and louder
	_bang_t -= delta
	if _bang_t <= 0.0 and _t > 1.0:
		_bang_t = lerpf(1.1, 0.25, _t / WARN)
		_bang(lerpf(-12.0, 2.0, _t / WARN))
		if _locker:
			_locker.play_door()
	if _t >= WARN:
		_kick_out()


func _kick_out() -> void:
	_bang(6.0)
	hit_player = true
	Game.death_details[id] = "table" if _locker and _locker.style == "table" else "locker"
	player.exit_locker()
	if player.hud:
		player.hud.flash(Color(0.4, 0.05, 0.5, 0.6), 0.6)
	player.take_damage(DAMAGE, id)
	_leave()


func _leave() -> void:
	if _leaving:
		return
	_leaving = true
	Game.screen_tint.emit(Color(0, 0, 0, 0))
	var tw := create_tween()
	tw.tween_property(sprite, "modulate:a", 0.0, 0.4)
	tw.tween_callback(finish)


func finish() -> void:
	Game.screen_tint.emit(Color(0, 0, 0, 0))
	super.finish()


func _bang(db: float) -> void:
	var a := AudioStreamPlayer3D.new()
	a.stream = Assets.sound("a10_kick")
	a.bus = "SFX"
	a.unit_size = 10.0
	a.volume_db = db
	a.pitch_scale = randf_range(0.85, 1.1)
	get_parent().add_child(a)
	a.global_position = global_position
	a.play()
	a.finished.connect(a.queue_free)
