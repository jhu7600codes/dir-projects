package app.venderian

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.venderian.ui.keyboard.KeyboardScreen
import app.venderian.ui.learn.LearnScreen
import app.venderian.ui.learn.LessonScreen
import app.venderian.ui.course.LessonPlayer
import app.venderian.ui.dictionary.DictionaryScreen
import app.venderian.ui.settings.SettingsScreen
import app.venderian.ui.practice.PracticeScreen
import app.venderian.ui.progress.ProgressScreen
import app.venderian.ui.quiz.QuizScreen
import app.venderian.ui.theme.VenderianTheme

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Learn("learn", "Learn", Icons.Outlined.AutoStories),
    Practice("practice", "Practice", Icons.Outlined.Style),
    Quiz("quiz", "Quiz", Icons.Outlined.Quiz),
    Keyboard("keyboard", "Keyboard", Icons.Outlined.Keyboard),
    Progress("progress", "Progress", Icons.Outlined.Whatshot),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { VenderianTheme { Root() } }
    }
}

@androidx.compose.runtime.Composable
private fun Root() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    Scaffold(
        bottomBar = {
            if (Tab.entries.any { it.route == route }) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            nav, Tab.Learn.route, Modifier.padding(padding),
            enterTransition = { fadeIn() }, exitTransition = { fadeOut() },
        ) {
            composable(Tab.Learn.route) {
                LearnScreen(
                    onOpenLesson = { nav.navigate("lesson/$it") },
                    onOpenCourse = { nav.navigate("course/$it") },
                    onOpenDictionary = { nav.navigate("dictionary") },
                    onOpenSettings = { nav.navigate("settings") },
                )
            }
            composable(
                "settings",
                enterTransition = { slideInHorizontally { it } + fadeIn() },
                popExitTransition = { slideOutHorizontally { it } + fadeOut() },
            ) { SettingsScreen(onBack = { nav.popBackStack() }) }
            composable(
                "dictionary",
                enterTransition = { slideInHorizontally { it } + fadeIn() },
                popExitTransition = { slideOutHorizontally { it } + fadeOut() },
            ) {
                DictionaryScreen(onBack = { nav.popBackStack() })
            }
            composable(
                "course/{id}",
                enterTransition = { slideInVertically { it } + fadeIn() },
                popExitTransition = { slideOutVertically { it } + fadeOut() },
            ) { LessonPlayer(it.arguments!!.getString("id")!!, onClose = { nav.popBackStack() }) }
            composable(
                "lesson/{index}",
                arguments = listOf(navArgument("index") { type = NavType.IntType }),
                enterTransition = { slideInHorizontally { it } + fadeIn() },
                popExitTransition = { slideOutHorizontally { it } + fadeOut() },
            ) { LessonScreen(it.arguments!!.getInt("index"), onDone = { nav.popBackStack() }) }
            composable(Tab.Practice.route) { PracticeScreen(onOpenDictionary = { nav.navigate("dictionary") }) }
            composable(Tab.Quiz.route) { QuizScreen() }
            composable(Tab.Keyboard.route) { KeyboardScreen() }
            composable(Tab.Progress.route) { ProgressScreen(onOpenSettings = { nav.navigate("settings") }) }
        }
    }
}
