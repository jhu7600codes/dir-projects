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


## a-10: a hole in the world. black middle, a wobbly purple rim
static func void_tex() -> Texture2D:
	if _cache.has("void"):
		return _cache["void"]
	var n := 128
	var img := _img(n, n)
	var c := Vector2(n / 2.0, n / 2.0)
	for y in n:
		for x in n:
			var p := Vector2(x, y) - c
			var a := p.angle()
			var r := p.length() / (n / 2.0)
			var edge := 0.82 + 0.08 * sin(a * 7.0) + 0.04 * sin(a * 13.0 + 1.0)
			if r < edge:
				var rim := clampf((r - edge + 0.18) / 0.18, 0.0, 1.0)
				img.set_pixel(x, y, Color(0.35 * rim, 0.0, 0.5 * rim, 1.0))
			elif r < edge + 0.12:
				var f := 1.0 - (r - edge) / 0.12
				img.set_pixel(x, y, Color(0.5, 0.1, 0.8, f * 0.7))
	return _done("void", img)


## lurkmask: a pale mask with dark eye holes and a too-wide grin
static func mask_tex() -> Texture2D:
	if _cache.has("mask"):
		return _cache["mask"]
	var w := 96
	var h := 128
	var img := _img(w, h)
	var c := Vector2(w / 2.0, h / 2.0)
	for y in h:
		for x in w:
			var p := (Vector2(x, y) - c) / Vector2(w / 2.0, h / 2.0)
			if p.length() < 0.95:
				img.set_pixel(x, y, Color(0.92, 0.88, 0.8))
	for e: Vector2 in [Vector2(-0.38, -0.2), Vector2(0.38, -0.2)]:
		for y in h:
			for x in w:
				var p := (Vector2(x, y) - c) / Vector2(w / 2.0, h / 2.0) - e
				if (p * Vector2(1.0, 0.7)).length() < 0.17:
					img.set_pixel(x, y, Color(0.02, 0.0, 0.0))
	for x in range(18, w - 18):
		var t := float(x - 18) / float(w - 36)
		var yy := int(h * 0.68 + sin(t * PI) * 12.0)
		for k in 4:
			img.set_pixel(x, yy + k, Color(0.15, 0.0, 0.0))
		if x % 7 == 0:
			for k in 6:
				img.set_pixel(x, yy - 3 + k, Color(0.15, 0.0, 0.0))
	return _done("mask", img)


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
