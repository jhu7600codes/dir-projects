class_name AdminPanel
extends CanvasLayer
## admin panel (only in admin runs, toggle with f1 or `). progress isn't saved in admin
## runs. noclip, god mode, speed, jumping, sliding, spawning entities, skipping rooms.

var _root: Control
var _door_box: SpinBox


func _ready() -> void:
	layer = 41
	process_mode = Node.PROCESS_MODE_ALWAYS
	_root = Control.new()
	_root.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	add_child(_root)
	var p := UIKit.panel(Vector2(360, 0))
	p.position = Vector2(16, 60)
	_root.add_child(p)
	var scroll := ScrollContainer.new()
	scroll.custom_minimum_size = Vector2(340, minf(560, get_viewport().get_visible_rect().size.y - 100))
	scroll.horizontal_scroll_mode = ScrollContainer.SCROLL_MODE_DISABLED
	p.add_child(scroll)
	var v := UIKit.vbox(6)
	v.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	scroll.add_child(v)
	v.add_child(UIKit.label("admin panel", 24, Color(1, 0.6, 0.3)))
	v.add_child(UIKit.label("progress is not saved", 13, Color(0.6, 0.6, 0.6)))
	var names := {"noclip": "noclip (space up, c down)", "god": "god mode", "speed": "speed boost", "jump": "allow jumping (space)", "slide": "allow sliding (crouch while sprinting)", "stamina": "infinite stamina"}
	for k in names:
		v.add_child(UIKit.check(names[k], Game.admin_flags[k], func(on): Game.admin_flags[k] = on))
	v.add_child(UIKit.label("spawn entity", 15, UIKit.ACCENT))
	var grid := GridContainer.new()
	grid.columns = 3
	v.add_child(grid)
	for id in EntityManager.SCRIPTS:
		grid.add_child(UIKit.button(id.insert(1, "-") if id[1].is_valid_int() else id, _spawn.bind(id), 16))
	v.add_child(UIKit.button("despawn all", _despawn, 16))
	v.add_child(UIKit.check("no natural spawns", false, func(on): Game.entities.natural_spawns = not on))
	v.add_child(UIKit.label("rooms", 15, UIKit.ACCENT))
	v.add_child(UIKit.button("open the next door", _open_next, 16))
	var row := HBoxContainer.new()
	_door_box = SpinBox.new()
	_door_box.min_value = 0
	_door_box.max_value = Game.LAST_DOOR
	_door_box.value = 100
	row.add_child(_door_box)
	row.add_child(UIKit.button("jump to door", _jump, 16))
	v.add_child(row)
	v.add_child(UIKit.label("player", 15, UIKit.ACCENT))
	v.add_child(UIKit.button("full heal", func(): Game.player.heal(100.0), 16))
	v.add_child(UIKit.button("give items", _give, 16))
	v.add_child(UIKit.button("+100 gold", func(): Save.data.gold = int(Save.data.gold) + 100, 16))
	visible = false


func _unhandled_input(event: InputEvent) -> void:
	if event.is_action_pressed("admin_panel"):
		toggle()
		get_viewport().set_input_as_handled()


func toggle() -> void:
	visible = not visible
	if not Settings.use_touch():
		Input.mouse_mode = Input.MOUSE_MODE_VISIBLE if visible else Input.MOUSE_MODE_CAPTURED


func _spawn(id: String) -> void:
	if Game.entities:
		Game.entities.spawn(id)


func _despawn() -> void:
	if Game.entities:
		Game.entities.clear_all()


func _jump() -> void:
	if Game.generator:
		Game.generator.jump_to(int(_door_box.value))


func _open_next() -> void:
	var gen = Game.generator
	if gen == null:
		return
	var r = gen.room(Game.door)
	if r and r.exit_door and not r.exit_door.is_open:
		r.exit_door.open()
		Game.player.teleport(r.global_exit() * Transform3D(Basis(Vector3.UP, PI), Vector3(0, 0.1, 1.0)))


func _give() -> void:
	var inv: Inventory = Game.player.inventory
	inv.has_flashlight = true
	inv.has_shakelight = true
	inv.battery = 100.0
	inv.batteries += 5
	inv.bandages += 5
	inv.vitamins += 3
	inv.changed.emit()
