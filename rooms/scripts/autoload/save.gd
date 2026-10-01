extends Node
## save data: best door, gold, achievements, a few stats and the run in progress
## (so you can continue where you left off). user://save.json
## admin runs lock the save: changes happen in memory and are thrown away after the run.

const PATH := "user://save.json"

var data := {
	"best_door": 0,
	"gold": 100,  # starter gold
	"starter_gold_given": true,
	"achievements": {},  # id -> unix time unlocked
	"deaths": 0,
	"runs": 0,
	"exits": 0,
	"run": {},  # the run in progress, empty when there is none
}
var locked := false
var _backup := {}


func _ready() -> void:
	load_data()


func load_data() -> void:
	if not FileAccess.file_exists(PATH):
		return
	var parsed = JSON.parse_string(FileAccess.get_file_as_string(PATH))
	if parsed is Dictionary:
		for k in parsed:
			data[k] = parsed[k]
		# saves from before starter gold existed get it once
		if not parsed.has("starter_gold_given"):
			data.gold = int(data.gold) + 100
			data.starter_gold_given = true
			write()


func write() -> void:
	if locked:
		return
	var f := FileAccess.open(PATH, FileAccess.WRITE)
	if f:
		f.store_string(JSON.stringify(data, "\t"))


## admin mode: snapshot now, restore when the run ends
func set_locked(on: bool) -> void:
	if on and not locked:
		_backup = data.duplicate(true)
	elif not on and locked:
		data = _backup
	locked = on


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
