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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * the app -- a full-screen, flat, black "Metro" (Windows Phone) look:
 * solid black, lowercase page title, one bright accent color, sharp
 * corners, no cards or elevation.
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
            .verticalScroll(rememberScrollState()),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = "Close settings", tint = Color.White)
            }
        }

        Text(
            text = "settings",
            color = Color.White,
            fontSize = 40.sp,
            fontWeight = FontWeight.Light,
            modifier = Modifier.padding(start = 24.dp, top = 4.dp, bottom = 28.dp),
        )

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            MetroSectionHeader("general")
            MetroToggleRow(
                label = "spin forever",
                checked = state.spinForever,
                onCheckedChange = onSpinForeverChange,
            )

            Spacer(Modifier.height(28.dp))
            MetroSectionHeader("coin skin")
            state.skins.forEach { skin ->
                MetroSelectRow(
                    label = skin.displayName.lowercase(),
                    selected = skin.id == state.selectedSkin.id,
                    onClick = { onSelectSkin(skin.id) },
                )
            }

            Spacer(Modifier.height(28.dp))
            MetroSectionHeader("your own coin")
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
}

@Composable
private fun MetroSectionHeader(text: String) {
    Text(
        text = text,
        color = MetroAccent,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
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
            Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = MetroAccent)
        }
    }
    HorizontalDivider(color = MetroDivider)
}

@Composable
private fun MetroActionRow(label: String, detail: String? = null, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Text(text = label, color = Color.White, fontSize = 18.sp, modifier = Modifier.weight(1f))
        if (detail != null) {
            Text(
                text = detail,
                color = MetroSecondaryText,
                fontSize = 14.sp,
                modifier = Modifier.padding(end = 6.dp),
            )
        }
        Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null, tint = MetroSecondaryText)
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
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MetroSurface),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(text = label, color = Color.White, fontSize = 18.sp, modifier = Modifier.weight(1f))
        Text(
            text = if (uri == null) "not set" else "set",
            color = MetroSecondaryText,
            fontSize = 14.sp,
            modifier = Modifier.padding(end = 6.dp),
        )
        Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null, tint = MetroSecondaryText)
    }
    HorizontalDivider(color = MetroDivider)
}
