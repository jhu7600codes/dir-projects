class_name Player
extends CharacterBody3D
## first person player: walk, sprint (stamina), crouch, interact, hide in lockers,
## health and damage. lights are in PlayerLights, items in Inventory.
## admin flags (Game.admin_flags) add noclip, god mode, speed, jumping and sliding.

signal died(cause: String)

const WALK := 4.0
const SPRINT := 6.8
const CROUCH := 2.0
const GRAVITY := 18.0
const HEAD_STAND := 1.55
const HEAD_CROUCH := 0.95
const STAMINA_DRAIN := 22.0
const STAMINA_REGEN := 16.0
const LOOK_SPEED := 0.0025
const PAD_LOOK_SPEED := 2.6

var health := 100.0
var stamina := 100.0
var dead := false
var hidden := false
var crouching := false
var sprinting := false
var current_locker: Locker = null
## the hotbar: the item in your hand is used with left click
const ITEMS := ["flashlight", "shakelight", "bandage", "vitamins"]
var selected := "flashlight"
var hud: Node = null  # set by the game scene

var inventory: Inventory
var lights: PlayerLights
var head: Node3D
var camera: Camera3D
var viewmodel: Viewmodel

var _ray: RayCast3D
var _shape: CollisionShape3D
var _capsule: CapsuleShape3D
var _steps: AudioStreamPlayer
var _step_dist := 0.0
var _stamina_wait := 0.0
var _speed_boost := 0.0     # vitamins timer
var _slide_t := 0.0
var _hold_t := 0.0          # holding interact on a hold_time target
var _busy := false          # locker animation
var _target: Interactable = null
# camera feel (head bob, strafe tilt, sprint fov, landing dip), doors style
var _bob_t := 0.0
var _turn_roll := 0.0
var _land_dip := 0.0
var _was_on_floor := true
var _time := 0.0


func _ready() -> void:
	collision_layer = 2
	collision_mask = 1
	floor_snap_length = 0.3
	_capsule = CapsuleShape3D.new()
	_capsule.radius = 0.33
	_capsule.height = 1.75
	_shape = CollisionShape3D.new()
	_shape.shape = _capsule
	_shape.position.y = 0.875
	add_child(_shape)
	head = Node3D.new()
	head.position.y = HEAD_STAND
	add_child(head)
	camera = Camera3D.new()
	camera.fov = float(Settings.data.fov)
	camera.near = 0.05
	camera.current = true
	head.add_child(camera)
	inventory = Inventory.new()
	add_child(inventory)
	lights = PlayerLights.new()
	lights.inventory = inventory
	camera.add_child(lights)
	viewmodel = Viewmodel.new()
	viewmodel.player = self
	camera.add_child(viewmodel)
	_ray = RayCast3D.new()
	_ray.target_position = Vector3(0, 0, -2.6)
	_ray.collision_mask = 1 | 4
	_ray.collide_with_areas = true
	_ray.add_exception(self)
	camera.add_child(_ray)
	_steps = AudioStreamPlayer.new()
	_steps.bus = "SFX"
	_steps.volume_db = -8.0
	add_child(_steps)


# ---- input helpers (a-90 / a-90b read these too) ---------------------------

func move_input() -> Vector2:
	var v := Input.get_vector("move_left", "move_right", "move_forward", "move_back")
	v += Game.touch_move
	return v.limit_length(1.0)


func look_input() -> Vector2:
	return Input.get_vector("look_left", "look_right", "look_up", "look_down")


func is_walking() -> bool:
	return not hidden and Vector2(velocity.x, velocity.z).length() > 0.8


func head_position() -> Vector3:
	return camera.global_position


func noclip_on() -> bool:
	return Game.admin and Game.admin_flags.noclip


func notify(text: String) -> void:
	if hud:
		hud.notify(text)


# ---- looking ----------------------------------------------------------------

