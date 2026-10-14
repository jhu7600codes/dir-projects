class_name ItemSlots
extends Control
## the inventory bar: one square per item with a drawn icon, the charge or count in the
## corner, the selected one outlined. tap a square (touch) or press its number to hold it.

const SLOT := 58.0
const GAP := 8.0
const ORDER := ["flashlight", "shakelight", "bandage", "vitamins", "stardevice", "starjug", "starhelp", "battery", "gold"]

var player: Player
var _rects := {}  # item -> Rect2 in screen space


func _ready() -> void:
	mouse_filter = Control.MOUSE_FILTER_IGNORE
	set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)


func _process(_d: float) -> void:
	queue_redraw()


func _shown() -> Array:
	var out := []
	var inv := player.inventory
	for it in ORDER:
		match it:
			"flashlight": if inv.has_flashlight: out.append(it)
			"shakelight": if inv.has_shakelight: out.append(it)
			"stardevice", "starjug", "starhelp": if player.owns(it): out.append(it)
			_: out.append(it)
	return out


## which item square is at a screen point ("" if none). used by the touch controls
func item_at(p: Vector2) -> String:
	for k in _rects:
		if _rects[k].has_point(p) and k in Player.ITEMS:
			return k
	return ""


func _draw() -> void:
	if player == null:
		return
	var items := _shown()
	var vs := get_viewport_rect().size
	var total := items.size() * SLOT + (items.size() - 1) * GAP
	var x := (vs.x - total) / 2.0
	var y := vs.y - SLOT - 12.0
	_rects.clear()
	var font := ThemeDB.fallback_font
	for it in items:
		var r := Rect2(x, y, SLOT, SLOT)
		_rects[it] = r
		var sel: bool = player.selected == it
		draw_rect(r, Color(0, 0, 0, 0.55))
		draw_rect(r, Color(1.0, 0.85, 0.4) if sel else Color(1, 1, 1, 0.25), false, 3.0 if sel else 1.5)
		_icon(it, r.get_center())
		# the lights get a charge bar along the bottom of the square instead of a number
		if it in ["flashlight", "shakelight", "starhelp"]:
			var f: float = (player.inventory.battery if it == "flashlight" else player.inventory.shake_charge) / 100.0
			if it == "starhelp":
				f = float(Save.data.get("starhelp_charge", 0.0)) / 100.0
			var bar := Rect2(r.position.x + 5, r.end.y - 9, SLOT - 10, 5)
			draw_rect(bar, Color(0, 0, 0, 0.7))
			var col := Color(1.0, 0.85, 0.35) if it == "flashlight" else (Color(0.6, 0.8, 1.0) if it == "starhelp" else Color(0.45, 1.0, 0.5))
			if f < 0.2:
				col = Color(1.0, 0.3, 0.25)
			draw_rect(Rect2(bar.position, Vector2(bar.size.x * clampf(f, 0.0, 1.0), bar.size.y)), col)
		var corner := _corner(it)
		if corner != "":
			draw_string(font, Vector2(r.position.x + 4, r.end.y - 5), corner, HORIZONTAL_ALIGNMENT_RIGHT, SLOT - 8, 13, Color(1, 1, 1, 0.9))
		var idx := Player.ITEMS.find(it)
		if idx >= 0 and not Settings.use_touch():
			draw_string(font, Vector2(r.position.x + 4, r.position.y + 14), str(idx + 1), HORIZONTAL_ALIGNMENT_LEFT, -1, 12, Color(1, 1, 1, 0.5))
		x += SLOT + GAP


func _corner(it: String) -> String:
	var inv := player.inventory
	match it:
		"bandage": return str(inv.bandages)
		"vitamins": return str(inv.vitamins)
		"battery": return str(inv.batteries)
		"gold": return str(int(Save.data.gold))
		"starjug": return str(int(Save.data.get("starjug", 0)))
		"stardevice": return "on" if Save.data.get("stardevice_on", true) else "off"
	return ""


