extends Node
## renders every office room type from a few corners, to spot things clipping into each other:
## xvfb-run godot --path . --rendering-method gl_compatibility res://tests/align_shots.tscn -- <out dir> [room filter]

var out := "user://align"
var only := ""


func _ready() -> void:
	var args := OS.get_cmdline_user_args()
	if args.size() > 0:
		out = args[0]
	if args.size() > 1:
		only = args[1]
	DirAccess.make_dir_recursive_absolute(out)
	print("align start")
	get_tree().current_scene = null
	get_window().size = Vector2i(1000, 560)
	Game.start_run(true, false, "offices", {"sightings": "office"})
	print("run started")
	await _frames(20)
	print("frames ok")
	Game.admin_flags.god = true
	var gen: RoomGenerator = Game.generator
	var cam := Camera3D.new()
	cam.fov = 80
	add_child(cam)
	var n := 40
	var all_pool: Array = gen.pool.duplicate()
	for p in all_pool:
		var nm: String = p.script.resource_path.get_file().get_basename()
		if only != "" and not nm.contains(only):
			continue
		for v in 2:
			n = 10 + v * 3  # under 15: no forced locker rooms
			var one: Dictionary = p.duplicate()
			one.min = 1
			gen.pool = [one]
			gen.jump_to(n)
			await _frames(4)
			Game.player.visible = false
			if Game.player.hud:
				Game.player.hud.visible = false
			Game.player.set_physics_process(false)
			var r: RoomBase = gen.room(n)
			var aabb := _bounds(r)
			cam.current = true
			var c := aabb.get_center()
			var lo := aabb.position + Vector3(0.5, 0, 0.5)
			var hi := aabb.end - Vector3(0.5, 0, 0.5)
			var views := [
				[Vector3(0, 1.6, 0.6), Vector3(0, 1.4, 6)],
				[Vector3(lo.x, 2.2, lo.z), Vector3(c.x, 0.8, c.z)],
				[Vector3(hi.x, 2.2, lo.z), Vector3(c.x, 0.8, c.z)],
				[Vector3(lo.x, 2.2, hi.z), Vector3(c.x, 0.8, c.z)],
				[Vector3(hi.x, 2.2, hi.z), Vector3(c.x, 0.8, c.z)],
			]
			var i := 0
			for vw in views:
				var from: Vector3 = r.global_transform * vw[0]
				var to: Vector3 = r.global_transform * vw[1]
				if i > 0:
					from = vw[0]
					to = vw[1]
				cam.global_position = from
				cam.look_at(to)
				await _shot("%s_%d_%d" % [nm, v, i])
				print("shot ", nm, v, i)
				i += 1
	get_tree().quit()


## the room's box in world space, from its floor / wall collision shapes
func _bounds(r: RoomBase) -> AABB:
	var a := AABB()
	var first := true
	for cs in r._body.get_children():
		if cs is CollisionShape3D and cs.shape is BoxShape3D:
			var sz: Vector3 = cs.shape.size
			var b: AABB = r.global_transform * (cs.transform * AABB(-sz / 2, sz))
			a = b if first else a.merge(b)
			first = false
	return a


func _frames(k: int) -> void:
	for i in k:
		await get_tree().process_frame


func _shot(name: String) -> void:
	await _frames(3)
	await RenderingServer.frame_post_draw
	get_viewport().get_texture().get_image().save_png(out.path_join(name + ".png"))
