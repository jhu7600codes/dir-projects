package dev.jhu.crossi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.jhu.crossi.ui.nav.CrossiNavHost
import dev.jhu.crossi.ui.theme.CrossiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CrossiTheme {
                CrossiNavHost()
            }
        }
    }
}
