extends Node3D
## the game scene: sets up environment, room generator, player, entities, hud and menus,
## and reacts to deaths / exits. fog and darkness follow the door number.

const LOBBY_A90_CHANCE := 0.04
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
		player.teleport(generator.room(0).global_transform * Transform3D(Basis(Vector3.UP, PI), Vector3(0, 0.1, 14.0 if Game.floor == "city" else 2.0)))
		Game.set_door(0)
		_apply_carry()
		# easter egg, like in doors: very rarely a-90 is already waiting in the lobby
		if randf() < LOBBY_A90_CHANCE and Game.floor == "offices":
			get_tree().create_timer(randf_range(6.0, 14.0)).timeout.connect(_lobby_a90)
	else:
		_load_run(run)

	# a quiet, steady room tone, plus the office ambience sound now and then. the ambience
	# file is only 2.5 seconds long, looping it nonstop turned into an annoying beat
	var tone := AudioStreamPlayer.new()
	tone.stream = Assets.sound("room_tone")
	tone.bus = "Ambience"
	tone.volume_db = -14.0
	add_child(tone)
	tone.play()
	_ambience = AudioStreamPlayer.new()
	_ambience.stream = Assets.sound("ambience")
	_ambience.bus = "Ambience"
	_ambience.volume_db = -10.0
	add_child(_ambience)
	var t := Timer.new()
	t.wait_time = 8.0
	t.timeout.connect(func():
		_ambience.pitch_scale = randf_range(0.85, 1.05)
		_ambience.play()
		t.start(randf_range(15.0, 40.0)))
	add_child(t)
	t.start()

	if not Settings.use_touch():
		Input.mouse_mode = Input.MOUSE_MODE_CAPTURED
	if Game.modifiers.is_empty():
		hud.notify("open the door to start. hide in lockers when you hear something.")
	else:
		var names := []
		for m in Game.modifiers:
			names.append(Game.MODIFIERS[m][0])
		hud.notify("modifiers: " + ", ".join(names))


func _lobby_a90() -> void:
	if Game.door != 0 or player == null or player.dead or entities.active.has("a90"):
		return
	if entities.spawn("a90"):
		Achievements.unlock("early_bird")


## switching floors mid-run (a-240 -> the wires -> back up): keep health and items
func _apply_carry() -> void:
	var c := Game.carry
	Game.carry = {}
	if c.is_empty():
		return
	player.health = float(c.get("health", player.health))
	var items: Dictionary = c.get("items", {})
	for k in items:
		var cur = player.inventory.get(k)
		player.inventory.set(k, int(items[k]) if cur is int else items[k])
	player.inventory.changed.emit()


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
	if Game.floor == "city":
		_city_sky()
	var we := WorldEnvironment.new()
	we.environment = env
	add_child(we)
	if Game.floor != "city":
		_apply_darkness(0)
	Settings.changed.connect(func():
		env.glow_enabled = int(Settings.data.quality) >= 1
		env.ssao_enabled = int(Settings.data.quality) >= 2)


## outside: dusk over the city, a low orange sun
func _city_sky() -> void:
	var sky_mat := ProceduralSkyMaterial.new()
	sky_mat.sky_top_color = Color(0.12, 0.16, 0.34)
	sky_mat.sky_horizon_color = Color(0.95, 0.55, 0.3)
	sky_mat.ground_horizon_color = Color(0.4, 0.3, 0.28)
	sky_mat.ground_bottom_color = Color(0.08, 0.08, 0.1)
	var sky := Sky.new()
	sky.sky_material = sky_mat
	env.background_mode = Environment.BG_SKY
	env.sky = sky
	env.ambient_light_source = Environment.AMBIENT_SOURCE_SKY
	env.ambient_light_energy = 0.8
	env.fog_light_color = Color(0.55, 0.5, 0.6)
	env.fog_density = 0.006
	var sun := DirectionalLight3D.new()
	sun.light_color = Color(1.0, 0.72, 0.5)
	sun.light_energy = 1.1
	sun.rotation = Vector3(deg_to_rad(-18), deg_to_rad(150), 0)
	sun.shadow_enabled = int(Settings.data.quality) > 0
	add_child(sun)


## normal until a-30, foggy after, dark from a-150 (but you can still see the next room)
func _apply_darkness(n: int) -> void:
	if Game.floor == "city":
		return
	var d := Game.darkness(n)
	if n == Game.LAST_DOOR and Game.floor == "offices":
		d = 1.0
	env.ambient_light_energy = lerpf(0.35, 0.07, d)
	env.fog_density = lerpf(0.004, 0.035, d) if n >= 30 else 0.004
	env.fog_light_color = Color(0.08, 0.08, 0.09).lerp(Color(0, 0, 0), d)
	env.fog_light_energy = 1.0 - d * 0.9


func _on_door_changed(n: int) -> void:
	_apply_darkness(n)
	if n == 30 and Game.floor != "city" and player.inventory.has_flashlight and not player.lights.flashlight_on:
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
	# the curious light talks to you first, then the normal death screen
	var cl := CuriousLight.new()
	add_child(cl)
	cl.setup(cause, str(Game.death_details.get(cause, "")))
	await cl.done
	var es := EndScreen.new()
	add_child(es)
	es.setup("death", cause)


func _on_finished(reason: String) -> void:
	if _ended:
		return
	_ended = true
	Save.clear_run()
	entities.clear_all()
	if reason == "a1000" and Game.floor == "offices":
		# the last door opens onto the street. the run keeps going outside
		Achievements.unlock("a1000")
		var c := Game.carry_state()
		c["fixed"] = Game.fixed_office
		Game.start_run.call_deferred(Game.admin, false, "city", c)
		return
	if reason == "city":
		Achievements.unlock("going_home")
	if reason == "wires":
		# the whole office has power again, and the elevator takes you back up
		Save.data.wires_done = int(Save.data.get("wires_done", 0)) + 1
		Save.write()
		Achievements.unlock("wired_up")
		Game.carry = Game.carry_state()
		Game.carry["fixed"] = true  # the elevator takes you up into the fixed office
	elif reason == "city":
		pass
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
