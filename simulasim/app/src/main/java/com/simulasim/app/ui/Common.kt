package com.simulasim.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SimCard(
    modifier: Modifier = Modifier,
    color: Color = Surface1,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(color)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(14.dp),
        content = content,
    )
}

@Composable
fun SimButton(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Accent,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = if (color == Yellow || color == Green) Color.Black else Color.White,
            disabledContainerColor = Surface2,
            disabledContentColor = TextDim,
        ),
    ) {
        Text(text, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, fontSize = 14.sp)
    }
}

@Composable
fun TopBar(title: String, onBack: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back", tint = TextMain)
        }
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextMain, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
fun StatBar(label: String, value: Float, color: Color, modifier: Modifier = Modifier, valueText: String? = null) {
    Column(modifier) {
        Row {
            Text(label, color = TextDim, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text(valueText ?: "${(value * 100).toInt()}", color = TextMain, fontSize = 11.sp)
        }
        Spacer(Modifier.height(3.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Surface2)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(value.coerceIn(0f, 1f))
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun Chip(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Accent else Surface2)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (selected) Color.White else TextDim, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

/** tiny line chart, green if the series ends above where it started, red otherwise */
@Composable
fun Sparkline(values: List<Double>, modifier: Modifier = Modifier, colorOverride: Color? = null) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val min = values.min()
        val max = values.max()
        val span = (max - min).takeIf { it > 0 } ?: 1.0
        val color = colorOverride ?: if (values.last() >= values.first()) Green else Red
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = size.width * i / (values.size - 1)
            val y = size.height - (size.height * ((v - min) / span)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        val fill = Path().apply {
            addPath(path)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.25f), Color.Transparent)))
        drawPath(path, color, style = Stroke(width = 2.5.dp.toPx()))
        val lastY = size.height - (size.height * ((values.last() - min) / span)).toFloat()
        drawCircle(color, 3.5.dp.toPx(), Offset(size.width, lastY))
    }
}

@Composable
fun Hspace(w: Int) = Spacer(Modifier.width(w.dp))

@Composable
fun Vspace(h: Int) = Spacer(Modifier.height(h.dp))

val RowGap = Arrangement.spacedBy(8.dp)

fun money(v: Double): String {
    val sign = if (v < 0) "-" else ""
    val a = kotlin.math.abs(v)
    return when {
        a >= 1_000_000 -> "$sign$%.2fM".format(a / 1_000_000)
        a >= 10_000 -> "$sign$%.1fk".format(a / 1_000)
        else -> "$sign$%,.2f".format(a)
    }
}
