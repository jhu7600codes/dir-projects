class_name RawWire
extends Node3D
## w-15 "sorry, raw wire": a live wire hanging from the ceiling in the wires. it doesn't
## move. walk into it and it shocks you for 15. it hangs too low to duck under.

const DAMAGE := 15.0
const COOLDOWN := 0.9

var height := 3.0  # ceiling height above this node
var _area: Area3D
var _spark: OmniLight3D
var _t := 0.0
var _audio: AudioStreamPlayer3D


func _ready() -> void:
	var bottom := randf_range(0.85, 1.25)
	var len := height - bottom
	var w := MeshInstance3D.new()
	var bm := BoxMesh.new()
	bm.size = Vector3(0.035, len, 0.035)
	w.mesh = bm
	w.material_override = Mats.get_mat("wire")
	w.position.y = bottom + len / 2.0
	w.rotation.z = randf_range(-0.06, 0.06)
	add_child(w)
	var tip := MeshInstance3D.new()
	var tm := BoxMesh.new()
	tm.size = Vector3(0.05, 0.12, 0.05)
	tip.mesh = tm
	tip.material_override = Mats.get_mat("copper")
	tip.position.y = bottom
	add_child(tip)
	_spark = OmniLight3D.new()
	_spark.light_color = Color(0.6, 0.8, 1.0)
	_spark.omni_range = 2.5
	_spark.light_energy = 0.0
	_spark.position.y = bottom
	add_child(_spark)
	_audio = AudioStreamPlayer3D.new()
	_audio.stream = Assets.sound("w15_zap")
	_audio.bus = "SFX"
	_audio.unit_size = 3.0
	_audio.position.y = bottom
	add_child(_audio)
	_area = Area3D.new()
	_area.collision_layer = 0
	_area.collision_mask = 2  # the player
	var cs := CollisionShape3D.new()
	var sh := CylinderShape3D.new()
	sh.radius = 0.32
	sh.height = height
	cs.shape = sh
	cs.position.y = height / 2.0
	_area.add_child(cs)
	add_child(_area)


func _process(delta: float) -> void:
	_t -= delta
	# a little crackle now and then so you can spot it
	if randf() < delta * 0.6:
		_flash(0.6)
	_spark.light_energy = maxf(0.0, _spark.light_energy - delta * 6.0)
	if _t > 0.0:
		return
	for b in _area.get_overlapping_bodies():
		if b is Player and not b.dead:
			_t = COOLDOWN
			_flash(3.0)
			_audio.play()
			Game.death_details["w15"] = "touched"
			b.take_damage(DAMAGE, "w15")


func _flash(e: float) -> void:
	_spark.light_energy = maxf(_spark.light_energy, e)
