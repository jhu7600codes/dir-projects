class_name Vitals
extends Control
## bottom left corner: health (heart) and stamina (running figure). health hides when you're
## at full health, unless star protection is up (then the star shield shows over it).
## stamina fades out when full. status icons sit to the right: god mode (shield), speed
## boost (lightning, with a timer ring for vitamins), noclip (ghost).

const W := 240.0
const MARGIN := Vector2(26, 26)
var player: Player
var _shown_hp := 100.0
var _hp_a := 0.0
var _stamina_a := 0.0


func _ready() -> void:
	mouse_filter = Control.MOUSE_FILTER_IGNORE
	set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)


func _process(delta: float) -> void:
	if player:
		# the light part of the bar drains a moment after you get hit
		_shown_hp = move_toward(_shown_hp, player.health, delta * 40.0) if _shown_hp > player.health else player.health
		var show_hp: bool = player.health < player.max_health - 0.5 or player.star_shield > 0.0 or _shown_hp > player.health + 0.5
		_hp_a = move_toward(_hp_a, 1.0 if show_hp else 0.0, delta * 3.0)
		var show_st: bool = player.stamina < 99.0 or player.sprinting
		_stamina_a = move_toward(_stamina_a, 1.0 if show_st else 0.0, delta * 3.0)
	queue_redraw()


func _draw() -> void:
	if player == null:
		return
	var vs := get_viewport_rect().size
	var x := MARGIN.x + 22
	var y := vs.y - MARGIN.y - 12
	# stamina at the very bottom, health above it
	if _stamina_a > 0.01:
		var c := Color(0.4, 0.75, 1.0, _stamina_a)
		_runner(Vector2(x - 16, y + 4), c)
		_bar(Rect2(x, y, W, 8), player.stamina / 100.0, player.stamina / 100.0, c)
	var hy := y - 22
	if _hp_a > 0.01:
		var maxhp: float = player.max_health
		_heart(Vector2(x - 16, hy + 6), Color(0.85, 0.18, 0.2, _hp_a))
		_bar(Rect2(x, hy, W, 12), _shown_hp / maxhp, player.health / maxhp, Color(0.82, 0.16, 0.18, _hp_a))
		if player.star_shield > 0.0:
			# the star shield: a pale blue band over the health bar, and a star
			var f: float = player.star_shield / maxf(1.0, player.star_max)
			draw_rect(Rect2(Vector2(x, hy - 5), Vector2(W * f, 4)), Color(0.6, 0.8, 1.0, _hp_a))
			_star(Vector2(x + W + 14, hy + 6), Color(0.6, 0.8, 1.0, _hp_a))
	# status icons
	var ix := x + W + (40.0 if player.star_shield > 0.0 else 18.0)
	var iy := hy + 6 if _hp_a > 0.01 else y + 4
	if Game.admin and Game.admin_flags.god:
		_shield(Vector2(ix, iy))
		ix += 28
	var boost: float = player.get("_speed_boost")
	if (Game.admin and Game.admin_flags.speed) or boost > 0.0 or Game.forced_sprint:
		_bolt(Vector2(ix, iy), Color(1.0, 0.85, 0.3) if not Game.forced_sprint else Color(0.5, 0.75, 1.0))
		if boost > 0.0:
			draw_arc(Vector2(ix, iy), 13, -PI / 2, -PI / 2 + TAU * clampf(boost / 12.0, 0.0, 1.0), 24, Color(1.0, 0.85, 0.3, 0.8), 2.0)
		ix += 28
	if Game.admin and Game.admin_flags.noclip:
		_ghost(Vector2(ix, iy))


func _bar(r: Rect2, lag: float, f: float, col: Color) -> void:
	draw_rect(Rect2(r.position - Vector2(2, 2), r.size + Vector2(4, 4)), Color(0, 0, 0, 0.55 * col.a))
	draw_rect(Rect2(r.position, Vector2(r.size.x * clampf(lag, 0, 1), r.size.y)), Color(1, 1, 1, 0.35 * col.a))
	draw_rect(Rect2(r.position, Vector2(r.size.x * clampf(f, 0, 1), r.size.y)), col)
	draw_rect(Rect2(r.position, Vector2(r.size.x * clampf(f, 0, 1), 2)), Color(1, 1, 1, 0.25 * col.a))


func _heart(c: Vector2, col: Color) -> void:
	draw_circle(c + Vector2(-3, -2), 4, col)
	draw_circle(c + Vector2(3, -2), 4, col)
	draw_colored_polygon(PackedVector2Array([c + Vector2(-7, -1), c + Vector2(7, -1), c + Vector2(0, 7)]), col)


func _star(c: Vector2, col: Color) -> void:
	var pts := PackedVector2Array()
	for i in 10:
		var r := 8.0 if i % 2 == 0 else 3.5
		var a := -PI / 2 + i * PI / 5
		pts.append(c + Vector2(cos(a), sin(a)) * r)
	draw_colored_polygon(pts, col)


func _runner(c: Vector2, col: Color) -> void:
	draw_circle(c + Vector2(2, -6), 2.2, col)
	draw_line(c + Vector2(1, -3), c + Vector2(-1, 2), col, 2)
	draw_line(c + Vector2(-1, 2), c + Vector2(-5, 6), col, 2)
	draw_line(c + Vector2(-1, 2), c + Vector2(3, 6), col, 2)
	draw_line(c + Vector2(0, -2), c + Vector2(-4, 0), col, 2)
	draw_line(c + Vector2(0, -2), c + Vector2(4, -1), col, 2)


func _shield(c: Vector2) -> void:
	var col := Color(0.55, 0.85, 1.0)
	var pts := PackedVector2Array([c + Vector2(-9, -10), c + Vector2(9, -10), c + Vector2(9, 0), c + Vector2(0, 11), c + Vector2(-9, 0)])
	draw_colored_polygon(pts, Color(col, 0.35))
	pts.append(pts[0])
	draw_polyline(pts, col, 2.0)


func _bolt(c: Vector2, col: Color) -> void:
	draw_colored_polygon(PackedVector2Array([c + Vector2(2, -11), c + Vector2(-6, 2), c + Vector2(0, 2), c + Vector2(-2, 11), c + Vector2(6, -2), c + Vector2(0, -2)]), col)


func _ghost(c: Vector2) -> void:
	var col := Color(0.85, 0.85, 0.95, 0.85)
	draw_circle(c + Vector2(0, -3), 8, col)
	draw_rect(Rect2(c + Vector2(-8, -3), Vector2(16, 11)), col)
	draw_circle(c + Vector2(-3, -4), 1.6, Color.BLACK)
	draw_circle(c + Vector2(3, -4), 1.6, Color.BLACK)
