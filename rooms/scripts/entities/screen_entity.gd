class_name ScreenEntity
extends Entity
## base for entities that live on your screen instead of in the rooms (a-90, a-90b).
## gives them a full screen overlay layer and an audio player on the "Alert" bus,
## which stays audible while a-90 mutes everything else.

var layer: CanvasLayer
var root: Control
var alert: AudioStreamPlayer


func make_overlay() -> void:
	layer = CanvasLayer.new()
	layer.layer = 60
	add_child(layer)
	root = Control.new()
	root.set_anchors_preset(Control.PRESET_FULL_RECT)
	root.mouse_filter = Control.MOUSE_FILTER_IGNORE
	layer.add_child(root)
	alert = AudioStreamPlayer.new()
	alert.bus = "Alert"
	add_child(alert)


func play_alert(key: String) -> void:
	alert.stream = Assets.sound(key)
	alert.play()


func full_rect(color: Color) -> ColorRect:
	var c := ColorRect.new()
	c.color = color
	c.set_anchors_preset(Control.PRESET_FULL_RECT)
	c.mouse_filter = Control.MOUSE_FILTER_IGNORE
	root.add_child(c)
	return c


func image(tex_key: String, size: float) -> TextureRect:
	var t := TextureRect.new()
	t.texture = Assets.texture(tex_key)
	t.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
	t.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_CENTERED
	t.size = Vector2(size, size)
	t.mouse_filter = Control.MOUSE_FILTER_IGNORE
	root.add_child(t)
	return t


func center(t: Control) -> void:
	var vs := root.get_viewport_rect().size
	t.position = (vs - t.size) / 2.0


## jumpscare + scream, then calls done.
##   "fill":  the attack frame covers the whole screen and shakes (a-90, like in doors)
##   "lunge": the face flies at you out of the dark over a red flash (a-90b)
func jumpscare(tex_key: String, scream_key: String, done: Callable, style := "fill") -> void:
	var vs := root.get_viewport_rect().size
	play_alert(scream_key)
	var j := TextureRect.new()
	j.texture = Assets.texture(tex_key)
	j.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
	j.mouse_filter = Control.MOUSE_FILTER_IGNORE
	j.texture_filter = CanvasItem.TEXTURE_FILTER_NEAREST
	if style == "lunge":
		var bg := full_rect(Color(0.05, 0.0, 0.0, 0.95))
		j.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_CENTERED
		j.size = vs
		j.pivot_offset = vs / 2.0
		j.scale = Vector2(0.25, 0.25)
		root.add_child(j)
		var tw := create_tween()
		tw.tween_property(j, "scale", Vector2(1.9, 1.9), 0.3).set_trans(Tween.TRANS_EXPO).set_ease(Tween.EASE_IN)
		tw.parallel().tween_property(bg, "color", Color(0.55, 0.0, 0.0, 0.95), 0.3)
	else:
		_red_static()
		j.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_CENTERED
		j.size = Vector2(vs.y, vs.y) * 1.15
		center(j)
		root.add_child(j)
	_shake(j, j.position, 0.85)
	after(0.9, done)


## full screen red tv static behind the face, redrawn every few frames
func _red_static() -> void:
	var img := Image.create(96, 54, false, Image.FORMAT_RGB8)
	var tex := ImageTexture.create_from_image(img)
	var r := TextureRect.new()
	r.texture = tex
	r.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
	r.stretch_mode = TextureRect.STRETCH_SCALE
	r.texture_filter = CanvasItem.TEXTURE_FILTER_NEAREST
	r.set_anchors_preset(Control.PRESET_FULL_RECT)
	r.mouse_filter = Control.MOUSE_FILTER_IGNORE
	root.add_child(r)
	var redraw := func():
		for y in img.get_height():
			for x in img.get_width():
				var v := randf()
				img.set_pixel(x, y, Color(0.35 + v * 0.65, v * v * 0.15, v * v * 0.12))
		tex.update(img)
	redraw.call()
	var t := Timer.new()
	t.wait_time = 0.05
	t.timeout.connect(redraw)
	add_child(t)
	t.start()


func _shake(c: Control, base: Vector2, time: float) -> void:
	var tw := create_tween()
	for i in int(time / 0.04):
		tw.tween_property(c, "position", base + Vector2(randf_range(-1, 1), randf_range(-1, 1)) * 18.0, 0.04)
