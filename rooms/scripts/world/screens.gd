class_name Screens
## what's on the screens in the office: computer monitors (desktop, spreadsheet, email,
## a crash screen, a screensaver, no signal), the atm and the arcade cabinet, plus the sky
## you see through the lobby skylight. all drawn here pixel by pixel (our own art), once,
## and shared: one material per design, so they merge with the room like everything else.

const W := 128
const H := 80
## designs a working office shows, and the few that show up in the abandoned one
const POWERED := ["desktop", "desktop", "sheet", "sheet", "email", "email", "error"]
const ABANDONED := ["saver", "nosignal", "desktop", "error"]

static var _mats := {}

## a tiny 3x5 pixel font for the few words on the screens
const FONT := {
	"A": ["010", "101", "111", "101", "101"], "B": ["110", "101", "110", "101", "110"],
	"C": ["011", "100", "100", "100", "011"], "D": ["110", "101", "101", "101", "110"],
	"E": ["111", "100", "110", "100", "111"], "G": ["011", "100", "101", "101", "011"],
	"I": ["111", "010", "010", "010", "111"], "L": ["100", "100", "100", "100", "111"],
	"M": ["101", "111", "111", "101", "101"], "N": ["101", "111", "111", "111", "101"],
	"O": ["010", "101", "101", "101", "010"], "P": ["110", "101", "110", "100", "100"],
	"R": ["110", "101", "110", "101", "101"], "S": ["011", "100", "010", "001", "110"],
	"T": ["111", "010", "010", "010", "010"], "U": ["101", "101", "101", "101", "111"],
	"W": ["101", "101", "111", "111", "101"], "Y": ["101", "101", "010", "010", "010"],
	"K": ["101", "101", "110", "101", "101"], "H": ["101", "101", "111", "101", "101"],
	"0": ["111", "101", "101", "101", "111"], "1": ["010", "110", "010", "010", "111"],
	":": ["000", "010", "000", "010", "000"], "(": ["001", "010", "010", "010", "001"],
	"!": ["010", "010", "010", "000", "010"], " ": ["000", "000", "000", "000", "000"],
}


## the glowing face of a screen, for a quad facing +z
static func material(kind: String) -> StandardMaterial3D:
	if _mats.has(kind):
		return _mats[kind]
	var img: Image
	match kind:
		"sky":
			img = _sky()
		"off":
			img = _off()
		_:
			img = Image.create(W, H, false, Image.FORMAT_RGB8)
			match kind:
				"desktop": _desktop(img)
				"sheet": _sheet(img)
				"email": _email(img)
				"error": _error(img)
				"saver": _saver(img)
				"nosignal": _nosignal(img)
				"atm": _atm(img)
				"arcade": _arcade(img)
	var m := StandardMaterial3D.new()
	m.albedo_texture = ImageTexture.create_from_image(img)
	if kind == "off":
		# a dark glassy screen that catches a little light
		m.roughness = 0.12
		m.metallic = 0.2
	else:
		m.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
		m.texture_filter = BaseMaterial3D.TEXTURE_FILTER_LINEAR if kind == "sky" else BaseMaterial3D.TEXTURE_FILTER_NEAREST
	_mats[kind] = m
	return m


## a flat screen face (QuadMesh) of the given size, facing +z in parent space
static func quad(parent: Node3D, size: Vector2, pos: Vector3, kind: String, basis := Basis()) -> MeshInstance3D:
	var mi := MeshInstance3D.new()
	var qm := QuadMesh.new()
	qm.size = size
	mi.mesh = qm
	mi.material_override = material(kind)
	mi.transform = Transform3D(basis, pos)
	parent.add_child(mi)
	return mi


static func pick(rng: RandomNumberGenerator, powered: bool) -> String:
	if powered:
		return POWERED[rng.randi() % POWERED.size()]
	if rng.randf() < 0.72:
		return "off"
	return ABANDONED[rng.randi() % ABANDONED.size()]


# ---- drawing helpers ----

static func _rect(img: Image, x: int, y: int, w: int, h: int, c: Color) -> void:
	img.fill_rect(Rect2i(x, y, w, h), c)


static func _text(img: Image, s: String, x: int, y: int, c: Color, scale := 1) -> void:
	for ch in s:
		var g: Array = FONT.get(ch, FONT[" "])
		for row in 5:
			for col in 3:
				if (g[row] as String)[col] == "1":
					_rect(img, x + col * scale, y + row * scale, scale, scale, c)
		x += 4 * scale


