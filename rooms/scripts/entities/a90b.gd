class_name A90B
extends ScreenEntity
## a-90b: the screen flushes red and it gives 5-10 orders in a row.
##   halt (stop sign)      - don't walk or run until the next order
##   proceed (green arrow) - keep walking until the next order
## you get a moment to react after each order, and a slow start or a tiny stop is forgiven.
## stay wrong for longer and it attacks for
## 20-30 damage and leaves.

const RULES := {
	"trigger": "timer", "min_door": 60, "timer": [200.0, 600.0], "chance": 0.5,
	"cooldown": 60.0, "group": "screen", "blocked_by": ["a90", "a60b"],
}
const REACT_TIME := 1.1
const FORGIVE := 0.5  # being wrong for less than this long (a slow start, a tiny stop) is fine

var _orders_left := 0
var _order := "halt"
var _check_t := 0.0  # time left in the current order
var _grace := 0.0
var _face: TextureRect
var _sign: TextureRect
var _failed := false
var _wrong_t := 0.0


func begin() -> void:
	id = "a90b"
	make_overlay()
	full_rect(Color(0.5, 0.0, 0.0, 0.3))
	var vs := root.get_viewport_rect().size
	_face = image("a90b_face", vs.y * 0.45)
	center(_face)
	_face.position.y -= vs.y * 0.12
	_sign = image("a90b_halt", vs.y * 0.22)
	center(_sign)
	_sign.position.y += vs.y * 0.25
	_sign.visible = false
	play_alert("a90b_spawn")
	_orders_left = randi_range(5, 10)
	after(1.2, _next_order)


func _next_order() -> void:
	if _failed:
		return
	if _orders_left <= 0:
		finish()
		return
	_orders_left -= 1
	_order = "halt" if randf() < 0.5 else "proceed"
	_sign.texture = Assets.texture("a90b_" + _order)
	_sign.visible = true
	play_alert("a90b_" + _order)
	Game.subtitle.emit(_order, Color(1, 0.3, 0.3) if _order == "halt" else Color(0.4, 1, 0.5))
	_grace = REACT_TIME
	_wrong_t = 0.0
	_check_t = randf_range(1.4, 2.8)


func _process(delta: float) -> void:
	if _failed or _check_t <= 0.0 or is_frozen():
		return
	if _grace > 0.0:
		_grace -= delta
		return
	_check_t -= delta
	var walking: bool = player.is_walking()
	if (_order == "halt" and walking) or (_order == "proceed" and not walking):
		_wrong_t += delta
		if _wrong_t >= FORGIVE:
			_fail()
			return
	else:
		_wrong_t = 0.0
	if _check_t <= 0.0:
		_next_order()


func _fail() -> void:
	_failed = true
	Game.death_details["a90b"] = _order
	hit_player = true
	_sign.visible = false
	_face.visible = false
	jumpscare("a90b_attack", "a90b_attack_sound", func():
		player.take_damage(randi_range(20, 30), "a90b")
		finish(), "lunge")
