class_name Prewarm
## builds everything that's generated on first use (procedural textures, materials, plant
## meshes, item models, entity glow sprites, placeholder sounds) up front, so opening a
## door never stutters because something new appeared. cached, so it's instant the 2nd time.

static var _done := false


static func run() -> void:
	if _done:
		return
	_done = true
	for key in Mats.DEFS:
		Mats.get_mat(key)
	for kind in ["fern", "snake", "bush"]:
		for v in 3:
			PropModels.plant(kind, v).free()
	PropModels.office_chair(Props.SEAT_H).free()
	PropModels.couch().free()
	PropModels.fridge().free()
	for item in ["flashlight", "shakelight", "battery", "bandage", "vitamins", "gold"]:
		ItemModels.build(item).free()
	for k in ["a60_face_1", "a60_face_2", "a60_face_3", "a60b_face", "a120_face", "a200_face"]:
		Assets.glow_texture(k)
	for section in ["sounds", "music"]:
		for key in Assets.manifest.get(section, {}):
			Assets.sound(key)
