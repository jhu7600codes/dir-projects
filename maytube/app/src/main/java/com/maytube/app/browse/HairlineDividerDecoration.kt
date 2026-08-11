package com.maytube.app.browse

import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.maytube.app.R

/** Holo used hairline dividers between list rows instead of card elevation/shadows. */
class HairlineDividerDecoration(context: android.content.Context) : RecyclerView.ItemDecoration() {

    private val paint = Paint().apply {
        color = context.getColor(R.color.holo_divider)
        strokeWidth = context.resources.displayMetrics.density
    }

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val left = parent.paddingLeft.toFloat()
        val right = (parent.width - parent.paddingRight).toFloat()
        for (i in 0 until parent.childCount) {
            val child: View = parent.getChildAt(i)
            val y = child.bottom.toFloat()
            canvas.drawLine(left, y, right, y, paint)
        }
    }
}
