class_name W10
extends Entity
## w-10 "need to get electrocuted?": a glitchy universal outlet. while a breaker room in the
## wires has no power, outlets keep popping up on the walls around you. one crackles for a
## moment, then lunges straight at where you were standing. sidestep it. find the switch
## and they stop.

const RULES := {"trigger": "manual", "floor": "wires", "group": "w10"}
const DAMAGE := 25.0
const LUNGE_SPEED := 13.0

var room_n := 0
var _next := 2.0
var _outlets: Array = []  # [sprite, light, state, timer, target]
static var _tex: Texture2D


func begin() -> void:
	id = "w10"
	if room_n == 0:
		room_n = Game.door


func _process(delta: float) -> void:
	if _done or is_frozen() or player == null or player.dead:
		return
	if Game.door != room_n:
		finish()
		return
	_next -= delta
	if _next <= 0.0 and _outlets.size() < 2:
		_next = randf_range(2.5, 4.5)
		_spawn_outlet()
	for o in _outlets.duplicate():
		_tick(o, delta)


func _spawn_outlet() -> void:
	var head: Vector3 = player.head_position()
	var space := get_world_3d().direct_space_state
	for attempt in 8:
		var a := randf() * TAU
		var dir := Vector3(cos(a), randf_range(-0.25, 0.15), sin(a)).normalized()
		var hit := space.intersect_ray(PhysicsRayQueryParameters3D.create(head, head + dir * 8.0, 1))
		if hit.is_empty() or head.distance_to(hit.position) < 2.5:
			continue
		var s := Sprite3D.new()
		s.texture = outlet_texture()
		s.pixel_size = 0.35 / 64.0
		s.shaded = false
		s.transparent = true
		s.billboard = BaseMaterial3D.BILLBOARD_ENABLED
		add_child(s)
		s.global_position = hit.position + hit.normal * 0.06
		var l := OmniLight3D.new()
		l.light_color = Color(0.5, 0.8, 1.0)
		l.omni_range = 2.5
		l.light_energy = 1.0
		s.add_child(l)
		var au := AudioStreamPlayer3D.new()
		au.stream = Assets.sound("w10_spark")
		au.bus = "SFX"
		au.unit_size = 4.0
		s.add_child(au)
		au.play()
		_outlets.append([s, l, "charge", 1.1, Vector3.ZERO])
		return


func _tick(o: Array, delta: float) -> void:
	var s: Sprite3D = o[0]
	var l: OmniLight3D = o[1]
	if not is_instance_valid(s):
		_outlets.erase(o)
		return
	o[3] -= delta
	if o[2] == "charge":
		# glitching: jitter, colour flicker
		s.modulate = [Color(1, 1, 1), Color(0.4, 1, 1), Color(1, 0.4, 1)][randi() % 3]
		s.offset = Vector2(randf_range(-4, 4), randf_range(-4, 4))
		l.light_energy = randf_range(0.5, 2.5)
		if o[3] <= 0.0:
			o[2] = "lunge"
			o[4] = player.head_position()
			o[3] = 2.0
			var au := AudioStreamPlayer3D.new()
			au.stream = Assets.sound("w10_lunge")
			au.bus = "SFX"
			s.add_child(au)
			au.play()
	elif o[2] == "lunge":
		var to: Vector3 = o[4] - s.global_position
		var step := LUNGE_SPEED * delta
		# keeps flying past the spot you were at, so it can't turn to follow you
		var dir := to.normalized() if to.length() > 0.05 else Vector3.ZERO
		s.global_position += dir * step
		if to.length() < step:
			o[4] = s.global_position + dir * 3.0
		if s.global_position.distance_to(player.head_position()) < 0.8 and not player.dead:
			hit_player = true
			Game.death_details["w10"] = "lunged"
			player.take_damage(DAMAGE, "w10")
			_remove(o)
			return
		if o[3] <= 0.0:
			_remove(o)


func _remove(o: Array) -> void:
	_outlets.erase(o)
	if is_instance_valid(o[0]):
		o[0].queue_free()


## a little universal outlet drawn in code: off-white plate, three slots, a round hole
static func outlet_texture() -> Texture2D:
	if _tex:
		return _tex
	var img := Image.create(64, 64, false, Image.FORMAT_RGBA8)
	img.fill(Color(0, 0, 0, 0))
	for y in range(6, 58):
		for x in range(10, 54):
			img.set_pixel(x, y, Color(0.9, 0.88, 0.82))
	var dark := Color(0.08, 0.08, 0.08)
	for y in range(18, 32):
		for x in range(20, 24):
			img.set_pixel(x, y, dark)
		for x in range(40, 44):
			img.set_pixel(x, y, dark)
	for y in range(38, 48):
		for x in range(27, 37):
			if Vector2(x - 32, y - 43).length() < 5.0:
				img.set_pixel(x, y, dark)
	_tex = ImageTexture.create_from_image(img)
	return _tex
