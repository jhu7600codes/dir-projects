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
	"modifiers": [],  # modifiers picked on the title screen for the next run
}

var profile := "main"
var data := DEFAULTS.duplicate(true)
var _lock := Mutex.new()
var _pending := {}  # path -> json text waiting to be written
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


## the file write happens on a worker thread, so a slow disk never hitches the game
## (it saves on every door and every bit of gold). only the newest data per file gets
## written, so an older save can never land after a newer one.
func write() -> void:
	_lock.lock()
	_pending[PATHS[profile]] = JSON.stringify(data, "\t")
	_lock.unlock()
	WorkerThreadPool.add_task(flush)


## writes whatever is waiting. also called directly when quitting or switching apps
func flush() -> void:
	_lock.lock()
	for path in _pending:
		var f := FileAccess.open(path, FileAccess.WRITE)
		if f:
			f.store_string(_pending[path])
			f.close()
	_pending.clear()
	_lock.unlock()


func _notification(what: int) -> void:
	if what == NOTIFICATION_WM_CLOSE_REQUEST or what == NOTIFICATION_APPLICATION_PAUSED or what == NOTIFICATION_PREDELETE:
		flush()


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
		var old := int(data.best_door)
		data.best_door = door
		write()
		# new journal pages
		for page in JournalScreen.PAGES:
			var need := int(page[2])
			if need > old and need <= door:
				Achievements.popup("journal page found", page[0], "read it in the journal on the title screen", Color(0.75, 0.82, 1.0))
