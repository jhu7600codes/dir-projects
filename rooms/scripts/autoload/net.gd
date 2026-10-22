extends Node
## multiplayer (co-op, up to 4 players). two ways to play together:
## - online: the host gets a 5 letter code, others type it in. goes through supabase
##   realtime (SupabasePeer), works from anywhere
## - lan: join by ip, or pick the host from the list (hosts shout on the lan every second)
##
## how it stays in sync:
## - everyone builds the same rooms from the same seed, so only door openings are sent
## - the host decides every entity spawn and sends it out (with a random seed, so the
##   entity's own dice rolls match); the host also streams entity positions
## - each player is checked against entities on their own machine (you die on your screen)
## - players send their position ~15 times a second; others see them as coworker avatars
## - lockers, pickups, drawers and the dispenser send what happened to them
## - dead players spectate. when everyone is dead, the run is over for everyone

signal players_changed
signal joined
signal left(reason: String)

const PORT := 24560
const DISCOVER_PORT := 24561
const MAX_PLAYERS := 4
const SEND_RATE := 1.0 / 15.0

var active := false
var is_host := false
var online := false
var code := ""
var my_name := "worker"
var players := {}  # peer id -> {name, dead}
var hosts_found := {}  # ip -> {name, players, t}
var public_game := false  # online host: listed in everyone's "games to join"
var lobby := OnlineLobby.new()
var points := {}  # battle: name -> kills
var seeker_id := 0  # hide and seek: who's the entity
var mg_scores := {}  # party minigame: name -> score, filled in as people die  # the public games list (see online_lobby.gd)
var remotes := {}  # peer id -> RemotePlayer (in the game scene)
var _send_t := 0.0
var _shout_t := 0.0
var _udp: PacketPeerUDP
var _listen: PacketPeerUDP
var _drop_n := 0


func _ready() -> void:
	process_mode = Node.PROCESS_MODE_ALWAYS
	multiplayer.peer_connected.connect(_on_peer_connected)
	multiplayer.peer_disconnected.connect(_on_peer_disconnected)
	multiplayer.connected_to_server.connect(func(): rpc_id(1, "_hello", my_name))
	multiplayer.connection_failed.connect(func(): stop("couldn't connect"))
	multiplayer.server_disconnected.connect(func(): stop("the host left"))


# ---------- lobby ----------

func host(player_name: String) -> String:
	stop("")
	my_name = player_name
	if OS.has_feature("web"):
		return "wifi games don't work in the browser, use host online"
	var peer = ClassDB.instantiate("ENetMultiplayerPeer")
	var err: int = peer.create_server(PORT, MAX_PLAYERS - 1)
	if err != OK:
		return Loc.t("couldn't host (port %d busy?)") % PORT
	multiplayer.multiplayer_peer = peer
	active = true
	is_host = true
	players = {1: {"name": my_name, "dead": false}}
	_udp = PacketPeerUDP.new()
	_udp.set_broadcast_enabled(true)
	players_changed.emit()
	return ""


## online: host a game through supabase, others join with the code
func host_online(player_name: String, make_public := false) -> String:
	stop("")
	my_name = player_name
	public_game = make_public
	if make_public:
		lobby.open()
	var letters := "ABCDEFGHJKLMNPQRSTUVWXYZ"
	code = ""
	for i in 5:
		code += letters[randi() % letters.length()]
	var peer := SupabasePeer.new()
	if peer.create_host(code) != OK:
		return "couldn't reach the server"
	multiplayer.multiplayer_peer = peer
	active = true
	is_host = true
	online = true
	players = {1: {"name": my_name, "dead": false}}
	players_changed.emit()
	return ""


func join_online(join_code: String, player_name: String) -> String:
	stop("")
	my_name = player_name
	code = join_code.strip_edges().to_upper()
	var peer := SupabasePeer.new()
	if peer.create_client(code) != OK:
		return "couldn't reach the server"
	multiplayer.multiplayer_peer = peer
	active = true
	is_host = false
	online = true
	return ""


## how often players send where they are. online it's slower (the free supabase plan
## only allows about 100 messages a second for everyone together)
func send_rate() -> float:
	return 0.2 if online else SEND_RATE


