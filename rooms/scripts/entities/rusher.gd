class_name Rusher
extends Entity
## shared movement for entities that fly through the rooms (a-60, a-60b, a-120, a-200).
## they follow each loaded room's path line, room by room.
##   dir = 1 goes toward newer rooms (from behind you), dir = -1 toward older rooms.
## subclasses set speed / sounds and override on_path_end() and should_kill().

var speed := 30.0
var dir := 1
var stop_at_closed_door := true  # don't pass the next unopened door (like rush)
var kill_range := 16.0
var moving := false
var room_n := 0
var pt := 0
var far_key := ""
var near_key := ""
var near_dist := 22.0

var sprite: Sprite3D
var light: OmniLight3D
var far_audio: AudioStreamPlayer
var near_audio: AudioStreamPlayer3D
var _target := Vector3.ZERO


## put the entity at the start of the oldest room (from_back) or end of the newest
func place(from_back: bool) -> void:
	var r: RoomBase = generator.oldest() if from_back else generator.newest()
	room_n = r.number
	var pts := r.path_global()
	pt = 0 if from_back else pts.size() - 1
	global_position = pts[pt]
	_target = global_position
	dir = 1 if from_back else -1


func setup_look(tex_key: String, color: Color, size := 2.2) -> void:
	sprite = make_sprite(tex_key, size)
	light = OmniLight3D.new()
	light.light_color = color
	light.light_energy = 3.0
	light.omni_range = 9.0
	add_child(light)
	if far_key != "":
		far_audio = AudioStreamPlayer.new()
		far_audio.stream = Assets.sound(far_key)
		far_audio.bus = "SFX"
		far_audio.volume_db = -30.0
		add_child(far_audio)
		far_audio.play()
	if near_key != "":
		near_audio = make_3d_audio(near_key, 10.0, near_dist * 2.0)


func set_visible_body(on: bool) -> void:
	if sprite:
		sprite.visible = on
	if light:
		light.visible = on


func _physics_process(delta: float) -> void:
	if is_frozen() or _done:
		return
	_update_audio()
	if moving:
		_move(speed * delta)
	if player and not player.dead:
		var d := global_position.distance_to(player.head_position())
		if d < kill_range and should_kill(d):
			kill()


func _move(step: float) -> void:
	var guard := 0
	while step > 0.0 and guard < 16:
		guard += 1
		var to := _target - global_position
		var dist := to.length()
		if dist > step:
			global_position += to / dist * step
			return
		global_position = _target
		step -= dist
		var nxt = _next_point()
		if nxt == null:
			on_path_end()
			return
		_target = nxt


func _next_point():
	var r: RoomBase = generator.room(room_n)
	if r == null:
		# our room got freed: jump onto the oldest room
		r = generator.oldest()
		if r == null:
			return null
		room_n = r.number
		pt = 0
		return r.path_global()[0]
	var pts := r.path_global()
	var np := pt + dir
	if np >= 0 and np < pts.size():
		pt = np
		return pts[pt]
	var nr := room_n + dir
	if dir > 0 and stop_at_closed_door and nr > Game.door:
		return null
	var r2: RoomBase = generator.room(nr)
	if r2 == null:
		return null
	room_n = nr
	var p2 := r2.path_global()
	pt = 0 if dir > 0 else p2.size() - 1
	return p2[pt]


## turn around and keep going (used for rebounds)
func reverse() -> void:
	dir = -dir
	var nxt = _next_point()
	if nxt != null:
		_target = nxt


func _update_audio() -> void:
	if player == null:
		return
	var d := global_position.distance_to(player.global_position)
	if far_audio:
		far_audio.volume_db = lerpf(-4.0, -32.0, clampf(d / 120.0, 0.0, 1.0))
	if near_audio:
		if d < near_dist and not near_audio.playing:
			near_audio.play()
		elif d > near_dist * 1.6 and near_audio.playing:
			near_audio.stop()


## override: what happens when the path runs out
func on_path_end() -> void:
	finish()


## override: default = kills anyone it can see who isn't in a locker
func should_kill(_dist: float) -> bool:
	return not player.hidden and sees_player()


func kill() -> void:
	hit_player = true
	player.take_damage(999, id)
