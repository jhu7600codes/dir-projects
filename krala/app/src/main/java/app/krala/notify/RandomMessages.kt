package app.krala.notify

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.krala.core.Daytime
import app.krala.core.Ids
import app.krala.core.KralaTags
import app.krala.core.Think
import app.krala.data.db.ConversationDao
import app.krala.data.db.ConversationEntity
import app.krala.data.db.MessageDao
import app.krala.data.db.MessageEntity
import app.krala.data.db.Role
import app.krala.data.memory.MemoryRepository
import app.krala.data.provider.ApiMessage
import app.krala.data.provider.ProviderRouter
import app.krala.data.settings.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** krala texting first, now and then, during the day */
object RandomMessages {
    private const val WORK = "krala-random-message"

    /** plans the next one; [replace] restarts the plan (setting changed or a message just went out) */
    fun schedule(context: Context, mode: String, replace: Boolean) {
        val wm = WorkManager.getInstance(context)
        val delay = Daytime.delay(LocalDateTime.now(), mode)
        if (delay == null) {
            wm.cancelUniqueWork(WORK)
            return
        }
        val req = OneTimeWorkRequestBuilder<RandomMessageWorker>()
            .setInitialDelay(delay.toMinutes(), TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        wm.enqueueUniqueWork(WORK, if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP, req)
    }
}

@HiltWorker
class RandomMessageWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val settings: SettingsRepository,
    private val conversations: ConversationDao,
    private val messages: MessageDao,
    private val memory: MemoryRepository,
    private val router: ProviderRouter,
    private val notifier: Notifier,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val s = settings.current()
        if (Daytime.gapHours(s.randomMessages) == null) return Result.success()
        val now = LocalDateTime.now()
        val chats = conversations.all().filter { !it.deleted && !it.incognito }
        val lastActivity = chats.maxOfOrNull { it.lastMessageAt } ?: 0L
        // not at night, and not right after a chat: that's just noise
        if (Daytime.isNight(now) || System.currentTimeMillis() - lastActivity < 2 * 3_600_000L) {
            RandomMessages.schedule(applicationContext, s.randomMessages, replace = true)
            return Result.success()
        }
        val block = KralaTags.build(
            KralaTags.Block(
                memory = KralaTags.formatMemory(memory.blockLines(exceptChatId = null)),
                systemPrompt = s.effectiveSystemPrompt,
                userInstructions = s.customInstructions,
            )
        )
        val ask = "(app note: it's ${now.toLocalTime().withSecond(0).withNano(0)} on a ${now.dayOfWeek.name.lowercase()}. " +
            "krala texts first, out of the blue, like a friend would. write one short message, 1 or 2 sentences: something she was " +
            "thinking about, a callback to a past chat or a running joke, asking how their plans went, or a random thought. " +
            "no 'hey how are you', no questions about whether they need help. reply with only the message.)"
        val text = try {
            KralaTags.strip(Think.answer(router.complete(s.defaultProviderId, s.defaultModel, listOf(ApiMessage("user", block + "\n\n" + ask))).first))
                .trim().trim('"')
        } catch (e: Exception) {
            return if (runAttemptCount < 3) Result.retry() else { RandomMessages.schedule(applicationContext, s.randomMessages, true); Result.success() }
        }
        if (text.isBlank()) {
            RandomMessages.schedule(applicationContext, s.randomMessages, replace = true)
            return Result.success()
        }
        // continue a chat from the last few days, or start a fresh one
        val t = System.currentTimeMillis()
        val recent = chats.filter { t - it.lastMessageAt < 3 * 24 * 3_600_000L }.maxByOrNull { it.lastMessageAt }
        val chatId = recent?.id ?: Ids.new().also { id ->
            conversations.upsert(ConversationEntity(id = id, title = "krala", titleAuto = true, createdAt = t, updatedAt = t, lastMessageAt = t))
        }
        messages.upsert(MessageEntity(id = Ids.new(), conversationId = chatId, role = Role.ASSISTANT, content = text, createdAt = t, updatedAt = t))
        conversations.touch(chatId, t)
        notifier.show(Notifier.CH_TEXTS, chatId, "krala", text)
        RandomMessages.schedule(applicationContext, s.randomMessages, replace = true)
        return Result.success()
    }
}
