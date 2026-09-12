package com.fivepesos.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "five_pesos_settings")

data class UserSettings(
    val spinForever: Boolean,
    val selectedSkinId: String,
    val selectedCr2032BrandId: String,
    val customHeadsUri: String?,
    val customTailsUri: String?,
)

/** Persists everything in the Settings panel: the Spin Forever toggle, the
 * chosen coin skin, which CR2032 brand is active, and the two image Uris
 * for "Your Own Coin". */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val SPIN_FOREVER = booleanPreferencesKey("spin_forever")
        val SELECTED_SKIN = stringPreferencesKey("selected_skin")
        val SELECTED_CR2032_BRAND = stringPreferencesKey("selected_cr2032_brand")
        val CUSTOM_HEADS_URI = stringPreferencesKey("custom_heads_uri")
        val CUSTOM_TAILS_URI = stringPreferencesKey("custom_tails_uri")
    }

    val settings: Flow<UserSettings> = context.settingsDataStore.data.map { prefs ->
        UserSettings(
            spinForever = prefs[Keys.SPIN_FOREVER] ?: true,
            selectedSkinId = prefs[Keys.SELECTED_SKIN] ?: BuiltInSkins.first().id,
            selectedCr2032BrandId = prefs[Keys.SELECTED_CR2032_BRAND] ?: Cr2032Brands.first().id,
            customHeadsUri = prefs[Keys.CUSTOM_HEADS_URI],
            customTailsUri = prefs[Keys.CUSTOM_TAILS_URI],
        )
    }

    suspend fun setSpinForever(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.SPIN_FOREVER] = value }
    }

    suspend fun setSelectedSkin(id: String) {
        context.settingsDataStore.edit { it[Keys.SELECTED_SKIN] = id }
    }

    suspend fun setSelectedCr2032Brand(id: String) {
        context.settingsDataStore.edit { it[Keys.SELECTED_CR2032_BRAND] = id }
    }

    suspend fun setCustomHeadsUri(uri: String) {
        context.settingsDataStore.edit { it[Keys.CUSTOM_HEADS_URI] = uri }
    }

    suspend fun setCustomTailsUri(uri: String) {
        context.settingsDataStore.edit { it[Keys.CUSTOM_TAILS_URI] = uri }
    }
}
