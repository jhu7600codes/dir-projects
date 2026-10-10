package com.simulasim.app.extra

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simulasim.app.ui.Accent
import com.simulasim.app.ui.SimButton
import com.simulasim.app.ui.TextDim
import com.simulasim.app.ui.TextMain
import com.simulasim.app.ui.TopBar

private const val BUBBLES = 48

@Composable
fun BubbleWrapScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("bubble", 0) }
    val vibrator = remember { ctx.getSystemService(Vibrator::class.java) }
    val popped = remember { mutableStateListOf(*Array(BUBBLES) { false }) }
    var total by remember { mutableIntStateOf(prefs.getInt("total", 0)) }

    fun pop(i: Int) {
        if (popped[i]) return
        popped[i] = true
        total++
        prefs.edit().putInt("total", total).apply()
        try {
            vibrator?.vibrate(VibrationEffect.createOneShot(25, 255))
        } catch (_: Exception) {}
    }

    Column(Modifier.fillMaxSize()) {
        TopBar("bubble wrap sim", onBack)
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${popped.count { it }}/$BUBBLES on this sheet", color = TextMain, fontWeight = FontWeight.Bold)
                Text("lifetime pops: $total", color = TextDim, fontSize = 12.sp)
            }
            SimButton("new sheet", color = Accent) { for (i in popped.indices) popped[i] = false }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(12.dp),
        ) {
            items(BUBBLES) { i -> Bubble(popped[i]) { pop(i) } }
        }
    }
}

@Composable
private fun Bubble(isPopped: Boolean, onPop: () -> Unit) {
    val s by animateFloatAsState(if (isPopped) 0.78f else 1f, label = "pop")
    Box(
        Modifier
            .padding(4.dp)
            .aspectRatio(1f)
            .scale(s)
            .background(
                if (isPopped) Brush.radialGradient(listOf(Color(0xFF2A2D38), Color(0xFF20232C)))
                else Brush.radialGradient(listOf(Color(0xFFE9F4FF), Color(0xFF8FB8E8), Color(0xFF5C83B8))),
                CircleShape,
            )
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onPop),
    )
}
