class_name Leaderboard
## online boards for the sightings (daily run, endless, solo battle), kept in the same
## supabase project as online multiplayer: table public.leaderboard (anyone can read and post).

const REST := "https://upehzacowjdlwfgijfdc.supabase.co/rest/v1/leaderboard"


static func _headers() -> PackedStringArray:
	return PackedStringArray([
		"apikey: " + SupabasePeer.KEY,
		"Authorization: Bearer " + SupabasePeer.KEY,
		"Content-Type: application/json",
		"Prefer: return=minimal",
	])


static func player_name() -> String:
	var n := str(Save.data.get("mp_name", "")).strip_edges()
	return n.substr(0, 16) if n != "" else "worker"


static func _request(tree: SceneTree, url: String, method: int, body: String, done: Callable) -> void:
	var h := HTTPRequest.new()
	h.timeout = 10.0
	tree.root.add_child(h)
	h.request_completed.connect(func(result: int, code: int, _hd, data: PackedByteArray):
		h.queue_free()
		done.call(result == HTTPRequest.RESULT_SUCCESS and code < 300, data.get_string_from_utf8()))
	if h.request(url, _headers(), method, body) != OK:
		h.queue_free()
		done.call(false, "")


## post a score (fire and forget)
static func submit(tree: SceneTree, board: String, score: int, day := "") -> void:
	if score <= 0:
		return
	var body := JSON.stringify({"board": board, "day": day, "name": player_name(), "score": score})
	_request(tree, REST, HTTPClient.METHOD_POST, body, func(_ok, _d): pass)


## the top 10: done.call(ok, [[name, score], ...])
static func top(tree: SceneTree, board: String, day: String, done: Callable) -> void:
	var url := REST + "?select=name,score&board=eq.%s&day=eq.%s&order=score.desc&limit=10" % [board, day.uri_encode()]
	_request(tree, url, HTTPClient.METHOD_GET, "", func(ok: bool, text: String):
		var rows := []
		var parsed = JSON.parse_string(text) if ok else null
		if parsed is Array:
			for r in parsed:
				rows.append([str(r.get("name", "?")), int(r.get("score", 0))])
		done.call(ok and parsed is Array, rows))
