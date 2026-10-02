extends Node
## save data: best door, gold, achievements, a few stats and the run in progress
## (so you can continue where you left off).
## there are two profiles: "main" (user://save.json) for normal runs and "admin"
## (user://save_admin.json) for admin runs, so admin runs have their own progress that
## never touches the normal one.

const PATHS := {"main": "user://save.json", "admin": "user://save_admin.json"}
const DEFAULTS := {
	"best_door": 0,
	"gold": 100,  # starter gold
	"starter_gold_given": true,
	"achievements": {},  # id -> unix time unlocked
	"deaths": 0,
	"runs": 0,
	"exits": 0,
	"run": {},  # the run in progress, empty when there is none
}

var profile := "main"
var data := DEFAULTS.duplicate(true)
var locked := false  # kept for older code paths, nothing locks the save anymore


func _ready() -> void:
	load_data()


## switch between the normal and the admin progress
func use_profile(p: String) -> void:
	if p == profile or not PATHS.has(p):
		return
	write()
	profile = p
	data = DEFAULTS.duplicate(true)
	load_data()


func is_admin_profile() -> bool:
	return profile == "admin"


func load_data() -> void:
	var path: String = PATHS[profile]
	if not FileAccess.file_exists(path):
		return
	var parsed = JSON.parse_string(FileAccess.get_file_as_string(path))
	if parsed is Dictionary:
		for k in parsed:
			data[k] = parsed[k]
		# saves from before starter gold existed get it once
		if not parsed.has("starter_gold_given"):
			data.gold = int(data.gold) + 100
			data.starter_gold_given = true
			write()


func write() -> void:
	var f := FileAccess.open(PATHS[profile], FileAccess.WRITE)
	if f:
		f.store_string(JSON.stringify(data, "\t"))


func has_run() -> bool:
	return data.get("run", {}) is Dictionary and not data.run.is_empty()


## called on every door and when you quit to the title screen
func save_run(run: Dictionary) -> void:
	if locked:
		return
	data.run = run
	write()


func clear_run() -> void:
	if locked:
		return
	data.run = {}
	write()


func add_gold(amount: int) -> void:
	data.gold = int(data.gold) + amount
	write()
	if int(data.gold) >= 500:
		Achievements.unlock("rich")


func spend_gold(amount: int) -> bool:
	if int(data.gold) < amount:
		return false
	data.gold = int(data.gold) - amount
	write()
	return true


func record_door(door: int) -> void:
	if door > int(data.best_door):
		data.best_door = door
		write()
