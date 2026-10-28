package dev.jhu.crossi.ui.howto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jhu.crossi.R
import dev.jhu.crossi.di.AppContainer
import dev.jhu.crossi.ui.common.crossiViewModel
import kotlinx.coroutines.launch

class HowToPlayViewModel(private val c: AppContainer) : ViewModel() {
    fun markSeen() = viewModelScope.launch { c.settings.setSeenHowToPlay() }
}

private data class Page(val icon: androidx.compose.ui.graphics.vector.ImageVector, val title: Int, val text: Int)

private val pages = listOf(
    Page(Icons.Filled.GridOn, R.string.howto_1_title, R.string.howto_1_text),
    Page(Icons.Filled.DoneAll, R.string.howto_2_title, R.string.howto_2_text),
    Page(Icons.Filled.Celebration, R.string.howto_3_title, R.string.howto_3_text),
    Page(Icons.Filled.Bolt, R.string.howto_4_title, R.string.howto_4_text),
)

@Composable
fun HowToPlayScreen(onDone: () -> Unit) {
    val vm = crossiViewModel { HowToPlayViewModel(it) }
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
            HorizontalPager(pager, Modifier.weight(1f)) { i ->
                val page = pages[i]
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(page.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(96.dp))
                    Spacer(Modifier.height(24.dp))
                    Text(stringResource(page.title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(page.text), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    repeat(pages.size) { i ->
                        Surface(
                            Modifier.size(8.dp),
                            shape = CircleShape,
                            color = if (i == pager.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                        ) {}
                    }
                }
                val last = pager.currentPage == pages.lastIndex
                Button(onClick = {
                    if (last) {
                        vm.markSeen()
                        onDone()
                    } else {
                        scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                    }
                }) { Text(stringResource(if (last) R.string.done else R.string.next)) }
            }
        }
    }
}
