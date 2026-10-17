class_name C15
extends Entity
## c-15, "the pigeon flock": pigeons on the street in the block you just walked into.
## walk slowly past and they don't care. run near them, or walk right into them, and the
## whole flock bursts up into your face: feathers everywhere, pecking, a bit of damage.

const RULES := {
	"floor": ["city"], "trigger": "door", "city_min": 2, "chance": 0.3,
	"cooldown": 20.0, "group": "pigeons",
}
const SCARE_RUN := 9.0
const SCARE_WALK := 2.2

var _room_n := 0
var _birds: Array = []
var _burst := false
var _t := 0.0
var _overlay: Control
var _coo: AudioStreamPlayer3D


func begin() -> void:
	id = "c15"
	_room_n = Game.door
	var r: RoomBase = generator.room(_room_n)
	if r == null:
		finish()
		return
	var pts := r.path_global()
	var mid: Vector3 = pts[pts.size() / 2]
	global_position = Vector3(mid.x, mid.y - 1.6, mid.z)
	for i in randi_range(7, 12):
		var s := Sprite3D.new()
		s.texture = Faces.pigeon_tex()
		s.pixel_size = 0.4 / 32.0
		var m := StandardMaterial3D.new()
		m.albedo_texture = s.texture
		m.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
		m.billboard_mode = BaseMaterial3D.BILLBOARD_FIXED_Y
		m.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA_SCISSOR
		m.disable_fog = true  # the dusk fog turned them pink
		m.texture_filter = BaseMaterial3D.TEXTURE_FILTER_NEAREST
		s.material_override = m
		s.flip_h = randf() < 0.5
		add_child(s)
		s.position = Vector3(randf_range(-2.5, 2.5), 0.2, randf_range(-3.0, 3.0))
		_birds.append(s)
	_coo = make_3d_audio("c15_coo", 4.0, 25.0)
	_coo.play()


func _process(delta: float) -> void:
	if _done or player == null or player.dead:
		return
	if Game.door != _room_n:
		finish()
		return
	_t += delta
	if not _burst:
		# pecking about
		for s in _birds:
			s.position.y = 0.2 + absf(sin(_t * 6.0 + s.position.x)) * 0.03
		var d := Vector2(player.global_position.x - global_position.x, player.global_position.z - global_position.z).length()
		var moving: bool = player.is_walking()
		if moving and ((player.sprinting and d < SCARE_RUN) or (not player.crouching and d < SCARE_WALK)):
			_scatter()
		return
	# the flock swirls around your head
	var head: Vector3 = player.head_position()
	for i in _birds.size():
		var s: Sprite3D = _birds[i]
		var a := _t * 5.0 + i
		var goal := head + Vector3(cos(a), sin(a * 1.7) * 0.3, sin(a)) * 0.8
		s.global_position = s.global_position.lerp(goal, minf(1.0, delta * 6.0))


func _scatter() -> void:
	_burst = true
	make_3d_audio("c15_flap", 6.0, 40.0).play()
	hit_player = true
	Game.death_details["c15"] = "ran"
	var cl := CanvasLayer.new()
	cl.layer = 20
	add_child(cl)
	_overlay = Control.new()
	_overlay.set_anchors_preset(Control.PRESET_FULL_RECT)
	_overlay.mouse_filter = Control.MOUSE_FILTER_IGNORE
	cl.add_child(_overlay)
	var vs := _overlay.get_viewport_rect().size
	for i in 40:
		var f := ColorRect.new()
		f.color = Color(0.55, 0.57, 0.6, 0.9)
		f.size = Vector2(randf_range(18, 46), randf_range(6, 14))
		f.position = Vector2(randf() * vs.x, randf() * vs.y)
		f.rotation = randf() * TAU
		f.mouse_filter = Control.MOUSE_FILTER_IGNORE
		_overlay.add_child(f)
	# pecks for a few seconds, then they fly off
	for k in 4:
		after(0.4 + k * 0.7, func():
			if player and not player.dead:
				player.take_damage(4.0, "c15"))
	after(3.4, func():
		var tw := create_tween()
		tw.tween_property(_overlay, "modulate:a", 0.0, 0.6)
		for s in _birds:
			tw.parallel().tween_property(s, "global_position", s.global_position + Vector3(randf_range(-6, 6), 12, randf_range(-6, 6)), 1.5)
		tw.tween_callback(finish))
