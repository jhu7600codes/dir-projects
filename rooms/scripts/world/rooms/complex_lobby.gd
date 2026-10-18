extends RoomBase
## the ground floor of the complex: where the bus drops you off. mailboxes, a dead plant,
## a sign up to the stairwell. 150 floors to go


func build() -> void:
	room_type = "complex_lobby"
	theme = "complex"
	var length := 9.0
	shell(-3.5, 3.5, length, 3.2, "front", 0.0, "concrete_floor")
	# a wall of mailboxes
	for i in 6:
		for j in 4:
			box(Vector3(0.08, 0.3, 0.38), Vector3(3.42, 0.9 + j * 0.34, 2.0 + i * 0.4), Mats.get_mat("metal"), false)
	box(Vector3(0.2, 1.4, 2.6), Vector3(3.4, 0.7 + 0.6, 3.0), Mats.get_mat("dark"))
	Props.plant(self, Vector3(-3.0, 0, 1.0), true)
	Props.couch(self, Vector3(-2.9, 0, 4.5), PI / 2)
	for t in [["THE COMPLEX", 64, Vector3(0, 2.6, length - 0.03), Color(0.9, 0.85, 0.7)], ["STAIRS  ->  FLOORS 1 - 150", 32, Vector3(0, 2.3, length - 0.03), Color(0.85, 0.85, 0.85)]]:
		var l := Label3D.new()
		l.text = t[0]
		l.font_size = t[1]
		l.pixel_size = 0.004
		l.modulate = t[3]
		l.outline_size = 0
		l.position = t[2]
		l.rotation.y = PI
		add_child(l)
	make_path([Vector3(0, 0, 3.0), Vector3(0, 0, length - 1.5)])