func join(ip: String, player_name: String) -> String:
	stop("")
	my_name = player_name
	if OS.has_feature("web"):
		return "wifi games don't work in the browser, use a code"
	var peer = ClassDB.instantiate("ENetMultiplayerPeer")
	var err: int = peer.create_client(ip.strip_edges(), PORT)
	if err != OK:
		return Loc.t("couldn't connect to ") + ip
	multiplayer.multiplayer_peer = peer
	active = true
	is_host = false
	return ""


func stop(reason: String) -> void:
	var was := active
	active = false
	is_host = false
	online = false
	public_game = false
	players.clear()
	remotes.clear()
	if multiplayer.multiplayer_peer and not (multiplayer.multiplayer_peer is OfflineMultiplayerPeer):
		multiplayer.multiplayer_peer.close()
	multiplayer.multiplayer_peer = OfflineMultiplayerPeer.new()
	if _udp:
		_udp.close()
		_udp = null
	if was:
		left.emit(reason)
		players_changed.emit()


func start_listening() -> void:
	lobby.open()
	if _listen or OS.has_feature("web"):
		return
	_listen = PacketPeerUDP.new()
	if _listen.bind(DISCOVER_PORT) != OK:
		_listen = null


func stop_listening() -> void:
	lobby.close()
	if _listen:
		_listen.close()
	_listen = null


func local_ips() -> Array:
	var out := []
	for ip in IP.get_local_addresses():
		if ip.count(".") == 3 and not ip.begins_with("127.") and not ip.begins_with("169.254"):
			out.append(ip)
	return out


func _on_peer_connected(id: int) -> void:
	if is_host:
		rpc_id(id, "_players", players)


func _on_peer_disconnected(id: int) -> void:
	players.erase(id)
	if remotes.has(id) and is_instance_valid(remotes[id]):
		remotes[id].queue_free()
	remotes.erase(id)
	players_changed.emit()
	if is_host:
		rpc("_players", players)
		_check_all_dead()


@rpc("any_peer", "reliable")
func _hello(player_name: String) -> void:
	if not is_host:
		return
	var id := multiplayer.get_remote_sender_id()
	players[id] = {"name": player_name.substr(0, 16), "dead": false}
	rpc("_players", players)
	players_changed.emit()


@rpc("authority", "reliable")
func _players(p: Dictionary) -> void:
	var first := players.is_empty()
	players = p
	players_changed.emit()
	if first:
		joined.emit()


func _process(delta: float) -> void:
	# a public online host shows up in the list until the run starts (or it fills up)
	if active and is_host and online and public_game and not _in_game() and players.size() < MAX_PLAYERS:
		lobby.advertise(code, my_name, players.size())
	else:
		lobby.stop_advertising()
	lobby.poll(delta)
	if _listen:
		while _listen.get_available_packet_count() > 0:
			var pkt := _listen.get_packet().get_string_from_utf8()
			var ip := _listen.get_packet_ip()
			var d = JSON.parse_string(pkt)
			if d is Dictionary and d.get("game", "") == "rooms-the-hallway":
				hosts_found[ip] = {"name": str(d.get("name", "?")), "players": int(d.get("players", 1)), "t": Time.get_ticks_msec()}
	if not active:
		return
	# the host shouts "i'm here" on the lan so people can find it
	if is_host and _udp and not online and not _in_game():
		_shout_t -= delta
		if _shout_t <= 0.0:
			_shout_t = 1.0
			_udp.set_dest_address("255.255.255.255", DISCOVER_PORT)
			_udp.put_packet(JSON.stringify({"game": "rooms-the-hallway", "name": my_name, "players": players.size()}).to_utf8_buffer())
	if not _in_game():
		return
	_send_t -= delta
	if _send_t <= 0.0:
		_send_t = send_rate()
		_send_me()


func _in_game() -> bool:
	return Game.player != null and is_instance_valid(Game.player) and Game.generator != null


# ---------- starting a run together ----------

