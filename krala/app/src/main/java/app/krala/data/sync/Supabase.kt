package app.krala.data.sync

import app.krala.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import javax.inject.Inject
import javax.inject.Singleton

/** null when no supabase url / key is configured: krala then runs fully local */
@Singleton
class SupabaseHolder @Inject constructor() {
    val configured: Boolean = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    val client: SupabaseClient? by lazy {
        if (!configured) null else runCatching {
            createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY) {
                install(Auth) {
                    scheme = "krala"
                    host = "auth-callback"
                }
                install(Postgrest)
                install(Realtime)
            }
        }.onFailure { android.util.Log.e("krala", "supabase init failed", it) }.getOrNull()
    }
}
