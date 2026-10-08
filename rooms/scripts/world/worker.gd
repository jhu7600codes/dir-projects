class_name Worker
extends Node3D
## an office worker in the powered office (after the wires). blocky like a roblox character.
## seated ones type away at their desk. stare at one for too long and they yell, snap back
## to their spot and come through the rooms after you (see overreact.gd). hide.
## local space: faces -z (toward the desk when seated).

const SKINS := [Color(0.87, 0.69, 0.55), Color(0.62, 0.45, 0.32), Color(0.95, 0.8, 0.68), Color(0.42, 0.3, 0.22)]
const SHIRTS := [Color(0.92, 0.92, 0.9), Color(0.55, 0.68, 0.85), Color(0.3, 0.32, 0.38), Color(0.7, 0.3, 0.3), Color(0.35, 0.55, 0.4), Color(0.85, 0.8, 0.65), Color(0.62, 0.55, 0.75)]
const PANTS := [Color(0.15, 0.16, 0.2), Color(0.1, 0.1, 0.11), Color(0.55, 0.48, 0.36), Color(0.35, 0.36, 0.38), Color(0.18, 0.22, 0.35)]
const TIES := [Color(0.6, 0.1, 0.12), Color(0.12, 0.2, 0.45), Color(0.15, 0.15, 0.15), Color(0.4, 0.3, 0.1)]
const HAIR := [Color(0.08, 0.06, 0.05), Color(0.25, 0.15, 0.08), Color(0.55, 0.38, 0.2), Color(0.85, 0.7, 0.4), Color(0.5, 0.5, 0.5)]
const FACES := 6

var seated := true
var head_texture: Texture2D = null  # entity workers get an entity face for a head
var _arms: Array[Node3D] = []
var dead := false
var face_key := "a60_face_1"  # what their face looks like when they come after you
var look_time := 1.5          # how long you can stare before they snap
var look_range := 8.0
var _home := Transform3D.IDENTITY
var _looked := 0.0
var _check_t := 0.0
var _calm_t := 0.0
static var _mats := {}


func _init() -> void:
	set_meta("no_merge", true)


func _ready() -> void:
	_home = transform
	var skin: Color = SKINS[randi() % SKINS.size()]
	var shirt: Color = SHIRTS[randi() % SHIRTS.size()]
	var pants: Color = PANTS[randi() % PANTS.size()]
	var shoes := Color(0.08, 0.06, 0.05) if randf() < 0.7 else Color(0.35, 0.22, 0.12)
	var hip_y := 0.68 if seated else 0.95  # seated: on top of the chair seat (Props.SEAT_H)
	# legs + shoes
	for s in [-1.0, 1.0]:
		if seated:
			_part(Vector3(0.19, 0.19, 0.48), Vector3(s * 0.11, hip_y, -0.2), pants)
			_part(Vector3(0.19, hip_y - 0.08, 0.19), Vector3(s * 0.11, (hip_y - 0.08) / 2.0 + 0.08, -0.42), pants)
			_part(Vector3(0.2, 0.09, 0.28), Vector3(s * 0.11, 0.045, -0.47), shoes)
		else:
			_part(Vector3(0.19, 0.87, 0.19), Vector3(s * 0.11, 0.515, 0), pants)
			_part(Vector3(0.2, 0.09, 0.28), Vector3(s * 0.11, 0.045, -0.04), shoes)
	# torso, belt, collar, maybe a tie, an id badge on a lanyard
	_part(Vector3(0.44, 0.6, 0.24), Vector3(0, hip_y + 0.3, 0), shirt)
	_part(Vector3(0.45, 0.06, 0.25), Vector3(0, hip_y + 0.03, 0), Color(0.1, 0.08, 0.07))
	_part(Vector3(0.2, 0.05, 0.02), Vector3(0, hip_y + 0.57, -0.122), shirt.lightened(0.25))
	if randf() < 0.45:
		_part(Vector3(0.07, 0.36, 0.02), Vector3(0, hip_y + 0.37, -0.125), TIES[randi() % TIES.size()])
	elif randf() < 0.6:
		_part(Vector3(0.02, 0.22, 0.01), Vector3(-0.06, hip_y + 0.45, -0.123), Color(0.15, 0.3, 0.6))
		_part(Vector3(0.09, 0.12, 0.01), Vector3(-0.06, hip_y + 0.3, -0.125), Color(0.95, 0.95, 0.95))
	# neck
	_part(Vector3(0.14, 0.08, 0.14), Vector3(0, hip_y + 0.63, 0), skin)
	# arms (typing pose when seated: reaching forward)
	for s in [-1.0, 1.0]:
		var pivot := Node3D.new()
		pivot.position = Vector3(s * 0.31, hip_y + 0.55, 0)
		add_child(pivot)
		var arm := _part(Vector3(0.17, 0.55, 0.17), Vector3(0, -0.27, 0), shirt, pivot)
		_part(Vector3(0.15, 0.14, 0.15), Vector3(0, -0.61, 0), skin, pivot)
		if seated:
			pivot.rotation.x = 1.2
			_arms.append(pivot)
		else:
			pivot.rotation.x = randf_range(-0.05, 0.05)
		arm.name = "arm"
	# head
	if head_texture:
		var s3 := Sprite3D.new()
		s3.texture = head_texture
		s3.pixel_size = 0.55 / maxf(1.0, head_texture.get_height())
		s3.shaded = false
		s3.transparent = true
		s3.billboard = BaseMaterial3D.BILLBOARD_FIXED_Y
		s3.position = Vector3(0, hip_y + 0.85, 0)
		add_child(s3)
	else:
		_head(Vector3(0, hip_y + 0.83, 0), skin)
	if seated:
		var tw := create_tween().set_loops()
		var speed := randf_range(0.09, 0.16)
		for i in 2:
			tw.tween_callback(func():
				if _arms.size() == 2 and not dead:
					_arms[0].rotation.x = 1.2 + randf_range(-0.12, 0.12)
					_arms[1].rotation.x = 1.2 + randf_range(-0.12, 0.12))
			tw.tween_interval(speed)


