class_name Loc
## translations. the english text in the code is the key, assets/i18n/<lang>.json maps it
## to the other language. ui made with UIKit translates by itself, Loc.t() is for text that
## gets glued together in code ("you died at " + door).

const LANGS := ["auto", "en", "ru"]
const NAMES := {"auto": "auto", "en": "English", "ru": "Русский"}

static var _loaded := {}


static func t(s: String) -> String:
	return String(TranslationServer.translate(s))


## "auto" follows the phone / computer language
static func use(lang: String) -> void:
	if lang == "auto":
		lang = "ru" if OS.get_locale_language() == "ru" else "en"
	if lang != "en" and not _loaded.has(lang):
		_loaded[lang] = true
		var path := "res://assets/i18n/%s.json" % lang
		var parsed = JSON.parse_string(FileAccess.get_file_as_string(path)) if FileAccess.file_exists(path) else null
		if parsed is Dictionary:
			var book := Translation.new()
			book.locale = lang
			for k in parsed:
				var v := str(parsed[k])
				book.add_message(k, v)
				# the ui capitalizes the first letter, so the capitalized english works too
				var ck := _cap(k)
				if ck != k:
					book.add_message(ck, _cap(v))
			TranslationServer.add_translation(book)
	TranslationServer.set_locale(lang)


static func current() -> String:
	return TranslationServer.get_locale().substr(0, 2)


static func _cap(s: String) -> String:
	return s if s.is_empty() else s.substr(0, 1).to_upper() + s.substr(1)
