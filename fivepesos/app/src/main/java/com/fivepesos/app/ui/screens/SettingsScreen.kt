package com.fivepesos.app.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fivepesos.app.ui.components.rememberUriImageBitmap
import com.fivepesos.app.ui.theme.MetroAccent
import com.fivepesos.app.ui.theme.MetroBackground
import com.fivepesos.app.ui.theme.MetroDivider
import com.fivepesos.app.ui.theme.MetroSecondaryText
import com.fivepesos.app.ui.theme.MetroSurface
import com.fivepesos.app.viewmodel.CoinUiState

/**
 * The Settings panel is a deliberate switch of register from the rest of
 * the app -- a full-screen, flat, black Windows-Phone-style "Metro" page,
 * not an iOS-style grouped list: solid black, a huge lowercase title,
 * UPPERCASE accent section labels, plain text rows with no chevrons and
 * no circular avatars, a text link instead of an icon button for "back",
 * sharp corners everywhere.
 */
@Composable
fun SettingsScreen(
    state: CoinUiState,
    onClose: () -> Unit,
    onSpinForeverChange: (Boolean) -> Unit,
    onSelectSkin: (String) -> Unit,
    onPickHeads: () -> Unit,
    onPickTails: () -> Unit,
    onOpenGoogleImport: () -> Unit,
) {
    BackHandler(onBack = onClose)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = "‹ back",
            color = MetroAccent,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onClose).padding(vertical = 8.dp),
        )

        Text(
            text = "settings",
            color = Color.White,
            fontSize = 40.sp,
            fontWeight = FontWeight.Light,
            modifier = Modifier.padding(top = 4.dp, bottom = 28.dp),
        )

        MetroSectionHeader("GENERAL")
        MetroToggleRow(
            label = "spin forever",
            checked = state.spinForever,
            onCheckedChange = onSpinForeverChange,
        )

        Spacer(Modifier.height(28.dp))
        MetroSectionHeader("COIN SKIN")
        state.skins.forEach { skin ->
            MetroSelectRow(
                label = skin.displayName.lowercase(),
                selected = skin.id == state.selectedSkin.id,
                onClick = { onSelectSkin(skin.id) },
            )
        }

        Spacer(Modifier.height(28.dp))
        MetroSectionHeader("YOUR OWN COIN")
        MetroImagePickRow(
            label = "heads image",
            uri = state.customHeadsUri,
            onClick = onPickHeads,
        )
        MetroImagePickRow(
            label = "tails image",
            uri = state.customTailsUri,
            onClick = onPickTails,
        )
        MetroActionRow(
            label = "search google for a coin",
            onClick = onOpenGoogleImport,
        )

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun MetroSectionHeader(text: String) {
    Text(
        text = text,
        color = MetroAccent,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Composable
private fun MetroToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
    ) {
        Text(text = label, color = Color.White, fontSize = 18.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MetroAccent,
                checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = MetroSecondaryText,
                uncheckedTrackColor = MetroSurface,
                uncheckedBorderColor = Color(0xFF3A3A3A),
            ),
        )
    }
    HorizontalDivider(color = MetroDivider)
}

@Composable
private fun MetroSelectRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Text(
            text = label,
            color = if (selected) MetroAccent else Color.White,
            fontSize = 18.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            // A plain glyph, not an icon-font checkmark -- keeps the row
            // typographic instead of reaching for Material iconography.
            Text(text = "✓", color = MetroAccent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
    HorizontalDivider(color = MetroDivider)
}

@Composable
private fun MetroActionRow(label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Text(text = label, color = MetroAccent, fontSize = 18.sp)
    }
    HorizontalDivider(color = MetroDivider)
}

@Composable
private fun MetroImagePickRow(label: String, uri: Uri?, onClick: () -> Unit) {
    val bitmap = rememberUriImageBitmap(uri)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        // A small square swatch, not a circular avatar -- Metro is a
        // square/rectangle grid (tiles), never circles.
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(MetroSurface),
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(text = label, color = Color.White, fontSize = 18.sp, modifier = Modifier.weight(1f))
        Text(
            text = if (uri == null) "not set" else "set",
            color = MetroSecondaryText,
            fontSize = 14.sp,
        )
    }
    HorizontalDivider(color = MetroDivider)
}
