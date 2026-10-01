extends Control
## title screen: play (with the admin panel toggle), achievements, settings, credits

var _admin := false
var _music: AudioStreamPlayer


func _ready() -> void:
	set_anchors_preset(Control.PRESET_FULL_RECT)
	Input.mouse_mode = Input.MOUSE_MODE_VISIBLE
	var bg := ColorRect.new()
	bg.color = Color(0.02, 0.02, 0.025)
	bg.set_anchors_preset(Control.PRESET_FULL_RECT)
	add_child(bg)
	var v := UIKit.vbox(10)
	v.custom_minimum_size = Vector2(380, 0)
	UIKit.centered(self, v)
	var title := UIKit.label("rooms: the hallway", 46, Color(0.95, 0.95, 0.95))
	title.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	v.add_child(title)
	var sub := UIKit.label("an unofficial fan game", 15, Color(0.55, 0.55, 0.55))
	sub.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	v.add_child(sub)
	var stats := UIKit.label("best: %s    gold: %d    deaths: %d" % [Game.door_label(int(Save.data.best_door)), int(Save.data.gold), int(Save.data.deaths)], 15, UIKit.ACCENT)
	stats.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	v.add_child(stats)
	v.add_child(Control.new())
	v.add_child(UIKit.button("play", func(): Game.start_run(_admin), 24))
	var adm := UIKit.check("enable admin panel (progress won't be saved)", false, func(on): _admin = on)
	v.add_child(adm)
	v.add_child(UIKit.button("achievements", func(): add_child(AchievementsScreen.new())))
	v.add_child(UIKit.button("settings", func(): add_child(SettingsMenu.new())))
	v.add_child(UIKit.button("credits", func(): add_child(CreditsScreen.new())))
	if not OS.has_feature("mobile") and not OS.has_feature("web"):
		v.add_child(UIKit.button("quit", func(): get_tree().quit()))
	_music = AudioStreamPlayer.new()
	_music.stream = Assets.sound("menu_music")
	_music.bus = "Music"
	add_child(_music)
	_music.play()
	CreditsScreen.check_all()
