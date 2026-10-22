class_name A20Door
## a-20 "the chameleon": it dresses up as a door with almost the right number on it.
## open the wrong one and it bursts out at you. rooms place it (room_base.gd), the
## sightings node scribbles the numbers out (sightings.gd). this is the burst.


static func burst(door: Door) -> void:
	if not is_instance_valid(door) or door.get_meta("burst", false):
		return
	door.set_meta("burst", true)
	var s := Sprite3D.new()
	s.texture = Faces.a20_tex()
	s.pixel_size = 2.0 / s.texture.get_height()
	s.billboard = BaseMaterial3D.BILLBOARD_ENABLED
	s.shaded = false
	s.no_depth_test = true
	s.position = Vector3(0, 1.2, -0.4)
	door.add_child(s)
	var a := AudioStreamPlayer3D.new()
	a.stream = Assets.sound("a20_burst")
	a.bus = "SFX"
	a.position = Vector3(0, 1.2, 0)
	door.add_child(a)
	a.play()
	if Game.player and Game.player.hud:
		Game.player.hud.flash(Color(0.4, 0.25, 0.1, 0.6), 0.4)
	# it lunges out, then the whole door melts back into the wall
	var tw := door.create_tween()
	tw.tween_property(s, "position:z", -1.4, 0.18)
	tw.parallel().tween_property(s, "scale", Vector3.ONE * 1.4, 0.18)
	tw.tween_interval(0.5)
	tw.tween_property(door, "scale", Vector3(1, 0.01, 1), 0.4)
	tw.tween_callback(door.queue_free)
