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
