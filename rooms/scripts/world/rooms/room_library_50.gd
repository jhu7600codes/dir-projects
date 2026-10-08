extends RoomBase
## a-050: the office library. tall bookcases in rows, reading tables with lamps, the front
## desk. the door out has a code lock, like the library in doors: four books glow somewhere
## on the shelves, each one shows where one digit of the code goes. find them all.

var code := ""
var found := ["_", "_", "_", "_"]


func build() -> void:
	room_type = "library"
	is_special = true
	var length := 24.0
	var w := 16.0
	var h := 4.2
	shell(-w / 2, w / 2, length, h, "front", 0.0, "carpet_red")
	code = "%04d" % rng.randi_range(0, 9999)
	# bookcases: rows on both sides of a central aisle
	var cases := []
	var z := 4.0
	while z < length - 4.0:
		for side in [-1.0, 1.0]:
			var c := Vector3(side * 4.6, 0, z)
			box(Vector3(5.0, 3.2, 0.6), c + Vector3(0, 1.6, 0), Mats.get_mat("wood_dark"), true)
			box(Vector3(4.8, 2.9, 0.62), c + Vector3(0, 1.6, 0), Mats.get_mat("books"), false)
			for k in 5:
				box(Vector3(5.0, 0.05, 0.64), c + Vector3(0, 0.25 + k * 0.62, 0), Mats.get_mat("wood_dark"), false)
			cases.append(c)
		z += 3.6
	# reading tables down the middle aisle, each with a green lamp
	for i in 3:
		var t := Vector3(0, 0, 6.0 + i * 5.0)
		Props.table(self, t, PI / 2, Vector3(2.2, Props.TABLE_H, 1.0))
		box(Vector3(0.25, 0.12, 0.15), t + Vector3(0, Props.TABLE_H + 0.3, 0), Mats.get_mat("tl_green"), false)
		box(Vector3(0.03, 0.3, 0.03), t + Vector3(0, Props.TABLE_H + 0.15, 0), Mats.get_mat("metal"), false)
		for s in [-1.0, 1.0]:
			Props.chair(self, t + Vector3(s * 0.8, 0, 0), PI / 2 * s)
	# the front desk by the entrance
	Props.counter(self, Vector3(-4.5, 0, 1.6), 0.0, 3.0)
	var sign := Label3D.new()
	sign.text = "LIBRARY\nquiet please"
	sign.font_size = 56
	sign.pixel_size = 0.005
	sign.modulate = Color(0.2, 0.18, 0.15)
	sign.outline_size = 0
	sign.position = Vector3(0, 3.4, length - 0.12)
	sign.rotation.y = PI
	add_child(sign)
	# the four hint books on random shelves
	cases.shuffle()
	for i in 4:
		var c: Vector3 = cases[i]
		var face := 1.0 if rng.randf() < 0.5 else -1.0
		var at := c + Vector3(rng.randf_range(-2.0, 2.0), 0.25 + rng.randi_range(1, 4) * 0.62 - 0.32, face * 0.34)
		_hint_book(at, i, face)
	make_path([Vector3(0, 0, length * 0.3), Vector3(0, 0, length * 0.7)])


func _hint_book(at: Vector3, slot: int, face: float) -> void:
	var b := Node3D.new()
	b.position = at
	b.set_meta("no_merge", true)
	add_child(b)
	var mi := MeshInstance3D.new()
	var bm := BoxMesh.new()
	bm.size = Vector3(0.08, 0.3, 0.24)
	mi.mesh = bm
	mi.material_override = Mats.get_mat("exit_glow")
	b.add_child(mi)
	var it := Interactable.make(Vector3(0.4, 0.5, 0.5), "read the book")
	it.used.connect(func(p):
		found[slot] = code[slot]
		var pattern := ""
		for k in 4:
			pattern += (code[slot] if k == slot else "_") + " "
		p.notify("the book has a note in it: " + pattern.strip_edges())
		Game.play_ui("pickup"))
	b.add_child(it)


func _ready() -> void:
	super._ready()
	if exit_door:
		exit_door.code_lock(_open_keypad)


func _open_keypad() -> void:
	var k := Keypad.new()
	k.code = code
	k.note = " ".join(found)
	k.solved.connect(func():
		exit_door.unlock()
		exit_door.open())
	get_parent().add_child(k)
