class_name Sightings
extends Node
## the sightings: ghlicht's versions of the office (title screen tab). added to the game
## scene when Game.sightings is set. runs the parts that belong to a mode, not a room:
##   a-20 scribbling the door numbers out, ghlicht parts, training, the daily seed,
##   endless getting worse every 100, battle (solo: faster, entities nonstop).
## the multiplayer side of battle / hide and seek is in net.gd and remote_player.gd.

static var instance: Sightings = null

## training: what comes, in order, and what to do about it
const TRAINING := [
	["a60", "a-60. you hear crackling, the lights flash red. get in a locker or under a table."],
	["a90", "a-90. when it shows up on your screen, don't move. don't look around. wait."],
	["a120", "a-120. it breaks some lockers first. hide in one that still works."],
	["a10", "a-10. it kicks every locker and table it passes. do NOT hide. stay out in the open."],
	["a200", "a-200. a locker door creaks open by itself. that's her warning. hide somewhere else."],
	["a150", "a-150. don't stand in its light."],
	["a25", "a-25. keep moving."],
]
## battle (solo): entities you face, picked at random over and over
const BATTLE_POOL := ["a60", "a120", "a10", "a90", "a200"]
## ghlicht parts: what he can make glitched
const GLITCH_POOL := ["a60", "a120", "a10", "a200", "a90"]
## hide and seek: the hiders win at this door, the seeker waits this long at the start
const HS_GOAL := 20
const SEEK_WAIT := 15.0

var _train_i := 0
var _battle_t := 8.0
var scribbled := false


func _enter_tree() -> void:
	instance = self


func _exit_tree() -> void:
	if instance == self:
		instance = null


## side rooms, a-10, a-20, ghlicht parts: the updated office content
static func has_extras() -> bool:
	return Game.sightings in ["office", "endless", "daily", "battle"] and Game.floor == "offices"


## today's seed, the same for everyone
static func daily_seed() -> int:
	var d := Time.get_date_dict_from_system(true)
	return int(d.year) * 10000 + int(d.month) * 100 + int(d.day)


static func today() -> String:
	return str(daily_seed())


func _ready() -> void:
	Game.generator.room_entered.connect(_on_room_entered)
	match Game.sightings:
		"training":
			Game.entities.natural_spawns = false
			_say("ghlicht: me make training. player learn. player no die here. me put player back.")
		"daily":
			Save.data.daily_date = today()
			Save.data.daily_best = 0
			Save.write()
			_say("daily run. one try. same rooms for everyone today.")
		"endless":
			_say("ghlicht: no a-1000. doors keep going. me like that.")
		"battle":
			if not Net.active:
				_say("battle. the rooms go fast. the entities don't stop. how far can you get?")
		"office":
			_say("ghlicht: me make this. player like. watch for doors with wrong numbers.")
		"hide_seek":
			if Game.seeker:
				_say(Loc.t("you're the entity. count to 15... then catch them all before they reach %s.") % Game.door_label(HS_GOAL))
				Game.player.hud.flash(Color(0, 0, 0, 1), SEEK_WAIT)
				Game.player.set_physics_process(false)
				get_tree().create_timer(SEEK_WAIT).timeout.connect(func():
					if Game.player:
						Game.player.set_physics_process(true)
						_say("ready or not."))
			else:
				_say(Loc.t("hide and seek. the entity is one of your friends. make it to %s.") % Game.door_label(HS_GOAL))


func _say(text: String) -> void:
	if Game.player and Game.player.hud:
		Game.player.hud.notify(text)


func _process(delta: float) -> void:
	if Game.player == null or Game.player.dead or Game.entities == null:
		return
	if Game.sightings == "battle" and not Net.active:
		# solo battle: something is always coming
		_battle_t -= delta
		if _battle_t <= 0.0 and Game.door > 1:
			_battle_t = randf_range(10.0, 20.0)
			var pool := BATTLE_POOL.filter(func(id): return not Game.entities.active.has(id))
			if not pool.is_empty() and Game.entities.active.size() < 2:
				Game.entities.spawn(pool[randi() % pool.size()])


func _on_room_entered(room: RoomBase) -> void:
	if Game.sightings == "hide_seek" and not Game.seeker and room.number >= HS_GOAL and not Game.player.dead:
		Game.run_finished.emit("hiders_win")
		return
	# a-20: a room wearing its fake doors scribbles out every number you've seen
	if not room.fake_doors.is_empty():
		set_scribbled(true)
	elif scribbled:
		set_scribbled(false)
	if Game.sightings == "training" and room.number >= 2 and room.number % 2 == 0 and _train_i < TRAINING.size():
		var step: Array = TRAINING[_train_i]
		_train_i += 1
		_say(step[1])
		get_tree().create_timer(4.0).timeout.connect(func():
			if Game.entities and Game.player and not Game.player.dead:
				Game.entities.spawn(step[0]))
	elif Game.sightings == "training" and _train_i >= TRAINING.size() and Game.entities.active.is_empty():
		_train_i += 1
		if _train_i == TRAINING.size() + 2:
			Save.data.training_done = true
			Save.write()
			_say("ghlicht: player learn everything. me proud. keep going if player want.")


func set_scribbled(on: bool) -> void:
	scribbled = on
	Game.scribbled = on
	for r in Game.generator.rooms:
		if is_instance_valid(r) and r.exit_door and r.number < Game.door:
			r.exit_door.scribble(on)
	Game.door_changed.emit(Game.door)  # the hud redraws its counter


## a-20 was wearing this door
static func on_fake_door(door: Door) -> void:
	var p: Player = Game.player
	if p == null or p.dead or not is_instance_valid(door):
		return
	Net.send_event(door, "open")
	A20Door.burst(door)
	Game.death_details["a20"] = "fake_door"
	p.take_damage(55.0, "a20")


## you found a ghlicht part: he notices, and makes one of them worse
static func ghlicht_part() -> void:
	Save.data.ghlicht_parts = int(Save.data.get("ghlicht_parts", 0)) + 1
	Save.write()
	Game.play_ui("glitch")
	Game.subtitle.emit("ghlicht: player find me part. me make something for player.", Color(0.6, 0.9, 1.0))
	if Game.player and Game.player.hud:
		Game.player.hud.flash(Color(0.5, 0.9, 1.0, 0.7), 0.8)
	if Game.entities == null:
		return
	var pool := GLITCH_POOL.filter(func(id): return not Game.entities.active.has(id))
	if pool.is_empty():
		return
	var id: String = pool[randi() % pool.size()]
	Game.player.get_tree().create_timer(3.0).timeout.connect(func():
		if Game.entities:
			Game.entities.spawn(id, {"glitched": true}))


## training: you'd have died. ghlicht puts you back instead
static func training_save(cause: String) -> void:
	var p: Player = Game.player
	p.health = 100.0
	Game.entities.clear_all()
	if Glitch.instance:
		Glitch.instance.teleport_player()
	Game.subtitle.emit(Loc.t("ghlicht: player die to %s. me no let that happen here. try again.") % cause, Color(0.6, 0.9, 1.0))