func _unhandled_input(event: InputEvent) -> void:
	if dead:
		return
	# taps on a phone also send a fake left click, those must not use items
	if event is InputEventMouseButton and event.device == InputEvent.DEVICE_ID_EMULATION:
		return
	if event is InputEventMouseMotion and Input.mouse_mode == Input.MOUSE_MODE_CAPTURED:
		_look(event.relative * LOOK_SPEED)
	elif event.is_action_pressed("interact"):
		_on_interact()
	elif event.is_action_pressed("use_item"):
		if Input.mouse_mode != Input.MOUSE_MODE_CAPTURED and not Settings.use_touch() and event is InputEventMouseButton:
			Input.mouse_mode = Input.MOUSE_MODE_CAPTURED  # first click just grabs the mouse again
		else:
			use_item()
	elif event.is_action_pressed("item_next"):
		cycle_item(1)
	elif event.is_action_pressed("item_prev"):
		cycle_item(-1)
	else:
		for i in ITEMS.size():
			if event.is_action_pressed("slot_%d" % (i + 1)):
				select_item(ITEMS[i])


func _look(rel: Vector2) -> void:
	var s := float(Settings.data.sensitivity)
	if hidden:
		# a little peeking inside the locker, no turning around
		head.rotation.y = clampf(head.rotation.y - rel.x * s, -0.5, 0.5)
	else:
		rotate_y(-rel.x * s)
		_turn_roll = clampf(_turn_roll - rel.x * s * 0.08, -0.03, 0.03)
		viewmodel.add_sway(rel / LOOK_SPEED)
	head.rotation.x = clampf(head.rotation.x - rel.y * s, -1.45, 1.45)


func _process(delta: float) -> void:
	if dead:
		return
	var pad := look_input()
	if pad.length() > 0.0:
		_look(pad * PAD_LOOK_SPEED * delta)
	if Game.touch_look != Vector2.ZERO:
		_look(Game.touch_look * LOOK_SPEED * 1.3)
		Game.touch_look = Vector2.ZERO
	_update_target(delta)
	_update_camera(delta)


## the small things that make the camera feel alive: a step bob that follows your
## footsteps, a slight lean when strafing or turning, wider fov when sprinting,
## a dip when landing and a little breathing sway when standing still
func _update_camera(delta: float) -> void:
	_time += delta
	var speed := Vector2(velocity.x, velocity.z).length()
	var on_floor := is_on_floor()
	var walking := speed > 0.5 and on_floor and not hidden and not _busy
	var target := Vector3.ZERO
	if walking:
		_bob_t += speed * delta * PI / (2.2 if sprinting else 1.6)
		var amp := clampf(speed / WALK, 0.0, 1.6) * (0.5 if crouching else 1.0)
		target = Vector3(sin(_bob_t) * 0.022 * amp, -absf(cos(_bob_t)) * 0.045 * amp, 0)
	else:
		target.y = sin(_time * 1.5) * 0.006
	if on_floor and not _was_on_floor:
		_land_dip = 0.09
	_was_on_floor = on_floor
	_land_dip = move_toward(_land_dip, 0.0, delta * 0.45)
	target.y -= _land_dip
	camera.position = camera.position.lerp(target, minf(1.0, 12.0 * delta))
	var strafe := move_input().x if not hidden else 0.0
	_turn_roll = move_toward(_turn_roll, 0.0, delta * 0.12)
	camera.rotation.z = lerpf(camera.rotation.z, -strafe * 0.022 + _turn_roll, minf(1.0, 7.0 * delta))
	var fov := float(Settings.data.fov) + (7.0 if sprinting and walking else 0.0)
	camera.fov = lerpf(camera.fov, fov, minf(1.0, 6.0 * delta))


# ---- movement ----------------------------------------------------------------