## a roblox style head: a slightly rounded block, hair on top, a drawn face on the front
func _head(c: Vector3, skin: Color) -> void:
	var mi := MeshInstance3D.new()
	var cap := CapsuleMesh.new()
	cap.radius = 0.17
	cap.height = 0.36
	cap.radial_segments = 12
	cap.rings = 4
	mi.mesh = cap
	mi.scale = Vector3(1.05, 0.95, 1.0)
	mi.material_override = _mat(skin)
	mi.position = c
	add_child(mi)
	var hair: Color = HAIR[randi() % HAIR.size()]
	var style := randi() % 4
	if style != 3:  # 3: bald
		_part(Vector3(0.37, 0.09, 0.36), c + Vector3(0, 0.15, 0.01), hair)
		_part(Vector3(0.37, 0.2, 0.08), c + Vector3(0, 0.06, 0.16), hair)
		if style == 1:  # long
			_part(Vector3(0.38, 0.38, 0.1), c + Vector3(0, -0.06, 0.15), hair)
			for sx in [-1.0, 1.0]:
				_part(Vector3(0.05, 0.3, 0.2), c + Vector3(sx * 0.18, -0.02, 0.05), hair)
		elif style == 2:  # fringe
			_part(Vector3(0.3, 0.06, 0.05), c + Vector3(0.02, 0.12, -0.16), hair)
	var face := MeshInstance3D.new()
	var q := QuadMesh.new()
	q.size = Vector2(0.3, 0.3)
	face.mesh = q
	face.material_override = _face_mat(randi() % FACES)
	face.position = c + Vector3(0, -0.01, -0.172)
	face.rotation.y = PI
	add_child(face)


static var _faces := {}


static func _face_mat(k: int) -> StandardMaterial3D:
	if _faces.has(k):
		return _faces[k]
	var img := Image.create(64, 64, false, Image.FORMAT_RGBA8)
	img.fill(Color(0, 0, 0, 0))
	var ink := Color(0.08, 0.06, 0.06)
	# eyes: dark ovals with a little white shine
	for ex in [22, 42]:
		for y in range(22, 34):
			for x in range(ex - 4, ex + 5):
				var d := pow((x - ex) / 4.0, 2) + pow((y - 28) / 6.0, 2)
				if d <= 1.0:
					img.set_pixel(x, y, ink)
		img.set_pixel(ex + 1, 25, Color.WHITE)
		img.set_pixel(ex + 2, 25, Color.WHITE)
		img.set_pixel(ex + 1, 26, Color.WHITE)
		# eyebrows
		var lift := 1 if k == 4 else 0
		for x in range(ex - 5, ex + 6):
			img.set_pixel(x, 17 - lift + (1 if absi(x - ex) > 3 else 0), ink)
			if k == 2:
				img.set_pixel(x, 16 - lift, ink)
	# mouth
	match k % 3:
		0:  # smile
			for x in range(22, 43):
				var y := 44 + int(round(5.0 * (1.0 - pow((x - 32) / 10.0, 2))))
				img.set_pixel(x, y, ink)
				img.set_pixel(x, y + 1, ink)
		1:  # small smile
			for x in range(26, 39):
				var y := 46 + int(round(2.5 * (1.0 - pow((x - 32) / 6.0, 2))))
				img.set_pixel(x, y, ink)
				img.set_pixel(x, y + 1, ink)
		2:  # neutral
			for x in range(26, 39):
				img.set_pixel(x, 47, ink)
				img.set_pixel(x, 48, ink)
	if k == 3 or k == 5:  # glasses
		for ex in [22, 42]:
			for x in range(ex - 8, ex + 9):
				img.set_pixel(x, 20, ink)
				img.set_pixel(x, 36, ink)
			for y in range(20, 37):
				img.set_pixel(ex - 8, y, ink)
				img.set_pixel(ex + 8, y, ink)
		for x in range(30, 35):
			img.set_pixel(x, 26, ink)
	if k == 5:  # freckles / blush
		for p in [Vector2i(14, 38), Vector2i(17, 40), Vector2i(47, 38), Vector2i(50, 40)]:
			img.set_pixelv(p, Color(0.75, 0.35, 0.3))
	var m := StandardMaterial3D.new()
	m.albedo_texture = ImageTexture.create_from_image(img)
	m.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA_SCISSOR
	m.texture_filter = BaseMaterial3D.TEXTURE_FILTER_NEAREST
	m.roughness = 0.9
	_faces[k] = m
	return m


