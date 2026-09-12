package com.fivepesos.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fivepesos.app.data.CoinBrand
import com.fivepesos.app.ui.theme.MetroAccent
import com.fivepesos.app.ui.theme.MetroBackground
import com.fivepesos.app.ui.theme.MetroSecondaryText
import com.fivepesos.app.ui.theme.MetroSurface
import com.fivepesos.app.viewmodel.CoinUiState

/**
 * The real Windows Phone Settings app has almost no chrome: no back
 * button (the hardware/gesture back key does that), no dividers between
 * rows, no icons, no chevrons -- just a huge lowercase title, ALL CAPS
 * accent section labels, and plain two-line list items (a white label,
 * a gray status value directly underneath) separated by generous
 * whitespace instead of lines. This screen follows that, not a
 * grouped-list-with-dividers pattern.
 */
@Composable
fun SettingsScreen(
    state: CoinUiState,
    onClose: () -> Unit,
    onSpinForeverChange: (Boolean) -> Unit,
    onSelectSkin: (String) -> Unit,
    onSelectCr2032Brand: (String) -> Unit,
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
        Text(
            text = "settings",
            color = Color.White,
            fontSize = 46.sp,
            fontWeight = FontWeight.Light,
            modifier = Modifier.padding(top = 20.dp, bottom = 36.dp),
        )

        MetroSectionHeader("general")
        MetroToggleItem(
            label = "spin forever",
            checked = state.spinForever,
            onToggle = { onSpinForeverChange(!state.spinForever) },
        )

        Spacer(Modifier.height(40.dp))
        MetroSectionHeader("coin skin")
        state.skins.forEach { skin ->
            MetroChoiceItem(
                label = skin.displayName.lowercase(),
                selected = skin.id == state.selectedSkin.id,
                onClick = { onSelectSkin(skin.id) },
            )
            // "extra options": once CR2032 is the active skin, its brand
            // list unfolds right under it -- tapping it again re-selects
            // it (a no-op) rather than hiding the list, so the previews
            // stay reachable without a separate expand/collapse control.
            if (skin.id == "cr2032" && skin.id == state.selectedSkin.id) {
                MetroBrandPicker(
                    brands = state.cr2032Brands,
                    selectedBrandId = state.selectedCr2032Brand.id,
                    onSelectBrand = onSelectCr2032Brand,
                )
            }
        }

        Spacer(Modifier.height(40.dp))
        MetroSectionHeader("your own coin")
        MetroListItem(
            label = "heads image",
            value = if (state.customHeadsUri != null) "photo set" else "not set",
            onClick = onPickHeads,
        )
        MetroListItem(
            label = "tails image",
            value = if (state.customTailsUri != null) "photo set" else "not set",
            onClick = onPickTails,
        )
        MetroLinkItem(
            label = "search google for a coin",
            onClick = onOpenGoogleImport,
        )

        Spacer(Modifier.height(48.dp))
    }
}

@Composable
private fun MetroSectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        color = MetroAccent,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        modifier = Modifier.padding(bottom = 16.dp),
    )
}

/** The single most recognizable Windows Phone Settings pattern: a plain
 * white label, a gray status value directly underneath, tap the whole
 * row to act on it. No switch widget, no icon, no chevron, no divider. */
@Composable
private fun MetroListItem(label: String, value: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
    ) {
        Text(text = label, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Normal)
        Text(
            text = value,
            color = MetroSecondaryText,
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun MetroToggleItem(label: String, checked: Boolean, onToggle: () -> Unit) {
    MetroListItem(label = label, value = if (checked) "on" else "off", onClick = onToggle)
}

/** A one-line option in an inline pick-one list -- the accent color and
 * bold weight on the current choice ARE the selection indicator (no
 * checkmark, no radio dot; that's how WP rendered these). */
@Composable
private fun MetroChoiceItem(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (selected) MetroAccent else Color.White,
        fontSize = 21.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    )
}

/** The "extra options" brand sub-picker for the CR2032 skin -- a small
 * square photo preview plus name per brand, indented under the CR2032
 * row it belongs to. Squares, not circles, for the same reason as the
 * rest of this screen: Metro is a tile grid, not an avatar list. */
@Composable
private fun MetroBrandPicker(
    brands: List<CoinBrand>,
    selectedBrandId: String,
    onSelectBrand: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)) {
        brands.forEach { brand ->
            MetroBrandItem(
                brand = brand,
                selected = brand.id == selectedBrandId,
                onClick = { onSelectBrand(brand.id) },
            )
        }
    }
}

@Composable
private fun MetroBrandItem(brand: CoinBrand, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        Image(
            painter = painterResource(id = brand.headsRes),
            contentDescription = null,
            modifier = Modifier
                .size(34.dp)
                .background(MetroSurface),
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = brand.displayName.lowercase(),
            color = if (selected) MetroAccent else Color.White,
            fontSize = 17.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

/** A one-line action, not a state -- styled entirely in accent color so
 * it reads as a command rather than a setting. */
@Composable
private fun MetroLinkItem(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = MetroAccent,
        fontSize = 21.sp,
        fontWeight = FontWeight.Normal,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
    )
}
