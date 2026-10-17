class_name Lurkmask
extends ScreenEntity
## lurkmask (cut from rooms: revisited): sometimes when you open a door there's a giggle,
## and a moment later a mask throws itself at your face. crouch when you hear the giggle and
## it flies over your head. otherwise: a scare and a bit of damage.

const RULES := {
	"floor": ["offices"], "trigger": "door", "min_door": 20, "chance": 0.07,
	"cooldown": 60.0, "group": "screen",
}
const DAMAGE := 15.0


func begin() -> void:
	id = "lurkmask"
	make_overlay()
	var g := AudioStreamPlayer.new()
	g.stream = Assets.sound("lurkmask_giggle")
	g.bus = "SFX"
	add_child(g)
	g.play()
	after(1.1, _jump)


func _jump() -> void:
	if player == null or player.dead:
		finish()
		return
	if player.crouching or player.hidden:
		# over your head and gone
		var w := make_3d_audio("lurkmask_whoosh", 6.0, 30.0)
		w.global_position = player.head_position() + Vector3(0, 0.8, 0)
		w.play()
		after(0.8, finish)
		return
	hit_player = true
	Game.death_details["lurkmask"] = "standing"
	play_alert("lurkmask_scream")
	var t := TextureRect.new()
	t.texture = Faces.mask_tex()
	t.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
	t.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_CENTERED
	t.set_anchors_preset(Control.PRESET_FULL_RECT)
	t.mouse_filter = Control.MOUSE_FILTER_IGNORE
	t.scale = Vector2(0.3, 0.3)
	root.add_child(t)
	t.pivot_offset = root.get_viewport_rect().size / 2.0
	var tw := t.create_tween()
	tw.tween_property(t, "scale", Vector2(1.3, 1.3), 0.12)
	tw.tween_interval(0.35)
	tw.tween_property(t, "modulate:a", 0.0, 0.25)
	player.take_damage(DAMAGE, "lurkmask")
	after(1.0, finish)
