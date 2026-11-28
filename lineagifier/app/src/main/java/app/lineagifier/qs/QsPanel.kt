package app.lineagifier.qs

import android.content.res.ColorStateList
import android.graphics.drawable.Animatable
import android.os.Build
import android.widget.ImageView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import app.lineagifier.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * lineage-20.0 quick settings, rebuilt from its systemui res/values (dimens.xml, config.xml):
 * qs_tile_height 80dp, qs_corner_radius 28dp, qs_tile_margin 8dp, qs_horizontal_margin 16dp,
 * qs_tile_start_padding 16dp, qs_icon_size 20dp, qs_label_container_margin 10dp, qs_tile_text_size 14sp,
 * quick_settings_num_columns 2, quick_settings_max_rows 4, quick_qs_panel_max_tiles 4,
 * rounded_slider_height 48dp / corner 24dp / icon 20dp inset 14dp, qs_footer_action_button_size 48dp.
 */
object Qs {
    val TILE_H = 80.dp
    val CORNER = 28.dp
    val GAP = 8.dp
    val SIDE = 16.dp
    val START_PAD = 16.dp
    val ICON = 20.dp
    val LABEL_MARGIN = 10.dp
    val SLIDER_H = 48.dp
    val SLIDER_CORNER = 24.dp
    val SLIDER_ICON_INSET = 14.dp
    val FOOTER_BTN = 48.dp
    const val COLUMNS = 2
    const val ROWS = 4
    const val QQS_TILES = 4
}

class QsUi(
    val tiles: List<TileDef>,
    val states: Map<String, TileState>,
    val brightness: Float,          // 0..1 in gamma space, like the aosp slider
    val autoBrightness: Boolean,
    val colors: SysUi.Colors,
    val sysui: SysUi,
)

interface QsActions {
    fun tap(t: TileDef)
    fun longPress(t: TileDef)
    fun brightness(gamma: Float)
    fun toggleAuto()
    fun close()
    fun edit()
    fun settings()
    fun power()
}

@Composable
fun QsPanel(ui: QsUi, actions: QsActions, shown: Boolean) {
    var expanded by remember { mutableStateOf(false) }
    val c = ui.colors
    Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { actions.close() } }) {
        AnimatedVisibility(shown, enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(), exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()) {
            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = Qs.CORNER, bottomEnd = Qs.CORNER))
                    .background(Color(c.background).copy(alpha = 0.97f))
                    .pointerInput(Unit) { detectTapGestures { } }
                    .pointerInput(expanded) {
                        var dy = 0f
                        detectVerticalDragGestures(onDragStart = { dy = 0f }, onDragEnd = {
                            when {
                                dy > 40.dp.toPx() -> expanded = true
                                dy < -40.dp.toPx() -> if (expanded) expanded = false else actions.close()
                            }
                        }) { _, d -> dy += d }
                    }
                    .statusBarsPadding()
                    .padding(horizontal = Qs.SIDE)
                    .padding(bottom = 12.dp)
            ) {
                Header(c, expanded)
                Spacer(Modifier.height(8.dp))
                BrightnessSlider(ui, actions)
                Spacer(Modifier.height(Qs.GAP + 8.dp))
                if (!expanded) {
                    Grid(ui.tiles.take(Qs.QQS_TILES), ui, actions)
                    // the handle under quick quick settings that hints at pulling down
                    Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.width(32.dp).height(4.dp).clip(CircleShape).background(Color(c.onSurfaceVariant).copy(alpha = 0.4f))
                            .clickable { expanded = true })
                    }
                } else {
                    val pages = ui.tiles.chunked(Qs.COLUMNS * Qs.ROWS).ifEmpty { listOf(emptyList()) }
                    val pager = rememberPagerState { pages.size }
                    HorizontalPager(pager, pageSpacing = Qs.SIDE) { p -> Grid(pages[p], ui, actions) }
                    FooterRow(c, pager.currentPage, pages.size, actions)
                    FooterActions(c, actions)
                }
            }
        }
    }
}

