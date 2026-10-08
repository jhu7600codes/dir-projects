class_name A120
extends Rusher
## a-120: slower, comes from the rooms ahead of you with a metallic clanging that gets louder.
## it can rebound like ambush, so stay in the locker until the sound is completely gone.

const RULES := {
	"floor": ["offices", "city"], "city_min": 6,
	"trigger": "door", "min_door": 120, "chance": 0.25,
	"rare_min": 40, "rare_chance": 0.03,
	"needs_spawn_room": true, "cooldown": 50.0,
	"group": "rusher", "blocked_by": ["a60b"],
}

var rebounds := 0
var max_rebounds := 0
var _blink_t := 0.0


func begin() -> void:
	id = "a120"
	far_key = "a120_far"
	near_key = "a120_near"
	speed = 13.0
	stop_at_closed_door = false
	kill_range = 14.0
	max_rebounds = [0, 1, 1, 2, 3][randi() % 5]
	place(false)
	_break_lockers()
	setup_look("a120_face", Color(0.85, 0.85, 1.0), 2.4)
	set_visible_body(false)
	after(_head_start(), func():
		set_visible_body(true)
		moving = true)


## the warning you can see: some lockers in the room you just walked into get torn open.
## rooms with up to 5 lockers lose 2-3, bigger ones lose 5-7. at least one always works
func _break_lockers() -> void:
	var r: RoomBase = generator.room(Game.door)
	if r == null:
		return
	var usable: Array = r.lockers.filter(func(l): return is_instance_valid(l) and not l.broken)
	if usable.size() <= 1:
		return
	usable.shuffle()
	var n := usable.size()
	var count := randi_range(2, 3) if n <= 5 else randi_range(5, 7)
	count = mini(count, n - 1)
	for i in count:
		usable[i].break_open()


## seconds before it starts moving: enough to sprint to the nearest free locker
## (in the cubicle room they're all the way at the far end) and get inside
func _head_start() -> float:
	var p: Player = Game.player
	var best := INF
	for r in generator.rooms:
		for lk in r.lockers:
			if is_instance_valid(lk) and not lk.occupied and not lk.broken:
				best = minf(best, p.global_position.distance_to(lk.global_position))
	if best == INF:
		return 2.5
	return clampf(best / Player.SPRINT + 2.0, 2.5, 6.0)


func _process(delta: float) -> void:
	if is_frozen() or sprite == null or not moving:
		return
	# it flickers in and out of sight while it moves
	_blink_t -= delta
	if _blink_t <= 0.0:
		_blink_t = randf_range(0.04, 0.2)
		sprite.visible = randf() > 0.25


func on_path_end() -> void:
	if rebounds < max_rebounds:
		rebounds += 1
		moving = false
		set_visible_body(false)
		after(randf_range(1.5, 3.0), func():
			set_visible_body(true)
			reverse()
			moving = true)
		return
	finish()
