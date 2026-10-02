extends Node3D
## the game scene: sets up environment, room generator, player, entities, hud and menus,
## and reacts to deaths / exits. fog and darkness follow the door number.

var env: Environment
var generator: RoomGenerator
var entities: EntityManager
var player: Player
var hud: HUD
var _ambience: AudioStreamPlayer
var _fake_t := 60.0
var _ended := false


func _ready() -> void:
	_make_environment()
	Prewarm.run()  # no first-time stutters when opening doors

	generator = RoomGenerator.new()
	generator.name = "Rooms"
	add_child(generator)
	Game.generator = generator

	entities = EntityManager.new()
	entities.name = "Entities"
	add_child(entities)
	Game.entities = entities

	add_child(Glitch.new())

	player = Player.new()
	player.name = "Player"
	add_child(player)
	Game.player = player

	hud = HUD.new()
	hud.player = player
	add_child(hud)
	player.hud = hud
	add_child(TouchControls.new())
	add_child(PauseMenu.new())
	if Game.admin:
		add_child(AdminPanel.new())

	generator.room_entered.connect(entities.on_room_entered)
	Game.door_changed.connect(_on_door_changed)
	Game.player_died.connect(_on_died)
	Game.run_finished.connect(_on_finished)

	var run := Game.pending_run
	Game.pending_run = {}
	if run.is_empty():
		generator.start(randi())
		player.teleport(generator.room(0).global_transform * Transform3D(Basis(Vector3.UP, PI), Vector3(0, 0.1, 2.0)))
		Game.set_door(0)
	else:
		_load_run(run)

	_ambience = AudioStreamPlayer.new()
	_ambience.stream = Assets.sound("ambience")
	_ambience.bus = "Ambience"
	_ambience.volume_db = -6.0
	add_child(_ambience)
	_ambience.play()

	if not Settings.use_touch():
		Input.mouse_mode = Input.MOUSE_MODE_CAPTURED
	hud.notify("open the door to start. hide in lockers when you hear something.")


## continue a saved run: same seed, same door, same items
func _load_run(run: Dictionary) -> void:
	var n := int(run.get("door", 1))
	generator.start(int(run.get("seed", randi())), n)
	player.teleport(generator.room(n).global_transform * Transform3D(Basis(Vector3.UP, PI), Vector3(0, 0.1, 1.2)))
	player.health = float(run.get("health", 100.0))
	Game.used_locker = bool(run.get("used_locker", false))
	var items: Dictionary = run.get("items", {})
	for k in items:
		var cur = player.inventory.get(k)
		# json turns every number into a float, put ints back as ints
		player.inventory.set(k, int(items[k]) if cur is int else items[k])
	player.inventory.changed.emit()
	Game.door = n
	Game.door_changed.emit(n)
	player.select_item(str(run.get("selected", "flashlight")))
	hud.notify("welcome back. you're at " + Game.door_label(n))


func _make_environment() -> void:
	env = Environment.new()
	env.background_mode = Environment.BG_COLOR
	env.background_color = Color(0.0, 0.0, 0.0)
	env.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	env.ambient_light_color = Color(0.9, 0.9, 0.95)
	env.tonemap_mode = Environment.TONE_MAPPER_ACES
	env.tonemap_exposure = 1.1
	env.fog_enabled = true
	env.fog_light_color = Color(0.08, 0.08, 0.09)
	env.glow_enabled = int(Settings.data.quality) >= 1
	env.glow_intensity = 0.35
	env.glow_hdr_threshold = 1.2
	env.ssao_enabled = int(Settings.data.quality) >= 2
	var we := WorldEnvironment.new()
	we.environment = env
	add_child(we)
	_apply_darkness(0)
	Settings.changed.connect(func():
		env.glow_enabled = int(Settings.data.quality) >= 1
		env.ssao_enabled = int(Settings.data.quality) >= 2)


## normal until a-30, foggy after, dark from a-150 (but you can still see the next room)
func _apply_darkness(n: int) -> void:
	var d := Game.darkness(n)
	if n == Game.LAST_DOOR:
		d = 1.0
	env.ambient_light_energy = lerpf(0.35, 0.07, d)
	env.fog_density = lerpf(0.004, 0.035, d) if n >= 30 else 0.004
	env.fog_light_color = Color(0.08, 0.08, 0.09).lerp(Color(0, 0, 0), d)
	env.fog_light_energy = 1.0 - d * 0.9


func _on_door_changed(n: int) -> void:
	_apply_darkness(n)
	if n == 30 and player.inventory.has_flashlight and not player.lights.flashlight_on:
		hud.notify("it's getting darker. " + ("tap item" if Settings.use_touch() else "left click") + " for your flashlight")


func _process(delta: float) -> void:
	# fake crackles after a-60 to keep you on your toes. they start without a door opening,
	# so a careful listener can tell them apart from the real a-60
	if Game.door < 60 or entities.active.size() > 0 or _ended:
		return
	_fake_t -= delta
	if _fake_t <= 0.0:
		_fake_t = randf_range(70.0, 180.0)
		var a := AudioStreamPlayer.new()
		a.stream = Assets.sound("a60_far")
		a.bus = "SFX"
		a.volume_db = -26.0
		add_child(a)
		a.play()
		var tw := create_tween()
		tw.tween_property(a, "volume_db", -60.0, 3.0)
		tw.tween_callback(a.queue_free)


func _on_died(cause: String) -> void:
	if _ended:
		return
	_ended = true
	Save.clear_run()
	Save.data.deaths = int(Save.data.deaths) + 1
	Save.write()
	var deaths := int(Save.data.deaths)
	Achievements.unlock("first_death")
	if deaths >= 10:
		Achievements.unlock("deaths_10")
	if deaths >= 50:
		Achievements.unlock("deaths_50")
	Game.play_ui("death")
	entities.clear_all()
	var es := EndScreen.new()
	add_child(es)
	es.setup("death", cause)


func _on_finished(reason: String) -> void:
	if _ended:
		return
	_ended = true
	Save.clear_run()
	entities.clear_all()
	if reason == "a1000":
		Achievements.unlock("a1000")
	else:
		Achievements.unlock("long_walk")
		Save.data.exits = int(Save.data.exits) + 1
		Save.write()
	if not Game.used_locker:
		Achievements.unlock("no_lockers")
	player.dead = true  # stops input
	var es := EndScreen.new()
	add_child(es)
	es.setup(reason)
	get_tree().paused = true


func _exit_tree() -> void:
	Game.player = null
	Game.generator = null
	Game.entities = null
	Game.tracked_entity = null
	Game.forced_sprint = false
	Game.touch_move = Vector2.ZERO
	Game.touch_count = 0
