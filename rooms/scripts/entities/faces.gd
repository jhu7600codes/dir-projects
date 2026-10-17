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
