package com.msfviewer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.msfviewer.ui.MsfViewerScreen
import com.msfviewer.ui.theme.MsfViewerTheme
import com.msfviewer.util.FileNameResolver

/**
 * The app's only screen. There's no launcher entry point -- this activity
 * is only ever reached via the VIEW intent-filter for .msf files declared
 * in the manifest, so [onCreate] and [onNewIntent] both just resolve the
 * incoming Uri's filename and hand it to the one Composable screen.
 */
class MainActivity : ComponentActivity() {

    private var openedUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openedUri = intent?.data

        setContent {
            MsfViewerTheme {
                val uri = openedUri
                val filename = uri?.let { FileNameResolver.resolve(this, it) }
                MsfViewerScreen(filename = filename)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openedUri = intent.data
    }
}
