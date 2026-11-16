package com.patternbox.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            PatternboxTheme {
                val ctx = LocalContext.current
                val state = remember { GameState(ctx.applicationContext) }
                var tab by rememberSaveable { mutableIntStateOf(0) }
                var round by remember { mutableIntStateOf(0) }
                val fight = { round++; tab = 1 }

                BackHandler(enabled = tab == 1) { tab = 0 }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CenterAlignedTopAppBar(title = {
                            Text("patternbox", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                        })
                    },
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = tab == 0, onClick = { tab = 0 },
                                icon = { Icon(Icons.Filled.Build, null) }, label = { Text("build") },
                            )
                            NavigationBarItem(
                                selected = tab == 1, onClick = { if (tab != 1) fight() },
                                icon = { Icon(Icons.Filled.PlayArrow, null) }, label = { Text("fight") },
                            )
                        }
                    },
                    floatingActionButton = {
                        if (tab == 0) ExtendedFloatingActionButton(
                            onClick = fight,
                            icon = { Icon(Icons.Filled.PlayArrow, null) },
                            text = { Text("fight") },
                        )
                    },
                ) { padding ->
                    if (tab == 0) BuildScreen(state, padding)
                    else FightScreen(state, padding, round, onRematch = { round++ })
                }
            }
        }
    }
}
