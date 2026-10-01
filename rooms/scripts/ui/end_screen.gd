class_name EndScreen
extends CanvasLayer
## shown when you die, leave through an exit door, or reach the end at a-1000

const CAUSES := {
	"a60": "a-60 found you outside a locker.",
	"a60b": "a-60b caught you. it doesn't get tired.",
	"a90": "you moved while a-90 was watching.",
	"a90b": "you didn't follow a-90b's orders.",
	"a120": "a-120 saw you. wait until the clanging stops completely.",
	"a200": "the happy scribble got you. white means hide, purple means get out.",
}


func setup(kind: String, cause := "") -> void:
	layer = 45
	process_mode = Node.PROCESS_MODE_ALWAYS
	var root := Control.new()
	root.set_anchors_preset(Control.PRESET_FULL_RECT)
	add_child(root)
	UIKit.full_screen_bg(root, Color(0, 0, 0, 0.0))
	var bg: ColorRect = root.get_child(0)
	create_tween().tween_property(bg, "color:a", 0.85, 1.2)
	var p := UIKit.panel(Vector2(460, 0))
	UIKit.centered(root, p)
	var v := UIKit.vbox(12)
	p.add_child(v)
	var where := Game.door_label(Game.door)
	match kind:
		"death":
			v.add_child(UIKit.label("you died at " + where, 30, Color(0.9, 0.2, 0.2)))
			var why := UIKit.label(CAUSES.get(cause, "the office got you."), 17)
			why.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
			v.add_child(why)
		"exit":
			v.add_child(UIKit.label("you got out at " + where, 30, UIKit.ACCENT))
			v.add_child(UIKit.label("the fresh air feels weird after all those rooms.", 17))
		"a1000":
			v.add_child(UIKit.label("a-1000", 34, UIKit.ACCENT))
			v.add_child(UIKit.label("you walked all the way through. it's over.", 17))
	v.add_child(UIKit.label("best: " + Game.door_label(int(Save.data.best_door)) + "    gold: %d" % int(Save.data.gold), 15, Color(0.6, 0.6, 0.6)))
	if Game.admin:
		v.add_child(UIKit.label("admin run - nothing was saved", 14, Color(1, 0.6, 0.3)))
	v.add_child(UIKit.button("play again", func(): Game.start_run(Game.admin)))
	v.add_child(UIKit.button("title screen", Game.to_menu))
	Input.mouse_mode = Input.MOUSE_MODE_VISIBLE
