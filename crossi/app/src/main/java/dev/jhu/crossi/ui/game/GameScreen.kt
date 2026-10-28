package dev.jhu.crossi.ui.game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jhu.crossi.R
import dev.jhu.crossi.game.BossTwist
import dev.jhu.crossi.game.GameEngine
import dev.jhu.crossi.game.GameMode
import dev.jhu.crossi.game.GameState
import dev.jhu.crossi.game.LevelCatalog
import dev.jhu.crossi.game.PowerUpType
import dev.jhu.crossi.game.Status
import dev.jhu.crossi.generator.ClueKind
import dev.jhu.crossi.generator.Direction
import dev.jhu.crossi.ui.common.CrossiTopBar
import dev.jhu.crossi.ui.common.crossiViewModel
import dev.jhu.crossi.ui.common.description
import dev.jhu.crossi.ui.common.formatTime
import dev.jhu.crossi.ui.common.icon
import dev.jhu.crossi.ui.common.label
import kotlinx.coroutines.launch

@Composable
fun GameScreen(mode: GameMode, onBack: () -> Unit, onPlayNext: (GameMode) -> Unit) {
    val vm = crossiViewModel(key = mode.toString()) { GameViewModel(mode, it) }
    val phase by vm.phase.collectAsStateWithLifecycle()
    val game by vm.game.collectAsStateWithLifecycle()
    val result by vm.result.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val inventory by vm.inventory.collectAsStateWithLifecycle()
    val bossIntro by vm.bossIntro.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var showClues by remember { mutableStateOf(false) }
    var confirmGiveUp by remember { mutableStateOf(false) }
    var hideResult by remember { mutableStateOf(false) }

    LifecycleResumeEffect(vm) {
        vm.setActive(true)
        onPauseOrDispose { vm.setActive(false) }
    }

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            val message = when (event) {
                is GameEvent.Checked -> if (event.result.solved) null else {
                    if (settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    context.getString(R.string.check_result, event.result.wrong, event.result.missing)
                }
                GameEvent.NoneLeft -> context.getString(R.string.none_left)
                GameEvent.CantUse -> context.getString(R.string.cant_use_here)
                GameEvent.Typed -> null
            }
            if (message != null) {
                snackbar.currentSnackbarData?.dismiss()
                scope.launch { snackbar.showSnackbar(message) }
            }
        }
    }

    Scaffold(
        topBar = {
            CrossiTopBar(title = title(mode), onBack = onBack) {
                game?.let { state ->
                    TopStatus(state, showTimer = settings.showTimer)
                    var menu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showClues = true }) {
                        Icon(Icons.AutoMirrored.Filled.FormatListBulleted, stringResource(R.string.clues))
                    }
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.more)) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.pencil)) },
                            leadingIcon = { Icon(Icons.Filled.Edit, null) },
                            trailingIcon = { if (state.pencilMode) Text("✓") },
                            onClick = { vm.togglePencil(); menu = false },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.give_up)) },
                            leadingIcon = { Icon(Icons.Filled.Flag, null) },
                            enabled = state.status == Status.PLAYING,
                            onClick = { confirmGiveUp = true; menu = false },
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (phase) {
                Phase.Loading -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.size(12.dp))
                    Text(stringResource(R.string.loading))
                }
                Phase.Error -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.load_failed))
                    Button(onClick = vm::load) { Text(stringResource(R.string.retry)) }
                }
                Phase.Ready -> game?.let { state ->
                    Column(Modifier.fillMaxSize()) {
                        PowerUpRow(
                            state = state,
                            inventory = inventory,
                            onUse = vm::usePowerUp,
                            onFinish = vm::finish,
                        )
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CrosswordGrid(
                                state = state,
                                highlightCrossing = settings.highlightCrossing,
                                onTap = vm::tap,
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                        }
                        val shown = result
                        if (shown != null && !hideResult) {
                            ResultCard(
                                info = shown,
                                onNext = { next -> onPlayNext(next) },
                                onHome = onBack,
                                onSeeAnswers = { hideResult = true },
                            )
                        } else if (state.status == Status.PLAYING) {
                            ClueBar(
                                state = state,
                                onPrev = { vm.moveEntry(-1) },
                                onNext = { vm.moveEntry(1) },
                                onToggle = vm::toggleDirection,
                                onOpenList = { showClues = true },
                            )
                            GameKeyboard(
                                language = vm.language,
                                scrambleSeed = state.keyboardSeed,
                                haptics = settings.haptics,
                                onKey = vm::type,
                                onBackspace = vm::backspace,
                                onToggleDirection = vm::toggleDirection,
                                modifier = Modifier.navigationBarsPadding(),
                            )
                        } else if (shown != null) {
                            Button(
                                onClick = { hideResult = false },
                                modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(),
                            ) { Text(stringResource(if (shown.won) R.string.solved else R.string.failed)) }
                        }
                    }
                    if (showClues) {
                        ClueSheet(
                            state = state,
                            onPick = { vm.selectEntry(it); showClues = false },
                            onDismiss = { showClues = false },
                        )
                    }
                }
            }
        }
    }

    if (bossIntro.isNotEmpty()) BossIntroDialog(bossIntro, onStart = vm::dismissBossIntro)

    if (confirmGiveUp) {
        AlertDialog(
            onDismissRequest = { confirmGiveUp = false },
            title = { Text(stringResource(R.string.give_up_title)) },
            text = { Text(stringResource(R.string.give_up_text)) },
            confirmButton = {
                TextButton(onClick = { confirmGiveUp = false; vm.giveUp() }) { Text(stringResource(R.string.give_up)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmGiveUp = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun title(mode: GameMode): String = when (mode) {
    is GameMode.Level ->
        if (LevelCatalog.spec(mode.number).isBoss) stringResource(R.string.boss_level_n, mode.number)
        else stringResource(R.string.level_n, mode.number)
    is GameMode.FreeRoam -> stringResource(mode.difficulty.label)
    is GameMode.Daily -> stringResource(R.string.daily) + " · " + stringResource(mode.difficulty.label)
}

@Composable
private fun TopStatus(state: GameState, showTimer: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        state.hearts?.let { hearts ->
            repeat(GameEngine.BOSS_HEARTS) { i ->
                Icon(
                    if (i < hearts) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        val limit = state.timeLimitMs
        if (limit != null) {
            val left = limit - state.elapsedMs
            Text(
                formatTime(left),
                color = if (left < 30_000) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
        } else if (showTimer) {
            Text(formatTime(state.elapsedMs), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun PowerUpRow(
    state: GameState,
    inventory: Map<PowerUpType, Int>,
    onUse: (PowerUpType) -> Unit,
    onFinish: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (type in PowerUpType.entries) {
                val count = inventory[type] ?: 0
                AssistChip(
                    onClick = { onUse(type) },
                    enabled = count > 0 && state.status == Status.PLAYING,
                    label = { Text(count.toString()) },
                    leadingIcon = { Icon(type.icon, stringResource(type.label), Modifier.size(AssistChipDefaults.IconSize)) },
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = onFinish,
                enabled = GameEngine.canFinish(state),
            ) { Text(stringResource(R.string.finish)) }
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(maxOf(state.checksLeft, 0).coerceAtMost(8)) {
                    Surface(Modifier.size(6.dp), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primary) {}
                }
            }
        }
    }
    if (state.checksLeft == 0 && state.status == Status.PLAYING) {
        Text(
            stringResource(R.string.no_checks_left),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun ClueBar(
    state: GameState,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToggle: () -> Unit,
    onOpenList: () -> Unit,
) {
    val scrambled = state.rules.has(BossTwist.SCRAMBLED_CLUES)
    val entryIndex = state.activeEntry
    val entry = state.puzzle.entries[entryIndex]
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 56.dp)) {
            IconButton(onClick = onPrev) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.prev_clue))
            }
            Column(
                Modifier.weight(1f).clickable(onClick = if (scrambled) onOpenList else onToggle).padding(vertical = 6.dp),
            ) {
                if (scrambled) {
                    Text(stringResource(R.string.pick_a_clue), style = MaterialTheme.typography.bodyLarge)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "${entry.number}${if (entry.direction == Direction.ACROSS) "A" else "D"}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge,
                        )
                        if (state.clueKindFor(entryIndex) == ClueKind.TRICK) {
                            SuggestionChip(onClick = {}, label = { Text(stringResource(R.string.trick)) }, modifier = Modifier.heightIn(max = 24.dp))
                        }
                    }
                    Text(
                        state.clueFor(entryIndex),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.next_clue))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClueSheet(state: GameState, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val scrambled = state.rules.has(BossTwist.SCRAMBLED_CLUES) && state.status == Status.PLAYING
    val entries = state.puzzle.entries
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 16.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp)) {
            if (scrambled) {
                items(state.clueOrder ?: entries.indices.toList()) { index ->
                    // scrambled: read only, tapping would give away where the clue goes
                    ClueRow(null, state.clueFor(index), done = false, active = false, onClick = null)
                }
            } else {
                for (dir in Direction.entries) {
                    item {
                        Text(
                            stringResource(if (dir == Direction.ACROSS) R.string.across else R.string.down),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                    items(entries.indices.filter { entries[it].direction == dir }) { index ->
                        val cells = state.geometry.entryCells[index]
                        ClueRow(
                            number = entries[index].number,
                            clue = state.clueFor(index),
                            done = cells.all { state.letterAt(it) != null },
                            active = index == state.activeEntry,
                            onClick = { onPick(index) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClueRow(number: Int?, clue: String, done: Boolean, active: Boolean, onClick: (() -> Unit)?) {
    val color = when {
        active -> MaterialTheme.colorScheme.primary
        done -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (number != null) Text(number.toString(), fontWeight = FontWeight.Bold, color = color, modifier = Modifier.width(28.dp))
        Text(clue, color = color)
    }
}

@Composable
private fun BossIntroDialog(twists: List<BossTwist>, onStart: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.boss)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (t in twists) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(t.icon, null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text(stringResource(t.label), fontWeight = FontWeight.Bold)
                            Text(stringResource(t.description), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onStart) { Text(stringResource(R.string.lets_go)) } },
    )
}

@Composable
private fun ResultCard(
    info: ResultInfo,
    onNext: (GameMode) -> Unit,
    onHome: () -> Unit,
    onSeeAnswers: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth().padding(12.dp).navigationBarsPadding(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(if (info.won) R.string.solved else R.string.failed),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = if (info.won) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
            if (!info.won) {
                Text(
                    stringResource(
                        when (info.state.failReason) {
                            dev.jhu.crossi.game.FailReason.TIME_UP -> R.string.fail_time_up
                            dev.jhu.crossi.game.FailReason.OUT_OF_HEARTS -> R.string.fail_hearts
                            else -> R.string.fail_gave_up
                        },
                    ),
                )
            }
            info.stars?.let { stars -> Text("★".repeat(stars) + "☆".repeat(3 - stars), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.secondary) }
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Stat(stringResource(R.string.time), formatTime(info.state.elapsedMs))
                Stat(stringResource(R.string.checks_used), info.state.checksUsed.toString())
                info.score?.let { Stat(stringResource(R.string.score), it.toString()) }
                info.runScore?.let { Stat(stringResource(R.string.run_score), it.toString()) }
            }
            if (info.newBest) Text(stringResource(R.string.new_best), color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
            if (info.rewards.isNotEmpty()) {
                Text(stringResource(R.string.rewards), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    info.rewards.groupingBy { it }.eachCount().forEach { (type, n) ->
                        AssistChip(onClick = {}, label = { Text("${stringResource(type.label)} ×$n") }, leadingIcon = { Icon(type.icon, null) })
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onHome) { Text(stringResource(R.string.home)) }
                if (!info.won) TextButton(onClick = onSeeAnswers) { Text(stringResource(R.string.see_answers)) }
                Spacer(Modifier.weight(1f))
                val next = info.next
                val retry = info.retry
                when {
                    next != null -> Button(onClick = { onNext(next) }) {
                        Text(stringResource(if (next is GameMode.Level) R.string.next_level else R.string.next_puzzle))
                    }
                    retry != null -> FilledTonalButton(onClick = { onNext(retry) }) { Text(stringResource(R.string.try_again)) }
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

