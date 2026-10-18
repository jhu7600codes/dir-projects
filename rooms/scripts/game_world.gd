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
	if Net.active:
		# the host left (or you lost connection): back to the title screen
		Net.left.connect(_on_net_left)
	Game.door_changed.connect(_on_door_changed)
	Game.player_died.connect(_on_died)
	Game.run_finished.connect(_on_finished)

	var run := Game.pending_run
	Game.pending_run = {}
	if run.is_empty():
		generator.start(Game.net_seed if Net.active else randi())
		player.teleport(generator.room(0).global_transform * Transform3D(Basis(Vector3.UP, PI), Vector3(0, 0.1, 14.0 if Game.floor == "city" else 2.0)))
		Game.set_door(0)
		_apply_carry()
		# easter egg, like in doors: very rarely a-90 is already waiting in the lobby
		if randf() < LOBBY_A90_CHANCE and Game.floor == "offices" and (not Net.active or Net.is_host):
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
	if Game.minigame != "":
		_start_minigame.call_deferred()
	elif Game.modifiers.is_empty():
		hud.notify("open the door to start. hide in lockers when you hear something.")
	else:
		var names := []
		for m in Game.modifiers:
			names.append(Loc.t(Game.MODIFIERS[m][0]))
		hud.notify(Loc.t("modifiers: ") + ", ".join(names))
	# "charge your phone": w-10 tags along through every room of the offices and the city
	if Game.mod("charge_phone") and Game.floor != "wires":
		var ct := Timer.new()
		ct.wait_time = 3.0
		ct.autostart = true
		ct.timeout.connect(func():
			if entities and player and not player.dead and not entities.active.has("w10"):
				entities.spawn("w10", {"follow": true}))
		add_child(ct)


func _on_net_left(_reason: String) -> void:
	Game.to_menu.call_deferred()


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
	hud.notify(Loc.t("welcome back. you're at ") + Game.door_label(n))


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
	# a light grade: a touch more contrast, a little less saturation
	env.adjustment_enabled = true
	env.adjustment_contrast = 1.07
	env.adjustment_saturation = 0.9
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


## a-120 turned the lights off: almost no ambient light, the ceiling panels stop glowing
func set_blackout(on: bool) -> void:
	var panel := Mats.get_mat("light_panel")
	if on:
		env.ambient_light_energy = 0.02
		env.fog_light_energy = 0.05
		panel.emission_energy_multiplier = 0.0
		panel.albedo_color = Color(0.25, 0.25, 0.25)
	else:
		panel.emission_energy_multiplier = 2.0
		panel.albedo_color = Color(1, 1, 0.96)
		_apply_darkness(Game.door)


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
	if n == 30 and Game.floor in ["offices", "wires"] and player.inventory.has_flashlight and not player.lights.flashlight_on:
		hud.notify(Loc.t("it's getting darker. tap item for your flashlight") if Settings.use_touch() else Loc.t("it's getting darker. left click for your flashlight"))


func _process(delta: float) -> void:
	if Game.minigame != "":
		_minigame_tick(delta)
		return
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
	if Game.minigame != "":
		_minigame_over()
		return
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
	if Net.active:
		# multiplayer: watch the others until everyone's dead
		_death_cause = cause
		Net.i_died()
		await cl.done
		if not _over:
			_spectate()
		return
	await cl.done
	var es := EndScreen.new()
	add_child(es)
	es.setup("death", cause)


func _on_finished(reason: String) -> void:
	if Net.active:
		# multiplayer: everyone's run ends together
		Net.finish(reason)
		net_finished(reason)
		return
	_end_run(reason)


## multiplayer: the run ended for everyone (someone left through an exit, reached a-1000,
## rode the elevator, took the bus, or everybody died)
func net_finished(reason: String) -> void:
	if _over:
		return
	_over = true
	if _spectator:
		_spectator.queue_free()
	match reason:
		"a1000":
			Achievements.unlock("a1000")
			if Net.is_host:
				Net.host_start("city", Game.fixed_office)
		"wires":
			Achievements.unlock("wired_up")
			if Net.is_host:
				Net.host_start("offices", true)
		"city":
			Achievements.unlock("going_home")
			if Net.is_host:
				Net.host_start("complex")
		"all_dead" when Game.minigame != "":
			var mr := MinigameResult.new()
			add_child(mr)
			mr.setup(Game.minigame, Net.mg_scores)
		"all_dead":
			var es := EndScreen.new()
			add_child(es)
			es.setup("death", _death_cause)
		_:
			_ended = false
			_end_run(reason)


var _over := false
var _death_cause := ""
var _spectator: Node = null


## dead in multiplayer: the camera follows one of the others. tap / click to switch
func _spectate() -> void:
	var s := Spectator.new()
	add_child(s)
	_spectator = s


