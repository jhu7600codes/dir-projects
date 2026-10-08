extends Node
## asset loader. every game object asks this node for its texture, sound, model or font
## by a key from assets/manifest.json, so files can be swapped without touching code.
##
## if an entry or its file is missing, a placeholder is used instead (generated texture,
## colored box, synthesized or silent sound) and a warning is printed once.
## files can be imported by godot (normal res:// resources) or raw files dropped into
## assets/local/ (png, jpg, webp, ogg, mp3, wav, glb, gltf, ttf, otf).

const MANIFEST := "res://assets/manifest.json"
const SYNTH_RATE := 22050

var manifest := {}
var _cache := {}
var _warned := {}


func _ready() -> void:
	if not FileAccess.file_exists(MANIFEST):
		_warn("manifest.json missing, everything uses placeholders")
		return
	var parsed = JSON.parse_string(FileAccess.get_file_as_string(MANIFEST))
	if parsed is Dictionary:
		manifest = parsed
	else:
		_warn("manifest.json is not valid json, everything uses placeholders")


func entry(section: String, key: String) -> Dictionary:
	var s = manifest.get(section, {})
	if s is Dictionary and s.get(key) is Dictionary:
		return s[key]
	return {}


func _warn(msg: String) -> void:
	if _warned.has(msg):
		return
	_warned[msg] = true
	push_warning("assets: " + msg)


func _have_file(path: String) -> bool:
	return path != "" and (ResourceLoader.exists(path) or FileAccess.file_exists(path))


# ---- textures -------------------------------------------------------------

func texture(key: String) -> Texture2D:
	var ck := "tex:" + key
	if _cache.has(ck):
		return _cache[ck]
	var e := entry("textures", key)
	var path: String = e.get("path", "")
	var tex: Texture2D = _load_texture(path) if _have_file(path) else null
	if tex == null:
		_warn("texture '%s' missing (%s), using placeholder" % [key, path if path != "" else "no entry"])
		tex = ImageTexture.create_from_image(_placeholder_image(e.get("placeholder", {})))
	_cache[ck] = tex
	return tex


## entity sprite version of a texture: the edges fade out in a soft circle, so wiki renders
## that come on a solid square background (like a-60's red one) turn into a glow
func glow_texture(key: String) -> Texture2D:
	var ck := "glow:" + key
	if _cache.has(ck):
		return _cache[ck]
	var src := texture(key)
	var img := src.get_image()
	if img == null:
		return src
	img = img.duplicate()
	if img.is_compressed():
		img.decompress()
	img.clear_mipmaps()
	img.convert(Image.FORMAT_RGBA8)
	var w := img.get_width()
	var h := img.get_height()
	var c := Vector2(w, h) / 2.0
	var r := minf(w, h) / 2.0
	for y in h:
		for x in w:
			var d := Vector2(x, y).distance_to(c) / r
			var fade := clampf((1.0 - d) / 0.7, 0.0, 1.0)
			if fade < 1.0:
				var px := img.get_pixel(x, y)
				px.a *= fade * fade
				img.set_pixel(x, y, px)
	img.generate_mipmaps()
	var t := ImageTexture.create_from_image(img)
	_cache[ck] = t
	return t


## true when the real file exists (used to skip placeholder-only tricks like tinting)
func has_real(section: String, key: String) -> bool:
	return _have_file(entry(section, key).get("path", ""))


func _load_texture(path: String) -> Texture2D:
	if ResourceLoader.exists(path):
		var r = load(path)
		if r is Texture2D:
			return r
	var img := Image.load_from_file(path)
	if img and not img.is_empty():
		img.generate_mipmaps()
		return ImageTexture.create_from_image(img)
	return null


