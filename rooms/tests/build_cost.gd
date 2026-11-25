extends Node
## room build cost per room type: godot --headless --path . res://tests/build_cost.tscn
## builds each pool room a few times and prints the time for setup() (making the nodes)
## and for the mesh merge that runs the frame after.

func _ready() -> void:
	get_tree().current_scene = null
	await get_tree().process_frame
	Game.start_run(true, false, "offices", {"sightings": "office"})
	for i in 20:
		await get_tree().process_frame
	Game.entities.natural_spawns = false
	var gen: RoomGenerator = Game.generator
	var tot_b := 0.0
	var tot_m := 0.0
	for p in gen.pool:
		var b := 0.0
		var m := 0.0
		var nodes := 0
		for k in 4:
			var r: RoomBase = p.script.new()
			var t := Time.get_ticks_usec()
			r.setup(30 + k, 1234 + k * 77)
			b += (Time.get_ticks_usec() - t) / 1000.0
			nodes += r.get_child_count()
			t = Time.get_ticks_usec()
			add_child(r)
			var t2 := Time.get_ticks_usec()
			r._merge_static()
			m += (Time.get_ticks_usec() - t2) / 1000.0
			r.queue_free()
			await get_tree().process_frame
		print("%-22s build %6.2f  merge %6.2f ms  (%d nodes)" % [p.script.resource_path.get_file().get_basename(), b / 4, m / 4, nodes / 4])
		tot_b += b / 4
		tot_m += m / 4
	print("average: build %.2f  merge %.2f ms" % [tot_b / gen.pool.size(), tot_m / gen.pool.size()])
	get_tree().quit()
