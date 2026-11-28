package app.krala.di

import android.content.Context
import androidx.room.Room
import app.krala.data.db.ConversationDao
import app.krala.data.db.EmbeddingDao
import app.krala.data.db.Fts
import app.krala.data.db.KralaDatabase
import app.krala.data.db.MemoryDao
import app.krala.data.db.MessageDao
import app.krala.data.db.ProviderDao
import app.krala.data.db.SearchDao
import app.krala.data.db.UsageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.requery.android.database.sqlite.RequerySQLiteOpenHelperFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppScope

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    @AppScope
    fun appScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): KralaDatabase =
        Room.databaseBuilder(context, KralaDatabase::class.java, "krala.db")
            // requery's sqlite ships fts5 on every android version
            .openHelperFactory(RequerySQLiteOpenHelperFactory())
            .addCallback(Fts.callback)
            .addMigrations(app.krala.data.db.MIGRATION_1_2, app.krala.data.db.MIGRATION_2_3)
            .build()

    @Provides fun conversations(db: KralaDatabase): ConversationDao = db.conversations()
    @Provides fun messages(db: KralaDatabase): MessageDao = db.messages()
    @Provides fun memory(db: KralaDatabase): MemoryDao = db.memory()
    @Provides fun providers(db: KralaDatabase): ProviderDao = db.providers()
    @Provides fun usage(db: KralaDatabase): UsageDao = db.usage()
    @Provides fun embeddings(db: KralaDatabase): EmbeddingDao = db.embeddings()
    @Provides fun search(db: KralaDatabase): SearchDao = db.search()

    @Provides
    @Singleton
    fun http(): HttpClient = HttpClient(OkHttp) {
        expectSuccess = false
        engine {
            config {
                retryOnConnectionFailure(true)
                connectTimeout(20, TimeUnit.SECONDS)
                readTimeout(180, TimeUnit.SECONDS)
            }
        }
        install(HttpTimeout) {
            connectTimeoutMillis = 20_000
            socketTimeoutMillis = 180_000
            requestTimeoutMillis = 15 * 60_000
        }
        defaultRequest { header("User-Agent", "krala-android/1.0") }
    }
}