static func _mat(c: Color) -> StandardMaterial3D:
	var key := c.to_html()
	if not _mats.has(key):
		var m := StandardMaterial3D.new()
		m.albedo_color = c
		m.roughness = 0.8
		_mats[key] = m
	return _mats[key]


func _part(size: Vector3, pos: Vector3, c: Color, parent: Node3D = null) -> MeshInstance3D:
	var mi := MeshInstance3D.new()
	var bm := BoxMesh.new()
	bm.size = size
	mi.mesh = bm
	var key := c.to_html()
	if not _mats.has(key):
		var m := StandardMaterial3D.new()
		m.albedo_color = c
		m.roughness = 0.8
		_mats[key] = m
	mi.material_override = _mats[key]
	mi.position = pos
	(parent if parent else self).add_child(mi)
	return mi


func _physics_process(delta: float) -> void:
	if dead or Game.player == null or Game.player.dead or Game.player.hidden:
		return
	_calm_t -= delta
	_check_t -= delta
	if _check_t > 0.0:
		return
	_check_t = 0.1
	if _calm_t <= 0.0 and _seen():
		_looked += 0.1
		if _looked >= look_time:
			overreact()
	else:
		_looked = maxf(0.0, _looked - 0.1)


func _seen() -> bool:
	var cam: Camera3D = Game.player.camera
	var head := global_position + Vector3(0, 1.5 if seated else 1.75, 0)
	var to := head - cam.global_position
	if to.length() > look_range:
		return false
	if (-cam.global_transform.basis.z).dot(to.normalized()) < 0.94:
		return false
	var q := PhysicsRayQueryParameters3D.create(cam.global_position, head, 1)
	return get_world_3d().direct_space_state.intersect_ray(q).is_empty()


## "IM AN INTROVERT". back to their spot in a blink, and then they come for you
func overreact() -> void:
	_looked = 0.0
	_calm_t = 12.0
	var yell := Label3D.new()
	yell.text = "IM AN INTROVERT"
	yell.font_size = 72
	yell.pixel_size = 0.006
	yell.modulate = Color(1.0, 0.3, 0.25)
	yell.outline_size = 8
	yell.billboard = BaseMaterial3D.BILLBOARD_ENABLED
	yell.no_depth_test = true
	yell.position = global_position + Vector3(0, 2.3, 0)
	get_parent().add_child(yell)
	var tw := yell.create_tween()
	tw.tween_property(yell, "position:y", yell.position.y + 0.6, 1.6)
	tw.parallel().tween_property(yell, "modulate:a", 0.0, 1.6).set_delay(0.6)
	tw.tween_callback(yell.queue_free)
	var a := AudioStreamPlayer3D.new()
	a.stream = Assets.sound("introvert")
	a.bus = "SFX"
	a.position = yell.position
	get_parent().add_child(a)
	a.play()
	a.finished.connect(a.queue_free)
	transform = _home
	on_overreact()
	var em = Game.entities
	if em and not em.active.has("worker"):
		var fk := face_key
		get_tree().create_timer(0.5).timeout.connect(func():
			if Game.entities and Game.player and not Game.player.dead:
				Game.entities.spawn("worker", {"face_key": fk}))


## subclasses reset their walking here
func on_overreact() -> void:
	pass


## an entity worker went off: everyone in the room drops
func fall_over() -> void:
	if dead:
		return
	dead = true
	var tw := create_tween()
	tw.tween_property(self, "rotation:x", -PI / 2 if randf() < 0.5 else PI / 2, 0.4).set_trans(Tween.TRANS_BOUNCE).set_ease(Tween.EASE_OUT)
