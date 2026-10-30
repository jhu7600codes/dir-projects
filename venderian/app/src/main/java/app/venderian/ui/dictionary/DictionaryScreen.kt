@file:OptIn(ExperimentalMaterial3Api::class)

package app.venderian.ui.dictionary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.venderian.convert.Entry
import app.venderian.convert.Sounds
import app.venderian.course.ExerciseMaker
import app.venderian.ui.VText
import app.venderian.ui.app
import kotlinx.coroutines.launch

/** every venderian word: search in english, roman letters or script */
@Composable
fun DictionaryScreen(onBack: () -> Unit) {
    val app = app()
    var query by rememberSaveable { mutableStateOf("") }
    var onlyLearned by rememberSaveable { mutableStateOf(false) }
    var open by remember { mutableStateOf<Entry?>(null) }
    val deck by app.repository.deckWords.collectAsState(initial = emptySet())
    val results = remember(query, onlyLearned, deck) {
        app.lookup.search(query).filter { !onlyLearned || it.word in deck }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Dictionary") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "back") } },
        )
        OutlinedTextField(
            query, { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            placeholder = { Text("search english or venderian") },
        )
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(!onlyLearned, { onlyLearned = false }, label = { Text("all ${app.lexicon.entries.size}") })
            FilterChip(onlyLearned, { onlyLearned = true }, label = { Text("learned ${deck.size}") })
        }
        LazyColumn(Modifier.weight(1f)) {
            items(results, key = { it.word }) { e ->
                Row(
                    Modifier.fillMaxWidth().clickable { open = e }.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    VText(if (e.pos == "suffix") "" else Sounds.scriptOf(e.word), size = 26.sp, modifier = Modifier.width(110.dp))
                    Column(Modifier.weight(1f)) {
                        Text(e.word, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(e.meaning, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                    }
                    if (e.word in deck) Icon(Icons.Filled.CheckCircle, "learned", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            }
            if (results.isEmpty()) item {
                Text("no word like that yet", Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    open?.let { e -> WordSheet(e, inDeck = e.word in deck, onDismiss = { open = null }) }
}

@Composable
private fun WordSheet(e: Entry, inDeck: Boolean, onDismiss: () -> Unit) {
    val app = app()
    val scope = rememberCoroutineScope()
    val taught = remember(e) { app.repository.lessonFor(e.word) }
    // course sentences that use the word, endings and all
    val examples = remember(e) {
        app.course.lessons.flatMap { it.sentences }.filter { s ->
            ExerciseMaker.tokens(s.v).any { app.lookup.analyze(it)?.entry?.word == e.word }
        }.distinctBy { it.v }.take(4)
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            if (e.pos != "suffix") VText(Sounds.scriptOf(e.word), size = 64.sp)
            Text(e.word, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(e.pos, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text(e.meaning, style = MaterialTheme.typography.titleLarge)
            if (taught != null) {
                Spacer(Modifier.height(12.dp))
                Text("taught in ${taught.first}, lesson “${taught.second.title}”",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (examples.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("examples", style = MaterialTheme.typography.labelLarge)
                examples.forEach { s ->
                    Card(Modifier.fillMaxWidth().padding(top = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                        Column(Modifier.padding(12.dp)) {
                            VText(Sounds.scriptOfText(s.v), size = 24.sp)
                            Text(s.v, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(s.e, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            if (e.pos != "suffix" && e.pos != "name") {
                Spacer(Modifier.height(20.dp))
                if (inDeck) OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("it's in your practice deck") }
                else Button(onClick = { scope.launch { app.repository.addWordCard(e.word) } }, modifier = Modifier.fillMaxWidth()) {
                    Text("practice this word")
                }
            }
        }
    }
}
