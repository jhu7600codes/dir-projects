class_name W50
extends Entity
## w-50 "the shock": a cloud of white static fog with a face you can barely see. it wakes up
## when you power the elevator at w-50 and drifts after you. being inside it hurts
## (5.5 every half second). outrun it to the elevator.

const RULES := {"trigger": "manual", "floor": "wires", "group": "w50"}
const SPEED := 3.0
const RADIUS := 2.4
const DAMAGE := 5.5
const TICK := 0.5

var room_n := 0
var _puffs: Array[Sprite3D] = []
var _face: Sprite3D
var _t := 0.0
static var _fog: Texture2D


func begin() -> void:
	id = "w50"
	if room_n == 0:
		room_n = Game.door
	var r: RoomBase = generator.room(room_n)
	global_position = (r.path_global()[0] if r else player.global_position + Vector3(0, 1.6, -8.0)) + Vector3(0, -0.4, -1.0)
	for i in 9:
		var s := Sprite3D.new()
		s.texture = fog_texture()
		s.pixel_size = randf_range(2.0, 3.2) / 128.0
		s.shaded = false
		s.transparent = true
		s.billboard = BaseMaterial3D.BILLBOARD_ENABLED
		s.modulate = Color(1, 1, 1, 0.33)
		s.position = Vector3(randf_range(-1.2, 1.2), randf_range(-0.6, 0.8), randf_range(-1.2, 1.2))
		add_child(s)
		_puffs.append(s)
	_face = Sprite3D.new()
	_face.texture = face_texture()
	_face.pixel_size = 1.4 / maxf(1.0, _face.texture.get_height())
	_face.shaded = false
	_face.transparent = true
	_face.billboard = BaseMaterial3D.BILLBOARD_ENABLED
	_face.modulate = Color(0.85, 0.9, 1.0, 0.13)
	add_child(_face)
	var l := OmniLight3D.new()
	l.light_color = Color(0.85, 0.9, 1.0)
	l.light_energy = 1.5
	l.omni_range = 5.0
	add_child(l)
	var hum := make_3d_audio("w50_hum", 6.0, 40.0)
	hum.play()


func _process(delta: float) -> void:
	if _done or is_frozen() or player == null or player.dead:
		return
	var target: Vector3 = player.head_position() + Vector3(0, -0.5, 0)
	var to := target - global_position
	to.y *= 0.3
	if to.length() > 0.2:
		global_position += to.normalized() * minf(SPEED * delta, to.length())
	var tm := Time.get_ticks_msec() / 1000.0
	for i in _puffs.size():
		var s := _puffs[i]
		s.position += Vector3(sin(tm * 1.3 + i), cos(tm * 1.1 + i * 2.0), sin(tm * 0.9 + i * 3.0)) * delta * 0.25
		s.position = s.position.limit_length(1.6)
	_face.modulate.a = 0.08 + 0.08 * absf(sin(tm * 0.7))
	_t -= delta
	if _t <= 0.0 and global_position.distance_to(player.head_position()) < RADIUS:
		_t = TICK
		hit_player = true
		Game.death_details["w50"] = "inside"
		player.take_damage(DAMAGE, "w50")


## the face in the fog: two dark eye holes and a long open mouth on a soft white blob
static var _face_tex: Texture2D


static func face_texture() -> Texture2D:
	if _face_tex:
		return _face_tex
	var img := Image.create(128, 128, false, Image.FORMAT_RGBA8)
	for y in 128:
		for x in 128:
			var d := Vector2((x - 64) / 0.8, y - 64).length() / 60.0
			var a := clampf(1.0 - d, 0.0, 1.0)
			var c := Color(1, 1, 1, a)
			for e in [Vector2(44, 52), Vector2(84, 52)]:
				if Vector2(x, y).distance_to(e) < 9.0:
					c = Color(0.05, 0.05, 0.1, a)
			if absf(x - 64) < 10.0 and y > 72 and y < 104:
				c = Color(0.05, 0.05, 0.1, a)
			img.set_pixel(x, y, c)
	_face_tex = ImageTexture.create_from_image(img)
	return _face_tex


## a soft round puff, drawn in code
static func fog_texture() -> Texture2D:
	if _fog:
		return _fog
	var img := Image.create(128, 128, false, Image.FORMAT_RGBA8)
	for y in 128:
		for x in 128:
			var d := Vector2(x - 64, y - 64).length() / 64.0
			var a := clampf(1.0 - d, 0.0, 1.0)
			img.set_pixel(x, y, Color(1, 1, 1, a * a))
	_fog = ImageTexture.create_from_image(img)
	return _fog
