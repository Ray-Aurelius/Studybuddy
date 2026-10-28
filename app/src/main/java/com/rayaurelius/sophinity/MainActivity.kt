package com.rayaurelius.sophinity

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.only
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.rayaurelius.sophinity.data.AppSettings
import com.rayaurelius.sophinity.data.NavPosition
import com.rayaurelius.sophinity.data.TabId
import com.rayaurelius.sophinity.ui.components.AppBackground
import com.rayaurelius.sophinity.ui.settings.AccessibilityScreen
import com.rayaurelius.sophinity.ui.settings.AppearanceScreen
import com.rayaurelius.sophinity.ui.settings.ToolbarScreen
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
import com.rayaurelius.sophinity.timer.TimerNotifier
import com.rayaurelius.sophinity.ui.badges.BadgesScreen
import com.rayaurelius.sophinity.ui.goals.GoalsScreen
import com.rayaurelius.sophinity.ui.library.LibraryScreen
import com.rayaurelius.sophinity.ui.onboarding.OnboardingScreen
import com.rayaurelius.sophinity.ui.settings.SettingsScreen
import com.rayaurelius.sophinity.ui.stats.StatsScreen
import com.rayaurelius.sophinity.ui.theme.SophinityTheme
import com.rayaurelius.sophinity.ui.today.TodayScreen
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable object TodayRoute
@Serializable object StatsRoute
@Serializable object LibraryRoute
@Serializable object GoalsRoute
@Serializable object BadgesRoute
@Serializable object SettingsRoute
@Serializable object AppearanceRoute
@Serializable object AccessibilityRoute
@Serializable object ToolbarRoute

private data class Tab(val route: Any, val cls: KClass<*>, @androidx.annotation.StringRes val label: Int, val on: ImageVector, val off: ImageVector, val id: TabId?)

private val tabs = listOf(
    Tab(TodayRoute, TodayRoute::class, R.string.nav_today, Icons.Filled.Timer, Icons.Outlined.Timer, null),
    Tab(StatsRoute, StatsRoute::class, R.string.nav_stats, Icons.Filled.BarChart, Icons.Outlined.BarChart, TabId.STATS),
    Tab(LibraryRoute, LibraryRoute::class, R.string.nav_library, Icons.Filled.AutoStories, Icons.Outlined.AutoStories, TabId.LIBRARY),
    Tab(GoalsRoute, GoalsRoute::class, R.string.nav_goals, Icons.Filled.Flag, Icons.Outlined.Flag, TabId.GOALS),
    Tab(BadgesRoute, BadgesRoute::class, R.string.nav_laurels, Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents, TabId.LAURELS),
)

class MainActivity : AppCompatActivity() {
    private val container get() = (application as SophinityApp).container

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
            SophinityTheme(s) {
                if (!s.onboarded) OnboardingScreen(onDone = {}) else AppShell(s)
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
private fun AppShell(settings: AppSettings) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val dest = entry?.destination
    val visibleTabs = tabs.filter { it.id == null || it.id !in settings.hiddenTabs }
    val onTab = tabs.any { t -> dest?.hasRoute(t.cls) == true }

    fun go(route: Any) = nav.navigate(route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    val showRail = onTab && settings.navPosition != NavPosition.BOTTOM
    val rail: @Composable () -> Unit = {
        NavigationRail(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
            Spacer(Modifier.weight(1f))
            visibleTabs.forEach { t ->
                val selected = dest?.hasRoute(t.cls) == true
                NavigationRailItem(
                    selected = selected,
                    onClick = { go(t.route) },
                    icon = { Icon(if (selected) t.on else t.off, contentDescription = if (settings.navLabels) null else stringResource(t.label)) },
                    label = if (settings.navLabels) ({ Text(stringResource(t.label), maxLines = 1, overflow = TextOverflow.Ellipsis) }) else null,
                    alwaysShowLabel = settings.navLabels,
                )
            }
            Spacer(Modifier.weight(1f))
        }
    }

    Box(Modifier.fillMaxSize()) {
        AppBackground(settings.background, settings.backgroundDim, settings.backgroundBlur)
        // The rail is placed by physical side (left/right) even in right-to-left languages.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(Modifier.fillMaxSize()) {
                if (showRail && settings.navPosition == NavPosition.LEFT) RestoreDirection { rail() }
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    RestoreDirection {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onBackground,
                            contentWindowInsets = if (showRail) ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Vertical)
                                else ScaffoldDefaults.contentWindowInsets,
                            bottomBar = {
                                if (onTab && settings.navPosition == NavPosition.BOTTOM) NavigationBar {
                                    visibleTabs.forEach { t ->
                                        val selected = dest?.hasRoute(t.cls) == true
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { go(t.route) },
                                            icon = { Icon(if (selected) t.on else t.off, contentDescription = if (settings.navLabels) null else stringResource(t.label)) },
                                            label = if (settings.navLabels) ({ Text(stringResource(t.label), maxLines = 1, overflow = TextOverflow.Ellipsis) }) else null,
                                            alwaysShowLabel = settings.navLabels,
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
                                composable<SettingsRoute> {
                                    SettingsScreen(
                                        onBack = { nav.popBackStack() },
                                        onOpenAppearance = { nav.navigate(AppearanceRoute) },
                                        onOpenAccessibility = { nav.navigate(AccessibilityRoute) },
                                        onOpenToolbar = { nav.navigate(ToolbarRoute) },
                                    )
                                }
                                composable<AppearanceRoute> { AppearanceScreen(onBack = { nav.popBackStack() }) }
                                composable<AccessibilityRoute> { AccessibilityScreen(onBack = { nav.popBackStack() }) }
                                composable<ToolbarRoute> { ToolbarScreen(onBack = { nav.popBackStack() }) }
                            }
                        }
                    }
                }
                if (showRail && settings.navPosition == NavPosition.RIGHT) RestoreDirection { rail() }
            }
        }
    }
}

/** Re-applies the app's real layout direction inside an LTR-forced container. */
@Composable
private fun RestoreDirection(content: @Composable () -> Unit) {
    val config = androidx.compose.ui.platform.LocalConfiguration.current
    val dir = if (config.layoutDirection == android.view.View.LAYOUT_DIRECTION_RTL) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides dir, content = content)
}