## host: everybody starts the same run. each player keeps their own items (carry)
func host_start(floor_id: String, fixed := false, minigame := "", sightings := "") -> void:
	if not is_host:
		return
	var s := randi()
	for id in players:
		players[id]["dead"] = false
	var mods: Array = [] if minigame != "" or sightings != "" else Game.modifiers
	var seek := 0
	if sightings == "hide_seek":
		var ids := players.keys()
		seek = int(ids[randi() % ids.size()])
	rpc("_start", s, floor_id, fixed, mods, minigame, sightings, seek)
	_start(s, floor_id, fixed, mods, minigame, sightings, seek)


@rpc("authority", "reliable")
func _start(seed_value: int, floor_id: String, fixed: bool, mods: Array, minigame := "", sightings := "", seek := 0) -> void:
	mg_scores.clear()
	points.clear()
	seeker_id = seek
	for id in players:
		players[id]["dead"] = false
	remotes.clear()
	_drop_n = 0
	lobby.close()  # the run started: off the public list, no more listening
	Game.net_seed = seed_value
	# going on to the next floor keeps your health and items (if you're still alive)
	var carry := {}
	var alive: bool = Game.player != null and is_instance_valid(Game.player) and not Game.player.dead
	if alive and (floor_id != "offices" or fixed):
		carry = Game.carry_state()
	if fixed:
		carry["fixed"] = true
	if minigame != "":
		carry = {"minigame": minigame}
	if sightings != "":
		carry = {"sightings": sightings, "seeker": seek == multiplayer.get_unique_id()}
	Game.start_run(false, false, floor_id, carry)
	Game.modifiers = mods


# ---------- players ----------

func _send_me() -> void:
	var p: Player = Game.player
	if p == null:
		return
	var state := [p.global_position, p.global_rotation.y, p.head.rotation.x, p.hidden, p.crouching, p.dead,
			p.lights.is_lit(), Game.door]
	# the host's update also carries where the entities are (one message instead of two)
	if is_host:
		state.append(_entity_positions())
	rpc("_state", state)


@rpc("any_peer", "unreliable_ordered")
func _state(s: Array) -> void:
	var id := multiplayer.get_remote_sender_id()
	if not _in_game():
		return
	var r = remotes.get(id)
	if r == null or not is_instance_valid(r):
		r = RemotePlayer.new()
		r.peer = id
		r.player_name = str(players.get(id, {}).get("name", "worker"))
		Game.player.get_parent().add_child(r)
		remotes[id] = r
	r.apply(s)
	if s.size() > 8 and id == 1:
		_entity_pos(s[8])


func alive_remotes() -> Array:
	var out := []
	for id in remotes:
		var r = remotes[id]
		if is_instance_valid(r) and not r.dead:
			out.append(r)
	return out


## you died: tell everyone (the host ends the run when nobody's left)
func i_died() -> void:
	if not active:
		return
	rpc("_died", multiplayer.get_unique_id())
	_died(multiplayer.get_unique_id())


@rpc("any_peer", "reliable")
func _died(id: int) -> void:
	if players.has(id):
		players[id]["dead"] = true
	if is_host:
		_check_all_dead()


func _check_all_dead() -> void:
	if not is_host or not _in_game() or players.is_empty():
		return
	# battle: the last one standing ends it. hide and seek: the seeker got everyone
	if Game.sightings == "battle" and players.size() > 1:
		var alive := players.keys().filter(func(id): return not players[id].get("dead", false))
		if alive.size() <= 1:
			rpc("_finish", "battle_over")
			_finish("battle_over")
		return
	if Game.sightings == "hide_seek":
		var hiders := players.keys().filter(func(id): return id != seeker_id and not players[id].get("dead", false))
		if hiders.is_empty():
			rpc("_finish", "seeker_win")
			_finish("seeker_win")
		return
	for id in players:
		if not players[id].get("dead", false):
			return
	rpc("_finish", "all_dead")
	_finish("all_dead")


## the run ended (an exit, a-1000, the wires elevator, the bus, or everyone died)
func finish(reason: String) -> void:
	if not active:
		return
	rpc("_finish", reason)


@rpc("any_peer", "reliable")
func _finish(reason: String) -> void:
	if _in_game():
		Game.player.get_parent().net_finished(reason)


