package app.venderian

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.TextPaint
import android.widget.RemoteViews
import androidx.core.content.res.ResourcesCompat
import app.venderian.convert.Entry
import app.venderian.convert.Sounds
import app.venderian.data.Holiday
import app.venderian.data.Repository
import app.venderian.data.Taglines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.ceil

/** the home screen widget: streak with a venderian tagline, and a word of the day */
class WordWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        scope.launch {
            try {
                update(context, manager, ids)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /** redraws every widget (called when your progress changes) */
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, WordWidget::class.java))
            if (ids.isNotEmpty()) scope.launch { update(context, manager, ids) }
        }

        /** the same word all day, from the words you've learned (or all words, before you've learned any) */
        fun wordOfTheDay(learned: Collection<String>, all: List<Entry>, day: Long): Entry {
            val pool = all.filter { it.word in learned && it.pos != "suffix" && it.pos != "name" }
                .ifEmpty { all.filter { it.pos != "suffix" && it.pos != "name" } }
                .sortedBy { it.word }
            return pool[Math.floorMod(day * 7919, pool.size.toLong()).toInt()]
        }

        private suspend fun update(context: Context, manager: AppWidgetManager, ids: IntArray) {
            manager.updateAppWidget(ids, views(context))
        }

        suspend fun views(context: Context): RemoteViews {
            val app = context.applicationContext as VenderianApp
            val repo = app.repository
            val progress = repo.progress.first()
            val learned = repo.deckWords.first()
            val word = wordOfTheDay(learned, app.lexicon.entries, Repository.today())
            val (tagline, english) = Holiday.today()?.let { it.greeting to it.english }
                ?: Taglines.forStreak(progress.streak, progress.todayXp > 0)

            return RemoteViews(context.packageName, R.layout.widget_word).apply {
                setTextViewText(R.id.streak, "${progress.streak} day${if (progress.streak == 1) "" else "s"}")
                setTextViewText(R.id.tagline, tagline)
                setTextViewText(R.id.tagline_english, english)
                setImageViewBitmap(R.id.word_script, scriptBitmap(context, Sounds.scriptOf(word.word)))
                setTextViewText(R.id.word, "${word.word} · ${word.meaning}")
                val open = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_IMMUTABLE))
            }
        }

        /** widgets can't use the app's font, so the script is drawn into a picture */
        private fun scriptBitmap(context: Context, script: String): Bitmap {
            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = ResourcesCompat.getFont(context, R.font.venderian)
                textSize = 96f
                color = Color.rgb(0xE9, 0xDD, 0xFF)
            }
            val fm = paint.fontMetrics
            val w = ceil(paint.measureText(script)).toInt().coerceAtLeast(1) + 8
            val h = ceil(fm.descent - fm.ascent).toInt() + 8
            return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also {
                Canvas(it).drawText(script, 4f, 4f - fm.ascent, paint)
            }
        }
    }
}