func _placeholder_image(p: Dictionary) -> Image:
	var kind: String = p.get("kind", "solid")
	var c1 := Color(p.get("color", "#ff00ff"))
	var c2 := Color(p.get("color2", "#000000"))
	match kind:
		"noise":
			return _img_noise(c1, c2, float(p.get("scale", 0.08)))
		"grid":
			return _img_grid(c1, c2)
		"windows":
			return _img_windows(c1, c2, int(p.get("seed", 1)))
		"face":
			return _img_face(c1, str(p.get("style", "smile")))
		"sign":
			return _img_sign(c1, str(p.get("style", "stop")))
		"star":
			return _img_star(c1)
		"wallpaper", "carpet", "tiles", "wood", "metal":
			return _img_surface(kind, c1, c2)
	var img := Image.create(8, 8, false, Image.FORMAT_RGBA8)
	img.fill(c1)
	return img


func _img_noise(a: Color, b: Color, scale: float) -> Image:
	var n := FastNoiseLite.new()
	n.frequency = scale
	n.seed = int(a.r8 * 31 + a.g8 * 7 + a.b8)
	var img := Image.create(128, 128, true, Image.FORMAT_RGBA8)
	for y in 128:
		for x in 128:
			# tileable-ish: sample on a torus by mixing two offsets
			var v := (n.get_noise_2d(x, y) + n.get_noise_2d(x + 128, y + 128) * 0.5) * 0.5 + 0.5
			img.set_pixel(x, y, a.lerp(b, clampf(v, 0.0, 1.0)))
	img.generate_mipmaps()
	return img


## seamless surface textures (they tile with no visible seams). 256px each.
##   wallpaper: off-white with faint vertical stripes   carpet: fine grain + soft blotches
##   tiles: 2x2 acoustic ceiling tiles with grooves     wood: grain lines
##   metal: brushed horizontal streaks
func _img_surface(kind: String, a: Color, b: Color) -> Image:
	const S := 256
	var big := _seamless(S, 0.012, 1)
	var fine := _seamless(S, 0.25, 2)
	var img := Image.create(S, S, false, Image.FORMAT_RGBA8)
	for y in S:
		for x in S:
			var n1 := big.get_pixel(x, y).r
			var n2 := fine.get_pixel(x, y).r
			var t := 0.0
			match kind:
				"wallpaper":
					var stripe := 0.5 + 0.5 * sin(TAU * x * 8.0 / S)
					t = n1 * 0.45 + n2 * 0.25 + stripe * 0.12
				"carpet":
					t = n2 * 0.7 + n1 * 0.4
				"tiles":
					t = n2 * 0.5 + n1 * 0.2
					# small dark pits like acoustic tiles
					if fine.get_pixel((x * 7) % S, (y * 5) % S).r > 0.82:
						t = 1.0
				"wood":
					t = 0.5 + 0.5 * sin(TAU * (y * 10.0 / S) + n1 * 4.0)
					t = t * 0.35 + n2 * 0.25 + n1 * 0.4
				"metal":
					t = fine.get_pixel(x, (y * 3) % S).r * 0.35 + n1 * 0.4 + fine.get_pixel((x * 9) % S, y).r * 0.25
			var c := a.lerp(b, clampf(t, 0.0, 1.0))
			if kind == "tiles":
				# grooves between tiles: texture holds 2x2 tiles
				var gx := x % (S / 2)
				var gy := y % (S / 2)
				if gx < 3 or gy < 3:
					c = c.darkened(0.35)
				elif gx < 5 or gy < 5:
					c = c.lightened(0.08)
			img.set_pixel(x, y, c)
	img.generate_mipmaps()
	return img


func _seamless(size: int, freq: float, seed_value: int) -> Image:
	var n := FastNoiseLite.new()
	n.seed = seed_value
	n.frequency = freq
	n.fractal_octaves = 3
	return n.get_seamless_image(size, size, false, false, 0.2, true)


