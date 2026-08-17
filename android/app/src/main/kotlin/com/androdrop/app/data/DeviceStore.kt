package com.androdrop.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class DeviceIdentity(
    val deviceId: String,
    val name: String,
    val pairCode: String,
)

private val Context.dataStore by preferencesDataStore(name = "androdrop_prefs")

/**
 * Local device identity, mirroring the web app's localStorage-backed device
 * id (see androdrop/src/lib/use-device.ts). Persisted via DataStore so it
 * survives process death; a Mutex serializes concurrent pair() calls the
 * same way the web hook guards against React Strict Mode's double-invoked
 * effect racing two device registrations.
 */
class DeviceStore(private val context: Context) {
    private val keyDeviceId = stringPreferencesKey("device_id")
    private val keyName = stringPreferencesKey("device_name")
    private val keyPairCode = stringPreferencesKey("pair_code")
    private val mutex = Mutex()

    suspend fun current(): DeviceIdentity? {
        val prefs = context.dataStore.data.first()
        val id = prefs[keyDeviceId] ?: return null
        val name = prefs[keyName] ?: return null
        val code = prefs[keyPairCode] ?: return null
        return DeviceIdentity(id, name, code)
    }

    /** Registers a new device, or re-pairs/renames the existing one. */
    suspend fun pair(name: String? = null): DeviceIdentity = mutex.withLock {
        val existingId = context.dataStore.data.first()[keyDeviceId]
        val response = ApiClient.service.pair(
            PairRequest(deviceId = existingId, name = name, platform = "android"),
        )
        context.dataStore.edit { prefs ->
            prefs[keyDeviceId] = response.deviceId
            prefs[keyName] = response.name
            prefs[keyPairCode] = response.pairCode
        }
        DeviceIdentity(response.deviceId, response.name, response.pairCode)
    }
}
