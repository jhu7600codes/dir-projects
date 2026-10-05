class_name EntityWorker
extends Worker
## in the powered office the entities come back... as coworkers. they walk around the room
## with their face for a head. don't look at them. if you do (for about half a second),
## they snap, and everyone in the room dies. you included.

const LOOK_TIME := 0.5
const NAMES := {"a60": "a60_face_1", "a120": "a120_face", "a90": "a90_face", "a200": "a200_face", "a60b": "a60b_face"}

var entity_id := "a60"
var room: RoomBase
var _target := Vector3.ZERO
var _looked := 0.0
var _triggered := false


func _init() -> void:
	super._init()
	seated = false


func _ready() -> void:
	head_texture = Assets.glow_texture(NAMES.get(entity_id, "a60_face_1"))
	super._ready()
	_pick_target()


func _pick_target() -> void:
	if room == null:
		return
	var pts := room.path_global()
	var p: Vector3 = pts[randi() % pts.size()]
	_target = Vector3(p.x, p.y - 1.6, p.z)


func _process(delta: float) -> void:
	if dead or _triggered or Game.player == null or Game.player.dead:
		return
	# wander
	var to := _target - global_position
	to.y = 0.0
	if to.length() < 0.3:
		_pick_target()
	else:
		global_position += to.normalized() * minf(1.1 * delta, to.length())
		look_at(global_position + to.normalized(), Vector3.UP)
	# being looked at
	if _seen():
		_looked += delta
		if _looked >= LOOK_TIME:
			_snap()
	else:
		_looked = maxf(0.0, _looked - delta)


func _seen() -> bool:
	var cam: Camera3D = Game.player.camera
	var head := global_position + Vector3(0, 1.8, 0)
	var to := head - cam.global_position
	if to.length() > 14.0:
		return false
	if (-cam.global_transform.basis.z).dot(to.normalized()) < 0.94:
		return false
	var q := PhysicsRayQueryParameters3D.create(cam.global_position, head, 1)
	return get_world_3d().direct_space_state.intersect_ray(q).is_empty()


func _snap() -> void:
	_triggered = true
	Game.play_ui("hurt")
	var a := AudioStreamPlayer.new()
	a.stream = Assets.sound("a60_near" if entity_id == "a60" else "w10_lunge")
	a.bus = "Alert"
	get_parent().add_child(a)
	a.play()
	if room:
		for w in room.find_children("*", "Worker", true, false):
			w.fall_over()
	Game.death_details["worker"] = entity_id
	Game.player.take_damage(999, "worker")
