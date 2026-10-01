class_name PlayerLights
extends Node3D
## the flashlight (batteries, toggle with f) and the shakelight (green, dim, can't be
## turned off, drains fast, charged by spamming left click (use item), loud while charging).
## lives on the player's camera.

const FLASH_DRAIN := 100.0 / 240.0   # a full battery lasts 4 minutes
const SHAKE_DRAIN := 100.0 / 35.0
const SHAKE_PER_PRESS := 9.0

var inventory: Inventory
var equipped := "flashlight"  # "flashlight", "shakelight" or "none"
var flashlight_on := false

var _flash: SpotLight3D
var _shake: SpotLight3D
var _audio: AudioStreamPlayer3D


func _ready() -> void:
	_flash = SpotLight3D.new()
	_flash.light_color = Color(1.0, 0.96, 0.85)
	_flash.light_energy = 1.4
	_flash.spot_range = 22.0
	_flash.spot_angle = 28.0
	_flash.spot_attenuation = 0.8
	_flash.shadow_enabled = int(Settings.data.quality) > 0
	_flash.position = Vector3(0.2, -0.15, 0)
	_flash.light_cull_mask = ~Viewmodel.LAYER  # don't light up your own hand
	add_child(_flash)
	_shake = SpotLight3D.new()
	_shake.light_color = Color(0.45, 1.0, 0.5)
	_shake.spot_range = 12.0
	_shake.spot_angle = 40.0
	_shake.position = Vector3(0.2, -0.15, 0)
	_shake.light_cull_mask = ~Viewmodel.LAYER
	add_child(_shake)
	_audio = AudioStreamPlayer3D.new()
	_audio.bus = "SFX"
	add_child(_audio)
	_refresh()


func toggle_flashlight() -> void:
	if equipped != "flashlight":
		equip("flashlight")
		return
	if not inventory.has_flashlight:
		return
	flashlight_on = not flashlight_on
	_click()
	_refresh()


func equip(what: String) -> void:
	if what == "flashlight" and not inventory.has_flashlight:
		return
	if what == "shakelight" and not inventory.has_shakelight:
		return
	equipped = what
	if what == "flashlight":
		flashlight_on = true
	_click()
	_refresh()


## returns true if it handled the interact press (shakelight equipped)
func shake() -> bool:
	if equipped != "shakelight":
		return false
	inventory.shake_charge = minf(100.0, inventory.shake_charge + SHAKE_PER_PRESS)
	_audio.stream = Assets.sound("shakelight")
	_audio.volume_db = 6.0  # loud on purpose
	_audio.play()
	return true


func _click() -> void:
	_audio.stream = Assets.sound("flashlight_click")
	_audio.volume_db = 0.0
	_audio.play()


func _process(delta: float) -> void:
	if equipped == "flashlight" and flashlight_on:
		inventory.battery -= FLASH_DRAIN * delta
		if inventory.battery <= 0.0:
			inventory.battery = 0.0
			if not inventory.use_battery():
				flashlight_on = false
	if equipped == "shakelight":
		inventory.shake_charge = maxf(0.0, inventory.shake_charge - SHAKE_DRAIN * delta)
	_refresh()


func _refresh() -> void:
	_flash.visible = equipped == "flashlight" and flashlight_on and inventory.battery > 0.0
	if _flash.visible:
		# flicker when the battery is almost dead
		_flash.light_energy = 1.4 if inventory.battery > 10.0 or randf() > 0.1 else 0.3
	_shake.visible = equipped == "shakelight"
	_shake.light_energy = 0.15 + 0.85 * (inventory.shake_charge / 100.0)


func is_lit() -> bool:
	return _flash.visible or (_shake.visible and inventory.shake_charge > 5.0)