## lines of fake text: little bars of random length
static func _lines(img: Image, rng: RandomNumberGenerator, x: int, y: int, w: int, rows: int, gap: int, c: Color) -> void:
	for r in rows:
		var len := rng.randi_range(w / 3, w)
		var cx := x
		while cx < x + len:
			var word := rng.randi_range(3, 9)
			_rect(img, cx, y + r * gap, mini(word, x + len - cx), 1, c)
			cx += word + 2


static func _window(img: Image, x: int, y: int, w: int, h: int, title: Color) -> void:
	_rect(img, x + 2, y + 2, w, h, Color(0, 0, 0, 1).lerp(Color.BLACK, 0.5))
	_rect(img, x, y, w, h, Color(0.94, 0.94, 0.93))
	_rect(img, x, y, w, 6, title)
	for k in 3:
		_rect(img, x + w - 6 - k * 5, y + 1, 4, 4, Color(0.95, 0.95, 0.95) if k > 0 else Color(0.85, 0.25, 0.2))


# ---- the designs ----

static func _desktop(img: Image) -> void:
	var rng := RandomNumberGenerator.new()
	rng.seed = 11
	for y in H:
		_rect(img, 0, y, W, 1, Color(0.1, 0.42, 0.62).lerp(Color(0.05, 0.2, 0.38), float(y) / H))
	# icons down the left
	var cols := [Color(0.95, 0.85, 0.35), Color(0.9, 0.9, 0.92), Color(0.4, 0.75, 0.4), Color(0.85, 0.4, 0.35)]
	for i in 4:
		_rect(img, 4, 4 + i * 15, 9, 8, cols[i])
		_rect(img, 3, 13 + i * 15, 11, 1, Color(0.92, 0.95, 1.0))
	_window(img, 26, 10, 78, 50, Color(0.18, 0.38, 0.75))
	_lines(img, rng, 30, 22, 66, 8, 4, Color(0.25, 0.25, 0.3))
	# taskbar, start button, clock
	_rect(img, 0, H - 8, W, 8, Color(0.75, 0.76, 0.78))
	_rect(img, 1, H - 7, 18, 6, Color(0.25, 0.6, 0.3))
	_rect(img, 22, H - 7, 26, 6, Color(0.88, 0.88, 0.9))
	_text(img, "10:00", W - 22, H - 7, Color(0.15, 0.15, 0.18))


static func _sheet(img: Image) -> void:
	var rng := RandomNumberGenerator.new()
	rng.seed = 22
	img.fill(Color(0.98, 0.98, 0.97))
	_rect(img, 0, 0, W, 7, Color(0.13, 0.48, 0.27))
	_rect(img, 0, 7, W, 5, Color(0.88, 0.9, 0.88))
	for x in range(0, W, 16):
		_rect(img, x, 12, 1, H - 12, Color(0.82, 0.83, 0.84))
	for y in range(12, H, 6):
		_rect(img, 0, y, W, 1, Color(0.82, 0.83, 0.84))
	for y in range(14, H - 2, 6):
		for x in range(2, 64, 16):
			if rng.randf() < 0.8:
				_rect(img, x, y + 1, rng.randi_range(4, 12), 2, Color(0.2, 0.2, 0.24))
	# a bar chart on the right
	_rect(img, 72, 16, 52, 40, Color(1, 1, 1))
	_rect(img, 72, 16, 52, 1, Color(0.7, 0.7, 0.72))
	for i in 6:
		var bh := rng.randi_range(8, 34)
		_rect(img, 76 + i * 8, 54 - bh, 5, bh, Color(0.2, 0.5, 0.85) if i % 2 == 0 else Color(0.95, 0.6, 0.2))


static func _email(img: Image) -> void:
	var rng := RandomNumberGenerator.new()
	rng.seed = 33
	img.fill(Color(0.96, 0.96, 0.97))
	_rect(img, 0, 0, W, 9, Color(0.16, 0.4, 0.78))
	_rect(img, 3, 2, 14, 5, Color(0.95, 0.95, 1.0))
	_rect(img, 0, 9, 44, H - 9, Color(0.9, 0.91, 0.93))
	for i in 9:
		var y := 11 + i * 7
		if i == 2:
			_rect(img, 0, y - 1, 44, 7, Color(0.75, 0.84, 0.97))
		_rect(img, 3, y + 1, rng.randi_range(14, 30), 2 if i < 3 else 1, Color(0.15, 0.15, 0.2))
		_rect(img, 3, y + 4, rng.randi_range(18, 38), 1, Color(0.55, 0.55, 0.6))
	_rect(img, 44, 9, 1, H - 9, Color(0.75, 0.76, 0.8))
	_rect(img, 50, 13, 50, 3, Color(0.12, 0.12, 0.16))
	_rect(img, 50, 19, 30, 1, Color(0.5, 0.5, 0.55))
	_lines(img, rng, 50, 26, 72, 12, 4, Color(0.25, 0.25, 0.3))


