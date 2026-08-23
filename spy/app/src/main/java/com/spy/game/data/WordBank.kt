package com.spy.game.data

import android.content.Context
import android.util.Log

/** A single secret word/location, tagged with the category it's shown under. */
data class WordEntry(val word: String, val category: String)

/**
 * Russian words the non-spy players can be shown. Every player but the spy
 * sees the same [WordEntry.word]; the [WordEntry.category] is shown
 * alongside it as a small hint of what kind of thing it is.
 *
 * Two sources feed the pool, so it's a big, open-ended list rather than a
 * fixed hand-picked one:
 *  - [curatedEntries]: ~90 hand-picked locations/professions/etc, grouped
 *    into real categories, always available even before [loadDictionary]
 *    has run.
 *  - a much larger (~11,200-word) general Russian word list bundled as
 *    `assets/words_ru.txt`, loaded once via [loadDictionary] and shown
 *    under the generic [CATEGORY_DICTIONARY] label since the source list
 *    carries no category data. It's built offline (not on-device) from the
 *    Leeds University Russian corpus frequency lists (CC BY 2.5 -- see
 *    README), run through a real morphological analyzer (pymorphy3) and
 *    kept only if its single most likely parse is a common noun in the
 *    nominative case -- no adjectives, no verbs, no other parts of speech,
 *    and no personal names (pymorphy's Name/Surn/Patr grammemes, plus a
 *    manual blocklist for names its dictionary under-tags) or org/brand
 *    abbreviations.
 */
object WordBank {

    private const val CATEGORY_DICTIONARY = "Слово"
    private const val WORDS_ASSET_PATH = "words_ru.txt"

    private const val CATEGORY_CITIES = "Город"
    private const val CATEGORY_PROFESSIONS = "Профессия"
    private const val CATEGORY_BUILDINGS = "Здание и место"
    private const val CATEGORY_TRANSPORT = "Транспорт"
    private const val CATEGORY_NATURE = "Природа и отдых"
    private const val CATEGORY_EVENTS = "Событие"
    private const val CATEGORY_INSTITUTIONS = "Учреждение"

    private val curatedEntries: List<WordEntry> = listOf(
        // Города
        "Москва", "Санкт-Петербург", "Казань", "Новосибирск", "Екатеринбург",
        "Владивосток", "Сочи", "Калининград", "Иркутск", "Мурманск",
        "Волгоград", "Самара", "Омск", "Ростов-на-Дону", "Красноярск",
    ).map { WordEntry(it, CATEGORY_CITIES) } +
        listOf(
            // Профессии
            "Врач", "Учитель", "Полицейский", "Пожарный", "Повар",
            "Программист", "Юрист", "Актёр", "Музыкант", "Пилот",
            "Строитель", "Парикмахер", "Журналист", "Фотограф", "Тренер",
        ).map { WordEntry(it, CATEGORY_PROFESSIONS) } +
        listOf(
            // Здания и места
            "Школа", "Больница", "Аэропорт", "Вокзал", "Библиотека",
            "Театр", "Музей", "Стадион", "Тюрьма", "Церковь",
            "Ресторан", "Казино", "Отель", "Супермаркет", "Банк",
        ).map { WordEntry(it, CATEGORY_BUILDINGS) } +
        listOf(
            // Транспорт
            "Поезд", "Самолёт", "Автобус", "Метро", "Круизный лайнер",
            "Подводная лодка", "Такси", "Велосипед", "Трамвай", "Ракета",
        ).map { WordEntry(it, CATEGORY_TRANSPORT) } +
        listOf(
            // Природа и отдых
            "Пляж", "Горы", "Лес", "Пустыня", "Остров",
            "Парк аттракционов", "Зоопарк", "Океан", "Река", "Водопад",
        ).map { WordEntry(it, CATEGORY_NATURE) } +
        listOf(
            // События
            "Свадьба", "Концерт", "Цирк", "Карнавал", "Похороны",
            "День рождения", "Экзамен", "Собеседование", "Футбольный матч", "Выборы",
        ).map { WordEntry(it, CATEGORY_EVENTS) } +
        listOf(
            // Учреждения
            "Полицейский участок", "Пожарная часть", "Посольство", "Университет", "Детский сад",
            "Военная база", "Космическая станция", "Подземный бункер", "Психиатрическая больница", "Оружейный завод",
        ).map { WordEntry(it, CATEGORY_INSTITUTIONS) } +
        listOf(
            // Разное
            "Пиратский корабль", "Замок", "Пирамида", "Остров сокровищ", "Заброшенный завод",
        ).map { WordEntry(it, CATEGORY_BUILDINGS) }

    private var dictionaryEntries: List<WordEntry> = emptyList()
    private var dictionaryLoaded = false

    /** Full pool words are drawn from: the curated list plus whatever's been loaded so far. */
    private val entries: List<WordEntry> get() = curatedEntries + dictionaryEntries

    fun random(): WordEntry = entries.random()

    /**
     * Reads `assets/words_ru.txt` into [dictionaryEntries]. Safe to call more
     * than once (only loads on the first call) and safe to call on the main
     * thread -- the asset is ~100KB of plain text, so this is fast, but it's
     * still meant to be called once up front (from the app's ViewModel init)
     * rather than per-frame.
     */
    fun loadDictionary(context: Context) {
        if (dictionaryLoaded) return
        dictionaryLoaded = true
        dictionaryEntries = try {
            context.assets.open(WORDS_ASSET_PATH).bufferedReader().useLines { lines ->
                lines
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .map { WordEntry(it.replaceFirstChar { c -> c.uppercaseChar() }, CATEGORY_DICTIONARY) }
                    .toList()
            }
        } catch (e: java.io.IOException) {
            // Missing/unreadable asset just means the pool falls back to
            // curatedEntries alone -- never crash the game over this.
            Log.w("WordBank", "Failed to load $WORDS_ASSET_PATH, falling back to the curated word list", e)
            emptyList()
        }
    }
}