## a building facade: rows of windows, some lit warm, most dark, in a concrete wall
func _img_windows(wall: Color, glass: Color, seed_value: int) -> Image:
	const S := 256
	var img := Image.create(S, S, true, Image.FORMAT_RGBA8)
	img.fill(wall)
	var rng := RandomNumberGenerator.new()
	rng.seed = seed_value
	for wy in 4:
		for wx in 4:
			var lit := rng.randf() < 0.22
			var c := Color(1.0, 0.82, 0.5) if lit else glass.lerp(Color(0.05, 0.06, 0.08), rng.randf_range(0.0, 0.5))
			for y in range(wy * 64 + 14, wy * 64 + 54):
				for x in range(wx * 64 + 10, wx * 64 + 54):
					img.set_pixel(x, y, c)
	img.generate_mipmaps()
	return img


func _img_grid(base: Color, line: Color) -> Image:
	var img := Image.create(128, 128, true, Image.FORMAT_RGBA8)
	img.fill(base)
	for i in 128:
		for t in 3:
			img.set_pixel(i, t, line)
			img.set_pixel(t, i, line)
	img.generate_mipmaps()
	return img


# crude drawn face, good enough to know which entity is which until real art is fetched
func _img_face(col: Color, style: String) -> Image:
	var s := 256
	var img := Image.create(s, s, false, Image.FORMAT_RGBA8)
	img.fill(Color(0, 0, 0, 0))
	var c := Vector2(s / 2.0, s / 2.0)
	for y in s:
		for x in s:
			var p := Vector2(x, y)
			var d := p.distance_to(c)
			if style == "scribble":
				# hand drawn ring + smile, no fill
				if absf(d - 110.0) < 5.0 + sin(atan2(y - c.y, x - c.x) * 9.0) * 2.0:
					img.set_pixel(x, y, col)
			elif d < 115.0:
				img.set_pixel(x, y, col.darkened(0.15 + 0.25 * (d / 115.0)))
			# eyes
			for ex in [-42.0, 42.0]:
				var ed := p.distance_to(c + Vector2(ex, -30))
				if ed < (22.0 if style != "dots" else 9.0):
					img.set_pixel(x, y, Color.BLACK if style != "scribble" else col)
			# mouth
			var m := p - (c + Vector2(0, 20))
			var rr := m.length()
			if m.y > 0 and absf(rr - 60.0) < (7.0 if style != "wide" else 22.0):
				img.set_pixel(x, y, Color.BLACK if style != "scribble" else col)
	return img


func _img_sign(col: Color, style: String) -> Image:
	var s := 256
	var img := Image.create(s, s, false, Image.FORMAT_RGBA8)
	img.fill(Color(0, 0, 0, 0))
	var c := Vector2(s / 2.0, s / 2.0)
	for y in s:
		for x in s:
			var p := Vector2(x, y) - c
			var inside := false
			if style == "stop":
				# octagon
				inside = maxf(absf(p.x), absf(p.y)) < 110 and (absf(p.x) + absf(p.y)) < 155
			else:
				inside = p.length() < 112
			if not inside:
				continue
			var white := false
			if style == "stop":
				# raised hand: palm + 4 fingers
				white = (absf(p.x) < 38 and p.y > -10 and p.y < 60)
				for fx in [-30.0, -10.0, 10.0, 30.0]:
					if absf(p.x - fx) < 7 and p.y > -70 and p.y <= -10:
						white = true
			else:
				# arrow pointing right
				white = (p.x > -70 and p.x < 10 and absf(p.y) < 18) or (p.x >= 10 and p.x < 70 and absf(p.y) < (70 - p.x) * 0.9)
			img.set_pixel(x, y, Color.WHITE if white else col)
	return img


func _img_star(col: Color) -> Image:
	var s := 64
	var img := Image.create(s, s, false, Image.FORMAT_RGBA8)
	img.fill(Color(0, 0, 0, 0))
	var c := Vector2(s / 2.0, s / 2.0)
	for y in s:
		for x in s:
			var p := Vector2(x, y) - c
			var a := atan2(p.y, p.x)
			var r := 14.0 + 14.0 * pow(absf(cos(a * 2.5)), 3.0)
			if p.length() < r:
				img.set_pixel(x, y, col)
	return img


