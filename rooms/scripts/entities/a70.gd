class_name A70
extends Entity
## a-70, the locker mimic (from rooms: revisited): it looks just like a locker, except for
## a faint face on the door. try to hide in it and it bursts out and chases you, hurting you
## on contact. run. it gives up after a while, or when you're two doors away.

const RULES := {"trigger": "manual", "floor": ["offices"], "group": "a70"}
const SPEED := 5.2       # slower than sprinting, faster than walking
const DAMAGE := 30.0
const LIFE := 14.0

var start := Vector3.ZERO
var _room_n := 0
var _hit_cd := 0.0
var _t := 0.0
var _body: Node3D
var _door: Node3D


func begin() -> void:
	id = "a70"
	_room_n = Game.door
	global_position = start
	_build_body()
	var l := OmniLight3D.new()
	l.light_color = Color(0.6, 0.6, 0.65)
	l.light_energy = 1.5
	l.omni_range = 5.0
	add_child(l)
	make_3d_audio("a70_scream", 12.0, 60.0).play()
	Game.play_ui("hurt")


func _physics_process(delta: float) -> void:
	if _done or is_frozen() or player == null or player.dead:
		return
	_t += delta
	if _t > LIFE or Game.door >= _room_n + 2:
		finish()
		return
	var target: Vector3 = player.global_position
	var to := target - global_position
	to.y = 0.0
	if to.length() > 0.05:
		global_position += to.normalized() * minf(SPEED * delta, to.length())
		look_at(global_position - to.normalized(), Vector3.UP)
	# it hops along, the door flapping like a jaw
	_body.position.y = absf(sin(_t * 9.0)) * 0.25
	_body.rotation.z = sin(_t * 9.0) * 0.12
	_door.rotation.y = -0.5 - absf(sin(_t * 7.0)) * 0.9
	_hit_cd -= delta
	if _hit_cd <= 0.0 and Vector2(to.x, to.z).length() < 1.0:
		_hit_cd = 1.2
		hit_player = true
		Game.death_details["a70"] = "caught"
		player.take_damage(DAMAGE, "a70")


## a locker, but the door is a mouth: dark inside, rows of teeth, two white eyes
func _build_body() -> void:
	_body = Node3D.new()
	add_child(_body)
	var grey := Mats.get_mat("locker")
	_part(Vector3(0.9, 2.1, 0.62), Vector3(0, 1.05, -0.04), grey, _body)
	_part(Vector3(0.8, 1.95, 0.02), Vector3(0, 1.05, 0.275), Mats.get_mat("dark"), _body)
	var tooth := Mats.get_mat("fridge_white")
	for i in 6:
		_part(Vector3(0.09, 0.16, 0.05), Vector3(-0.32 + i * 0.13, 1.95, 0.28), tooth, _body, PI / 4)
		_part(Vector3(0.09, 0.16, 0.05), Vector3(-0.32 + i * 0.13, 0.15, 0.28), tooth, _body, PI / 4)
	for x in [-0.18, 0.18]:
		_part(Vector3(0.12, 0.07, 0.02), Vector3(x, 1.45, 0.29), Mats.get_mat("white_glow"), _body)
	_door = Node3D.new()
	_door.position = Vector3(-0.43, 0, 0.3)
	_body.add_child(_door)
	_part(Vector3(0.86, 2.0, 0.04), Vector3(0.43, 1.05, 0), grey, _door)
	for i in 5:
		_part(Vector3(0.5, 0.025, 0.02), Vector3(0.43, 1.55 + i * 0.06, 0.025), Mats.get_mat("dark"), _door)


func _part(size: Vector3, pos: Vector3, mat: Material, parent: Node3D, rz := 0.0) -> void:
	var mi := MeshInstance3D.new()
	var bm := BoxMesh.new()
	bm.size = size
	mi.mesh = bm
	mi.material_override = mat
	mi.position = pos
	mi.rotation.z = rz
	parent.add_child(mi)
