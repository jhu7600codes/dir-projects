package app.venderian.ime

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import app.venderian.R
import java.io.File
import kotlin.math.ceil

/**
 * venderian text as a picture, so it can be sent to apps that don't have the venderian
 * font: the script big, the romanization small underneath so anyone can read it.
 */
object ScriptImage {
    private const val MAX_TEXT_WIDTH = 960
    private const val PAD = 56f
    private const val GAP = 20f

    fun render(context: Context, script: String, roman: String): Bitmap {
        val scriptPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = ResourcesCompat.getFont(context, R.font.venderian)
            textSize = 104f
            color = Color.rgb(0xE9, 0xDD, 0xFF)
        }
        val romanPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textSize = 38f
            color = Color.rgb(0xCA, 0xC4, 0xD0)
        }
        val scriptLayout = layout(script, scriptPaint)
        val romanLayout = layout(roman, romanPaint)
        val width = (maxOf(lineWidth(scriptLayout), lineWidth(romanLayout)) + PAD * 2).toInt()
        val height = (scriptLayout.height + GAP + romanLayout.height + PAD * 2).toInt()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(0x2B, 0x29, 0x30) }
        canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), 48f, 48f, bg)
        canvas.save()
        canvas.translate(PAD, PAD)
        scriptLayout.draw(canvas)
        canvas.translate(0f, scriptLayout.height + GAP)
        romanLayout.draw(canvas)
        canvas.restore()
        return bitmap
    }

    /** writes the picture to the app cache and returns a content:// uri other apps can read */
    fun save(context: Context, bitmap: Bitmap): Uri {
        val dir = File(context.cacheDir, "images").apply { mkdirs() }
        // keep only the latest few
        dir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(5)?.forEach { it.delete() }
        val file = File(dir, "venderian-${System.currentTimeMillis()}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return FileProvider.getUriForFile(context, "${context.packageName}.images", file)
    }

    private fun layout(text: String, paint: TextPaint): StaticLayout {
        val natural = ceil(StaticLayout.getDesiredWidth(text, paint)).toInt().coerceIn(1, MAX_TEXT_WIDTH)
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, natural)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.15f)
            .build()
    }

    private fun lineWidth(l: StaticLayout) = (0 until l.lineCount).maxOfOrNull { l.getLineWidth(it) } ?: 0f
}