@Composable
private fun Header(c: SysUi.Colors, expanded: Boolean) {
    val now by produceState(Date()) { while (true) { value = Date(); kotlinx.coroutines.delay(1000) } }
    val ctx = LocalContext.current
    val time = SimpleDateFormat(if (android.text.format.DateFormat.is24HourFormat(ctx)) "HH:mm" else "h:mm", Locale.getDefault()).format(now)
    val date = SimpleDateFormat(android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEMMMd"), Locale.getDefault()).format(now)
    Column(Modifier.padding(top = 8.dp, bottom = 4.dp)) {
        Text(time, color = Color(c.onSurface), fontSize = if (expanded) 32.sp else 14.sp, fontWeight = if (expanded) FontWeight.Normal else FontWeight.Medium)
        Text(date, color = Color(c.onSurfaceVariant), fontSize = 14.sp)
    }
}

@Composable
private fun BrightnessSlider(ui: QsUi, a: QsActions) {
    val c = ui.colors
    var v by remember(ui.brightness) { mutableFloatStateOf(ui.brightness) }
    var w by remember { mutableIntStateOf(1) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.weight(1f).height(Qs.SLIDER_H).clip(RoundedCornerShape(Qs.SLIDER_CORNER)).background(Color(c.inactive))
                .onSizeChanged { w = it.width }
                .pointerInput(Unit) {
                    detectTapGestures { o -> v = (o.x / w).coerceIn(0f, 1f); a.brightness(v) }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(onDragEnd = { a.brightness(v) }) { ch, d ->
                        ch.consume(); v = (v + d / w).coerceIn(0f, 1f); a.brightness(v)
                    }
                }
        ) {
            // the filled part never gets narrower than the slider's height (aosp's rounded slider does the same)
            Box(Modifier.fillMaxHeight().fillMaxWidth(v).widthIn(min = Qs.SLIDER_H).clip(RoundedCornerShape(Qs.SLIDER_CORNER)).background(Color(c.active)))
            SysIcon(ui.sysui, "ic_brightness_full", null, c.labelActive, Modifier.align(Alignment.CenterStart).padding(start = Qs.SLIDER_ICON_INSET).size(Qs.ICON))
        }
        // lineage's auto brightness toggle next to the slider (quick_settings_brightness_dialog.xml)
        Box(Modifier.padding(start = 8.dp).size(Qs.SLIDER_H).clip(CircleShape).clickable { a.toggleAuto() }, contentAlignment = Alignment.Center) {
            Icon(painterResource(if (ui.autoBrightness) R.drawable.los_ic_qs_brightness_auto_on else R.drawable.los_ic_qs_brightness_auto_off),
                null, tint = Color(c.onSurface), modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun Grid(tiles: List<TileDef>, ui: QsUi, a: QsActions) {
    Column(verticalArrangement = Arrangement.spacedBy(Qs.GAP)) {
        tiles.chunked(Qs.COLUMNS).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Qs.GAP)) {
                row.forEach { t -> TileView(t, ui.states[t.spec] ?: TileState(TileState.OFF), ui, a, Modifier.weight(1f)) }
                repeat(Qs.COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TileView(t: TileDef, st: TileState, ui: QsUi, a: QsActions, modifier: Modifier) {
    val c = ui.colors
    val bg by animateColorAsState(Color(when (st.state) { TileState.ON -> c.active; TileState.OFF -> c.inactive; else -> c.inactive }).let {
        if (st.state == TileState.UNAVAILABLE) it.copy(alpha = 0.3f) else it })   // QSTileViewImpl UNAVAILABLE_ALPHA
    val label = when (st.state) { TileState.ON -> c.labelActive; TileState.OFF -> c.labelInactive; else -> c.labelUnavailable }
    val secondaryColor = when (st.state) { TileState.ON -> c.secondaryActive; TileState.OFF -> c.secondaryInactive; else -> c.labelUnavailable }
    val states = remember(t.statesArray) { ui.sysui.states(t.statesArray) }
    val title = remember(t.spec) { ui.sysui.string(t.labelRes) ?: t.fallbackLabel }
    Row(
        modifier.height(Qs.TILE_H).clip(RoundedCornerShape(Qs.CORNER)).background(bg)
            .combinedClickable(onClick = { a.tap(t) }, onLongClick = { a.longPress(t) })
            .padding(start = Qs.START_PAD, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (t.bundled != null) Icon(painterResource(t.bundled), null, tint = Color(label), modifier = Modifier.size(Qs.ICON))
        else SysIcon(ui.sysui, if (st.state == TileState.ON) t.iconOn else t.iconOff, t.bundled, label, Modifier.size(Qs.ICON))
        Column(Modifier.weight(1f).padding(start = Qs.LABEL_MARGIN)) {
            Text(title, color = Color(label), fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp, letterSpacing = 0.14.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(st.secondary ?: states.getOrElse(st.state) { "" }, color = Color(secondaryColor), fontSize = 14.sp, lineHeight = 20.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (t.chevron) SysIcon(ui.sysui, "ic_chevron_end", null, secondaryColor, Modifier.size(16.dp))
    }
}

/** shows a systemui drawable through an ImageView so its animated vector actually animates, like in systemui */
@Composable
fun SysIcon(sysui: SysUi, name: String?, bundled: Int?, tint: Int, modifier: Modifier) {
    AndroidView(
        factory = { ImageView(it) },
        modifier = modifier,
        update = { iv ->
            val key = "$name/$bundled"
            if (iv.tag != key) {
                iv.tag = key
                val d = sysui.drawable(name)?.mutate() ?: bundled?.let { b -> iv.context.getDrawable(b) }
                iv.setImageDrawable(d)
                (d as? Animatable)?.start()
            }
            iv.imageTintList = ColorStateList.valueOf(tint)
        },
    )
}

@Composable
private fun FooterRow(c: SysUi.Colors, page: Int, pages: Int, a: QsActions) {
    // qs_footer_impl.xml: build text, page indicator, edit
    Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(Build.DISPLAY, color = Color(c.onSurfaceVariant), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(pages) { i -> Box(Modifier.size(if (i == page) 8.dp else 6.dp).clip(CircleShape).background(Color(c.onSurface).copy(alpha = if (i == page) 1f else 0.4f))) }
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            Box(Modifier.size(48.dp).clip(CircleShape).clickable { a.edit() }, contentAlignment = Alignment.Center) {
                Icon(painterResource(android.R.drawable.ic_menu_edit), null, tint = Color(c.onSurface), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun FooterActions(c: SysUi.Colors, a: QsActions) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
        Box(Modifier.size(Qs.FOOTER_BTN).clip(CircleShape).background(Color(c.inactive)).clickable { a.settings() }, contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.los_ic_settings), null, tint = Color(c.onSurface), modifier = Modifier.size(20.dp))
        }
        Box(Modifier.size(Qs.FOOTER_BTN).clip(CircleShape).background(Color(c.active)).clickable { a.power() }, contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.los_ic_settings_power), null, tint = Color(c.labelActive), modifier = Modifier.size(20.dp))
        }
    }
}
