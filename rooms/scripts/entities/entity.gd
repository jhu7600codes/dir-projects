class_name Entity
extends Node3D
## base for every entity. the EntityManager reads each entity script's RULES constant to
## decide when it spawns, then calls begin(). the entity calls finish() when it's done.
##
## RULES keys (all optional):
##   trigger:   "door" (rolled when a door is opened), "timer" (random timer), "door_number"
##   min_door:  usual first door,  rare_min / rare_chance: rare early spawns
##   chance:    chance per door open (trigger "door")
##   needs_spawn_room: only when entering a room tagged entity_spawn_ok (has lockers)
##   timer:     [min, max] seconds between timer spawns
##   at_door:   exact door for trigger "door_number"
##   cooldown:  seconds before it may spawn again
##   group:     entities in the same group never run at the same time
##   blocked_by: ids that stop this one from spawning while active
##   pause_timer_while: ids that pause this one's timer while active
##   needs_mod: only spawns with this modifier on (the doors entities use "more_doors")
##   needs_dark: only in dark rooms (a-130 and deeper, or with lights out)

signal finished(entity: Entity, survived: bool)

var id := "entity"
var manager: Node = null
var player: Node = null
var generator: Node = null
var hit_player := false  # did it damage the player at all (for the "survive" achievements)
var _done := false


## called by the manager right after the entity is added to the tree
func begin() -> void:
	pass


func is_frozen() -> bool:
	return manager != null and manager.frozen and id != "a90"


func finish() -> void:
	if _done:
		return
	_done = true
	var survived: bool = player != null and not player.dead and not hit_player
	finished.emit(self, survived)
	queue_free()


## line of sight from this entity to the player's head, walls and closed doors block it
func sees_player() -> bool:
	var q := PhysicsRayQueryParameters3D.create(global_position, player.head_position(), 1)
	return get_world_3d().direct_space_state.intersect_ray(q).is_empty()


## run a callable after some seconds. the timer belongs to the entity, so it's
## cleaned up safely if the entity is freed first. respects the a-90 freeze.
func after(seconds: float, what: Callable) -> void:
	var t := Timer.new()
	t.one_shot = true
	t.wait_time = maxf(seconds, 0.01)
	t.timeout.connect(func():
		t.queue_free()
		if not _done:
			what.call())
	add_child(t)
	t.start()


## billboard sprite, unshaded so it shows in the dark. edges fade (Assets.glow_texture)
func make_sprite(tex_key: String, size := 2.2) -> Sprite3D:
	var s := Sprite3D.new()
	s.texture = Assets.glow_texture(tex_key)
	s.pixel_size = size / maxf(1.0, s.texture.get_height())
	var m := StandardMaterial3D.new()
	m.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
	# normal blending: additive glow vanished against the bright white office walls
	m.billboard_mode = BaseMaterial3D.BILLBOARD_ENABLED
	m.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	m.cull_mode = BaseMaterial3D.CULL_DISABLED
	m.albedo_texture = s.texture
	s.material_override = m
	add_child(s)
	return s


## swap the picture on a sprite made by make_sprite (keeps the glow material in sync)
func set_sprite_texture(s: Sprite3D, tex: Texture2D) -> void:
	s.texture = tex
	(s.material_override as StandardMaterial3D).albedo_texture = tex


func set_sprite_color(s: Sprite3D, c: Color) -> void:
	(s.material_override as StandardMaterial3D).albedo_color = c


func make_3d_audio(key: String, unit := 8.0, max_dist := 120.0) -> AudioStreamPlayer3D:
	var a := AudioStreamPlayer3D.new()
	a.stream = Assets.sound(key)
	a.bus = "SFX"
	a.unit_size = unit
	a.max_distance = max_dist
	add_child(a)
	return a
