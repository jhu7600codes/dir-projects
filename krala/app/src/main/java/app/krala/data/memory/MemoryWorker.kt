package app.krala.data.memory

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import app.krala.data.db.ConversationDao
import app.krala.data.provider.AllProvidersFailed
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@HiltWorker
class MemoryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val writer: MemoryNotesWriter,
    private val embeddings: EmbeddingIndex,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val chatId = inputData.getString(KEY_CHAT) ?: return Result.failure()
        return try {
            when (writer.writeFor(chatId)) {
                MemoryNotesWriter.Outcome.BUSY -> return Result.retry()
                else -> Unit
            }
            writer.condenseIfNeeded()
            runCatching { embeddings.indexPending() }
            Result.success()
        } catch (e: AllProvidersFailed) {
            android.util.Log.w("krala", "memory notes failed for $chatId", e)
            if (runAttemptCount < 6) Result.retry() else Result.failure()
        } catch (e: Exception) {
            android.util.Log.e("krala", "memory worker crashed", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val KEY_CHAT = "chat"
    }
}

@Singleton
class MemoryScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val conversations: ConversationDao,
) {
    /** a chat ended (left, switched away, or the app went to the background) */
    fun chatEnded(chatId: String) {
        val req = OneTimeWorkRequestBuilder<MemoryWorker>()
            .setInputData(workDataOf(MemoryWorker.KEY_CHAT to chatId))
            .setInitialDelay(4, TimeUnit.SECONDS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(TAG)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("notes-$chatId", ExistingWorkPolicy.REPLACE, req)
    }

    /** catches chats whose notes never got written (app killed, offline, ...) */
    suspend fun sweep(exceptChatId: String? = null) {
        conversations.needingNotes(exceptChatId ?: "").forEach { chatEnded(it.id) }
    }

    fun cancel(chatId: String) = WorkManager.getInstance(context).cancelUniqueWork("notes-$chatId")

    companion object {
        const val TAG = "memory"
    }
}
