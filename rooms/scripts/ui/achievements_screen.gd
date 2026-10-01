class_name AchievementsScreen
extends Control
## list of every achievement, unlocked ones highlighted

signal closed


func _ready() -> void:
	set_anchors_preset(Control.PRESET_FULL_RECT)
	UIKit.full_screen_bg(self, Color(0, 0, 0, 0.85))
	var p := UIKit.panel(Vector2(560, 0))
	UIKit.centered(self, p)
	var outer := UIKit.vbox(10)
	p.add_child(outer)
	var got := 0
	for id in Achievements.LIST:
		if Achievements.has(id):
			got += 1
	outer.add_child(UIKit.label("achievements  %d / %d" % [got, Achievements.LIST.size()], 26, UIKit.ACCENT))
	var scroll := ScrollContainer.new()
	scroll.custom_minimum_size = Vector2(540, minf(460, get_viewport_rect().size.y - 160))
	outer.add_child(scroll)
	var v := UIKit.vbox(6)
	v.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	scroll.add_child(v)
	for id in Achievements.LIST:
		var on := Achievements.has(id)
		var a: Array = Achievements.LIST[id]
		v.add_child(UIKit.label(a[0], 18, UIKit.ACCENT if on else Color(0.45, 0.45, 0.45)))
		var dl := UIKit.label(a[1] if on else "??? - " + a[1], 14, Color(0.75, 0.75, 0.75) if on else Color(0.4, 0.4, 0.4))
		v.add_child(dl)
	outer.add_child(UIKit.button("back", func():
		closed.emit()
		queue_free()))