static func _error(img: Image) -> void:
	img.fill(Color(0.1, 0.32, 0.72))
	_text(img, ":(", 8, 8, Color.WHITE, 4)
	var rng := RandomNumberGenerator.new()
	rng.seed = 44
	_lines(img, rng, 8, 36, 100, 5, 5, Color(0.9, 0.93, 1.0))
	_text(img, "STOP: 0", 8, 64, Color.WHITE)


static func _saver(img: Image) -> void:
	img.fill(Color(0.01, 0.01, 0.02))
	var rng := RandomNumberGenerator.new()
	rng.seed = 55
	for i in 40:
		var c := Color(0.6, 0.65, 0.8) * rng.randf_range(0.3, 1.0)
		_rect(img, rng.randi_range(0, W - 1), rng.randi_range(0, H - 1), 1, 1, c)
	_text(img, "ROOMS", 38, 34, Color(0.25, 0.85, 0.55), 2)


static func _nosignal(img: Image) -> void:
	img.fill(Color(0.02, 0.04, 0.16))
	_rect(img, 26, 28, 76, 24, Color(0.05, 0.08, 0.3))
	_rect(img, 26, 28, 76, 1, Color(0.6, 0.65, 0.9))
	_rect(img, 26, 51, 76, 1, Color(0.6, 0.65, 0.9))
	_text(img, "NO SIGNAL", 29, 35, Color(0.85, 0.88, 1.0), 2)


static func _atm(img: Image) -> void:
	img.fill(Color(0.04, 0.22, 0.14))
	_rect(img, 0, 0, W, 12, Color(0.08, 0.38, 0.24))
	_text(img, "WELCOME", 34, 3, Color(0.92, 1.0, 0.92), 2)
	for i in 3:
		_rect(img, 6, 22 + i * 17, 46, 12, Color(0.1, 0.48, 0.3))
		_rect(img, 76, 22 + i * 17, 46, 12, Color(0.1, 0.48, 0.3))
		_rect(img, 2, 26 + i * 17, 3, 4, Color(0.9, 0.95, 0.9))
		_rect(img, W - 5, 26 + i * 17, 3, 4, Color(0.9, 0.95, 0.9))
	_text(img, "GOLD", 14, 26, Color(0.95, 0.85, 0.35))
	_text(img, "DEPOSIT", 80, 26, Color(0.92, 1.0, 0.92))


static func _arcade(img: Image) -> void:
	img.fill(Color(0.02, 0.0, 0.05))
	for y in range(0, H, 2):
		_rect(img, 0, y, W, 1, Color(0.04, 0.02, 0.09))
	_text(img, "OUTLET", 28, 8, Color(1.0, 0.85, 0.25), 3)
	_text(img, "DODGE", 34, 28, Color(1.0, 0.45, 0.2), 3)
	# a little plug running from a spark
	_rect(img, 54, 50, 14, 10, Color(0.9, 0.9, 0.92))
	_rect(img, 57, 46, 2, 4, Color(0.75, 0.75, 0.8))
	_rect(img, 63, 46, 2, 4, Color(0.75, 0.75, 0.8))
	_rect(img, 76, 52, 3, 3, Color(1.0, 1.0, 0.5))
	_rect(img, 80, 50, 2, 2, Color(1.0, 0.8, 0.3))
	_text(img, "PRESS", 46, 68, Color(0.6, 0.95, 1.0))


## a dark glass screen, slightly lighter at the top like it's catching the ceiling light
static func _off() -> Image:
	var img := Image.create(8, 8, false, Image.FORMAT_RGB8)
	for y in 8:
		_rect(img, 0, y, 8, 1, Color(0.06, 0.065, 0.075).lerp(Color(0.015, 0.017, 0.02), y / 7.0))
	return img


## the sky through the skylight: deep blue, lighter toward one side, soft clouds
static func _sky() -> Image:
	var n := 192
	var img := Image.create(n, n, false, Image.FORMAT_RGB8)
	var noise := FastNoiseLite.new()
	noise.seed = 7
	noise.noise_type = FastNoiseLite.TYPE_PERLIN
	noise.fractal_octaves = 4
	noise.frequency = 0.012
	for y in n:
		for x in n:
			var base := Color(0.22, 0.5, 0.92).lerp(Color(0.55, 0.76, 1.0), float(x + y) / (2.0 * n))
			var v := noise.get_noise_2d(x, y) * 0.5 + 0.5
			var cloud := smoothstep(0.5, 0.72, v)
			img.set_pixel(x, y, base.lerp(Color(0.97, 0.98, 1.0), cloud * 0.95))
	return img
