class_name Faces
## pictures for the newer entities, drawn in code (no image files needed)

static var _cache := {}


static func _img(w: int, h: int) -> Image:
	var img := Image.create(w, h, false, Image.FORMAT_RGBA8)
	img.fill(Color(0, 0, 0, 0))
	return img


static func _done(key: String, img: Image) -> Texture2D:
	_cache[key] = ImageTexture.create_from_image(img)
	return _cache[key]


## c-15: a pigeon
static func pigeon_tex() -> Texture2D:
	if _cache.has("pigeon"):
		return _cache["pigeon"]
	var img := _img(32, 32)
	for y in 32:
		for x in 32:
			if Vector2((x - 15) / 1.4, y - 18).length() < 8:
				img.set_pixel(x, y, Color(0.45, 0.47, 0.52))
			if Vector2(x - 22, y - 10).length() < 4.5:
				img.set_pixel(x, y, Color(0.35, 0.45, 0.42))
	img.set_pixel(24, 9, Color.BLACK)
	for x in range(26, 29):
		img.set_pixel(x, 11, Color(0.9, 0.6, 0.2))
	return _done("pigeon", img)


## the complex anomalies: a long pale face with black eyes, dressed for where it lives
static func anomaly_tex(theme: String) -> Texture2D:
	var key := "anomaly_" + theme
	if _cache.has(key):
		return _cache[key]
	var w := 64
	var h := 128
	var img := _img(w, h)
	var body: Color = {"hospital": Color(0.35, 0.6, 0.55), "hotel": Color(0.55, 0.08, 0.1), "apartment": Color(0.3, 0.28, 0.32)}.get(theme, Color(0.3, 0.3, 0.3))
	# a too-tall body
	for y in range(40, 128):
		var half := 9 + int((y - 40) * 0.08)
		for x in range(32 - half, 32 + half):
			img.set_pixel(x, y, body)
	# arms down to the knees
	for y in range(44, 110):
		for x in [20, 21, 42, 43]:
			img.set_pixel(x, y, body.darkened(0.3))
	# the head: long and pale
	for y in range(4, 42):
		for x in range(18, 46):
			if Vector2((x - 32) / 13.0, (y - 23) / 19.0).length() < 1.0:
				img.set_pixel(x, y, Color(0.88, 0.86, 0.8))
	for e in [24, 37]:
		for y in range(16, 26):
			for x in range(e, e + 4):
				img.set_pixel(x, y, Color(0.02, 0.0, 0.0))
	for x in range(25, 40):
		img.set_pixel(x, 33 + int(absf(x - 32) * 0.2), Color(0.3, 0.0, 0.0))
	match theme:
		"hospital":
			# surgical mask and a scalpel
			for y in range(28, 38):
				for x in range(21, 43):
					if Vector2((x - 32) / 13.0, (y - 23) / 19.0).length() < 1.0:
						img.set_pixel(x, y, Color(0.6, 0.85, 0.8))
			for y in range(100, 116):
				img.set_pixel(44, y, Color(0.85, 0.85, 0.9))
			for y in range(96, 112):
				img.set_pixel(20, y, Color(0.7, 0.05, 0.05))  # red gloves
		"hotel":
			# a little bellboy cap
			for y in range(0, 8):
				for x in range(22, 42):
					img.set_pixel(x, y, Color(0.6, 0.05, 0.08))
			for x in range(22, 42):
				img.set_pixel(x, 8, Color(0.9, 0.75, 0.2))
	return _done(key, img)


## a-10: a hunched, heavy figure with one huge boot up, ready to kick
static func a10_tex() -> Texture2D:
	if _cache.has("a10"):
		return _cache["a10"]
	var img := _img(96, 128)
	var body := Color(0.16, 0.14, 0.13)
	# torso, leaning forward
	for y in range(30, 92):
		var half := 16 + int((y - 30) * 0.12)
		var lean := int((92 - y) * 0.15)
		for x in range(40 - half + lean, 40 + half + lean):
			img.set_pixel(clampi(x, 0, 95), y, body)
	# head
	for y in range(6, 36):
		for x in range(30, 66):
			if Vector2((x - 48) / 16.0, (y - 21) / 14.0).length() < 1.0:
				img.set_pixel(x, y, body.darkened(0.2))
	# angry white eyes
	for e: Vector2 in [Vector2(41, 19), Vector2(55, 19)]:
		for y in range(-3, 3):
			for x in range(-4, 4):
				if y > -2 - (x if e.x < 48 else -x) * 0.4:
					img.set_pixel(int(e.x) + x, int(e.y) + y, Color(0.95, 0.95, 0.9))
	# standing leg
	for y in range(90, 126):
		for x in range(26, 38):
			img.set_pixel(x, y, body)
	for y in range(118, 127):
		for x in range(20, 42):
			img.set_pixel(x, y, Color(0.05, 0.05, 0.05))
	# the kicking leg, out to the side, with a huge boot
	for i in 36:
		var cx := 50 + i
		var cy := 88 + int(i * 0.35)
		for d in range(-5, 6):
			img.set_pixel(clampi(cx, 0, 95), clampi(cy + d, 0, 127), body)
	for y in range(92, 118):
		for x in range(78, 96):
			img.set_pixel(x, y, Color(0.05, 0.05, 0.05))
	for x in range(78, 96):
		img.set_pixel(x, 116, Color(0.5, 0.35, 0.2))
	return _done("a10", img)


## a-20 "the chameleon": a face the colour of a door, with a scribbled-on grin
static func a20_tex() -> Texture2D:
	if _cache.has("a20"):
		return _cache["a20"]
	var img := _img(64, 96)
	var wood := Color(0.42, 0.26, 0.14)
	for y in 96:
		for x in 64:
			var f := Vector2((x - 32) / 26.0, (y - 46) / 42.0).length()
			if f < 1.0:
				var grain := sin(y * 0.9 + sin(x * 0.2) * 2.0) * 0.05
				img.set_pixel(x, y, wood.lightened(grain + 0.05))
	# big black eyes
	for e: Vector2 in [Vector2(22, 34), Vector2(42, 34)]:
		for y in range(-7, 8):
			for x in range(-5, 6):
				if Vector2(x / 5.0, y / 7.0).length() < 1.0:
					img.set_pixel(int(e.x) + x, int(e.y) + y, Color(0.02, 0.02, 0.02))
	# scribbled grin: zigzag black marker lines
	var rng := RandomNumberGenerator.new()
	rng.seed = 20
	for pass_i in 3:
		var y0 := 60 + pass_i * 4
		for x in range(12, 52):
			var y := y0 + int(sin(x * 1.3 + pass_i) * 3.0) + rng.randi_range(-1, 1)
			for t in 2:
				img.set_pixel(x, clampi(y + t, 0, 95), Color(0.03, 0.03, 0.03))
	return _done("a20", img)
