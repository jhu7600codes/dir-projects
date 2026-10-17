class_name OnlineLobby
extends RefCounted
## the list of public online games. everyone with the multiplayer menu open listens on one
## shared supabase channel ("rooms-lobby"), and a public host says "i'm here" every 3 seconds
## (code, name, how many players). a game drops off the list when the host starts the run,
## leaves, or hasn't been heard from in 8 seconds.

const TOPIC := "realtime:rooms-lobby"
const VERSION := 2      # only hosts on the same version show up (so nobody joins an incompatible game)
const AD_EVERY := 3.0
const FORGET := 8.0

var games := {}         # code -> {name, players, t}
var _ws: WebSocketPeer = null
var _joined := false
var _beat := 0.0
var _retry := 0.0
var _ad := {}           # what we advertise right now (empty = nothing)
var _ad_t := 0.0
var _ref := 1


func open() -> void:
	if _ws:
		return
	_ws = WebSocketPeer.new()
	_joined = false
	if _ws.connect_to_url(SupabasePeer.URL % SupabasePeer.KEY) != OK:
		_ws = null


func close() -> void:
	stop_advertising()
	if _ws:
		_ws.poll()
		_ws.close()
	_ws = null
	games.clear()


func is_open() -> bool:
	return _ws != null


func advertise(game_code: String, host_name: String, players: int) -> void:
	var fresh := _ad.is_empty()
	_ad = {"code": game_code, "name": host_name.substr(0, 16), "players": players, "v": VERSION}
	if fresh:
		_ad_t = AD_EVERY  # say it right away


func stop_advertising() -> void:
	if not _ad.is_empty():
		_bc("gone", {"code": _ad.code})
		_ad = {}


func poll(dt: float) -> void:
	if _ws == null:
		return
	dt = minf(dt, 0.5)  # time the app spent paused doesn't count
	_ws.poll()
	var st := _ws.get_ready_state()
	if st == WebSocketPeer.STATE_CLOSED:
		# dropped: try again in a few seconds
		_retry += dt
		if _retry > 3.0:
			_retry = 0.0
			_ws = null
			open()
		return
	if st != WebSocketPeer.STATE_OPEN:
		return
	if not _joined:
		_joined = true
		_send("phx_join", {"config": {"broadcast": {"self": false, "ack": false}, "presence": {"key": ""}, "private": false}})
	while _ws.get_available_packet_count() > 0:
		_handle(_ws.get_packet().get_string_from_utf8())
	_beat += dt
	if _beat > 25.0:
		_beat = 0.0
		_send("heartbeat", {}, "phoenix")
	if not _ad.is_empty():
		_ad_t += dt
		if _ad_t >= AD_EVERY:
			_ad_t = 0.0
			_bc("ad", _ad)
	var now := Time.get_ticks_msec()
	for c in games.keys():
		if now - int(games[c].t) > FORGET * 1000.0:
			games.erase(c)


func _handle(text: String) -> void:
	var msg = JSON.parse_string(text)
	if not (msg is Dictionary) or msg.get("event", "") != "broadcast":
		return
	var p: Dictionary = msg.get("payload", {})
	var d: Dictionary = p.get("payload", {})
	var c := str(d.get("code", ""))
	if c == "":
		return
	match str(p.get("event", "")):
		"ad":
			if int(d.get("v", 0)) == VERSION:
				games[c] = {"name": str(d.get("name", "?")), "players": int(d.get("players", 1)), "t": Time.get_ticks_msec()}
		"gone":
			games.erase(c)


func _bc(kind: String, data: Dictionary) -> void:
	_send("broadcast", {"type": "broadcast", "event": kind, "payload": data})


func _send(event: String, payload: Dictionary, topic := TOPIC) -> void:
	if _ws == null or _ws.get_ready_state() != WebSocketPeer.STATE_OPEN:
		return
	_ref += 1
	_ws.send_text(JSON.stringify({"topic": topic, "event": event, "payload": payload, "ref": str(_ref)}))
