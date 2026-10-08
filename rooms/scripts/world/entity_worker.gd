class_name EntityWorker
extends Worker
## in the powered office the entities come back... as coworkers. they walk around the room
## with their face for a head. they're a lot touchier than the others: half a second of
## staring and they overreact (see Worker.overreact).

const NAMES := {"a60": "a60_face_1", "a120": "a120_face", "a90": "a90_face", "a200": "a200_face", "a60b": "a60b_face"}

var entity_id := "a60"
var room: RoomBase
var _target := Vector3.ZERO


func _init() -> void:
	super._init()
	seated = false
	look_time = 0.5
	look_range = 14.0


func _ready() -> void:
	face_key = NAMES.get(entity_id, "a60_face_1")
	head_texture = Assets.glow_texture(face_key)
	super._ready()
	_pick_target()


func _pick_target() -> void:
	if room == null:
		return
	var pts := room.path_global()
	var p: Vector3 = pts[randi() % pts.size()]
	_target = Vector3(p.x, p.y - 1.6, p.z)


func on_overreact() -> void:
	_pick_target()


func _process(delta: float) -> void:
	if dead or Game.player == null or Game.player.dead:
		return
	var to := _target - global_position
	to.y = 0.0
	if to.length() < 0.3:
		_pick_target()
	else:
		global_position += to.normalized() * minf(1.1 * delta, to.length())
		look_at(global_position + to.normalized(), Vector3.UP)
