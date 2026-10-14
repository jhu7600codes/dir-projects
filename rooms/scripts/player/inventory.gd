class_name Inventory
extends Node
## what the player carries during a run. gold goes straight into the save (it's shared
## between runs and spent in the shops).

signal changed

var has_flashlight := true
var has_shakelight := false
var battery := 100.0   # flashlight charge, 0..100
var shake_charge := 60.0
var batteries := 0
var bandages := 0
var vitamins := 0


func add(item: String, amount := 1) -> void:
	var p = get_parent()
	match item:
		"gold":
			if Game.mod("gold_rush"):
				amount *= 2
			Save.add_gold(amount)
			Game.play_ui("gold")
			p.notify("+%d gold" % amount)
		"battery":
			batteries += amount
			p.notify("+ battery")
		"bandage":
			bandages += amount
			p.notify("+ bandage")
		"vitamins":
			vitamins += amount
			p.notify("+ vitamins")
		"flashlight":
			has_flashlight = true
			battery = 100.0
			p.notify("got a flashlight")
		"stardevice":
			Save.data.stardevice = true
			Save.data.stardevice_on = true
			Save.write()
			p.notify("got the STARDEVICE - it warns you when something's coming (5)")
		"starjug":
			Save.data.starjug = 6
			Save.write()
			p.notify("got the STARJUG - 6 sips of starlight (6)")
		"starhelp":
			Save.data.starhelp = true
			Save.write()
			p.notify("got the STARHELP - runs on batteries (7)")
		"shakelight":
			has_shakelight = true
			p.notify("got a shakelight - press 2, then spam left click to charge it" if not Settings.use_touch() else "got a shakelight - tap its square, then spam item to charge it")
	if item != "gold":
		Game.play_ui("pickup")
	changed.emit()


## swap in a fresh battery when the flashlight dies
func use_battery() -> bool:
	if batteries <= 0:
		return false
	batteries -= 1
	battery = 100.0
	changed.emit()
	return true


func use_bandage() -> bool:
	if bandages <= 0:
		return false
	bandages -= 1
	changed.emit()
	return true


func use_vitamins() -> bool:
	if vitamins <= 0:
		return false
	vitamins -= 1
	changed.emit()
	return true