# ---- sounds ---------------------------------------------------------------

func sound(key: String) -> AudioStream:
	var ck := "snd:" + key
	if _cache.has(ck):
		return _cache[ck]
	var e := entry("sounds", key)
	if e.is_empty():
		e = entry("music", key)
	var path: String = e.get("path", "")
	var loop: bool = e.get("loop", false)
	var st: AudioStream = _load_sound(path, loop) if _have_file(path) else null
	if st == null:
		_warn("sound '%s' missing (%s), using placeholder" % [key, path if path != "" else "no entry"])
		st = _synth(e.get("placeholder", {"kind": "silent"}), loop)
	_cache[ck] = st
	return st


func _load_sound(path: String, loop: bool) -> AudioStream:
	var st: AudioStream = null
	if ResourceLoader.exists(path):
		st = load(path) as AudioStream
	else:
		match path.get_extension().to_lower():
			"ogg":
				st = AudioStreamOggVorbis.load_from_file(path)
			"mp3":
				var mp3 := AudioStreamMP3.new()
				mp3.data = FileAccess.get_file_as_bytes(path)
				st = mp3
			"wav":
				st = AudioStreamWAV.load_from_file(path)
	if st == null:
		return null
	if st is AudioStreamOggVorbis or st is AudioStreamMP3:
		st.set("loop", loop)
	elif st is AudioStreamWAV and loop:
		var w := st as AudioStreamWAV
		var bytes_per := (1 if w.format == AudioStreamWAV.FORMAT_8_BITS else 2) * (2 if w.stereo else 1)
		w.loop_mode = AudioStreamWAV.LOOP_FORWARD
		w.loop_begin = 0
		w.loop_end = w.data.size() / bytes_per
	return st


