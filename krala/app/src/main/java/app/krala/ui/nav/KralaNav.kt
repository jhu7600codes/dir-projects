package app.krala.ui.nav

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.krala.Incoming
import app.krala.IntentRouter
import app.krala.ui.account.AccountScreen
import app.krala.ui.chat.ChatScreen
import app.krala.ui.memory.MemoryScreen
import app.krala.ui.providers.ProviderEditScreen
import app.krala.ui.providers.ProvidersScreen
import app.krala.ui.settings.PersonalityScreen
import app.krala.ui.settings.SettingsScreen
import app.krala.ui.usage.UsageScreen
import kotlinx.serialization.Serializable
import java.io.File

@Serializable object ChatRoute
@Serializable object SettingsRoute
@Serializable object PersonalityRoute
@Serializable object ProvidersRoute
@Serializable data class ProviderEditRoute(val id: String? = null, val preset: String? = null)
@Serializable object MemoryRoute
@Serializable object AccountRoute
@Serializable object UsageRoute
@Serializable data class TalkRoute(val chatId: String? = null)
@Serializable object VoiceRoute
@Serializable object PluginsRoute

private const val DURATION = 320

@Composable
fun KralaNav(router: IntentRouter, attachmentFile: (String) -> File) {
    val nav = rememberNavController()
    fun back() { nav.popBackStack() }
    fun openChat(id: String) {
        router.post(Incoming.OpenChat(id))
        nav.popBackStack(ChatRoute, inclusive = false)
    }
    val incoming by router.incoming.collectAsState()
    LaunchedEffect(incoming) {
        if (incoming != null && nav.currentDestination?.hasRoute<ChatRoute>() == false) nav.popBackStack(ChatRoute, inclusive = false)
    }
    NavHost(
        navController = nav,
        startDestination = ChatRoute,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(DURATION), initialOffset = { it / 4 }) + fadeIn(tween(DURATION)) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(DURATION), targetOffset = { it / 8 }) + fadeOut(tween(DURATION / 2)) },
        popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(DURATION), initialOffset = { it / 8 }) + fadeIn(tween(DURATION)) },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(DURATION), targetOffset = { it / 4 }) + fadeOut(tween(DURATION / 2)) },
    ) {
        composable<ChatRoute> {
            ChatScreen(
                vm = hiltViewModel(),
                router = router,
                attachmentFile = attachmentFile,
                onSettings = { nav.navigate(SettingsRoute) },
                onMemory = { nav.navigate(MemoryRoute) },
                onAccount = { nav.navigate(AccountRoute) },
                onProviders = { nav.navigate(ProvidersRoute) },
                onTalk = { nav.navigate(TalkRoute(it)) },
                onPlugins = { nav.navigate(PluginsRoute) },
            )
        }
        composable<SettingsRoute> {
            SettingsScreen(
                vm = hiltViewModel(),
                onBack = ::back,
                onPersonality = { nav.navigate(PersonalityRoute) },
                onProviders = { nav.navigate(ProvidersRoute) },
                onUsage = { nav.navigate(UsageRoute) },
                onMemory = { nav.navigate(MemoryRoute) },
                onAccount = { nav.navigate(AccountRoute) },
                onVoice = { nav.navigate(VoiceRoute) },
                onPlugins = { nav.navigate(PluginsRoute) },
            )
        }
        composable<PluginsRoute> { app.krala.ui.plugins.PluginsScreen(hiltViewModel(), ::back) }
        composable<VoiceRoute> { app.krala.ui.voice.VoiceScreen(hiltViewModel(), ::back, onProviders = { nav.navigate(ProvidersRoute) }) }
        composable<PersonalityRoute> { PersonalityScreen(hiltViewModel(), ::back) }
        composable<ProvidersRoute> {
            ProvidersScreen(
                vm = hiltViewModel(),
                onBack = ::back,
                onEdit = { nav.navigate(ProviderEditRoute(id = it)) },
                onAdd = { nav.navigate(ProviderEditRoute(preset = it)) },
            )
        }
        composable<ProviderEditRoute> { entry ->
            entry.toRoute<ProviderEditRoute>()
            ProviderEditScreen(hiltViewModel(), ::back)
        }
        composable<MemoryRoute> { MemoryScreen(hiltViewModel(), ::back, onOpenChat = ::openChat) }
        composable<AccountRoute> { AccountScreen(hiltViewModel(), ::back) }
        composable<UsageRoute> { UsageScreen(hiltViewModel(), ::back) }
        composable<TalkRoute>(
            enterTransition = { fadeIn(tween(DURATION)) },
            popExitTransition = { fadeOut(tween(DURATION)) },
        ) {
            app.krala.ui.talk.TalkScreen(hiltViewModel(), onVoiceSettings = { nav.navigate(VoiceRoute) }) { id ->
                if (id != null) router.post(Incoming.OpenChat(id))
                nav.popBackStack(ChatRoute, inclusive = false)
            }
        }
    }
}
