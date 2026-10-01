class_name Glitch
extends Node
## failsafe in the spirit of doors' glitch: if a room fails to generate, or the player
## ends up somewhere they shouldn't be (fell out of the world, walked into the void),
## glitch grabs them and puts them back in the current room.

static var instance: Glitch = null

var _check_t := 0.0


func _enter_tree() -> void:
	instance = self


func _exit_tree() -> void:
	if instance == self:
		instance = null


static func failsafe(reason: String) -> void:
	push_warning("glitch: " + reason)
	if instance:
		instance.teleport_player.call_deferred()


func _physics_process(delta: float) -> void:
	_check_t -= delta
	if _check_t > 0.0:
		return
	_check_t = 0.5
	var p = Game.player
	var gen = Game.generator
	if p == null or gen == null or p.dead or p.noclip_on():
		return
	if p.global_position.y < -15.0:
		teleport_player()
		return
	# too far from every loaded room: something went wrong
	var near := false
	for r in gen.rooms:
		if r.global_position.distance_to(p.global_position) < 80.0:
			near = true
			break
	if not near:
		teleport_player()


func teleport_player() -> void:
	var p = Game.player
	if p == null or Game.generator == null:
		return
	p.teleport(Game.generator.safe_spot())
	Game.play_ui("glitch")
	Game.subtitle.emit("...", Color(0.6, 0.9, 1.0))
	if p.hud:
		p.hud.flash(Color(0.5, 0.9, 1.0, 0.8), 0.6)
