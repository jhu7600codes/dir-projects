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


## big jumpscare frame + scream, then calls done
func jumpscare(tex_key: String, scream_key: String, done: Callable) -> void:
	var vs := root.get_viewport_rect().size
	var j := image(tex_key, minf(vs.x, vs.y) * 1.1)
	center(j)
	play_alert(scream_key)
	var tw := create_tween()
	tw.tween_property(j, "scale", Vector2(1.15, 1.15), 0.7)
	after(0.8, done)
