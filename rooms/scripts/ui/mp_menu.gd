class_name MPMenu
extends Control
## multiplayer: host a run or join one on the same wifi / network.
## hosts show up in the list by themselves; you can also type the host's ip.

var _name: LineEdit
var _ip: LineEdit
var _status: Label
var _list: VBoxContainer
var _found: VBoxContainer
var _start: Control
var _mode := ""  # "", "host", "join"
var _public: CheckBox


func _ready() -> void:
	set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	UIKit.full_screen_bg(self, Color(0, 0, 0, 0.9))
	var p := UIKit.panel(Vector2(520, 0))
	UIKit.centered(self, p)
	var v := UIKit.vbox(10)
	p.add_child(v)
	v.add_child(UIKit.label("multiplayer", 28, UIKit.ACCENT))
	v.add_child(UIKit.label("up to 4 workers. host online and send your friends the code,\nor play on the same wifi.", 15, Color(0.7, 0.7, 0.7)))
	var nr := HBoxContainer.new()
	nr.add_child(UIKit.label("your name", 16))
	_name = LineEdit.new()
	_name.text = str(Save.data.get("mp_name", Loc.t("worker %d") % randi_range(10, 99)))
	_name.max_length = 16
	_name.custom_minimum_size = Vector2(260, 40)
	_name.text_changed.connect(func(t):
		Save.data.mp_name = t
		Save.write())
	nr.add_child(_name)
	v.add_child(nr)
	var row := HBoxContainer.new()
	row.add_theme_constant_override("separation", 10)
	row.add_child(UIKit.button("host online", _host_online, 18))
	if not OS.has_feature("web"):
		row.add_child(UIKit.button("host on wifi", _host, 16))
	v.add_child(row)
	_public = UIKit.check("public game (anyone can join from the list)", bool(Save.data.get("mp_public", true)), func(on):
		Save.data.mp_public = on
		Save.write())
	v.add_child(_public)
	row = HBoxContainer.new()
	row.add_theme_constant_override("separation", 10)
	_ip = LineEdit.new()
	_ip.placeholder_text = "code (ABCDE) or wifi ip"
	_ip.text = str(Save.data.get("mp_last_ip", ""))
	_ip.custom_minimum_size = Vector2(220, 40)
	row.add_child(_ip)
	row.add_child(UIKit.button("join", func(): _join(_ip.text), 18))
	v.add_child(row)
	v.add_child(UIKit.label("games to join:", 15, Color(0.7, 0.7, 0.7)))
	_found = UIKit.vbox(4)
	v.add_child(_found)
	_status = UIKit.label("", 15, UIKit.ACCENT)
	_status.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	v.add_child(_status)
	_list = UIKit.vbox(4)
	v.add_child(_list)
	var sr := HBoxContainer.new()
	sr.add_theme_constant_override("separation", 10)
	sr.add_child(UIKit.button("start: the offices", func(): Net.host_start("offices"), 18))
	sr.add_child(UIKit.button("start: the wires", func(): Net.host_start("wires"), 18))
	var pr := HBoxContainer.new()
	pr.add_theme_constant_override("separation", 10)
	for mg in Game.MINIGAMES:
		pr.add_child(UIKit.button(Loc.t("party: ") + Loc.t(Game.MINIGAMES[mg][0]), func(): Net.host_start("offices", false, mg), 16))
	var sg := HBoxContainer.new()
	sg.add_theme_constant_override("separation", 10)
	for sid in ["office", "battle", "hide_seek"]:
		sg.add_child(UIKit.button(Loc.t("sightings: ") + Loc.t(Game.SIGHTINGS[sid][0]), func(): Net.host_start("offices", false, "", sid), 16))
	var both := UIKit.vbox(8)
	both.add_child(sr)
	both.add_child(pr)
	both.add_child(sg)
	_start = both
	_start.visible = false
	v.add_child(_start)
	v.add_child(UIKit.button("back", func():
		Net.stop("")
		Net.stop_listening()
		queue_free(), 16))
	Net.players_changed.connect(_refresh)
	Net.joined.connect(_on_joined)
	Net.left.connect(_on_left)
	Net.start_listening()
	var t := Timer.new()
	t.wait_time = 1.0
	t.autostart = true
	t.timeout.connect(_refresh_found)
	add_child(t)
	_refresh()


func _on_joined() -> void:
	_status.text = "joined! waiting for the host to start..."


func _on_left(reason: String) -> void:
	_mode = ""
	_status.text = reason
	_start.visible = false
	_refresh()


func _host_online() -> void:
	var err := Net.host_online(_name.text, _public.button_pressed)
	if err != "":
		_status.text = err
		return
	_mode = "host"
	_status.text = Loc.t("hosting online! your code:  %s\nsend it to your friends, then come back to the game.") % Net.code
	_start.visible = true


func _host() -> void:
	var err := Net.host(_name.text)
	if err != "":
		_status.text = err
		return
	_mode = "host"
	var ips := Net.local_ips()
	_status.text = Loc.t("hosting! others can join with: ") + (", ".join(ips) if not ips.is_empty() else Loc.t("your ip"))
	_start.visible = true


func _join(ip: String) -> void:
	if ip.strip_edges() == "":
		_status.text = "type the host's ip, or pick a game from the list"
		return
	Save.data.mp_last_ip = ip
	Save.write()
	# a code (letters) goes online, an ip (numbers and dots) goes over the wifi
	var by_ip := ip.count(".") >= 3
	var err := Net.join(ip, _name.text) if by_ip else Net.join_online(ip, _name.text)
	if err != "":
		_status.text = err
		return
	_mode = "join"
	_status.text = Loc.t("connecting to %s...") % ip.to_upper()


func _refresh() -> void:
	for c in _list.get_children():
		c.queue_free()
	for id in Net.players:
		var n: String = Net.players[id].get("name", "?")
		_list.add_child(UIKit.label(("  host: " if int(id) == 1 else "  ") + n, 16))
	_start.visible = Net.is_host


func _refresh_found() -> void:
	for c in _found.get_children():
		c.queue_free()
	var now := Time.get_ticks_msec()
	for ip in Net.hosts_found.keys():
		var h: Dictionary = Net.hosts_found[ip]
		if now - int(h.t) > 4000:
			Net.hosts_found.erase(ip)
			continue
		if Net.is_host:
			continue
		_found.add_child(UIKit.button(Loc.t("join %s  (%d/4)  -  %s") % [h.name, h.players, ip], func(): _join(ip), 15))
	# public online games (from anywhere)
	if not Net.active:
		for c in Net.lobby.games.keys():
			var g: Dictionary = Net.lobby.games[c]
			_found.add_child(UIKit.button(Loc.t("join %s  (%d/4)  -  online") % [g.name, g.players], func(): _join(c), 15))
	if _found.get_child_count() == 0:
		_found.add_child(UIKit.label("  none yet", 14, Color(0.5, 0.5, 0.5)))
