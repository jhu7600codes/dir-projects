package com.jhulian.android.youtube.classic.ui.settings

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.jhulian.android.youtube.classic.R
import com.jhulian.android.youtube.classic.YtClassicApp
import com.jhulian.android.youtube.classic.auth.LoginActivity
import com.jhulian.android.youtube.classic.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        supportFragmentManager.beginTransaction()
            .replace(binding.settingsContainer.id, SettingsFragment())
            .commit()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            val sessionManager = (requireActivity().application as YtClassicApp).sessionManager
            val accountPref = findPreference<Preference>("pref_account")
            refreshAccountPref(accountPref, sessionManager.isSignedIn, sessionManager.accountName)
            accountPref?.setOnPreferenceClickListener {
                if (sessionManager.isSignedIn) {
                    sessionManager.signOut()
                    refreshAccountPref(accountPref, false, null)
                } else {
                    startActivity(Intent(requireContext(), LoginActivity::class.java))
                }
                true
            }

            findPreference<Preference>("pref_clear_watch_history")?.setOnPreferenceClickListener {
                Toast.makeText(requireContext(), "Watch history cleared", Toast.LENGTH_SHORT).show()
                true
            }
        }

        override fun onResume() {
            super.onResume()
            val sessionManager = (requireActivity().application as YtClassicApp).sessionManager
            refreshAccountPref(findPreference("pref_account"), sessionManager.isSignedIn, sessionManager.accountName)
        }

        private fun refreshAccountPref(pref: Preference?, signedIn: Boolean, accountName: String?) {
            pref ?: return
            pref.title = if (signedIn) getString(R.string.sign_out) else getString(R.string.sign_in)
            pref.summary = if (signedIn) {
                getString(R.string.signed_in_as, accountName ?: "you")
            } else {
                getString(R.string.sign_in_prompt)
            }
        }
    }
}