## tiny synth for placeholder sounds, so the horror cues work before real audio exists.
## kinds: silent, static, hiss, hum, clang, knock, creak, tone, rattle, glitch, scream,
## chime, pad, whoosh, step, thud, bitcrush, buzz
func _synth(p: Dictionary, loop: bool) -> AudioStreamWAV:
	var kind: String = p.get("kind", "silent")
	var dur: float = p.get("length", 1.0)
	var vol: float = p.get("volume", 0.6)
	var freq: float = p.get("freq", 440.0)
	var n := int(SYNTH_RATE * dur)
	var s := PackedFloat32Array()
	s.resize(n)
	var rng := RandomNumberGenerator.new()
	rng.seed = hash(kind + str(freq))
	var prev := 0.0
	for i in n:
		var t := float(i) / SYNTH_RATE
		var v := 0.0
		match kind:
			"static":
				v = rng.randf_range(-1, 1) * (0.35 + (0.65 if rng.randf() < 0.04 else 0.0)) * (0.7 + 0.3 * sin(t * 37.0))
			"hiss":
				var w := rng.randf_range(-1, 1)
				v = (w - prev) * 0.6
				prev = w
			"hum":
				v = sin(TAU * 60.0 * t) * 0.5 + sin(TAU * 120.0 * t) * 0.25 + rng.randf_range(-0.04, 0.04)
			"buzz":
				v = (fmod(t * 110.0, 1.0) * 2.0 - 1.0) * 0.5 * (0.6 + 0.4 * sin(t * 23.0)) + rng.randf_range(-0.3, 0.3)
			"clang":
				var tt := fmod(t, 0.6)
				var env := exp(-tt * 5.0)
				v = (sin(TAU * 220.0 * tt) + sin(TAU * 587.0 * tt) * 0.7 + sin(TAU * 931.0 * tt) * 0.5 + sin(TAU * 1390.0 * tt) * 0.3) * env * 0.4
			"knock":
				for k0 in [0.0, 0.22, 0.44]:
					var dt: float = t - k0
					if dt >= 0.0:
						v += (sin(TAU * 95.0 * dt) + rng.randf_range(-0.5, 0.5)) * exp(-dt * 35.0)
			"creak":
				var f := 90.0 + 40.0 * sin(t * 3.0)
				v = (fmod(t * f, 1.0) * 2.0 - 1.0) * sin(PI * t / dur) * 0.5
			"tone":
				v = sin(TAU * freq * t) * minf(1.0, (dur - t) * 8.0)
			"rattle":
				var ph := fmod(t, 0.07)
				v = rng.randf_range(-1, 1) * exp(-ph * 60.0)
			"glitch":
				var seg := int(t * 30.0)
				var sf := 100.0 + float((seg * 7919) % 900)
				v = (1.0 if fmod(t * sf, 1.0) < 0.5 else -1.0) * 0.4
			"scream":
				var f2 := 300.0 + t * 900.0
				v = ((fmod(t * f2, 1.0) * 2.0 - 1.0) * 0.6 + rng.randf_range(-0.6, 0.6)) * minf(1.0, (dur - t) * 4.0)
			"chime":
				v = (sin(TAU * freq * t) + sin(TAU * freq * 1.5 * t) * 0.5) * exp(-t * 3.0)
			"pad":
				v = (sin(TAU * 220.0 * t) + sin(TAU * 330.0 * t) * 0.7 + sin(TAU * 440.0 * t) * 0.4) * (0.6 + 0.4 * sin(TAU * t / dur)) * 0.3
			"whoosh":
				v = rng.randf_range(-1, 1) * sin(PI * t / dur)
			"step":
				v = rng.randf_range(-1, 1) * exp(-t * 40.0) + sin(TAU * 70.0 * t) * exp(-t * 25.0)
			"thud":
				v = sin(TAU * 55.0 * t) * exp(-t * 8.0) + rng.randf_range(-0.3, 0.3) * exp(-t * 20.0)
			"bitcrush":
				var note := int(t * 12.0)
				var bf := 200.0 + float((note * 4561) % 600)
				v = floor(sin(TAU * bf * t) * 3.0) / 3.0 * 0.5
		s[i] = clampf(v * vol, -1.0, 1.0)
	var bytes := PackedByteArray()
	bytes.resize(n * 2)
	for i in n:
		bytes.encode_s16(i * 2, int(s[i] * 32767.0))
	var w := AudioStreamWAV.new()
	w.format = AudioStreamWAV.FORMAT_16_BITS
	w.mix_rate = SYNTH_RATE
	w.stereo = false
	w.data = bytes
	if loop:
		w.loop_mode = AudioStreamWAV.LOOP_FORWARD
		w.loop_begin = 0
		w.loop_end = n
	return w


# ---- models ---------------------------------------------------------------

## returns an instanced model, or null when missing (callers then build their placeholder)
func model(key: String) -> Node3D:
	var e := entry("models", key)
	var path: String = e.get("path", "")
	if not _have_file(path):
		_warn("model '%s' missing (%s), using placeholder boxes" % [key, path if path != "" else "no entry"])
		return null
	var node: Node3D = null
	if ResourceLoader.exists(path):
		var ps = load(path)
		if ps is PackedScene:
			node = ps.instantiate()
	else:
		var doc := GLTFDocument.new()
		var state := GLTFState.new()
		if doc.append_from_file(path, state) == OK:
			node = doc.generate_scene(state)
	if node and e.has("scale"):
		node.scale = Vector3.ONE * float(e.scale)
	return node


# ---- fonts ----------------------------------------------------------------

func font(key: String) -> Font:
	var ck := "font:" + key
	if _cache.has(ck):
		return _cache[ck]
	var path: String = entry("fonts", key).get("path", "")
	var f: Font = null
	if ResourceLoader.exists(path):
		f = load(path) as Font
	elif FileAccess.file_exists(path):
		var ff := FontFile.new()
		if ff.load_dynamic_font(path) == OK:
			f = ff
	if f == null:
		_warn("font '%s' missing (%s), using the default font" % [key, path])
		f = ThemeDB.fallback_font
	_cache[ck] = f
	return f
