package com.spy.game.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.spy.game.R
import com.spy.game.ui.components.MascotImage
import com.spy.game.ui.components.SpyPrimaryButton
import com.spy.game.ui.components.SpyTextButton
import com.spy.game.ui.theme.SpyOnSurfaceMuted
import com.spy.game.ui.theme.SpyOutline
import com.spy.game.ui.theme.SpyRed
import com.spy.game.ui.theme.SpySurface

private const val MIN_PLAYERS = 3
private const val MAX_PLAYERS = 16

@Composable
fun SetupScreen(onStartGame: (List<String>) -> Unit, onShowDemo: () -> Unit) {
    val players = remember { mutableStateListOf<String>() }
    var input by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current

    fun addCurrentInput() {
        val name = input.trim()
        if (name.isNotEmpty() && players.size < MAX_PLAYERS) {
            players.add(name)
            input = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "Spy",
            modifier = Modifier.height(96.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Игра на компанию: найдите шпиона или не спалитесь сами",
            style = MaterialTheme.typography.bodyMedium,
            color = SpyOnSurfaceMuted,
        )
        SpyTextButton(text = "Как играть — демо-игра", onClick = onShowDemo)

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = input,
            onValueChange = { if (it.length <= 24) input = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Имя игрока") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { addCurrentInput() }),
            trailingIcon = {
                Text(
                    "Добавить",
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = input.isNotBlank()) {
                            addCurrentInput()
                            keyboard?.show()
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    color = if (input.isNotBlank()) SpyRed else SpyOutline,
                    fontWeight = FontWeight.Bold,
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SpyRed,
                unfocusedBorderColor = SpyOutline,
                cursorColor = SpyRed,
            ),
        )

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Игроки",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "${players.size} / $MAX_PLAYERS",
                style = MaterialTheme.typography.bodyMedium,
                color = SpyOnSurfaceMuted,
            )
        }

        Spacer(Modifier.height(8.dp))

        if (players.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MascotImage(painter = painterResource(R.drawable.mascot_leicher), size = 72.dp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Добавьте минимум $MIN_PLAYERS игроков, чтобы начать",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SpyOnSurfaceMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(players.size) { index ->
                    PlayerRow(
                        index = index + 1,
                        name = players[index],
                        onRemove = { players.removeAt(index) },
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        SpyPrimaryButton(
            text = if (players.size < MIN_PLAYERS) {
                "Нужно ещё ${MIN_PLAYERS - players.size}"
            } else {
                "Начать игру"
            },
            enabled = players.size >= MIN_PLAYERS,
            onClick = { onStartGame(players.toList()) },
        )
    }
}

@Composable
private fun PlayerRow(index: Int, name: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clip(RoundedCornerShape(16.dp))
            .background(SpySurface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(SpyRed.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Person, contentDescription = null, tint = SpyRed, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            name,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            "#$index",
            style = MaterialTheme.typography.bodyMedium,
            color = SpyOnSurfaceMuted,
        )
        Spacer(Modifier.width(12.dp))
        Icon(
            Icons.Filled.Close,
            contentDescription = "Удалить",
            tint = SpyOnSurfaceMuted,
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .clickable(onClick = onRemove),
        )
    }
}
