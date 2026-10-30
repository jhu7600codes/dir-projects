package app.venderian.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import app.venderian.VenderianApp

@Composable
fun app(): VenderianApp = LocalContext.current.applicationContext as VenderianApp

/** a viewModel built from the app container */
@Composable
inline fun <reified VM : ViewModel> appViewModel(crossinline create: (VenderianApp) -> VM): VM {
    val app = app()
    return viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T = create(app) as T
    })
}

@Composable
fun ScreenTitle(title: String, subtitle: String? = null, onOpenDictionary: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            if (onOpenDictionary != null) {
                FilledTonalIconButton(onClick = onOpenDictionary) { Icon(Icons.AutoMirrored.Outlined.MenuBook, "dictionary") }
            }
        }
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** says venderian out loud (see VenderianApp.speakVenderian) */
@Composable
fun SpeakButton(roman: String, modifier: Modifier = Modifier) {
    val app = app()
    FilledTonalIconButton(onClick = { app.speakVenderian(roman) }, modifier = modifier) {
        Icon(Icons.AutoMirrored.Outlined.VolumeUp, "say it")
    }
}

/** on holidays: the greeting, in script, with its translation */
@Composable
fun HolidayBanner() {
    val h = app.venderian.data.Holiday.today() ?: return
    androidx.compose.material3.Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                VText(app.venderian.convert.Sounds.scriptOfText(h.greeting), size = 26.sp)
                Text(h.greeting, style = MaterialTheme.typography.titleMedium)
                Text(h.english, style = MaterialTheme.typography.bodySmall)
            }
            SpeakButton(h.greeting)
        }
    }
}