func _icon(it: String, c: Vector2) -> void:
	var dim := 1.0
	var inv := player.inventory
	if (it == "bandage" and inv.bandages == 0) or (it == "vitamins" and inv.vitamins == 0) or (it == "battery" and inv.batteries == 0):
		dim = 0.35
	var m := Color(1, 1, 1, dim)
	match it:
		"flashlight":
			_poly([c + Vector2(-16, -5), c + Vector2(6, -5), c + Vector2(6, 5), c + Vector2(-16, 5)], Color(0.15, 0.15, 0.17) * m)
			_poly([c + Vector2(6, -8), c + Vector2(15, -10), c + Vector2(15, 10), c + Vector2(6, 8)], Color(0.2, 0.2, 0.22) * m)
			draw_rect(Rect2(c + Vector2(14, -9), Vector2(3, 18)), Color(1.0, 0.95, 0.7) * m)
			draw_rect(Rect2(c + Vector2(-4, -7), Vector2(4, 2)), Color(0.7, 0.15, 0.15) * m)
		"shakelight":
			_poly([c + Vector2(-16, -5), c + Vector2(6, -5), c + Vector2(6, 5), c + Vector2(-16, 5)], Color(0.2, 0.55, 0.25) * m)
			_poly([c + Vector2(6, -8), c + Vector2(15, -10), c + Vector2(15, 10), c + Vector2(6, 8)], Color(0.1, 0.3, 0.12) * m)
			draw_rect(Rect2(c + Vector2(14, -9), Vector2(3, 18)), Color(0.6, 1.0, 0.6) * m)
			for i in 3:
				draw_line(c + Vector2(-12 + i * 6, -5), c + Vector2(-12 + i * 6, 5), Color(0.1, 0.3, 0.12) * m, 2)
		"bandage":
			draw_circle(c + Vector2(-3, 0), 13, Color(0.95, 0.93, 0.88) * m)
			draw_circle(c + Vector2(-3, 0), 5, Color(0.75, 0.72, 0.66) * m)
			draw_rect(Rect2(c + Vector2(4, 8), Vector2(14, 6)), Color(0.95, 0.93, 0.88) * m)
		"vitamins":
			draw_rect(Rect2(c + Vector2(-10, -8), Vector2(20, 22)), Color(0.95, 0.55, 0.1) * m)
			draw_rect(Rect2(c + Vector2(-10, -1), Vector2(20, 8)), Color(0.96, 0.96, 0.92) * m)
			draw_rect(Rect2(c + Vector2(-8, -15), Vector2(16, 7)), Color(0.95, 0.95, 0.95) * m)
		"battery":
			draw_rect(Rect2(c + Vector2(-7, -10), Vector2(14, 24)), Color(0.12, 0.12, 0.12) * m)
			draw_rect(Rect2(c + Vector2(-7, -14), Vector2(14, 6)), Color(0.85, 0.5, 0.2) * m)
			draw_rect(Rect2(c + Vector2(-2, -17), Vector2(4, 3)), Color(0.75, 0.75, 0.75) * m)
		"stardevice":
			draw_rect(Rect2(c + Vector2(-11, -14), Vector2(22, 28)), Color(0.12, 0.13, 0.18))
			draw_rect(Rect2(c + Vector2(-8, -11), Vector2(16, 13)), Color(0.15, 0.3, 0.5))
			_star(c + Vector2(0, -4), 5.0, Color(0.7, 0.88, 1.0))
			draw_circle(c + Vector2(0, 8), 3, Color(0.6, 0.8, 1.0) if Save.data.get("stardevice_on", true) else Color(0.3, 0.3, 0.3))
		"starjug":
			draw_colored_polygon(PackedVector2Array([c + Vector2(-5, -15), c + Vector2(5, -15), c + Vector2(5, -9), c + Vector2(11, -4), c + Vector2(11, 14), c + Vector2(-11, 14), c + Vector2(-11, -4), c + Vector2(-5, -9)]), Color(0.55, 0.7, 0.9, 0.55))
			draw_rect(Rect2(c + Vector2(-10, 0), Vector2(20, 13)), Color(0.35, 0.55, 1.0, 0.9))
			_star(c + Vector2(-3, 6), 3.0, Color(1, 1, 1))
			_star(c + Vector2(5, 3), 2.0, Color(1, 1, 1))
		"starhelp":
			draw_rect(Rect2(c + Vector2(-12, -12), Vector2(24, 24)), Color(0.12, 0.13, 0.18))
			_star(c + Vector2(-2, -2), 6.0, Color(0.6, 0.8, 1.0))
			draw_rect(Rect2(c + Vector2(6, 2), Vector2(5, 9)), Color(0.85, 0.5, 0.2))
		"gold":
			draw_circle(c + Vector2(-5, 3), 10, Color(0.85, 0.62, 0.15))
			draw_circle(c + Vector2(4, -3), 10, Color(1.0, 0.78, 0.25))
			draw_circle(c + Vector2(4, -3), 6, Color(1.0, 0.86, 0.45))


func _poly(pts: Array, col: Color) -> void:
	draw_colored_polygon(PackedVector2Array(pts), col)


func _star(c: Vector2, r: float, col: Color) -> void:
	var pts := PackedVector2Array()
	for i in 10:
		var rr := r if i % 2 == 0 else r * 0.45
		var a := -PI / 2 + i * PI / 5
		pts.append(c + Vector2(cos(a), sin(a)) * rr)
	draw_colored_polygon(pts, col)
