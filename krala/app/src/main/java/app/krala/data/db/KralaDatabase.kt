package app.krala.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        MemoryEntryEntity::class,
        ProviderEntity::class,
        UsageEntity::class,
        EmbeddingEntity::class,
        PluginEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class KralaDatabase : RoomDatabase() {
    abstract fun conversations(): ConversationDao
    abstract fun messages(): MessageDao
    abstract fun memory(): MemoryDao
    abstract fun providers(): ProviderDao
    abstract fun usage(): UsageDao
    abstract fun embeddings(): EmbeddingDao
    abstract fun search(): SearchDao
    abstract fun plugins(): PluginDao
}

/**
 * Full text search with sqlite fts5, kept in sync by triggers. Room only knows fts3/4, so the
 * virtual tables are created here. The requery sqlite build always has fts5; if it ever fails,
 * [ftsAvailable] stays false and search falls back to LIKE.
 */
val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE messages ADD COLUMN sources TEXT")
    }
}

val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE messages ADD COLUMN reasoning TEXT")
    }
}

val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `plugins` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL, `instructions` TEXT NOT NULL, `web` INTEGER NOT NULL, `enabled` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
    }
}

object Fts {
    @Volatile
    var ftsAvailable = false
        private set

    private val statements = listOf(
        "CREATE VIRTUAL TABLE IF NOT EXISTS messages_fts USING fts5(content, content='messages', content_rowid='rowid', tokenize='unicode61 remove_diacritics 2')",
        "CREATE TRIGGER IF NOT EXISTS messages_fts_ai AFTER INSERT ON messages BEGIN INSERT INTO messages_fts(rowid, content) VALUES (new.rowid, new.content); END",
        "CREATE TRIGGER IF NOT EXISTS messages_fts_ad AFTER DELETE ON messages BEGIN INSERT INTO messages_fts(messages_fts, rowid, content) VALUES ('delete', old.rowid, old.content); END",
        "CREATE TRIGGER IF NOT EXISTS messages_fts_au AFTER UPDATE OF content ON messages BEGIN INSERT INTO messages_fts(messages_fts, rowid, content) VALUES ('delete', old.rowid, old.content); INSERT INTO messages_fts(rowid, content) VALUES (new.rowid, new.content); END",
        "CREATE VIRTUAL TABLE IF NOT EXISTS memory_fts USING fts5(chatName, notes, content='memory_entries', content_rowid='rowid', tokenize='unicode61 remove_diacritics 2')",
        "CREATE TRIGGER IF NOT EXISTS memory_fts_ai AFTER INSERT ON memory_entries BEGIN INSERT INTO memory_fts(rowid, chatName, notes) VALUES (new.rowid, new.chatName, new.notes); END",
        "CREATE TRIGGER IF NOT EXISTS memory_fts_ad AFTER DELETE ON memory_entries BEGIN INSERT INTO memory_fts(memory_fts, rowid, chatName, notes) VALUES ('delete', old.rowid, old.chatName, old.notes); END",
        "CREATE TRIGGER IF NOT EXISTS memory_fts_au AFTER UPDATE OF chatName, notes ON memory_entries BEGIN INSERT INTO memory_fts(memory_fts, rowid, chatName, notes) VALUES ('delete', old.rowid, old.chatName, old.notes); INSERT INTO memory_fts(rowid, chatName, notes) VALUES (new.rowid, new.chatName, new.notes); END",
    )

    fun install(db: SupportSQLiteDatabase) {
        try {
            val existed = db.query("SELECT name FROM sqlite_master WHERE name = 'messages_fts'").use { it.moveToFirst() }
            statements.forEach { db.execSQL(it) }
            if (!existed) {
                db.execSQL("INSERT INTO messages_fts(messages_fts) VALUES ('rebuild')")
                db.execSQL("INSERT INTO memory_fts(memory_fts) VALUES ('rebuild')")
            }
            ftsAvailable = true
        } catch (e: Exception) {
            android.util.Log.w("krala", "fts5 unavailable, falling back to LIKE search", e)
            ftsAvailable = false
        }
    }

    val callback = object : RoomDatabase.Callback() {
        override fun onOpen(db: SupportSQLiteDatabase) = install(db)
    }
}
