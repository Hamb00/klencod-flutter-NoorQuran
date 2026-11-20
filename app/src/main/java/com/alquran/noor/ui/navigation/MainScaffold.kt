package com.alquran.noor.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.alquran.noor.QuranApp
import com.alquran.noor.R
import com.alquran.noor.ui.bookmarks.BookmarksScreen
import com.alquran.noor.ui.components.MiniPlayer
import com.alquran.noor.ui.home.HomeScreen
import com.alquran.noor.ui.reader.ReaderScreen
import com.alquran.noor.ui.search.SearchScreen
import com.alquran.noor.ui.settings.SettingsScreen

private data class TopLevelTab(
    val route: Any,
    val label: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

@Composable
fun MainScaffold() {
    val context = LocalContext.current
    val container = remember { (context.applicationContext as QuranApp).container }
    val nav = rememberNavController()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val playback by container.player.state.collectAsStateWithLifecycle()

    val tabs = remember {
        listOf(
            TopLevelTab(HomeRoute, R.string.nav_quran, Icons.AutoMirrored.Outlined.MenuBook, Icons.AutoMirrored.Filled.MenuBook),
            TopLevelTab(SearchRoute, R.string.nav_search, Icons.Outlined.Search, Icons.Filled.Search),
            TopLevelTab(BookmarksRoute, R.string.nav_bookmarks, Icons.Outlined.Bookmarks, Icons.Filled.Bookmarks),
            TopLevelTab(SettingsRoute, R.string.nav_settings, Icons.Outlined.Settings, Icons.Filled.Settings),
        )
    }
    val showNavigationBar = tabs.any { destination?.hasRoute(it.route::class) == true }

    // طلب إذن الإشعارات (Android 13+) عند أول تشغيل للتلاوة
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(playback.hasMedia) {
        if (playback.hasMedia &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Column {
                if (playback.hasMedia) {
                    MiniPlayer(
                        state = playback,
                        controller = container.player,
                        padNavigationBar = !showNavigationBar,
                        onOpen = {
                            nav.navigate(ReaderRoute(playback.surah, playback.ayah)) { launchSingleTop = true }
                        },
                    )
                }
                if (showNavigationBar) {
                    NavigationBar {
                        tabs.forEach { tab ->
                            val selected = destination?.hasRoute(tab.route::class) == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    nav.navigate(tab.route) {
                                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.icon,
                                        contentDescription = null,
                                    )
                                },
                                label = { Text(stringResource(tab.label)) },
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = HomeRoute,
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            composable<HomeRoute> {
                HomeScreen(onOpenReader = { surah, ayah -> nav.navigate(ReaderRoute(surah, ayah)) })
            }
            composable<SearchRoute> {
                SearchScreen(onOpenReader = { surah, ayah -> nav.navigate(ReaderRoute(surah, ayah)) })
            }
            composable<BookmarksRoute> {
                BookmarksScreen(onOpenReader = { surah, ayah -> nav.navigate(ReaderRoute(surah, ayah)) })
            }
            composable<SettingsRoute> {
                SettingsScreen()
            }
            composable<ReaderRoute> {
                ReaderScreen(
                    onBack = { nav.popBackStack() },
                    onOpenSurah = { surah ->
                        nav.navigate(ReaderRoute(surah)) {
                            popUpTo<ReaderRoute> { inclusive = true }
                        }
                    },
                )
            }
        }
    }
}
