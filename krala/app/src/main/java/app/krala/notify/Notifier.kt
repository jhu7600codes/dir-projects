package app.krala.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import app.krala.ActiveChat
import app.krala.MainActivity
import app.krala.R
import app.krala.core.Markdown
import app.krala.data.chat.ChatEngine
import app.krala.data.chat.ChatEvent
import app.krala.data.db.ConversationDao
import app.krala.data.db.MessageDao
import app.krala.data.db.Role
import app.krala.data.settings.SettingsRepository
import app.krala.di.AppScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Notifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val engine: ChatEngine,
    private val conversations: ConversationDao,
    private val messages: MessageDao,
    private val settings: SettingsRepository,
    private val activeChat: ActiveChat,
    @AppScope private val scope: CoroutineScope,
) {
    fun createChannels() {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_REPLIES, context.getString(R.string.notif_channel_replies), NotificationManager.IMPORTANCE_HIGH))
        nm.createNotificationChannel(NotificationChannel(CH_TEXTS, context.getString(R.string.notif_channel_texts), NotificationManager.IMPORTANCE_HIGH))
    }

    /** posts a notification when a reply finishes and krala isn't on screen showing that chat */
    fun watchReplies() {
        scope.launch {
            engine.events.collect { e ->
                if (e !is ChatEvent.ReplyDone) return@collect
                if (!settings.current().notifyReplies) return@collect
                val foreground = withContext(Dispatchers.Main) {
                    ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                }
                if (foreground && activeChat.id == e.conversationId) return@collect
                val conv = conversations.get(e.conversationId) ?: return@collect
                val last = messages.listFor(e.conversationId).lastOrNull { it.role == Role.ASSISTANT } ?: return@collect
                show(CH_REPLIES, e.conversationId, conv.title.ifBlank { "krala" }, last.content)
            }
        }
    }

    fun show(channel: String, chatId: String, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val plain = Markdown.toPlain(text).replace(Regex("""\[[\p{L} ]{2,20}]"""), "").trim().take(1_000)
        val open = PendingIntent.getActivity(
            context, chatId.hashCode(),
            Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_CHAT, chatId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setColor(0xFFB3121F.toInt())
            .setContentTitle(if (channel == CH_TEXTS) "krala" else title)
            .setContentText(plain)
            .setStyle(NotificationCompat.BigTextStyle().bigText(plain))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(chatId.hashCode(), n) }
    }

    companion object {
        const val CH_REPLIES = "replies"
        const val CH_TEXTS = "texts"
    }
}
