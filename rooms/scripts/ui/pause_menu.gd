class_name PauseMenu
extends CanvasLayer
## escape menu: resume, settings, back to the title screen

var _root: Control
var _where: Label
var _info: Label


func _ready() -> void:
	layer = 40
	process_mode = Node.PROCESS_MODE_ALWAYS
	_root = Control.new()
	_root.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	add_child(_root)
	UIKit.full_screen_bg(_root, Color(0, 0, 0, 0.6))
	var p := UIKit.panel(Vector2(320, 0))
	UIKit.centered(_root, p)
	var v := UIKit.vbox(10)
	p.add_child(v)
	v.add_child(UIKit.label("paused", 28, UIKit.ACCENT))
	_where = UIKit.label("", 40, Color(0.95, 0.95, 0.95))
	v.add_child(_where)
	_info = UIKit.label("", 15, Color(0.6, 0.6, 0.62))
	v.add_child(_info)
	var gap := Control.new()
	gap.custom_minimum_size = Vector2(0, 6)
	v.add_child(gap)
	v.add_child(UIKit.button("resume", toggle))
	v.add_child(UIKit.button("settings", func(): _root.add_child(SettingsMenu.new())))
	v.add_child(UIKit.button("quit to title", Game.to_menu))
	visible = false


func _unhandled_input(event: InputEvent) -> void:
	if event.is_action_pressed("pause"):
		if Game.player and Game.player.dead:
			return
		toggle()
		get_viewport().set_input_as_handled()


func toggle() -> void:
	visible = not visible
	if visible:
		_refresh()
	get_tree().paused = visible and not Net.active
	if not Settings.use_touch():
		Input.mouse_mode = Input.MOUSE_MODE_VISIBLE if visible else Input.MOUSE_MODE_CAPTURED


## where you are right now (it used to show the door you started at)
func _refresh() -> void:
	_where.text = Game.door_label(Game.door)
	var floor_name: String = {"offices": "the offices", "wires": "the wires", "city": "the great city"}.get(Game.floor, Game.floor)
	if Game.office_powered():
		floor_name = "the fixed office"
	var parts := [Loc.t(floor_name), Loc.t("%d gold") % int(Save.data.gold)]
	if not Game.modifiers.is_empty():
		parts.append(Loc.t("%d modifiers") % Game.modifiers.size())
	if Net.active:
		parts.append(Loc.t("%d players") % Net.players.size())
	_info.text = "   ·   ".join(parts)
