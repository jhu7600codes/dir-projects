package com.fivepesos.app.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fivepesos.app.data.CoinArt
import com.fivepesos.app.data.Face
import com.fivepesos.app.data.FlipPhase
import com.fivepesos.app.data.ImageTarget
import com.fivepesos.app.ui.components.CoinFaceView
import com.fivepesos.app.viewmodel.CoinUiState

@Composable
fun CoinScreen(
    state: CoinUiState,
    onPrimaryAction: () -> Unit,
    onOpenSettings: () -> Unit,
    onCloseSettings: () -> Unit,
    onSpinForeverChange: (Boolean) -> Unit,
    onSelectSkin: (String) -> Unit,
    onPickHeads: () -> Unit,
    onPickTails: () -> Unit,
    onOpenGoogleImport: () -> Unit,
    onCloseGoogleImport: () -> Unit,
    onImportImage: (ImageTarget, Uri) -> Unit,
) {
    val customIncomplete = state.selectedSkin.art is CoinArt.Custom &&
        (state.customHeadsUri == null || state.customTailsUri == null)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 110.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CoinFaceView(
                skin = state.selectedSkin,
                face = state.displayedFace,
                customHeadsUri = state.customHeadsUri,
                customTailsUri = state.customTailsUri,
                // A fraction of screen width, not a fixed dp size -- a fixed
                // 220.dp rendered far larger than the original design's coin
                // (which sits at roughly a quarter of screen width) on a
                // typical phone.
                modifier = Modifier
                    .fillMaxWidth(0.32f)
                    .aspectRatio(1f),
            )

            Spacer(Modifier.height(40.dp))

            CoinActionButton(
                text = primaryButtonLabel(state),
                enabled = state.phase != FlipPhase.FLIPPING || state.spinForever,
                onClick = onPrimaryAction,
            )

            when {
                state.phase == FlipPhase.RESULT -> {
                    Spacer(Modifier.height(28.dp))
                    Text(
                        text = "Result: ${resultLabel(state.displayedFace)}",
                        color = Color.White,
                        fontSize = 18.sp,
                    )
                }

                customIncomplete -> {
                    Spacer(Modifier.height(28.dp))
                    Text(
                        text = "Choose two images in Settings to use your own coin.",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 44.dp),
                    )
                }
            }
        }

        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 28.dp, end = 20.dp)
                .size(40.dp)
                .border(BorderStroke(2.dp, Color.White)),
        ) {
            Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White)
        }

        // Settings is a deliberate full-screen "Metro" panel, not a small
        // floating popover -- see SettingsScreen.kt.
        AnimatedVisibility(
            visible = state.settingsOpen,
            enter = slideInHorizontally(animationSpec = tween(220)) { it } + fadeIn(tween(220)),
            exit = slideOutHorizontally(animationSpec = tween(180)) { it } + fadeOut(tween(180)),
        ) {
            SettingsScreen(
                state = state,
                onClose = onCloseSettings,
                onSpinForeverChange = onSpinForeverChange,
                onSelectSkin = onSelectSkin,
                onPickHeads = onPickHeads,
                onPickTails = onPickTails,
                onOpenGoogleImport = onOpenGoogleImport,
            )
        }

        AnimatedVisibility(
            visible = state.googleImportOpen,
            enter = slideInHorizontally(animationSpec = tween(220)) { it } + fadeIn(tween(220)),
            exit = slideOutHorizontally(animationSpec = tween(180)) { it } + fadeOut(tween(180)),
        ) {
            GoogleCoinImportScreen(
                onClose = onCloseGoogleImport,
                onImageChosen = onImportImage,
            )
        }
    }
}

@Composable
private fun CoinActionButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RectangleShape,
        border = BorderStroke(2.dp, Color.White.copy(alpha = if (enabled) 1f else 0.4f)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color.White,
            disabledContentColor = Color.White.copy(alpha = 0.4f),
        ),
        contentPadding = PaddingValues(horizontal = 34.dp, vertical = 14.dp),
    ) {
        Text(text = text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

private fun primaryButtonLabel(state: CoinUiState): String = when (state.phase) {
    FlipPhase.IDLE -> if (state.spinForever) "spin" else "flip"
    FlipPhase.FLIPPING -> if (state.spinForever) "stop" else "flipping…"
    FlipPhase.RESULT -> if (state.spinForever) "spin again" else "flip again"
}

private fun resultLabel(face: Face): String = when (face) {
    Face.HEADS -> "Heads (Front)"
    Face.TAILS -> "Tails (Back)"
}
