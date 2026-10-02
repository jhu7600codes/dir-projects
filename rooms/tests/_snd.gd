extends Node
func _ready():
	get_tree().current_scene = null
	Game.start_run(true)
	await get_tree().create_timer(1.0).timeout
	Game.entities.natural_spawns = false
	Game.admin_flags.god = true
	Game.generator.jump_to(30)
	Game.generator.room(30).exit_door.open()
	await get_tree().create_timer(0.5).timeout
	print("t=1.5 spawn a60")
	var e = Game.entities.spawn("a60")
	for i in 16:
		await get_tree().create_timer(0.5).timeout
		if is_instance_valid(e):
			print("t=%.1f d=%.1f far_vol=%.1f far_playing=%s near_playing=%s moving=%s" % [2.0 + i * 0.5, e.global_position.distance_to(Game.player.global_position), e.far_audio.volume_db, e.far_audio.playing, e.near_audio.playing, e.moving])
		else:
			print("t=%.1f a60 gone" % (2.0 + i * 0.5))
	get_tree().quit()
