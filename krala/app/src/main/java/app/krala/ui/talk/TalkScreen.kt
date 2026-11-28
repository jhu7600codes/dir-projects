package app.krala.ui.talk

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.krala.R
import app.krala.core.VoiceScript
import app.krala.ui.components.KralaStar
import app.krala.ui.components.StarMode
import app.krala.ui.theme.LocalHaptics

@Composable
fun TalkScreen(vm: TalkViewModel, onClose: (chatId: String?) -> Unit) {
    val context = LocalContext.current
    val h = LocalHaptics.current
    val cs = MaterialTheme.colorScheme
    val state by vm.state.collectAsStateWithLifecycle()
    val level by vm.level.collectAsStateWithLifecycle()
    val speaking by vm.speaking.collectAsStateWithLifecycle()
    val energy by vm.energy.collectAsStateWithLifecycle()
    val title by vm.title.collectAsStateWithLifecycle()

    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        if (ok) vm.listen()
    }
    LaunchedEffect(Unit) { if (granted) vm.listen() else ask.launch(Manifest.permission.RECORD_AUDIO) }

    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    fun close() { vm.close(); onClose(vm.chatId()) }
    BackHandler { close() }

    val mode = when {
        speaking -> StarMode.TALKING
        state.phase == TalkPhase.THINKING -> StarMode.THINKING
        state.phase == TalkPhase.LISTENING -> StarMode.LISTENING
        else -> StarMode.IDLE
    }
    LaunchedEffect(mode) { if (mode == StarMode.LISTENING) h.tick() }

    Surface(color = cs.surface, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { h.click(); close() }) { Icon(Icons.Rounded.Close, stringResource(R.string.end_talk)) }
                Text(
                    title.ifBlank { stringResource(R.string.talk_mode) }, style = MaterialTheme.typography.titleMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 4.dp),
                )
                FilterChip(
                    selected = state.handsFree,
                    onClick = { h.tick(); vm.setHandsFree(!state.handsFree) },
                    label = { Text(stringResource(R.string.hands_free)) },
                    leadingIcon = { Icon(if (state.handsFree) Icons.Rounded.Hearing else Icons.Rounded.TouchApp, null, Modifier.size(18.dp)) },
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                KralaStar(
                    mode = mode, size = 260.dp, level = level, energy = energy,
                    modifier = Modifier.clickable(remember { MutableInteractionSource() }, indication = null) {
                        h.click()
                        if (!granted) ask.launch(Manifest.permission.RECORD_AUDIO) else vm.tapStar()
                    },
                )
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp).heightIn(min = 200.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedContent(
                    targetState = when {
                        !granted -> stringResource(R.string.mic_permission)
                        !vm.recognitionAvailable -> stringResource(R.string.voice_unavailable)
                        state.phase == TalkPhase.ERROR -> state.error ?: stringResource(R.string.reply_failed)
                        speaking -> stringResource(R.string.tap_to_interrupt)
                        state.phase == TalkPhase.LISTENING -> stringResource(R.string.listening)
                        state.phase == TalkPhase.THINKING -> stringResource(R.string.thinking)
                        else -> stringResource(R.string.tap_to_talk)
                    },
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "status",
                ) { s ->
                    Text(s, style = MaterialTheme.typography.labelLarge, color = if (state.phase == TalkPhase.ERROR) cs.error else cs.primary, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(14.dp))
                if (state.heard.isNotBlank()) {
                    Text(
                        state.heard, style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant,
                        textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(10.dp))
                }
                val reply = remember(state.reply) { VoiceScript.clean(state.reply).replace(Regex("""\[[\p{L} ]{2,20}]"""), "").replace(Regex("\\s+"), " ").trim() }
                if (reply.isNotBlank()) {
                    Text(
                        reply.takeLast(320), style = MaterialTheme.typography.titleMedium, color = cs.onSurface,
                        textAlign = TextAlign.Center, maxLines = 5, overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
