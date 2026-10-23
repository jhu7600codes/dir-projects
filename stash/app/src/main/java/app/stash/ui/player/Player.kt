package app.stash.ui.player

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import app.stash.data.db.DownloadState
import app.stash.player.PlayerUiState
import app.stash.ui.LocalActions
import app.stash.ui.components.Artwork
import app.stash.ui.components.TrackOptionsSheet
import app.stash.ui.components.formatDuration
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.delay

/** polls the controller position while on screen; media3 doesn't push position updates */
@Composable
private fun rememberPosition(): Pair<Long, Long> {
    val player = LocalActions.current.player
    var pos by remember { mutableLongStateOf(player.positionMs) }
    var buf by remember { mutableLongStateOf(player.bufferedMs) }
    LaunchedEffect(Unit) {
        while (true) {
            pos = player.positionMs
            buf = player.bufferedMs
            delay(500)
        }
    }
    return pos to buf
}

@Composable
fun MiniPlayer(state: PlayerUiState, onOpen: () -> Unit) {
    val actions = LocalActions.current
    val (pos, _) = rememberPosition()
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp).clickable(onClick = onOpen),
    ) {
        Column {
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Artwork(state.artworkUri, Modifier.size(44.dp), MaterialTheme.shapes.small)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        state.title, style = MaterialTheme.typography.titleSmall, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        state.error?.let { "can't play: $it" } ?: state.artist,
                        style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = if (state.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                    )
                }
                IconButton(onClick = { actions.player.togglePlay() }) {
                    if (state.isBuffering && !state.isPlaying) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp)
                    } else {
                        Icon(if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "play/pause")
                    }
                }
                IconButton(onClick = { actions.player.next() }) { Icon(Icons.Rounded.SkipNext, "Next") }
            }
            LinearProgressIndicator(
                progress = { if (state.durationMs > 0) pos.toFloat() / state.durationMs else 0f },
                modifier = Modifier.fillMaxWidth().height(2.dp).padding(horizontal = 12.dp),
                drawStopIndicator = {},
                gapSize = 0.dp,
            )
        }
    }
}

/** dominant + vibrant colors pulled from the album art, for the now-playing background */
@Composable
private fun rememberArtColors(url: Any?): Pair<Color?, Color?> {
    val context = LocalContext.current
    var colors by remember(url) { mutableStateOf<Pair<Color?, Color?>>(null to null) }
    LaunchedEffect(url) {
        if (url == null) return@LaunchedEffect
        val result = context.imageLoader.execute(
            ImageRequest.Builder(context).data(url).allowHardware(false).size(128).build()
        )
        val bitmap = (result.drawable as? BitmapDrawable)?.bitmap ?: return@LaunchedEffect
        val p = Palette.from(bitmap).generate()
        val dominant = p.dominantSwatch?.rgb?.let { Color(it) }
        val vibrant = (p.vibrantSwatch ?: p.lightVibrantSwatch ?: p.mutedSwatch)?.rgb?.let { Color(it) }
        colors = dominant to vibrant
    }
    return colors
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingSheet(state: PlayerUiState, onDismiss: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scheme = MaterialTheme.colorScheme
    val (dominant, vibrant) = rememberArtColors(state.artworkUri)
    val base = scheme.surface
    val top by animateColorAsState((dominant?.copy(alpha = 0.55f)?.compositeOver(base)) ?: scheme.surfaceContainerHigh, label = "top")
    val accent by animateColorAsState(
        vibrant?.let { v -> if (base.luminance() < 0.5f && v.luminance() < 0.25f) v.copy(alpha = 0.6f).compositeOver(Color.White) else v }
            ?: scheme.primary,
        label = "accent",
    )
    var showQueue by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = base,
    ) {
        Box(
            Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(top, base, base)))
        ) {
            if (showQueue) {
                QueueView(state, accent, onClose = { showQueue = false })
            } else {
                NowPlayingContent(state, accent, onCollapse = onDismiss, onQueue = { showQueue = true })
            }
        }
    }
}

