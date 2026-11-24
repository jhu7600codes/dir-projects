package com.patternbox.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.platform.LocalContext

/** SharedPreferences already has the exact shape of Store */
class AndroidStore(private val p: SharedPreferences) : Store {
    override fun getString(key: String, def: String?) = p.getString(key, def)
    override fun getBoolean(key: String, def: Boolean) = p.getBoolean(key, def)
    override fun getInt(key: String, def: Int) = p.getInt(key, def)
    override fun getLong(key: String, def: Long) = p.getLong(key, def)
    override fun getFloat(key: String, def: Float) = p.getFloat(key, def)
    override fun getStringSet(key: String, def: Set<String>?): Set<String>? = p.getStringSet(key, def)
    override fun edit(): Store.Editor = object : Store.Editor {
        val e = p.edit()
        override fun putString(key: String, v: String?) = apply { e.putString(key, v) }
        override fun putBoolean(key: String, v: Boolean) = apply { e.putBoolean(key, v) }
        override fun putInt(key: String, v: Int) = apply { e.putInt(key, v) }
        override fun putLong(key: String, v: Long) = apply { e.putLong(key, v) }
        override fun putFloat(key: String, v: Float) = apply { e.putFloat(key, v) }
        override fun putStringSet(key: String, v: Set<String>?) = apply { e.putStringSet(key, v) }
        override fun apply() = e.apply()
    }
}

class MainActivity : ComponentActivity() {
    private var sfx: Sfx? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val fx = runCatching { Sfx(applicationContext) }.getOrNull()
        sfx = fx
        val state = GameState(AndroidStore(applicationContext.getSharedPreferences("patternbox", Context.MODE_PRIVATE)))
        val platform = Platform(
            name = "android",
            touchControls = true,
            hasDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
            dynamicScheme = { dark ->
                val ctx = LocalContext.current
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) (if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)) else null
            },
            feedback = fx ?: object : Feedback {},
            lan = NsdLanFinder(applicationContext),
            backHandler = { enabled, onBack -> BackHandler(enabled, onBack) },
        )
        importShared(intent, state)
        this.state = state
        setContent { PatternboxApp(state, platform) }
    }

    private var state: GameState? = null

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        state?.let { importShared(intent, it) }
    }

    /** text shared from another app (a chat message with codes, the "copy all" export...) */
    private fun importShared(intent: android.content.Intent?, state: GameState) {
        if (intent?.action != android.content.Intent.ACTION_SEND) return
        val text = intent.getStringExtra(android.content.Intent.EXTRA_TEXT) ?: return
        android.widget.Toast.makeText(this, importMessage(state.importText(text), "shared text"), android.widget.Toast.LENGTH_LONG).show()
        intent.action = null
    }

    override fun onDestroy() {
        super.onDestroy()
        sfx?.release()
    }
}
