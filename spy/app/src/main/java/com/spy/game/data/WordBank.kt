package com.spy.game.data

/** A single secret word/location, tagged with the category it's shown under. */
data class WordEntry(val word: String, val category: String)

/**
 * ~90 Russian words the non-spy players can be shown, grouped by category.
 * Every player but the spy sees the same [WordEntry.word]; the [WordEntry.category]
 * is shown alongside it as a small hint of what kind of thing it is.
 */
object WordBank {

    private const val CATEGORY_CITIES = "Город"
    private const val CATEGORY_PROFESSIONS = "Профессия"
    private const val CATEGORY_BUILDINGS = "Здание и место"
    private const val CATEGORY_TRANSPORT = "Транспорт"
    private const val CATEGORY_NATURE = "Природа и отдых"
    private const val CATEGORY_EVENTS = "Событие"
    private const val CATEGORY_INSTITUTIONS = "Учреждение"

    val entries: List<WordEntry> = listOf(
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

    fun random(): WordEntry = entries.random()
}
