extends Node
## long run with entities on, on every floor and sightings mode, in god mode:
## godot --headless --path . res://tests/soak_test.tscn
## any script error shows up in the log. prints where it is as it goes.

const RUNS := [
	["offices", "", 260],
	["offices", "office", 120],
	["offices", "endless", 60],
	["offices", "battle", 40],
	["offices", "training", 30],
	["offices", "daily", 40],
	["wires", "", 50],
	["city", "", 25],
	["complex", "", 12],
]


func _ready() -> void:
	get_tree().current_scene = null
	await _frames(2)
	for run in RUNS:
		var carry := {"sightings": run[1]} if run[1] != "" else {}
		Game.start_run(true, false, run[0], carry)
		await _frames(25)
		if Game.player == null or Game.generator == null:
			print("SOAK FAIL: no game for ", run)
			continue
		Game.admin_flags.god = true
		var stuck := 0
		var last := -1
		var t0 := Time.get_ticks_msec()
		while Game.door < int(run[2]) and Time.get_ticks_msec() - t0 < 240000:
			var r = Game.generator.room(Game.door)
			if r == null:
				await _frames(5)
				continue
			if Game.player.hidden:
				Game.player.exit_locker()
			# walk the path like a player would, so entities get a chance at you
			var pts: Array = r.path_global()
			for p in pts:
				Game.player.global_position = p - Vector3(0, 1.5, 0)
				await _frames(3)
			var d = r.exit_door
			if d and not d.is_open:
				if d.locked or d.lock_handler.is_valid():
					d.unlock()
				d.open()
			await _frames(4)
			if r.exit_door:
				Game.player.teleport(r.global_exit() * Transform3D(Basis(Vector3.UP, PI), Vector3(0, 0.1, 1.0)))
			await _frames(6)
			if Game.door == last:
				stuck += 1
				if stuck > 6:
					print("SOAK STUCK: %s %s at %s (%s)" % [run[0], run[1], Game.door_label(Game.door), r.room_type])
					Game.generator.jump_to(Game.door + 1)
					await _frames(10)
					stuck = 0
			else:
				stuck = 0
			last = Game.door
		print("soak: %s/%s reached %s in %.0fs" % [run[0], run[1], Game.door_label(Game.door), (Time.get_ticks_msec() - t0) / 1000.0])
	print("soak test done")
	get_tree().quit()


func _frames(k: int) -> void:
	for i in k:
		await get_tree().process_frame