@Composable
private fun NowPlayingContent(state: PlayerUiState, accent: Color, onCollapse: () -> Unit, onQueue: () -> Unit) {
    val actions = LocalActions.current
    val player = actions.player
    val trackFlow = remember(state.currentId) { actions.library.tracks.observe(state.currentId ?: "") }
    val track by trackFlow.collectAsState(initial = null)
    val (pos, buffered) = rememberPosition()
    var scrubbing by remember { mutableStateOf(false) }
    var scrub by remember { mutableFloatStateOf(0f) }
    var menu by remember { mutableStateOf(false) }
    val onAccent = if (accent.luminance() > 0.5f) Color.Black else Color.White

    // expressive bits: the art breathes with play/pause, the play button morphs circle -> squircle
    val artScale by animateFloatAsState(
        if (state.isPlaying) 1f else 0.86f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "art",
    )
    val playCorner by animateDpAsState(if (state.isPlaying) 28.dp else 48.dp, label = "corner")

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCollapse) { Icon(Icons.Rounded.KeyboardArrowDown, "Close") }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Now playing", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                track?.album?.let {
                    Text(it, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "More") }
        }

        Spacer(Modifier.weight(0.6f))
        Artwork(
            state.artworkUri,
            Modifier.fillMaxWidth().aspectRatio(1f).scale(artScale),
            MaterialTheme.shapes.extraLarge,
        )
        Spacer(Modifier.weight(0.6f))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    state.title, style = MaterialTheme.typography.headlineSmall, maxLines = 1,
                    modifier = Modifier.basicMarquee(),
                )
                Text(
                    state.artist, style = MaterialTheme.typography.titleMedium, maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, overflow = TextOverflow.Ellipsis,
                )
            }
            track?.let { t ->
                IconButton(onClick = {
                    when (t.downloadState) {
                        DownloadState.DONE -> actions.toast("Already downloaded")
                        DownloadState.NONE, DownloadState.FAILED -> actions.download(listOf(t))
                        else -> actions.toast("Already in the download queue")
                    }
                }) {
                    when (t.downloadState) {
                        DownloadState.DONE -> Icon(Icons.Rounded.DownloadDone, "Downloaded", tint = accent)
                        DownloadState.DOWNLOADING -> CircularProgressIndicator(
                            progress = { t.downloadProgress }, Modifier.size(22.dp), color = accent, strokeWidth = 2.5.dp,
                        )
                        else -> Icon(Icons.Rounded.Download, "Download")
                    }
                }
                IconButton(onClick = { actions.setLiked(t, !t.liked) }) {
                    Icon(
                        if (t.liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Like",
                        tint = if (t.liked) accent else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        state.error?.let {
            Text("Can't play this one: $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp), maxLines = 2)
        }

        Spacer(Modifier.height(12.dp))
        val duration = state.durationMs.coerceAtLeast(1)
        Slider(
            value = if (scrubbing) scrub else (pos.toFloat() / duration).coerceIn(0f, 1f),
            onValueChange = { scrubbing = true; scrub = it },
            onValueChangeFinished = { player.seekTo((scrub * duration).toLong()); scrubbing = false },
            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent),
        )
        Row(Modifier.fillMaxWidth()) {
            Text(formatDuration(if (scrubbing) (scrub * duration).toLong() else pos).ifEmpty { "0:00" },
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            if (state.isBuffering) {
                Text(
                    if (buffered <= pos) "Loading…" else "Buffering…",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
            }
            Text(formatDuration(state.durationMs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { player.toggleShuffle() }) {
                Icon(Icons.Rounded.Shuffle, "Shuffle", tint = if (state.shuffle) accent else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FilledIconButton(
                onClick = { player.previous() }, modifier = Modifier.size(56.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
            ) { Icon(Icons.Rounded.SkipPrevious, "Previous", Modifier.size(30.dp)) }
            FilledIconButton(
                onClick = { player.togglePlay() },
                modifier = Modifier.size(width = 96.dp, height = 80.dp),
                shape = RoundedCornerShape(playCorner),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = accent, contentColor = onAccent),
            ) {
                if (state.isBuffering && !state.isPlaying) CircularProgressIndicator(Modifier.size(32.dp), color = onAccent)
                else Icon(if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "play/pause", Modifier.size(40.dp))
            }
            FilledIconButton(
                onClick = { player.next() }, modifier = Modifier.size(56.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
            ) { Icon(Icons.Rounded.SkipNext, "Next", Modifier.size(30.dp)) }
            IconButton(onClick = { player.cycleRepeat() }) {
                Icon(
                    if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, "Repeat",
                    tint = if (state.repeatMode != Player.REPEAT_MODE_OFF) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.weight(0.4f))
        Surface(
            onClick = onQueue,
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        ) {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Rounded.QueueMusic, null)
                Spacer(Modifier.width(12.dp))
                val upNext = state.queue.getOrNull(state.queueIndex + 1)
                Text(
                    upNext?.let { "up next: ${it.mediaMetadata.title}" } ?: "Queue",
                    style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    if (menu) track?.let { TrackOptionsSheet(it, onDismiss = { menu = false }) }
}

@Composable
private fun QueueView(state: PlayerUiState, accent: Color, onClose: () -> Unit) {
    val player = LocalActions.current.player
    val list = rememberLazyListState(initialFirstVisibleItemIndex = state.queueIndex.coerceAtLeast(0))
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Rounded.KeyboardArrowDown, "Back") }
            Text("Queue", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Text("${state.queue.size} songs", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 16.dp))
        }
        LazyColumn(state = list, modifier = Modifier.weight(1f)) {
            itemsIndexed(state.queue, key = { i, item -> "$i:${item.mediaId}" }) { i, item ->
                val current = i == state.queueIndex
                Row(
                    Modifier.fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(if (current) accent.copy(alpha = 0.16f) else Color.Transparent)
                        .clickable { player.skipTo(i) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box {
                        Artwork(item.mediaMetadata.artworkUri, Modifier.size(48.dp))
                        if (current) Box(
                            Modifier.size(48.dp).clip(MaterialTheme.shapes.small).background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Rounded.GraphicEq, null, tint = Color.White) }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.mediaMetadata.title?.toString() ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = if (current) accent else MaterialTheme.colorScheme.onSurface)
                        Text(item.mediaMetadata.artist?.toString() ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (!current) IconButton(onClick = { player.removeAt(i) }) { Icon(Icons.Rounded.Close, "Remove") }
                }
            }
        }
    }
}