func _physics_process(delta: float) -> void:
	if dead or _busy or hidden:
		return
	if noclip_on():
		_noclip(delta)
		return
	var admin_on := Game.admin
	var inp := move_input()
	var wish := (global_basis * Vector3(inp.x, 0, inp.y))
	wish.y = 0
	wish = wish.normalized() * inp.length()

	# crouch (hold c / ctrl, or the touch toggle)
	var want_crouch := Input.is_action_pressed("crouch")
	if admin_on and Game.admin_flags.slide and want_crouch and not crouching and sprinting and _slide_t <= 0.0:
		_slide_t = 0.7
	crouching = want_crouch
	head.position.y = lerpf(head.position.y, HEAD_CROUCH if crouching else HEAD_STAND, 12.0 * delta)
	_capsule.height = 1.15 if crouching else 1.75
	_shape.position.y = _capsule.height / 2.0

	# sprint + stamina
	var want_sprint := Input.is_action_pressed("sprint") or Game.forced_sprint
	sprinting = want_sprint and inp.length() > 0.1 and not crouching and (stamina > 0.0 or Game.forced_sprint)
	var infinite: bool = admin_on and Game.admin_flags.stamina
	if sprinting and not infinite and not Game.forced_sprint:
		stamina = maxf(0.0, stamina - STAMINA_DRAIN * delta)
		_stamina_wait = 1.0
	elif _stamina_wait > 0.0:
		_stamina_wait -= delta
	else:
		stamina = minf(100.0, stamina + STAMINA_REGEN * delta)

	var speed := WALK
	if crouching:
		speed = CROUCH
	elif sprinting:
		speed = SPRINT
	if _speed_boost > 0.0:
		_speed_boost -= delta
		speed *= 1.45
	if Game.forced_sprint:
		speed = SPRINT * 2.0
	if admin_on and Game.admin_flags.speed:
		speed *= 2.0
	if _slide_t > 0.0:
		_slide_t -= delta
		speed = SPRINT * (1.0 + _slide_t * 1.6)

	velocity.x = wish.x * speed
	velocity.z = wish.z * speed
	if not is_on_floor():
		velocity.y -= GRAVITY * delta
	elif admin_on and Game.admin_flags.jump and Input.is_action_just_pressed("jump"):
		velocity.y = 6.0
	move_and_slide()
	_auto_open_door()

	# footsteps
	if is_on_floor():
		_step_dist += Vector2(velocity.x, velocity.z).length() * delta
		var stride := 2.2 if sprinting else 1.6
		if _step_dist > stride:
			_step_dist = 0.0
			_steps.stream = Assets.sound("step")
			_steps.pitch_scale = randf_range(0.85, 1.15)
			_steps.volume_db = -14.0 if crouching else -8.0
			_steps.play()


## doors open by themselves when you walk up to them (setting "open doors automatically")
func _auto_open_door() -> void:
	if not Settings.data.get("auto_doors", true) or Game.generator == null:
		return
	var r = Game.generator.room(Game.door)
	if r == null or r.exit_door == null or r.exit_door.is_open or r.exit_door.locked:
		return
	var door_pos: Vector3 = r.exit_door.global_position
	if Vector2(global_position.x - door_pos.x, global_position.z - door_pos.z).length() < 1.6:
		r.exit_door.open()


func _noclip(delta: float) -> void:
	var inp := move_input()
	var dir := camera.global_basis * Vector3(inp.x, 0, inp.y)
	if Input.is_action_pressed("jump"):
		dir.y += 1.0
	if Input.is_action_pressed("crouch"):
		dir.y -= 1.0
	global_position += dir * 12.0 * delta
	velocity = Vector3.ZERO


# ---- interaction --------------------------------------------------------------

func _update_target(delta: float) -> void:
	_target = null
	if not hidden and _ray.is_colliding():
		var c := _ray.get_collider()
		if c is Interactable and c.enabled:
			_target = c
	# hold-to-use targets (exit doors)
	if _target and _target.hold_time > 0.0 and Input.is_action_pressed("interact"):
		_hold_t += delta
		if _hold_t >= _target.hold_time:
			_hold_t = 0.0
			_target.interact(self)
	else:
		_hold_t = 0.0
	if hud:
		var text := ""
		if hidden:
			text = "interact - leave locker"
		elif _target:
			text = _target.prompt
		hud.set_prompt(text, (_hold_t / _target.hold_time) if _target and _target.hold_time > 0.0 else -1.0)


func _on_interact() -> void:
	if _busy:
		return
	if hidden:
		exit_locker()
	elif _target and _target.hold_time <= 0.0:
		_target.interact(self)


func press_interact() -> void:
	# touch button
	_on_interact()


