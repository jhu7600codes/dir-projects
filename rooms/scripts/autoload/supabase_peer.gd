class_name SupabasePeer
extends MultiplayerPeerExtension
## online multiplayer through supabase realtime: every player connects to the same
## broadcast channel ("rooms-<code>") and the game's packets go through it, so it works
## from anywhere without opening ports. godot's normal rpc code runs on top of this.
##
## the free plan allows ~100 realtime messages a second for the whole project, so the game
## sends player positions only 5 times a second when online (see Net.send_rate()).

const URL := "wss://upehzacowjdlwfgijfdc.supabase.co/realtime/v1/websocket?apikey=%s&vsn=1.0.0"
const KEY := "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVwZWh6YWNvd2pkbHdmZ2lqZmRjIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTE5MDE1MzUsImV4cCI6MjEwNzQ3NzUzNX0.XzGJObl-4LsVZTelzIj0EEUJW9M4O_374ErHyhrNCfk"
const TIMEOUT := 10.0   # a player we haven't heard from in this long is gone
const JOIN_WAIT := 8.0  # how long a joining player waits for the host to answer

var _ws := WebSocketPeer.new()
var _topic := ""
var _id := 0
var _server := false
var _status := MultiplayerPeer.CONNECTION_DISCONNECTED
var _joined_channel := false
var _target := 0
var _mode := MultiplayerPeer.TRANSFER_MODE_RELIABLE
var _channel := 0
var _in: Array = []   # [from, bytes]
var _peers := {}      # id -> seconds since we last heard from them
var _ref := 1
var _beat := 0.0
var _alive_t := 0.0
var _wait := 0.0
var _last_poll := 0


func create_host(code: String) -> Error:
	_server = true
	_id = 1
	return _open(code)


func create_client(code: String) -> Error:
	_server = false
	_id = randi_range(2, 1 << 30)
	return _open(code)


func _open(code: String) -> Error:
	_topic = "realtime:rooms-" + code.to_upper()
	_ws.inbound_buffer_size = 1 << 20
	_ws.outbound_buffer_size = 1 << 20
	var err := _ws.connect_to_url(URL % KEY)
	if err != OK:
		return err
	_status = MultiplayerPeer.CONNECTION_CONNECTING
	_last_poll = Time.get_ticks_msec()
	return OK


func _send_raw(event: String, payload: Dictionary, topic := "") -> void:
	if _ws.get_ready_state() != WebSocketPeer.STATE_OPEN:
		return
	_ref += 1
	_ws.send_text(JSON.stringify({"topic": topic if topic != "" else _topic, "event": event, "payload": payload, "ref": str(_ref)}))


func _bc(kind: String, data: Dictionary) -> void:
	data["f"] = _id
	_send_raw("broadcast", {"type": "broadcast", "event": kind, "payload": data})


# ---------- MultiplayerPeer ----------

func _poll() -> void:
	var now := Time.get_ticks_msec()
	var dt := (now - _last_poll) / 1000.0
	_last_poll = now
	_ws.poll()
	var st := _ws.get_ready_state()
	if st == WebSocketPeer.STATE_OPEN and not _joined_channel:
		_joined_channel = true
		_send_raw("phx_join", {"config": {"broadcast": {"self": false, "ack": false}, "presence": {"key": ""}, "private": false}})
		if _server:
			_status = MultiplayerPeer.CONNECTION_CONNECTED
		else:
			_wait = 0.0
	elif st == WebSocketPeer.STATE_CLOSED and _status != MultiplayerPeer.CONNECTION_DISCONNECTED:
		_drop_all()
		return
	while _ws.get_available_packet_count() > 0:
		_handle(_ws.get_packet().get_string_from_utf8())
	if st != WebSocketPeer.STATE_OPEN:
		return
	# phoenix wants a heartbeat every so often
	_beat += dt
	if _beat > 25.0:
		_beat = 0.0
		_send_raw("heartbeat", {}, "phoenix")
	# still waiting for the host to answer?
	if not _server and _status == MultiplayerPeer.CONNECTION_CONNECTING:
		_wait += dt
		if int(_wait / 1.5) != int((_wait - dt) / 1.5):
			_bc("hi", {})
		if _wait > JOIN_WAIT:
			_drop_all()
		return
	# let the others know we're still here
	_alive_t += dt
	if _alive_t > 2.5:
		_alive_t = 0.0
		_bc("ka", {})
	for id in _peers.keys():
		_peers[id] += dt
		if _peers[id] > TIMEOUT:
			_peers.erase(id)
			peer_disconnected.emit(id)


