package com.rayaurelius.studybuddy

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rayaurelius.studybuddy.timer.TimerNotifier
import com.rayaurelius.studybuddy.ui.badges.BadgesScreen
import com.rayaurelius.studybuddy.ui.goals.GoalsScreen
import com.rayaurelius.studybuddy.ui.library.LibraryScreen
import com.rayaurelius.studybuddy.ui.onboarding.OnboardingScreen
import com.rayaurelius.studybuddy.ui.settings.SettingsScreen
import com.rayaurelius.studybuddy.ui.stats.StatsScreen
import com.rayaurelius.studybuddy.ui.theme.StudybuddyTheme
import com.rayaurelius.studybuddy.ui.today.TodayScreen
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable object TodayRoute
@Serializable object StatsRoute
@Serializable object LibraryRoute
@Serializable object GoalsRoute
@Serializable object BadgesRoute
@Serializable object SettingsRoute

private data class Tab(val route: Any, val cls: KClass<*>, val label: String, val on: ImageVector, val off: ImageVector)

private val tabs = listOf(
    Tab(TodayRoute, TodayRoute::class, "Today", Icons.Filled.Timer, Icons.Outlined.Timer),
    Tab(StatsRoute, StatsRoute::class, "Stats", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    Tab(LibraryRoute, LibraryRoute::class, "Library", Icons.Filled.AutoStories, Icons.Outlined.AutoStories),
    Tab(GoalsRoute, GoalsRoute::class, "Goals", Icons.Filled.Flag, Icons.Outlined.Flag),
    Tab(BadgesRoute, BadgesRoute::class, "Badges", Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents),
)

class MainActivity : ComponentActivity() {
    private val container get() = (application as StudyBuddyApp).container

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        splash.setKeepOnScreenCondition { container.timer.state.value == null }

        setContent {
            val settings by container.settingsStore.settings.collectAsStateWithLifecycle(initialValue = null)
            val s = settings ?: return@setContent
            LifecycleResumeEffect(Unit) {
                container.repo.refreshDate()
                onPauseOrDispose { }
            }
            StudybuddyTheme(s.themeMode, s.dynamicColor) {
                if (!s.onboarded) OnboardingScreen(onDone = {}) else AppShell()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val id = intent?.getLongExtra(TimerNotifier.EXTRA_REFLECT_SESSION, -1L) ?: -1L
        if (id > 0) {
            container.pendingReflection.value = id
            intent?.removeExtra(TimerNotifier.EXTRA_REFLECT_SESSION)
        }
    }
}

@Composable
private fun AppShell() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val dest = entry?.destination
    val onTab = tabs.any { t -> dest?.hasRoute(t.cls) == true }

    fun go(route: Any) = nav.navigate(route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (onTab) NavigationBar {
                tabs.forEach { t ->
                    val selected = dest?.hasRoute(t.cls) == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = { go(t.route) },
                        icon = { Icon(if (selected) t.on else t.off, null) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { pad ->
        NavHost(nav, startDestination = TodayRoute) {
            composable<TodayRoute> {
                TodayScreen(pad, onOpenSettings = { nav.navigate(SettingsRoute) }, onOpenGoals = { go(GoalsRoute) }, onOpenBadges = { go(BadgesRoute) })
            }
            composable<StatsRoute> { StatsScreen(pad) }
            composable<LibraryRoute> { LibraryScreen(pad) }
            composable<GoalsRoute> { GoalsScreen(pad) }
            composable<BadgesRoute> { BadgesScreen(pad) }
            composable<SettingsRoute> { SettingsScreen(onBack = { nav.popBackStack() }) }
        }
    }
}