func enter_locker(lk: Locker) -> void:
	if hidden or _busy or dead or lk.occupied:
		return
	_busy = true
	hidden = true
	Game.used_locker = true
	current_locker = lk
	lk.occupied = true
	lk.play_door()
	_shape.set_deferred("disabled", true)
	velocity = Vector3.ZERO
	var t := lk.inside_transform()
	var dur := 0.12 if Game.forced_sprint else 0.35
	var tw := create_tween().set_parallel()
	tw.tween_property(self, "global_transform", t, dur)
	tw.tween_property(head, "rotation", Vector3.ZERO, dur)
	tw.chain().tween_callback(func():
		_busy = false
		lk.set_inside(true)
		if hud:
			hud.set_locker_view(true))


func exit_locker() -> void:
	if not hidden or _busy or current_locker == null:
		return
	_busy = true
	current_locker.play_door()
	current_locker.set_inside(false)
	if hud:
		hud.set_locker_view(false)
	var t := current_locker.outside_transform()
	var dur := 0.12 if Game.forced_sprint else 0.3
	var tw := create_tween()
	tw.tween_property(self, "global_transform", t, dur)
	tw.tween_callback(func():
		current_locker.occupied = false
		current_locker = null
		hidden = false
		_busy = false
		# the peek turn inside the locker is on the head; move it onto the body, otherwise
		# walking forward would go sideways from where you're looking
		rotate_y(head.rotation.y)
		head.rotation.y = 0.0
		velocity = Vector3.ZERO
		_shape.set_deferred("disabled", false))


func teleport(t: Transform3D) -> void:
	if hidden and current_locker:
		current_locker.occupied = false
		current_locker.set_inside(false)
		current_locker = null
		hidden = false
		if hud:
			hud.set_locker_view(false)
	_busy = false
	_shape.set_deferred("disabled", false)
	global_transform = t
	head.rotation.y = 0.0
	velocity = Vector3.ZERO


# ---- health -------------------------------------------------------------------

func take_damage(amount: float, cause: String) -> void:
	if dead:
		return
	if Game.admin and Game.admin_flags.god:
		if hud:
			hud.flash(Color(1, 1, 1, 0.3), 0.2)
		return
	health -= amount
	if hud:
		hud.flash(Color(0.8, 0, 0, 0.6), 0.5)
	Game.play_ui("hurt")
	if health <= 0.0:
		health = 0.0
		die(cause)


func heal(amount: float) -> void:
	health = minf(100.0, health + amount)


func owns(item: String) -> bool:
	match item:
		"flashlight":
			return inventory.has_flashlight
		"shakelight":
			return inventory.has_shakelight
		"bandage":
			return inventory.bandages > 0
		"vitamins":
			return inventory.vitamins > 0
	return false


func select_item(item: String) -> void:
	if not owns(item):
		notify("you don't have " + ("any " if item in ["bandage", "vitamins"] else "a ") + item + ("s" if item == "bandage" else ""))
		return
	selected = item
	# lights only shine while you hold them
	lights.equip(item if item in ["flashlight", "shakelight"] else "none")
	if hud:
		hud.notify(item)


func cycle_item(dir: int) -> void:
	var i := ITEMS.find(selected)
	for k in ITEMS.size():
		i = wrapi(i + dir, 0, ITEMS.size())
		if owns(ITEMS[i]):
			select_item(ITEMS[i])
			return


## left click: use whatever is in your hand
func use_item() -> void:
	if dead or _busy:
		return
	viewmodel.kick(0.4 if selected == "flashlight" else 1.0)
	match selected:
		"flashlight":
			if not hidden:
				lights.toggle_flashlight()
		"shakelight":
			lights.shake()
		"bandage":
			use_bandage()
		"vitamins":
			use_vitamins()
	# used the last one: go back to something you still have
	if not owns(selected):
		cycle_item(-1)


func use_bandage() -> void:
	if health >= 100.0:
		notify("you're already at full health")
	elif inventory.use_bandage():
		heal(35.0)
		notify("+35 health")


func use_vitamins() -> void:
	if inventory.use_vitamins():
		_speed_boost = 12.0
		Game.play_ui("vitamins")
		notify("speed boost")


func die(cause: String) -> void:
	dead = true
	velocity = Vector3.ZERO
	var tw := create_tween()
	tw.tween_property(head, "position:y", 0.3, 0.6)
	tw.parallel().tween_property(head, "rotation:z", 1.2, 0.6)
	died.emit(cause)
	Game.player_died.emit(cause)