# ---------- battle / hide and seek ----------

## you hit another player (battle), or caught them (hide and seek)
func hit_player(peer: int, damage: float, cause: String) -> void:
	if not active:
		return
	rpc_id(peer, "_hit", damage, cause, my_name)


@rpc("any_peer", "reliable")
func _hit(damage: float, cause: String, from_name: String) -> void:
	var p: Player = Game.player
	if p == null or p.dead:
		return
	Game.death_details[cause] = from_name
	p.take_damage(damage, cause)
	if p.dead:
		rpc("_kill_credit", from_name)
		_kill_credit(from_name)


@rpc("any_peer", "reliable")
func _kill_credit(who: String) -> void:
	points[who] = int(points.get(who, 0)) + 1
	mg_scores = points.duplicate()


# ---------- doors ----------

func door_opened(n: int) -> void:
	if active:
		rpc("_door", n)


@rpc("any_peer", "reliable")
func _door(n: int) -> void:
	if not _in_game():
		return
	for r in Game.generator.rooms:
		if is_instance_valid(r) and r.exit_door and r.exit_door.number == n:
			r.exit_door.open_remote()
			return


# ---------- entities ----------

## host -> everyone: an entity spawns
func entity_spawned(id: String, opts: Dictionary, seed_value: int) -> void:
	if active and is_host:
		rpc("_entity", id, opts, seed_value)


@rpc("authority", "reliable")
func _entity(id: String, opts: Dictionary, seed_value: int) -> void:
	if _in_game() and Game.entities:
		Game.entities.spawn_remote(id, opts, seed_value)


func entity_gone(id: String) -> void:
	if active and is_host:
		rpc("_entity_gone", id)


@rpc("authority", "reliable")
func _entity_gone(id: String) -> void:
	if _in_game() and Game.entities and Game.entities.active.has(id):
		var e = Game.entities.active[id]
		Game.entities.active.erase(id)
		if is_instance_valid(e):
			e.queue_free()


func _entity_positions() -> Dictionary:
	var pos := {}
	var em = Game.entities
	if em == null:
		return pos
	for id in em.active:
		var e = em.active[id]
		if is_instance_valid(e):
			pos[id] = e.global_position
	return pos


func _entity_pos(pos: Dictionary) -> void:
	if not _in_game() or Game.entities == null:
		return
	for id in pos:
		var e = Game.entities.active.get(id)
		if e and is_instance_valid(e):
			e.net_pos = pos[id]
			e.has_net_pos = true


# ---------- things in rooms (lockers, pickups, drawers, the dispenser) ----------

## a node inside a room, by room number + the child indices down to it. rooms are built
## the same way on every machine, so this points at the same thing everywhere
func node_ref(n: Node) -> Array:
	var idx := []
	var c := n
	while c and not (c is RoomBase):
		idx.push_front(c.get_index())
		c = c.get_parent()
	if c == null:
		return []
	return [c.number, idx]


func find_ref(ref: Array) -> Node:
	if ref.size() != 2 or not _in_game():
		return null
	var n: Node = Game.generator.room(int(ref[0]))
	if n == null:
		return null
	for i in ref[1]:
		if int(i) < 0 or int(i) >= n.get_child_count():
			return null
		n = n.get_child(int(i))
	return n


func send_event(node: Node, what: String, arg = null) -> void:
	if not active:
		return
	var ref := node_ref(node)
	if ref.is_empty():
		return
	rpc("_event", ref, what, arg)


@rpc("any_peer", "reliable")
func _event(ref: Array, what: String, arg) -> void:
	var n := find_ref(ref)
	if n and n.has_method("net_event"):
		n.net_event(what, arg)


func next_drop_name() -> String:
	_drop_n += 1
	return "drop_%d" % _drop_n


# ---------- party minigames ----------

## you're out: tell everyone your score (the results list fills in as people die)
func mg_score(score: int) -> void:
	if not active:
		return
	rpc("_mg_score", my_name, score)
	_mg_score(my_name, score)
	i_died()


@rpc("any_peer", "reliable")
func _mg_score(who: String, score: int) -> void:
	mg_scores[who] = score
