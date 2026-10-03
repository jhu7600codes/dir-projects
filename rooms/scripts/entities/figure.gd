class_name Figure
extends Entity
## figure (from doors, "got any more doors?" modifier): it's blind and walks around the
## room you just entered, listening. sprinting near it, or walking close to it, and it hears
## you and charges. crouch-walk past it, or hide in a locker until you can sneak out.
## the door out has a code lock: find the paper with the code somewhere in the room.
## it stays in its room.

const RULES := {
	"trigger": "door", "min_door": 20, "chance": 0.07, "cooldown": 90.0,
	"group": "figure", "blocked_by": ["seek", "a60b"], "needs_mod": "more_doors",
}
const WALK_SPEED := 1.6
const CHASE_SPEED := 6.2
const HEAR_SPRINT := 14.0
const HEAR_WALK := 5.0
const HEAR_CROUCH := 1.6

var _room_n := 0
var _target := Vector3.ZERO
var _chasing := false
var _step_t := 0.0
var sprite: Sprite3D
var _steps: AudioStreamPlayer3D
var code := ""
var note := ""  # filled in once you've read the paper
var _paper: Node3D


func begin() -> void:
	id = "figure"
	_room_n = Game.door
	var r: RoomBase = generator.room(_room_n)
	if r == null:
		finish()
		return
	var pts := r.path_global()
	# start away from the entrance so you get a moment to see it
	global_position = _floor(pts[pts.size() - 1])
	_pick_target()
	sprite = make_sprite("figure_body", 2.7)
	set_sprite_texture(sprite, Assets.texture("figure_body"))  # it's a cut-out render, no glow edges
	(sprite.material_override as StandardMaterial3D).billboard_mode = BaseMaterial3D.BILLBOARD_FIXED_Y
	sprite.position.y = 1.35
	_setup_puzzle(r)
	_steps = make_3d_audio("figure_step", 6.0, 40.0)
	var g := make_3d_audio("figure_growl", 8.0, 50.0)
	g.play()


## lock the way out and hide the paper with the code somewhere in the room
func _setup_puzzle(r: RoomBase) -> void:
	code = "%04d" % randi_range(0, 9999)
	if r.exit_door == null:
		return
	r.exit_door.code_lock(_open_keypad)
	var pts := r.path_global()
	var space := get_world_3d().direct_space_state
	var spot := _floor(pts[0])
	# somewhere along a wall, away from the entrance, not right at the door
	for attempt in 12:
		var i := randi_range(1, maxi(1, pts.size() - 2))
		var a: Vector3 = pts[i - 1]
		var b: Vector3 = pts[i]
		var along := b - a
		if along.length() < 0.2:
			continue
		var side := Vector3(-along.z, 0, along.x).normalized() * (1.0 if randf() < 0.5 else -1.0)
		var from := a.lerp(b, randf_range(0.2, 0.8)) + Vector3(0, -1.2, 0)
		var hit := space.intersect_ray(PhysicsRayQueryParameters3D.create(from, from + side * 10.0, 1))
		if hit.is_empty():
			continue
		var d: float = from.distance_to(hit.position)
		if d < 1.0:
			continue
		spot = _floor(from + Vector3(0, 1.2, 0)) + side * (d - 0.55)
		break
	_paper = Node3D.new()
	r.add_child(_paper)
	_paper.global_position = spot + Vector3(0, 0.01, 0)
	_paper.rotation.y = randf() * TAU
	var sheet := MeshInstance3D.new()
	var bm := BoxMesh.new()
	bm.size = Vector3(0.24, 0.004, 0.32)
	sheet.mesh = bm
	sheet.material_override = Mats.get_mat("plastic")
	_paper.add_child(sheet)
	var ink := Label3D.new()
	ink.text = code
	ink.font_size = 40
	ink.pixel_size = 0.002
	ink.modulate = Color(0.15, 0.15, 0.3)
	ink.outline_size = 0
	ink.rotation.x = -PI / 2
	ink.position.y = 0.004
	_paper.add_child(ink)
	var glow := OmniLight3D.new()
	glow.light_energy = 0.4
	glow.omni_range = 1.2
	glow.position.y = 0.3
	_paper.add_child(glow)
	var it := Interactable.make(Vector3(0.5, 0.4, 0.5), "read the paper")
	it.position.y = 0.15
	it.used.connect(func(_p):
		note = code
		player.notify("the paper says " + " ".join(code.split("")))
		Game.play_ui("pickup"))
	_paper.add_child(it)


func _open_keypad() -> void:
	var k := Keypad.new()
	k.code = code
	k.note = note
	k.solved.connect(func():
		var r: RoomBase = generator.room(_room_n)
		if r and r.exit_door:
			r.exit_door.unlock()
			r.exit_door.open())
	get_parent().add_child(k)


func _exit_tree() -> void:
	# never leave you locked in (despawned by the admin panel, a-90 cleanup...)
	var r: RoomBase = generator.room(_room_n) if generator else null
	if r and r.exit_door and r.exit_door.locked:
		r.exit_door.unlock()


func _floor(p: Vector3) -> Vector3:
	return Vector3(p.x, p.y - 1.6, p.z)


func _pick_target() -> void:
	var r: RoomBase = generator.room(_room_n)
	if r == null:
		return
	var pts := r.path_global()
	_target = _floor(pts[randi() % pts.size()])


func _process(delta: float) -> void:
	if _done or is_frozen() or player == null or player.dead:
		return
	if Game.door > _room_n or generator.room(_room_n) == null:
		finish()  # you got through its door
		return
	var me := global_position
	var p: Vector3 = player.global_position
	var flat_d := Vector2(me.x - p.x, me.z - p.z).length()
	if not _chasing and not player.hidden and _hears(flat_d):
		_chasing = true
		var g := make_3d_audio("figure_growl", 10.0, 60.0)
		g.pitch_scale = 0.8
		g.play()
	if _chasing and player.hidden:
		_chasing = false  # lost you
		_pick_target()
	var goal := Vector3(p.x, me.y, p.z) if _chasing else _target
	var speed := CHASE_SPEED if _chasing else WALK_SPEED
	var to := goal - me
	to.y = 0.0
	if to.length() < 0.3:
		_pick_target()
	else:
		global_position += to.normalized() * minf(speed * delta, to.length())
	_step_t -= delta
	if _step_t <= 0.0:
		_step_t = 0.35 if _chasing else 0.8
		_steps.play()
	if _chasing and flat_d < 1.2:
		_kill()


func _hears(d: float) -> bool:
	if not player.is_walking():
		return false
	if player.sprinting:
		return d < HEAR_SPRINT
	if player.crouching:
		return d < HEAR_CROUCH
	return d < HEAR_WALK


func _kill() -> void:
	hit_player = true
	Game.death_details["figure"] = "sprinting" if player.sprinting else "heard"
	var a := AudioStreamPlayer.new()
	a.stream = Assets.sound("figure_kill")
	a.bus = "Alert"
	get_parent().add_child(a)
	a.play()
	player.take_damage(999, "figure")
	finish()
