package app.stash.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.stash.providers.RemoteArtist
import app.stash.ui.player.MiniPlayer
import app.stash.ui.player.NowPlayingSheet
import app.stash.ui.screens.AlbumScreen
import app.stash.ui.screens.ArtistPageScreen
import app.stash.ui.screens.DownloadsScreen
import app.stash.ui.screens.HomeScreen
import app.stash.ui.screens.ImportProgressScreen
import app.stash.ui.screens.RescanScreen
import app.stash.ui.screens.CoverGrabScreen
import app.stash.ui.screens.YouTubeLoginScreen
import app.stash.ui.screens.YtPlaylistScreen
import app.stash.ui.screens.ImportScreen
import app.stash.ui.screens.LibraryScreen
import app.stash.ui.screens.LikedScreen
import app.stash.ui.screens.PlaylistScreen
import app.stash.ui.screens.SearchScreen
import app.stash.ui.screens.SettingsScreen
import kotlinx.coroutines.flow.Flow

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val LIBRARY = "library"
    const val PLAYLIST = "playlist/{id}"
    const val ALBUM = "album?name={name}&artist={artist}"
    const val ARTIST = "artist?name={name}"
    const val LIKED = "liked"
    const val DOWNLOADS = "downloads"
    const val SETTINGS = "settings"
    const val IMPORT = "import?link={link}"
    const val IMPORT_PROGRESS = "import/progress"
    const val RESCAN = "rescan"
    const val COVER_GRAB = "covers/exportify"
    const val YOUTUBE_LOGIN = "youtube/login"
    const val YT_PLAYLIST = "ytplaylist/{id}"
    const val FIX = "fix/{id}?q={q}"
    const val REMOTE_ARTIST = "rartist/{provider}/{id}?name={name}&art={art}"

    fun playlist(id: Long) = "playlist/$id"
    fun album(name: String, artist: String) = "album?name=${Uri.encode(name)}&artist=${Uri.encode(artist)}"
    fun artist(name: String) = "artist?name=${Uri.encode(name)}"
    fun import(link: String? = null) = if (link == null) "import" else "import?link=${Uri.encode(link)}"
    fun fix(unmatchedId: Long, query: String) = "fix/$unmatchedId?q=${Uri.encode(query)}"
    fun ytPlaylist(id: String) = "ytplaylist/${Uri.encode(id)}"
    fun remoteArtist(a: RemoteArtist) =
        "rartist/${a.provider}/${Uri.encode(a.id)}?name=${Uri.encode(a.name)}&art=${Uri.encode(a.avatarUrl ?: "")}"
}

val LocalNav = staticCompositionLocalOf<NavHostController> { error("No nav") }

private data class Tab(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Outlined.Home, Icons.Rounded.Home),
    Tab(Routes.SEARCH, "Search", Icons.Outlined.Search, Icons.Rounded.Search),
    Tab(Routes.LIBRARY, "Your library", Icons.Outlined.LibraryMusic, Icons.Rounded.LibraryMusic),
)

@Composable
fun StashRoot(app: AppViewModel, navRequests: Flow<String>, openPlayerRequests: Flow<Unit>) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    // only "is anything playing" here; the full player state is read inside the player views,
    // so position / buffering updates don't redraw the whole app
    val currentId by app.player.currentId.collectAsState()
    var showPlayer by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { app.messages.collect { snackbar.currentSnackbarData?.dismiss(); snackbar.showSnackbar(it) } }
    LaunchedEffect(Unit) { navRequests.collect { nav.navigate(it) { launchSingleTop = true } } }
    LaunchedEffect(Unit) { openPlayerRequests.collect { showPlayer = true } }

    CompositionLocalProvider(LocalActions provides app, LocalNav provides nav) {
        val backStack by nav.currentBackStackEntryAsState()
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            contentWindowInsets = WindowInsets(0),
            bottomBar = {
                Column {
                    AnimatedVisibility(
                        visible = currentId != null,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut(),
                    ) {
                        val player by app.player.state.collectAsState()
                        MiniPlayer(state = player, onOpen = { showPlayer = true })
                    }
                    NavigationBar {
                        tabs.forEach { tab ->
                            val selected = backStack?.destination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    nav.navigate(tab.route) {
                                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(if (selected) tab.selectedIcon else tab.icon, null) },
                                label = { Text(tab.label) },
                            )
                        }
                    }
                }
            },
        ) { padding ->
            NavHost(
                navController = nav,
                startDestination = Routes.HOME,
                modifier = Modifier.padding(padding).consumeWindowInsets(padding),
            ) {
                composable(Routes.HOME) { HomeScreen() }
                composable(Routes.SEARCH) { SearchScreen() }
                composable(Routes.LIBRARY) { LibraryScreen() }
                composable(Routes.PLAYLIST, listOf(navArgument("id") { type = NavType.LongType })) {
                    PlaylistScreen(it.arguments!!.getLong("id"))
                }
                composable(Routes.ALBUM, listOf(navArgument("name") { defaultValue = "" }, navArgument("artist") { defaultValue = "" })) {
                    AlbumScreen(it.arguments!!.getString("name")!!, it.arguments!!.getString("artist")!!)
                }
                composable(Routes.ARTIST, listOf(navArgument("name") { defaultValue = "" })) { ArtistPageScreen() }
                composable(
                    Routes.REMOTE_ARTIST,
                    listOf(
                        navArgument("provider") {}, navArgument("id") {},
                        navArgument("name") { defaultValue = "" }, navArgument("art") { defaultValue = "" },
                    ),
                ) { ArtistPageScreen() }
                composable(Routes.LIKED) { LikedScreen() }
                composable(Routes.DOWNLOADS) { DownloadsScreen() }
                composable(Routes.SETTINGS) { SettingsScreen() }
                composable(Routes.IMPORT, listOf(navArgument("link") { nullable = true; defaultValue = null })) {
                    ImportScreen(it.arguments?.getString("link"))
                }
                composable(Routes.IMPORT_PROGRESS) { ImportProgressScreen() }
                composable(Routes.RESCAN) { RescanScreen() }
                composable(Routes.COVER_GRAB) { CoverGrabScreen() }
                composable(Routes.YOUTUBE_LOGIN) { YouTubeLoginScreen() }
                composable(Routes.YT_PLAYLIST, listOf(navArgument("id") {})) { YtPlaylistScreen() }
                composable(
                    Routes.FIX,
                    listOf(navArgument("id") { type = NavType.LongType }, navArgument("q") { defaultValue = "" }),
                ) {
                    SearchScreen(fixUnmatchedId = it.arguments!!.getLong("id"), initialQuery = it.arguments!!.getString("q")!!)
                }
            }
        }

        if (showPlayer && currentId != null) {
            val player by app.player.state.collectAsState()
            NowPlayingSheet(state = player, onDismiss = { showPlayer = false })
        }
    }
}
