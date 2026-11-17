package com.patternbox.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.patternbox.core.Entry
import com.patternbox.core.GameMap
import com.patternbox.core.Mode
import com.patternbox.core.Presets
import com.patternbox.core.Tournament

sealed interface Screen {
    data object Tabs : Screen
    data class Editor(val id: Long) : Screen
    data class Setup(val mode: Mode) : Screen
    data class Fight(val config: FightConfig) : Screen
    data object Bracket : Screen
}

class MainActivity : ComponentActivity() {
    private var sfx: Sfx? = null

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        sfx = runCatching { Sfx(applicationContext) }.getOrNull()
        val state = GameState(applicationContext)
        setContent {
            PatternboxTheme(state) {
                val stack = remember { mutableStateListOf<Screen>(Screen.Tabs) }
                var tab by rememberSaveable { mutableIntStateOf(0) }
                var tournament by remember { mutableStateOf<Tournament?>(null) }
                var bracketVersion by remember { mutableIntStateOf(0) }
                val screen = stack.last()
                fun push(s: Screen) { stack.add(s) }
                fun pop() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }

                BackHandler(enabled = stack.size > 1) { pop() }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        if (screen == Screen.Tabs) {
                            CenterAlignedTopAppBar(title = {
                                Text("patternbox", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                            })
                        } else {
                            TopAppBar(
                                title = {
                                    Text(
                                        when (screen) {
                                            is Screen.Editor -> state.byId(screen.id)?.displayName ?: "fighter"
                                            is Screen.Setup -> screen.mode.label
                                            is Screen.Fight -> screen.config.mode.label
                                            Screen.Bracket -> "tournament"
                                            Screen.Tabs -> ""
                                        },
                                        maxLines = 1,
                                    )
                                },
                                navigationIcon = { IconButton(onClick = { pop() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "back") } },
                            )
                        }
                    },
                    bottomBar = {
                        if (screen == Screen.Tabs) NavigationBar {
                            NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Filled.Face, null) }, label = { Text("fighters") })
                            NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.Filled.PlayArrow, null) }, label = { Text("play") })
                            NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Icon(Icons.Filled.Settings, null) }, label = { Text("settings") })
                        }
                    },
                    floatingActionButton = {
                        if (screen == Screen.Tabs && tab == 0) ExtendedFloatingActionButton(
                            onClick = {
                                val l = com.patternbox.core.Loadout.random()
                                val f = state.add(l.autoName, l)
                                push(Screen.Editor(f.id))
                            },
                            icon = { Icon(Icons.Filled.Add, null) },
                            text = { Text("new") },
                        )
                    },
                ) { padding ->
                    when (val s = screen) {
                        Screen.Tabs -> when (tab) {
                            0 -> RosterScreen(state, padding) { push(Screen.Editor(it)) }
                            1 -> PlayScreen(state, padding) { push(Screen.Setup(it)) }
                            else -> SettingsScreen(state, padding)
                        }
                        is Screen.Editor -> EditorScreen(
                            state, s.id, padding,
                            onTry = { f ->
                                val dummy = Presets.all.random()
                                push(Screen.Fight(FightConfig(Mode.SANDBOX, listOf(f.entry(), Entry(dummy.first, dummy.second)), GameMap.FLAT, humans = listOf(0))))
                            },
                            onDeleted = { pop() },
                        )
                        is Screen.Setup -> SetupScreen(
                            state, s.mode, padding,
                            onStart = { push(Screen.Fight(it)) },
                            onTournament = { picks, map, size ->
                                tournament = Tournament(picks.map { it.entry() }, map, size)
                                bracketVersion++
                                stack.removeAt(stack.lastIndex)
                                push(Screen.Bracket)
                            },
                        )
                        is Screen.Fight -> FightScreen(
                            state, sfx, s.config, padding,
                            onDone = { winner ->
                                val t = tournament
                                val tm = s.config.tournamentMatch
                                if (t != null && tm != null) {
                                    val w = winner?.takeIf { it == 0 || it == 1 } ?: (0..1).random()
                                    t.record(tm.first, tm.second, w)
                                    bracketVersion++
                                }
                                pop()
                            },
                            onExit = { pop() },
                        )
                        Screen.Bracket -> tournament?.let { t ->
                            TournamentScreen(state, t, padding, bracketVersion, onWatch = { push(Screen.Fight(it)) }, onChanged = { bracketVersion++ })
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sfx?.release()
    }
}
