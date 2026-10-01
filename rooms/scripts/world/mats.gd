class_name Mats
## shared materials, built once from manifest textures. world space triplanar mapping, so
## textures line up across separate wall pieces and keep the same real-world size.
## the scale column = how many texture repeats per meter.

static var _cache := {}

# key -> [texture key or "", tint, roughness, metallic, triplanar scale, emission]
const DEFS := {
	"wall": ["wall", Color.WHITE, 0.95, 0.0, 0.5, 0.0],
	"carpet_red": ["carpet_red", Color.WHITE, 1.0, 0.0, 0.8, 0.0],
	"carpet_blue": ["carpet_blue", Color.WHITE, 1.0, 0.0, 0.8, 0.0],
	"ceiling": ["ceiling", Color.WHITE, 0.95, 0.0, 0.8333, 0.0],
	"trim": ["trim", Color.WHITE, 0.6, 0.0, 1.0, 0.0],
	"door": ["door", Color.WHITE, 0.7, 0.0, 0.8, 0.0],
	"frame": ["", Color(0.85, 0.85, 0.82), 0.6, 0.0, 1.0, 0.0],
	"locker": ["locker", Color.WHITE, 0.45, 0.6, 0.8, 0.0],
	"wood": ["wood", Color.WHITE, 0.75, 0.0, 0.8, 0.0],
	"metal": ["", Color(0.55, 0.57, 0.6), 0.4, 0.7, 1.0, 0.0],
	"dark": ["", Color(0.12, 0.12, 0.13), 0.8, 0.0, 1.0, 0.0],
	"plastic": ["", Color(0.92, 0.92, 0.9), 0.5, 0.0, 1.0, 0.0],
	"fabric": ["", Color(0.1, 0.1, 0.11), 1.0, 0.0, 1.0, 0.0],
	"cubicle": ["", Color(0.45, 0.5, 0.55), 1.0, 0.0, 1.0, 0.0],
	"plant": ["", Color(0.18, 0.45, 0.2), 0.9, 0.0, 1.0, 0.0],
	"pot": ["", Color(0.55, 0.32, 0.22), 0.9, 0.0, 1.0, 0.0],
	"sign": ["", Color(0.95, 0.8, 0.2), 0.6, 0.0, 1.0, 0.0],
	"glass": ["", Color(0.7, 0.85, 0.9, 0.25), 0.1, 0.0, 1.0, 0.0],
	"light_panel": ["", Color(1, 1, 0.96), 0.5, 0.0, 1.0, 2.0],
	"light_off": ["", Color(0.6, 0.6, 0.6), 0.5, 0.0, 1.0, 0.0],
	"skylight": ["", Color(0.75, 0.88, 1.0), 0.5, 0.0, 1.0, 3.0],
	"exit_glow": ["", Color(1.0, 0.9, 0.6), 0.5, 0.0, 1.0, 1.5],
	"void_door": ["", Color(1.0, 0.95, 0.75), 0.5, 0.0, 1.0, 4.0],
	"white_glow": ["", Color(0.95, 0.95, 1.0), 0.5, 0.0, 1.0, 1.0],
	"paper": ["", Color(0.96, 0.95, 0.9), 0.9, 0.0, 1.0, 0.0],
	"gold": ["", Color(1.0, 0.8, 0.2), 0.3, 1.0, 1.0, 0.4],
}


static func get_mat(key: String) -> StandardMaterial3D:
	if _cache.has(key):
		return _cache[key]
	var d: Array = DEFS.get(key, ["", Color.MAGENTA, 1.0, 0.0, 1.0, 0.0])
	var m := StandardMaterial3D.new()
	if d[0] != "":
		m.albedo_texture = Assets.texture(d[0])
		m.uv1_triplanar = true
		m.uv1_world_triplanar = true
		m.uv1_scale = Vector3.ONE * float(d[4])
		m.uv1_triplanar_sharpness = 6.0  # no smeared blending on box edges
		m.texture_filter = BaseMaterial3D.TEXTURE_FILTER_LINEAR_WITH_MIPMAPS_ANISOTROPIC
	m.albedo_color = d[1]
	m.roughness = d[2]
	m.metallic = d[3]
	if d[1].a < 1.0:
		m.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	if d[5] > 0.0:
		m.emission_enabled = true
		m.emission = d[1]
		m.emission_energy_multiplier = d[5]
	_cache[key] = m
	return m


static func unshaded(color: Color) -> StandardMaterial3D:
	var m := StandardMaterial3D.new()
	m.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
	m.albedo_color = color
	if color.a < 1.0:
		m.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	return m
