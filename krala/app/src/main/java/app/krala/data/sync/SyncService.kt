package app.krala.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.hilt.work.HiltWorker
import app.krala.data.db.ConversationDao
import app.krala.data.db.ConversationEntity
import app.krala.data.db.KralaDatabase
import app.krala.data.db.MemoryDao
import app.krala.data.db.MemoryEntryEntity
import app.krala.data.db.MessageDao
import app.krala.data.db.MessageEntity
import app.krala.data.db.ProviderDao
import app.krala.data.db.ProviderEntity
import app.krala.data.provider.ProviderRepository
import app.krala.data.settings.SettingsRepository
import app.krala.data.settings.SyncedSettings
import app.krala.di.AppScope
import app.krala.security.KeyVault
import app.krala.security.PassphraseCrypto
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline first sync. Room is the source of truth; every row carries updated_at (epoch millis)
 * and the newer side wins, on the device and (via a trigger) on the server too. Deletes are
 * tombstones so they travel between devices. Incognito chats never leave the device.
 */
@Singleton
class SyncService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val holder: SupabaseHolder,
    private val auth: AuthRepository,
    private val db: KralaDatabase,
    private val conversations: ConversationDao,
    private val messages: MessageDao,
    private val memory: MemoryDao,
    private val providerDao: ProviderDao,
    private val providers: ProviderRepository,
    private val settings: SettingsRepository,
    private val vault: KeyVault,
    @AppScope private val scope: CoroutineScope,
) {
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private var realtimeJob: Job? = null
    private var channel: RealtimeChannel? = null
    private var watchJob: Job? = null
    private var authJob: Job? = null

    private suspend fun enabledUser(): Pair<SupabaseClient, String>? {
        val c = holder.client ?: return null
        if (!settings.current().syncEnabled) return null
        val uid = auth.state.value.userId ?: return null
        return c to uid
    }

    /** one full round: push local changes, then pull remote ones */
    suspend fun syncNow(): Result<Unit> = mutex.withLock {
        val (client, uid) = enabledUser() ?: return Result.success(Unit)
        _running.value = true
        try {
            if (settings.syncUser() != uid) {
                // a different account (or the first sync): everything local is new to it
                conversations.resetSynced(); messages.resetSynced(); memory.resetSynced(); providerDao.resetSynced()
                settings.resetSyncCursors()
                settings.setSyncUser(uid)
            }
            pushAll(client, uid)
            pullAll(client)
            settings.setLastSync(System.currentTimeMillis(), null)
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.w("krala", "sync failed", e)
            settings.setLastSync(0, e.message ?: e.javaClass.simpleName)
            Result.failure(e)
        } finally {
            _running.value = false
        }
    }

    // ---------------- push ----------------

    private suspend fun pushAll(c: SupabaseClient, uid: String) {
        conversations.dirty().chunked(200).forEach { batch ->
            c.from(T_CONV).upsert(batch.map { it.toRemote(uid) }) { onConflict = "user_id,id" }
            batch.forEach { conversations.markSynced(it.id, it.updatedAt) }
        }
        messages.dirty().chunked(200).forEach { batch ->
            c.from(T_MSG).upsert(batch.map { it.toRemote(uid) }) { onConflict = "user_id,id" }
            batch.forEach { messages.markSynced(it.id, it.updatedAt) }
        }
        memory.dirty().chunked(200).forEach { batch ->
            c.from(T_MEM).upsert(batch.map { it.toRemote(uid) }) { onConflict = "user_id,id" }
            batch.forEach { memory.markSynced(it.id, it.updatedAt) }
        }
        val s = settings.current()
        val passphrase = if (s.syncKeys) vault.decrypt(s.syncPassphraseCipher) else null
        providerDao.dirty().forEach { p ->
            val cipher = if (passphrase != null) PassphraseCrypto.encrypt(providers.secretsJson(providers.secrets(p)), passphrase) else null
            c.from(T_PROV).upsert(p.toRemote(uid, cipher)) { onConflict = "user_id,id" }
            providerDao.markSynced(p.id, p.updatedAt)
        }
        val pushedSettings = settings.lastPulled("settings_pushed")
        if (s.settingsUpdatedAt > pushedSettings) {
            val data = json.encodeToString(SyncedSettings.serializer(), settings.toSynced(s))
            c.from(T_SET).upsert(RemoteSettings(uid, data, s.settingsUpdatedAt)) { onConflict = "user_id" }
            settings.setLastPulled("settings_pushed", s.settingsUpdatedAt)
        }
    }

    /** re-uploads every provider, e.g. after "sync keys" was switched on or off */
    suspend fun markProvidersDirty() {
        providerDao.resetSynced()
    }

    // ---------------- pull ----------------

    private suspend inline fun <reified T : Any> pullTable(c: SupabaseClient, table: String, updatedAt: (T) -> Long, apply: (List<T>) -> Unit) {
        var cursor = settings.lastPulled(table)
        while (true) {
            val rows = c.from(table).select {
                filter { gt("updated_at", cursor) }
                order("updated_at", Order.ASCENDING)
                limit(500)
            }.decodeList<T>()
            if (rows.isEmpty()) break
            apply(rows)
            cursor = rows.maxOf(updatedAt)
            settings.setLastPulled(table, cursor)
            if (rows.size < 500) break
        }
    }

    private suspend fun pullAll(c: SupabaseClient) {
        val convs = mutableListOf<RemoteConversation>()
        pullTable<RemoteConversation>(c, T_CONV, { it.updatedAt }) { convs += it }
        for (r in convs) {
            val local = conversations.get(r.id)
            if (local == null || r.updatedAt > local.updatedAt) conversations.upsert(r.toLocal(local))
        }
        val msgs = mutableListOf<RemoteMessage>()
        pullTable<RemoteMessage>(c, T_MSG, { it.updatedAt }) { msgs += it }
        val newer = msgs.filter { r -> messages.get(r.id).let { it == null || r.updatedAt > it.updatedAt } }
        if (newer.isNotEmpty()) messages.upsertAll(newer.map { it.toLocal() })

        val mems = mutableListOf<RemoteMemory>()
        pullTable<RemoteMemory>(c, T_MEM, { it.updatedAt }) { mems += it }
        for (r in mems) {
            val local = memory.get(r.id)
            if (local == null || r.updatedAt > local.updatedAt) memory.upsert(r.toLocal())
        }

        val provs = mutableListOf<RemoteProvider>()
        pullTable<RemoteProvider>(c, T_PROV, { it.updatedAt }) { provs += it }
        val s = settings.current()
        val passphrase = vault.decrypt(s.syncPassphraseCipher)
        for (r in provs) {
            val local = providerDao.get(r.id)
            if (local != null && r.updatedAt <= local.updatedAt) continue
            val secretCipher = r.keyCipher?.let { kc ->
                passphrase?.let { PassphraseCrypto.decrypt(kc, it) }?.let { plain -> providers.parseSecrets(plain) }?.let { providers.encryptSecrets(it) }
            } ?: local?.secretCipher
            providerDao.upsert(r.toLocal(local, secretCipher))
        }

        val rows = c.from(T_SET).select().decodeList<RemoteSettings>()
        rows.firstOrNull()?.let { r ->
            if (r.updatedAt > s.settingsUpdatedAt) {
                runCatching { json.decodeFromString(SyncedSettings.serializer(), r.data) }.getOrNull()?.let {
                    settings.applySynced(it, r.updatedAt)
                    settings.setLastPulled("settings_pushed", r.updatedAt)
                }
            }
        }
    }

    // ---------------- live ----------------

    /** starts watching local changes and remote realtime events while the app is open */
    fun startLive() {
        if (holder.client == null) return
        if (watchJob?.isActive != true) {
            watchJob = scope.launch {
                merge(
                    db.invalidationTracker.createFlow(T_CONV_LOCAL, "messages", "memory_entries", "providers").map { },
                    settings.settings.map { it.settingsUpdatedAt }.distinctUntilChanged().map { },
                ).debounce(8_000).collect { SyncScheduler.soon(context) }
            }
        }
        if (authJob?.isActive != true) {
            authJob = scope.launch {
                kotlinx.coroutines.flow.combine(auth.state, settings.settings.map { it.syncEnabled }) { a, on -> if (on) a.userId else null }
                    .distinctUntilChanged()
                    .collect { uid ->
                        stopRealtime()
                        if (uid != null) {
                            startRealtime()
                            SyncScheduler.soon(context)
                        }
                    }
            }
        }
    }

    private fun stopRealtime() {
        realtimeJob?.cancel(); realtimeJob = null
        val ch = channel
        channel = null
        if (ch != null) scope.launch { runCatching { ch.unsubscribe() } }
    }

    fun stopLive() {
        watchJob?.cancel(); watchJob = null
        authJob?.cancel(); authJob = null
        stopRealtime()
    }

    private suspend fun startRealtime() {
        val (c, uid) = enabledUser() ?: return
        if (realtimeJob?.isActive == true) return
        runCatching {
            val ch = c.channel("krala-$uid")
            val flows = listOf(T_CONV, T_MSG, T_MEM, T_PROV, T_SET).map { t ->
                ch.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = t
                    filter("user_id", FilterOperator.EQ, uid)
                }
            }
            channel = ch
            realtimeJob = scope.launch {
                merge(*flows.toTypedArray()).debounce(1_200).collect { syncNow() }
            }
            ch.subscribe()
        }.onFailure { android.util.Log.w("krala", "realtime failed", it) }
    }

    // ---------------- mapping ----------------

    private fun ConversationEntity.toRemote(uid: String) = RemoteConversation(
        id, uid, title, titleAuto, pinned, providerId, model, systemPrompt, createdAt, updatedAt, lastMessageAt, deleted,
    )

    private fun RemoteConversation.toLocal(local: ConversationEntity?) = ConversationEntity(
        id = id, title = title, titleAuto = titleAuto, pinned = pinned, incognito = false, providerId = providerId,
        model = model, systemPrompt = systemPrompt, createdAt = createdAt, updatedAt = updatedAt,
        lastMessageAt = lastMessageAt, deleted = deleted, syncedAt = updatedAt, notesAt = local?.notesAt ?: 0,
    )

    private fun MessageEntity.toRemote(uid: String) = RemoteMessage(
        id, uid, conversationId, role, content, hiddenPrefix, attachments, status, error, providerName, model, createdAt, updatedAt, deleted, sources,
    )

    private fun RemoteMessage.toLocal() = MessageEntity(
        id = id, conversationId = conversationId, role = role, content = content, hiddenPrefix = hiddenPrefix,
        attachments = attachments, status = if (status == "streaming") "stopped" else status, error = error,
        providerName = providerName, model = model, createdAt = createdAt, updatedAt = updatedAt, deleted = deleted, syncedAt = updatedAt,
        sources = sources,
    )

    private fun MemoryEntryEntity.toRemote(uid: String) = RemoteMemory(
        id, uid, kind, chatId, chatName, notes, archivedInto, createdAt, updatedAt, deleted,
    )

    private fun RemoteMemory.toLocal() = MemoryEntryEntity(
        id = id, kind = kind, chatId = chatId, chatName = chatName, notes = notes, archivedInto = archivedInto,
        createdAt = createdAt, updatedAt = updatedAt, deleted = deleted, syncedAt = updatedAt,
    )

    private fun ProviderEntity.toRemote(uid: String, cipher: String?) = RemoteProvider(
        id, uid, name, baseUrl, models, defaultModel, contextSize, priority, enabled, presetId, supportsVision, cipher, createdAt, updatedAt, deleted,
    )

    private fun RemoteProvider.toLocal(local: ProviderEntity?, secretCipher: String?) = ProviderEntity(
        id = id, name = name, baseUrl = baseUrl, models = models, defaultModel = defaultModel, contextSize = contextSize,
        priority = priority, enabled = enabled, presetId = presetId, supportsVision = supportsVision, secretCipher = secretCipher,
        rateLimitedUntil = local?.rateLimitedUntil ?: 0, remainingRequests = local?.remainingRequests,
        lastError = local?.lastError, lastErrorAt = local?.lastErrorAt ?: 0, lastLatencyMs = local?.lastLatencyMs ?: 0,
        createdAt = createdAt, updatedAt = updatedAt, deleted = deleted, syncedAt = updatedAt,
    )

    companion object {
        const val T_CONV = "conversations"
        const val T_CONV_LOCAL = "conversations"
        const val T_MSG = "messages"
        const val T_MEM = "memory_entries"
        const val T_PROV = "providers"
        const val T_SET = "user_settings"
    }
}

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sync: SyncService,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result =
        if (sync.syncNow().isSuccess) Result.success() else if (runAttemptCount < 5) Result.retry() else Result.failure()
}

object SyncScheduler {
    private val network = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun soon(context: Context) {
        val req = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(network)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("sync-soon", ExistingWorkPolicy.KEEP, req)
    }

    fun periodic(context: Context) {
        val req = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS).setConstraints(network).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("sync-periodic", ExistingPeriodicWorkPolicy.KEEP, req)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork("sync-periodic")
        WorkManager.getInstance(context).cancelUniqueWork("sync-soon")
    }
}
