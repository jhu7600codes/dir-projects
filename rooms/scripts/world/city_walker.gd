class_name CityWalker
extends Worker
## someone walking up and down a sidewalk in the great city (only after the wires)

var lane_x := 9.0
var z_min := 4.0
var z_max := 160.0
var _dir := 1.0
var _speed := 1.3
var _t := 0.0


func _init() -> void:
	super._init()
	seated = false


func _ready() -> void:
	super._ready()
	_dir = 1.0 if randf() < 0.5 else -1.0
	_speed = randf_range(1.0, 1.6)
	rotation.y = 0.0 if _dir < 0 else PI


func _process(delta: float) -> void:
	if dead:
		return
	position.z += _dir * _speed * delta
	if position.z > z_max or position.z < z_min:
		_dir = -_dir
		rotation.y = 0.0 if _dir < 0 else PI
	# a little bob while walking
	_t += delta * _speed * 5.0
	position.y = absf(sin(_t)) * 0.05