func _end_run(reason: String) -> void:
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
		var adm := Game.admin
		if Game.use_cutscenes():
			Game.play_cutscene.call_deferred("outside", func(): Game.start_run(adm, false, "city", c))
		else:
			Game.start_run.call_deferred(adm, false, "city", c)
		return
	if reason == "city":
		# the bus drops you off at your apartment complex. 150 floors up to your flat
		Achievements.unlock("going_home")
		var cc := Game.carry_state()
		var adm3 := Game.admin
		if Game.use_cutscenes():
			Game.play_cutscene.call_deferred("bus", func(): Game.start_run(adm3, false, "complex", cc))
		else:
			Game.start_run.call_deferred(adm3, false, "complex", cc)
		return
	if reason == "complex":
		# home. you lie down... and that's not the end of it
		Game.play_cutscene.call_deferred("ending", Game.to_menu)
		return
	if reason == "wires":
		# the whole office has power again, and the elevator takes you back up
		Save.data.wires_done = int(Save.data.get("wires_done", 0)) + 1
		Save.write()
		Achievements.unlock("wired_up")
		Game.carry = Game.carry_state()
		Game.carry["fixed"] = true  # the elevator takes you up into the fixed office
		if Game.use_cutscenes():
			var cw := Game.carry
			var adm2 := Game.admin
			Game.play_cutscene.call_deferred("elevator", func(): Game.start_run(adm2, false, "offices", cw))
			return
	elif reason == "city":
		pass
	else:
		Achievements.unlock("long_walk")
		# leaving through an exit door gives you the starhelp, for good
		if not Save.data.get("starhelp", false):
			Save.data.starhelp = true
			Save.data.starhelp_charge = 100.0
			Game.starhelp_new = true
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
	# the panel material is shared: don't carry an a-120 blackout into the next run
	Mats.get_mat("light_panel").emission_energy_multiplier = 2.0
	Mats.get_mat("light_panel").albedo_color = Color(1, 1, 0.96)
	Game.player = null
	Game.generator = null
	Game.entities = null
	Game.tracked_entity = null
	Game.forced_sprint = false
	Game.touch_move = Vector2.ZERO
	Game.touch_count = 0


# ---------- minigames ----------
# a normal game scene, locked in the lobby: the door doesn't open, nothing spawns by itself,
# and the minigame keeps throwing its thing at you until you're dead

var _mg_t := 0.0      # seconds survived
var _mg_count := 0    # a-90s survived
var _mg_label: Label
var _mg_next := 6.0
var _mg_a90_on := false


func _start_minigame() -> void:
	entities.natural_spawns = false
	var d: Door = generator.room(0).exit_door
	if d:
		d.code_lock(func(): hud.notify("no way out. survive."), "locked", false)
	var cl := CanvasLayer.new()
	cl.layer = 30
	add_child(cl)
	_mg_label = UIKit.label("", 26, UIKit.ACCENT)
	_mg_label.set_anchors_and_offsets_preset(Control.PRESET_CENTER_TOP)
	_mg_label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	_mg_label.offset_top = 14
	_mg_label.grow_horizontal = Control.GROW_DIRECTION_BOTH
	cl.add_child(_mg_label)
	hud.notify(Game.MINIGAMES[Game.minigame][1])
	if Game.minigame == "w10" and (not Net.active or Net.is_host):
		entities.spawn("w10", {"follow": true, "endless": 0.01})


func _minigame_tick(delta: float) -> void:
	if _mg_label == null or player == null or player.dead:
		return
	_mg_t += delta
	match Game.minigame:
		"w10":
			_mg_label.text = "%d:%02d" % [int(_mg_t) / 60, int(_mg_t) % 60]
			# something might clear it (a glitch): bring it back, as fast as it was
			if (not Net.active or Net.is_host) and not entities.active.has("w10"):
				entities.spawn("w10", {"follow": true, "endless": maxf(0.01, _mg_t)})
		"a90":
			_mg_label.text = Loc.t("a-90s survived: %d") % _mg_count
			var on := entities.active.has("a90")
			if _mg_a90_on and not on:
				_mg_count += 1  # it left and you're still here
			_mg_a90_on = on
			if not on:
				_mg_next -= delta
				if _mg_next <= 0.0:
					_mg_next = maxf(2.5, 8.0 - _mg_count * 0.25) * randf_range(0.8, 1.3)
					if not Net.active or Net.is_host:
						entities.spawn("a90")


func mg_score() -> int:
	return int(_mg_t) if Game.minigame == "w10" else _mg_count


func _minigame_over() -> void:
	Game.play_ui("death")
	entities.clear_all()
	var score := mg_score()
	var key := "mg_best_" + Game.minigame
	var best := int(Save.data.get(key, 0))
	if score > best:
		Save.data[key] = score
		Save.write()
	if Net.active:
		Net.mg_score(score)
		_spectate()
		return
	var mr := MinigameResult.new()
	add_child(mr)
	mr.setup(Game.minigame, {Net.my_name if Net.my_name != "" else "you": score}, best)

