package com.fivepesos.app.ui.screens

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fivepesos.app.data.CoinArt
import com.fivepesos.app.ui.components.rememberUriImageBitmap
import com.fivepesos.app.viewmodel.CoinUiState

/** The floating "Settings" card anchored under the gear icon -- Spin
 * Forever, coin skin picker, and (when "Your Own Coin" is selected) the two
 * image pickers. Tapping anywhere outside the card dismisses it. */
@Composable
fun SettingsOverlay(
    state: CoinUiState,
    onDismiss: () -> Unit,
    onSpinForeverChange: (Boolean) -> Unit,
    onSelectSkin: (String) -> Unit,
    onPickHeads: () -> Unit,
    onPickTails: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss,
            ),
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 80.dp, end = 20.dp)
                .widthIn(min = 230.dp, max = 300.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {},
                ),
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSpinForeverChange(!state.spinForever) },
                ) {
                    Checkbox(checked = state.spinForever, onCheckedChange = onSpinForeverChange)
                    Text(text = "Spin Forever", fontSize = 16.sp, color = Color.Black)
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFE0E0E0))
                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Coin Skin",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF666666),
                )
                Spacer(Modifier.height(4.dp))

                state.skins.forEach { skin ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectSkin(skin.id) },
                    ) {
                        RadioButton(selected = skin.id == state.selectedSkin.id, onClick = { onSelectSkin(skin.id) })
                        Text(text = skin.displayName, fontSize = 15.sp, color = Color.Black)
                    }
                }

                if (state.selectedSkin.art is CoinArt.Custom) {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFFE0E0E0))
                    Spacer(Modifier.height(12.dp))
                    CustomImagePickRow(label = "Heads", uri = state.customHeadsUri, onPick = onPickHeads)
                    Spacer(Modifier.height(10.dp))
                    CustomImagePickRow(label = "Tails", uri = state.customTailsUri, onPick = onPickTails)
                }
            }
        }
    }
}

@Composable
private fun CustomImagePickRow(label: String, uri: Uri?, onPick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        val bitmap = rememberUriImageBitmap(uri)
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFEDEDED)),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.AddPhotoAlternate,
                    contentDescription = null,
                    tint = Color(0xFF999999),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        TextButton(onClick = onPick) {
            Text(text = if (uri == null) "Choose $label image" else "Change $label image", fontSize = 13.sp)
        }
    }
}
