class_name Seek
extends Rusher
## seek (from doors, "got any more doors?" modifier): eyes open up on the walls of the room
## you're in. through the next door it rises up behind you and the chase starts. run!
## doors open by themselves while it's chasing. it gives up after CHASE_DOORS doors.
## lockers don't help. the rooms on the way have obstacles like in doors: fallen beams you
## have to crouch under, and black hands reaching out of the floor on one side.

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
var _obstacles: Array[Node3D] = []
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
	# the next room gets built right after this signal, so add its obstacles a moment later
	if (_chasing or n > _start_door) and n + 1 <= _start_door + CHASE_DOORS:
		_add_obstacles.call_deferred(n + 1)
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
	Game.subtitle.emit("run! crouch under the beams, stay away from the hands", Color(1, 0.4, 0.4))
	after(1.2, func(): moving = true)


## the chase is over once you're through the last door: wait until you're clear of the
## doorway, then the curious light slams it shut in seek's face
## one or two obstacles somewhere along the room's path
func _add_obstacles(n: int) -> void:
	var r: RoomBase = generator.room(n)
	if r == null or _done:
		return
	var pts := r.path_global()
	if pts.size() < 3:
		return
	var kinds := ["beam", "hands"]
	kinds.shuffle()
	var count := 1 if randf() < 0.4 else 2
	for i in count:
		# spread them out: first one early in the room, second later
		var k := clampi(1 + i * (pts.size() - 2) / 2, 1, pts.size() - 2)
		var a: Vector3 = pts[k]
		var b: Vector3 = pts[k + 1]
		var at := a.lerp(b, 0.5)
		var along := (b - a)
		along.y = 0.0
		if along.length() < 0.5:
			continue
		if kinds[i] == "beam":
			_beam(r, at, along.normalized())
		else:
			_hands(r, at, along.normalized())


## distances to the walls on the left and right of a point (raycasts)
func _walls(at: Vector3, fwd: Vector3) -> Array:
	var space := get_world_3d().direct_space_state
	var side := Vector3(-fwd.z, 0, fwd.x)
	var out := []
	for s in [side, -side]:
		var hit := space.intersect_ray(PhysicsRayQueryParameters3D.create(at, at + s * 12.0, 1))
		out.append(at.distance_to(hit.position) if not hit.is_empty() else 4.0)
	return out


func _beam(r: RoomBase, at: Vector3, fwd: Vector3) -> void:
	var w: Array = _walls(at, fwd)
	var side := Vector3(-fwd.z, 0, fwd.x)
	var floor_y := at.y - 1.6
	var center: Vector3 = at + side * (float(w[0]) - float(w[1])) / 2.0
	var width: float = float(w[0]) + float(w[1])
	var body := StaticBody3D.new()
	body.collision_layer = 1
	r.add_child(body)
	body.global_position = Vector3(center.x, floor_y + 1.42, center.z)
	body.look_at(body.global_position + fwd, Vector3.UP)
	var cs := CollisionShape3D.new()
	var sh := BoxShape3D.new()
	sh.size = Vector3(width, 0.34, 0.3)
	cs.shape = sh
	body.add_child(cs)
	var mesh := MeshInstance3D.new()
	var bm := BoxMesh.new()
	bm.size = sh.size
	mesh.mesh = bm
	mesh.material_override = Mats.get_mat("wood_dark")
	mesh.rotation.z = randf_range(-0.05, 0.05)
	body.add_child(mesh)
	_obstacles.append(body)


func _hands(r: RoomBase, at: Vector3, fwd: Vector3) -> void:
	var w: Array = _walls(at, fwd)
	var side := Vector3(-fwd.z, 0, fwd.x)
	var which := 0 if randf() < 0.5 else 1
	var dist: float = w[which]
	var dir := side if which == 0 else -side
	var floor_y := at.y - 1.6
	var depth := maxf(0.8, (float(w[0]) + float(w[1])) * 0.5 - 0.4)  # leaves the other half free
	var root := Area3D.new()
	root.collision_layer = 0
	root.collision_mask = 2  # the player
	r.add_child(root)
	var wall_pt := at + dir * dist
	root.global_position = Vector3(wall_pt.x, floor_y, wall_pt.z) - dir * depth / 2.0
	root.look_at(root.global_position + fwd, Vector3.UP)
	var cs := CollisionShape3D.new()
	var sh := BoxShape3D.new()
	sh.size = Vector3(depth, 1.6, 2.6)
	cs.shape = sh
	cs.position.y = 0.8
	root.add_child(cs)
	for i in 9:
		var arm := MeshInstance3D.new()
		var bm := BoxMesh.new()
		bm.size = Vector3(0.07, randf_range(0.6, 1.2), 0.07)
		arm.mesh = bm
		arm.material_override = Mats.get_mat("dark")
		arm.position = Vector3(randf_range(-depth / 2, depth / 2), bm.size.y / 2, randf_range(-1.2, 1.2))
		arm.rotation = Vector3(randf_range(-0.5, 0.5), randf(), randf_range(-0.5, 0.5))
		root.add_child(arm)
		var tw := arm.create_tween().set_loops()
		tw.tween_property(arm, "rotation:x", arm.rotation.x + 0.3, randf_range(0.4, 0.8))
		tw.tween_property(arm, "rotation:x", arm.rotation.x - 0.3, randf_range(0.4, 0.8))
	root.body_entered.connect(func(b):
		if b == player and not player.dead:
			Game.death_details["seek"] = "hands"
			player.take_damage(25, "seek"))
	_obstacles.append(root)


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
