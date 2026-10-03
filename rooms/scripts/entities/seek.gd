class_name Seek
extends Rusher
## seek (from doors, "got any more doors?" modifier): eyes open up on the walls of the room
## you're in. through the next door it rises up behind you and the chase starts. run!
## doors open by themselves while it's chasing. it gives up after CHASE_DOORS doors.
## lockers don't help.

const RULES := {
	"trigger": "door", "min_door": 25, "chance": 0.05, "cooldown": 240.0,
	"group": "seek", "blocked_by": ["a60b", "a60", "a120", "a200", "rush", "ambush", "figure"],
	"needs_mod": "more_doors",
}
const CHASE_DOORS := 5
const EYES := 14

var _start_door := 0
var _chasing := false
var _eye_nodes: Array[Node3D] = []
var _ending := false
var _last_door: Door = null


func begin() -> void:
	id = "seek"
	speed = 6.1  # a bit slower than you sprinting
	stop_at_closed_door = true
	kill_range = 1.6
	_start_door = Game.door
	_spawn_eyes(generator.room(Game.door))
	var a := make_3d_audio("seek_eyes", 10.0, 60.0)
	a.global_position = player.global_position
	a.play()
	Game.door_changed.connect(_on_door)


func _spawn_eyes(r: RoomBase) -> void:
	if r == null:
		return
	var space := get_world_3d().direct_space_state
	var pts := r.path_global()
	var tex := Assets.glow_texture("seek_eye")
	for i in EYES:
		var a: Vector3 = pts[randi() % pts.size()]
		var b: Vector3 = pts[mini(pts.size() - 1, randi() % pts.size() + 1)]
		var along := (b - a)
		var side := Vector3(-along.z, 0, along.x).normalized() if along.length() > 0.1 else Vector3.RIGHT
		if randf() < 0.5:
			side = -side
		var from := a.lerp(b, randf()) + Vector3(0, randf_range(-0.9, 0.9), 0)
		var hit := space.intersect_ray(PhysicsRayQueryParameters3D.create(from, from + side * 12.0, 1))
		if hit.is_empty():
			continue
		var s := Sprite3D.new()
		s.texture = tex
		s.pixel_size = randf_range(0.35, 0.7) / maxf(1.0, tex.get_height())
		s.shaded = false
		s.transparent = true
		r.add_child(s)
		s.global_position = hit.position + hit.normal * 0.03
		s.look_at(s.global_position + hit.normal, Vector3.UP if absf(hit.normal.y) < 0.9 else Vector3.FORWARD)
		s.rotate_object_local(Vector3.UP, PI)  # look_at points -z at the wall; sprites face +z
		_eye_nodes.append(s)


func _on_door(n: int) -> void:
	if _done:
		return
	if not _chasing and n > _start_door:
		_start_chase()
	elif _chasing and n >= _start_door + 1 + CHASE_DOORS:
		_end_chase()


func _start_chase() -> void:
	_chasing = true
	Game.chase = true
	place(true)
	setup_look("seek_body", Color(0.2, 0.2, 0.2), 2.6)
	set_sprite_texture(sprite, Assets.texture("seek_body"))
	(sprite.material_override as StandardMaterial3D).billboard_mode = BaseMaterial3D.BILLBOARD_FIXED_Y
	sprite.position.y = -0.3
	light.light_energy = 0.6
	var scream := make_3d_audio("seek_scream", 20.0, 120.0)
	scream.play()
	near_key = ""
	var steps := make_3d_audio("seek_steps", 8.0, 60.0)
	steps.play()
	Game.subtitle.emit("run!", Color(1, 0.4, 0.4))
	after(1.2, func(): moving = true)


## the chase is over once you're through the last door: wait until you're clear of the
## doorway, then the curious light slams it shut in seek's face
func _end_chase() -> void:
	Game.chase = false
	_ending = true
	_last_door = generator.room(Game.door - 1).exit_door if generator.room(Game.door - 1) else null


func _process(_delta: float) -> void:
	if not _ending or _done:
		return
	if _last_door == null or not is_instance_valid(_last_door):
		finish()
		return
	if player.global_position.distance_to(_last_door.global_position) > 2.2:
		_ending = false
		_chasing = false
		moving = false
		_last_door.slam_shut()
		Game.subtitle.emit("...", Color(1.0, 0.86, 0.45))
		after(1.0, finish)


func should_kill(_dist: float) -> bool:
	return _chasing and not _ending  # lockers don't save you from seek


func kill() -> void:
	hit_player = true
	Game.death_details["seek"] = "caught"
	var a := AudioStreamPlayer.new()
	a.stream = Assets.sound("seek_kill")
	a.bus = "Alert"
	get_parent().add_child(a)
	a.play()
	player.take_damage(999, "seek")


## at the newest door it waits right behind you
func on_path_end() -> void:
	pass


func _exit_tree() -> void:
	Game.chase = false
	for e in _eye_nodes:
		if is_instance_valid(e):
			e.queue_free()
