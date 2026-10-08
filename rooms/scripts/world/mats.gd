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
	"wood_old": ["wood", Color(0.62, 0.52, 0.42), 0.9, 0.0, 1.2, 0.0],
	"wood_dark": ["wood", Color(0.38, 0.29, 0.22), 0.9, 0.0, 1.2, 0.0],
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
	# office dressing
	"poster_red": ["", Color(0.62, 0.18, 0.16), 0.8, 0.0, 1.0, 0.0],
	"poster_blue": ["", Color(0.2, 0.32, 0.55), 0.8, 0.0, 1.0, 0.0],
	"poster_green": ["", Color(0.24, 0.45, 0.3), 0.8, 0.0, 1.0, 0.0],
	"poster_yellow": ["", Color(0.85, 0.7, 0.25), 0.8, 0.0, 1.0, 0.0],
	"poster_grey": ["", Color(0.55, 0.55, 0.58), 0.8, 0.0, 1.0, 0.0],
	"exit_green": ["", Color(0.1, 0.85, 0.35), 0.5, 0.0, 1.0, 1.5],
	"extinguisher": ["", Color(0.78, 0.08, 0.06), 0.35, 0.2, 1.0, 0.0],
	"cabinet": ["", Color(0.6, 0.62, 0.62), 0.45, 0.5, 1.0, 0.0],
	"stain": ["", Color(0.12, 0.08, 0.06), 1.0, 0.0, 1.0, 0.0],
	"cork": ["", Color(0.66, 0.5, 0.32), 1.0, 0.0, 1.0, 0.0],
	"rack": ["", Color(0.1, 0.11, 0.13), 0.5, 0.6, 1.0, 0.0],
	"led_blue": ["", Color(0.3, 0.6, 1.0), 0.5, 0.0, 1.0, 2.0],
	"led_green": ["", Color(0.3, 1.0, 0.45), 0.5, 0.0, 1.0, 2.0],
	"led_red": ["", Color(1.0, 0.25, 0.2), 0.5, 0.0, 1.0, 2.0],
	# the wires
	"concrete": ["concrete", Color.WHITE, 0.95, 0.0, 0.5, 0.0],
	"concrete_floor": ["concrete_floor", Color.WHITE, 1.0, 0.0, 0.5, 0.0],
	"pipe": ["", Color(0.33, 0.35, 0.38), 0.5, 0.6, 1.0, 0.0],
	"rust": ["", Color(0.42, 0.24, 0.14), 0.9, 0.3, 1.0, 0.0],
	"wire": ["", Color(0.05, 0.05, 0.06), 0.6, 0.0, 1.0, 0.0],
	"copper": ["", Color(0.85, 0.5, 0.25), 0.35, 1.0, 1.0, 0.6],
	"bulb": ["", Color(1.0, 0.72, 0.38), 0.5, 0.0, 1.0, 2.5],
	"caution": ["", Color(0.95, 0.75, 0.1), 0.7, 0.0, 1.0, 0.0],
	"vine": ["", Color(0.18, 0.36, 0.14), 0.9, 0.0, 1.0, 0.0],
	"crack": ["", Color(0.02, 0.02, 0.02), 1.0, 0.0, 1.0, 0.0],
	"switch_off": ["", Color(0.85, 0.1, 0.1), 0.5, 0.0, 1.0, 1.2],
	"switch_on": ["", Color(0.15, 0.95, 0.25), 0.5, 0.0, 1.0, 1.6],
	"door_metal": ["", Color(0.38, 0.4, 0.43), 0.5, 0.6, 1.0, 0.0],
	"screen": ["", Color(0.2, 0.9, 0.4), 0.4, 0.0, 1.0, 1.2],
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
