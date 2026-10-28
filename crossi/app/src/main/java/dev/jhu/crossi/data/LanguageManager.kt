package dev.jhu.crossi.data

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dev.jhu.crossi.game.Language
import java.util.Locale

/** App language = game language. Stored by appcompat itself (per-app locale). */
object LanguageManager {
    fun current(): Language {
        val locales = AppCompatDelegate.getApplicationLocales()
        val tag = if (locales.isEmpty) Locale.getDefault().language else locales[0]?.language
        return if (tag == "ru") Language.RU else Language.EN
    }

    fun set(language: Language) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.code))
    }
}
