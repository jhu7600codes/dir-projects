class_name ModifiersScreen
extends Control
## pick modifiers for your next runs. locked until you've left through an exit door.

signal closed


func _ready() -> void:
	set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	UIKit.full_screen_bg(self, Color(0, 0, 0, 0.85))
	var p := UIKit.panel(Vector2(520, 0))
	UIKit.centered(self, p)
	var v := UIKit.vbox(10)
	p.add_child(v)
	v.add_child(UIKit.label("modifiers", 26, UIKit.ACCENT))
	if not Game.modifiers_unlocked():
		var l := UIKit.label("leave through an exit door to unlock modifiers.", 17)
		l.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
		v.add_child(l)
	else:
		var hint := UIKit.label("they're used for every new run until you turn them off. continuing a run keeps the ones it started with.", 14, Color(0.6, 0.6, 0.6))
		hint.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
		v.add_child(hint)
		var on := Array(Save.data.get("modifiers", []))
		for id in Game.MODIFIERS:
			var m: Array = Game.MODIFIERS[id]
			v.add_child(UIKit.check(m[0] + "  -  " + m[1], on.has(id), func(t): _toggle(id, t)))
	v.add_child(UIKit.button("back", func():
		closed.emit()
		# the title screen shows how many are on
		get_tree().reload_current_scene.call_deferred()))


func _toggle(id: String, on: bool) -> void:
	var list := Array(Save.data.get("modifiers", []))
	list.erase(id)
	if on:
		list.append(id)
	Save.data.modifiers = list
	Save.write()
