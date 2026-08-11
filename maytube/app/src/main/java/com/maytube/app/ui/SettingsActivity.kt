package com.maytube.app.ui

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.maytube.app.R
import com.maytube.app.data.ServerConfig
import com.maytube.app.data.ServerConfigRepository

/**
 * Lets the user point maytube at a yt2009 instance (IP/hostname + port) and
 * flip the handful of playback flags the WebView layer needs to know about.
 * Everything here is stored locally via [ServerConfigRepository]; nothing is
 * sent anywhere except to the instance itself once you hit connect.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var repository: ServerConfigRepository

    private lateinit var hostLayout: TextInputLayout
    private lateinit var portLayout: TextInputLayout
    private lateinit var hostInput: TextInputEditText
    private lateinit var portInput: TextInputEditText
    private lateinit var httpsSwitch: MaterialSwitch
    private lateinit var sabrSwitch: MaterialSwitch
    private lateinit var hd1080Switch: MaterialSwitch
    private lateinit var darkModeSwitch: MaterialSwitch
    private lateinit var nativePlayerSwitch: MaterialSwitch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        repository = ServerConfigRepository(this)

        setSupportActionBar(findViewById(R.id.settingsToolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        hostLayout = findViewById(R.id.hostLayout)
        portLayout = findViewById(R.id.portLayout)
        hostInput = findViewById(R.id.hostInput)
        portInput = findViewById(R.id.portInput)
        httpsSwitch = findViewById(R.id.httpsSwitch)
        sabrSwitch = findViewById(R.id.sabrSwitch)
        hd1080Switch = findViewById(R.id.hd1080Switch)
        darkModeSwitch = findViewById(R.id.darkModeSwitch)
        nativePlayerSwitch = findViewById(R.id.nativePlayerSwitch)

        repository.get()?.let { config ->
            hostInput.setText(config.host)
            portInput.setText(config.port.toString())
            httpsSwitch.isChecked = config.useHttps
            sabrSwitch.isChecked = config.sabrEnabled
            hd1080Switch.isChecked = config.prefer1080p
            darkModeSwitch.isChecked = config.darkMode
            nativePlayerSwitch.isChecked = config.nativePlayer
        } ?: run {
            portInput.setText(ServerConfigRepository.DEFAULT_PORT.toString())
        }

        findViewById<MaterialButton>(R.id.saveButton).setOnClickListener { save() }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun save() {
        hostLayout.error = null
        portLayout.error = null

        val host = hostInput.text?.toString()?.trim().orEmpty()
            .removePrefix("http://")
            .removePrefix("https://")
            .trimEnd('/')
            .substringBefore("/")

        if (host.isEmpty()) {
            hostLayout.error = getString(R.string.error_host_required)
            return
        }

        val portText = portInput.text?.toString()?.trim().orEmpty()
        val port = portText.toIntOrNull()
        if (port == null || port !in 1..65535) {
            portLayout.error = getString(R.string.error_port_invalid)
            return
        }

        val config = ServerConfig(
            host = host,
            port = port,
            useHttps = httpsSwitch.isChecked,
            sabrEnabled = sabrSwitch.isChecked,
            prefer1080p = hd1080Switch.isChecked,
            darkMode = darkModeSwitch.isChecked,
            nativePlayer = nativePlayerSwitch.isChecked
        )
        repository.save(config)

        setResult(Activity.RESULT_OK)
        Snackbar.make(hostInput, getString(R.string.action_save), Snackbar.LENGTH_SHORT).show()
        finish()
    }
}
