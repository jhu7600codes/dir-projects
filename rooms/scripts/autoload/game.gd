extends Node
## run state shared by everything: door counter, admin flags, references to the
## player / generator / entity manager of the current run, and touch input.

signal door_changed(door: int)
signal player_died(cause: String)
signal run_finished(reason: String)  # "exit" or "a1000"
signal subtitle(text: String, color: Color)
signal screen_tint(color: Color)      # a-200 uses this, Color(0,0,0,0) clears it

const LAST_DOOR := 1000
const WIRES_LAST := 50  # the wires subfloor: W-01 .. W-50
## each wires switch powers one stretch of the office
const POWER_RANGES := [[1, 200], [201, 400], [401, 600], [601, 800], [801, 1000]]

var admin := false
var admin_flags := {
	"noclip": false, "god": false, "speed": false, "jump": false,
	"slide": false, "stamina": false,
}
var door := 0
var used_locker := false
var forced_sprint := false  # a-60b
var ui_open := false        # a keypad is open: no walking or looking around
var chase := false          # seek is chasing you: no stamina limit, doors open by themselves
var death_details := {}     # cause -> what exactly went wrong, for the curious light
var modifiers: Array = []   # modifier ids active this run (see MODIFIERS)
var floor := "offices"      # "offices" (the main a-floor) or "wires" (the w-subfloor)
var carry := {}             # health + items brought along when you switch floors mid-run
var has_key := false        # the management key from a drawer at a-240
var power := [false, false, false, false, false]  # wires switches flipped this run (a-001..a-200, ..)
var elevator_power := false
var fixed_office := false   # this run is the office after the wires: lit, people, no entities

## unlocked by the "welp, thats been a long walk." achievement (leave through an exit door).
## id -> [name, what it does]
const MODIFIERS := {
	"lights_out": ["lights out", "every room is dark, right from a-001"],
	"rush_hour": ["rush hour", "a-60 and a-120 show up twice as often"],
	"in_a_hurry": ["in a hurry", "everything that rushes at you is 30% faster"],
	"fragile": ["fragile", "you only have 50 health"],
	"staring_contest": ["staring contest", "a-90 shows up way more often"],
	"cheap_batteries": ["cheap batteries", "your flashlight drains twice as fast"],
	"gold_rush": ["gold rush", "all the gold you find is doubled"],
	"no_hiding": ["nowhere to hide", "rooms only ever have one locker"],
	"out_of_shape": ["out of shape", "running wears you out twice as fast"],
	"empty_pockets": ["empty pockets", "drawers are mostly empty and there's less loot lying around"],
	"inflation": ["inflation", "everything in the shops costs twice as much"],
	"more_doors": ["got any more doors?", "\"They're for my friends Rush, Ambush, Eyes, Screech, Figure and Seek.\" the entities from doors' hotel move into the office"],
}

# set by the game scene
var player: Node = null
var generator: Node = null
var entities: Node = null
var tracked_entity: Node3D = null  # a-60b star indicator
var pending_run: Dictionary = {}   # a saved run to load when the game scene starts
var menu_admin := false            # the title screen's admin toggle, remembered between runs

# touch controls write here, the player reads it
var touch_move := Vector2.ZERO
var touch_look := Vector2.ZERO
var touch_count := 0

var _ui_player: AudioStreamPlayer


func _ready() -> void:
	_ui_player = AudioStreamPlayer.new()
	_ui_player.bus = "SFX"
	add_child(_ui_player)


## continue the saved run from the title screen
func continue_run() -> void:
	if not Save.has_run():
		return
	pending_run = Save.data.run.duplicate(true)
	start_run(Save.is_admin_profile(), true)


func start_run(with_admin: bool, continuing := false, floor_id := "offices", carry_over := {}) -> void:
	if not continuing:
		pending_run = {}
	floor = str(pending_run.get("floor", "offices")) if continuing else floor_id
	carry = carry_over
	admin = with_admin
	# admin runs keep their own progress (user://save_admin.json)
	Save.use_profile("admin" if admin else "main")
	if not continuing:
		Save.clear_run()
	for k in admin_flags:
		admin_flags[k] = false
	door = 0
	used_locker = false
	forced_sprint = false
	chase = false
	ui_open = false
	has_key = bool(pending_run.get("has_key", false)) if continuing else false
	power = Array(pending_run.get("power", [false, false, false, false, false])) if continuing else [false, false, false, false, false]
	elevator_power = bool(pending_run.get("elevator_power", false)) if continuing else false
	# riding the elevator up from the wires gives you the fixed office for that run
	fixed_office = bool(pending_run.get("fixed_office", false)) if continuing else bool(carry_over.get("fixed", false))
	death_details = {}
	if continuing:
		modifiers = Array(pending_run.get("modifiers", []))
	else:
		modifiers = Array(Save.data.get("modifiers", [])) if modifiers_unlocked() else []
	tracked_entity = null
	if not continuing:
		Save.data.runs = int(Save.data.runs) + 1
		Save.write()
	Achievements.unlock("first_run")
	get_tree().paused = false
	get_tree().change_scene_to_file("res://scenes/game.tscn")