func _handle(text: String) -> void:
	var msg = JSON.parse_string(text)
	if not (msg is Dictionary) or msg.get("event", "") != "broadcast":
		return
	var p: Dictionary = msg.get("payload", {})
	var kind: String = p.get("event", "")
	var d: Dictionary = p.get("payload", {})
	var from := int(d.get("f", 0))
	if from == 0 or from == _id:
		return
	match kind:
		"hi":
			# someone new. the host answers with everyone who's already here
			if _server:
				_bc("welcome", {"t": from, "peers": _peers.keys()})
			_add(from)
		"welcome":
			if int(d.get("t", 0)) != _id:
				_add(from)
				return
			if _status == MultiplayerPeer.CONNECTION_CONNECTING:
				_status = MultiplayerPeer.CONNECTION_CONNECTED
				_add(from)
				for other in d.get("peers", []):
					if int(other) != _id:
						_add(int(other))
		"ka":
			if _peers.has(from):
				_peers[from] = 0.0
			elif _status == MultiplayerPeer.CONNECTION_CONNECTED:
				_add(from)
		"bye":
			if _peers.has(from):
				_peers.erase(from)
				peer_disconnected.emit(from)
		"p":
			var t := int(d.get("t", 0))
			if t != 0 and t != _id and not (t < 0 and -t != _id):
				return
			if t < 0 and -t == _id:
				return
			if not _peers.has(from):
				if _status != MultiplayerPeer.CONNECTION_CONNECTED:
					return
				_add(from)
			_peers[from] = 0.0
			_in.append([from, Marshalls.base64_to_raw(str(d.get("d", "")))])


func _add(id: int) -> void:
	if _peers.has(id):
		_peers[id] = 0.0
		return
	_peers[id] = 0.0
	peer_connected.emit(id)


func _drop_all() -> void:
	for id in _peers.keys():
		peer_disconnected.emit(id)
	_peers.clear()
	_status = MultiplayerPeer.CONNECTION_DISCONNECTED


func _get_available_packet_count() -> int:
	return _in.size()


func _get_packet_script() -> PackedByteArray:
	if _in.is_empty():
		return PackedByteArray()
	return _in.pop_front()[1]


func _get_packet_peer() -> int:
	return _in[0][0] if not _in.is_empty() else 0


func _put_packet_script(buf: PackedByteArray) -> Error:
	if _status != MultiplayerPeer.CONNECTION_CONNECTED:
		return ERR_UNCONFIGURED
	_bc("p", {"t": _target, "d": Marshalls.raw_to_base64(buf)})
	return OK


func _get_max_packet_size() -> int:
	return 1 << 16


func _get_packet_channel() -> int:
	return 0


func _get_packet_mode() -> MultiplayerPeer.TransferMode:
	return MultiplayerPeer.TRANSFER_MODE_RELIABLE


func _set_transfer_channel(c: int) -> void:
	_channel = c


func _get_transfer_channel() -> int:
	return _channel


func _set_transfer_mode(m: MultiplayerPeer.TransferMode) -> void:
	_mode = m


func _get_transfer_mode() -> MultiplayerPeer.TransferMode:
	return _mode


func _set_target_peer(p: int) -> void:
	_target = p


func _is_server() -> bool:
	return _server


func _is_server_relay_supported() -> bool:
	return false


func _get_unique_id() -> int:
	return _id


func _get_connection_status() -> MultiplayerPeer.ConnectionStatus:
	return _status


func _close() -> void:
	if _ws.get_ready_state() == WebSocketPeer.STATE_OPEN:
		_bc("bye", {})
		_ws.poll()
		_ws.close()
	_peers.clear()
	_status = MultiplayerPeer.CONNECTION_DISCONNECTED


func _disconnect_peer(p: int, _force: bool) -> void:
	if _peers.has(p):
		_peers.erase(p)
		peer_disconnected.emit(p)


func _set_refuse_new_connections(_e: bool) -> void:
	pass


func _is_refusing_new_connections() -> bool:
	return false