func to_menu() -> void:
	save_run()
	floor = "offices"
	get_tree().paused = false
	Settings.set_world_audio_muted(false)
	admin = false
	player = null
	generator = null
	entities = null
	get_tree().change_scene_to_file("res://scenes/main_menu.tscn")


func set_door(n: int) -> void:
	door = n
	if floor == "wires":
		if n > int(Save.data.get("wires_best", 0)):
			Save.data.wires_best = n
			Save.write()
	else:
		Save.record_door(n)
	save_run()
	if n >= 150 and floor == "offices":
		Achievements.unlock("lights_out")
	door_changed.emit(n)


func _notification(what: int) -> void:
	# closing the window, or switching apps on android, saves the run too
	if what == NOTIFICATION_WM_CLOSE_REQUEST or what == NOTIFICATION_APPLICATION_PAUSED:
		save_run()
		Save.flush()  # right now, not on a worker thread: the app may be about to close


## health + items right now, to take along to another floor
func carry_state() -> Dictionary:
	if player == null:
		return {}
	var inv = player.inventory
	return {
		"health": player.health,
		"items": {
			"has_flashlight": inv.has_flashlight, "has_shakelight": inv.has_shakelight,
			"battery": inv.battery, "shake_charge": inv.shake_charge,
			"batteries": inv.batteries, "bandages": inv.bandages, "vitamins": inv.vitamins,
		},
	}


## remember the run in progress (door, seed, health and items)
func save_run() -> void:
	if player == null or generator == null or player.dead or door <= 0:
		return
	var inv = player.inventory
	Save.save_run({
		"door": door,
		"seed": generator.run_seed,
		"health": player.health,
		"used_locker": used_locker,
		"floor": floor,
		"has_key": has_key,
		"power": power,
		"elevator_power": elevator_power,
		"fixed_office": fixed_office,
		"modifiers": modifiers,
		"selected": player.selected,
		"items": {
			"has_flashlight": inv.has_flashlight, "has_shakelight": inv.has_shakelight,
			"battery": inv.battery, "shake_charge": inv.shake_charge,
			"batteries": inv.batteries, "bandages": inv.bandages, "vitamins": inv.vitamins,
		},
	})


func door_label(n: int) -> String:
	if floor == "wires":
		return "W-%02d" % n
	return "A-%03d" % n


## always the office numbering (best door, journal pages)
static func a_label(n: int) -> String:
	return "A-%03d" % n


## the office after the wires: lit, full of people, no entities (except the coworkers)
func office_powered() -> bool:
	return floor == "offices" and fixed_office


func last_door() -> int:
	return WIRES_LAST if floor == "wires" else LAST_DOOR


## the wires switch that powers office door n (0-4), -1 for the lobby
static func power_range(n: int) -> int:
	if n < 1:
		return -1
	return clampi((n - 1) / 200, 0, 4)


func powered(n: int) -> bool:
	var r := power_range(n)
	return r >= 0 and r < power.size() and bool(power[r])


## 0 = normal office, 1 = pitch black. normal until a-30, foggy after, black by a-150
func modifiers_unlocked() -> bool:
	return Achievements.has("long_walk")


func mod(id: String) -> bool:
	return modifiers.has(id)


func darkness(n: int) -> float:
	if floor == "wires":
		return 0.45  # always dim down there, just the cage lamps
	if office_powered():
		return 0.0  # you switched the power back on down in the wires
	if n > 0 and mod("lights_out"):
		return 0.75
	if n < 30:
		return 0.0
	if n < 130:
		return lerpf(0.0, 0.5, (n - 30) / 100.0)
	if n < 150:
		return lerpf(0.5, 0.75, (n - 130) / 20.0)
	# capped: deep rooms are dark and foggy, but never a black void
	return 0.75


func play_ui(key: String) -> void:
	_ui_player.stream = Assets.sound(key)
	_ui_player.play()
