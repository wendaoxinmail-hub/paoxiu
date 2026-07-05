package com.wendao.run.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wendao.run.core.database.entity.RunRecordEntity
import com.wendao.run.feature.auth.LoginScreen
import com.wendao.run.feature.breakthrough.BreakthroughScreen
import com.wendao.run.feature.cultivate.CultivateScreen
import com.wendao.run.feature.grotto.GrottoScreen
import com.wendao.run.feature.market.MarketScreen
import com.wendao.run.feature.profile.ProfileScreen
import com.wendao.run.feature.profile.ProfileEditScreen
import com.wendao.run.feature.run.RunDetailScreen
import com.wendao.run.feature.run.RunHistoryScreen
import com.wendao.run.feature.run.RunScreen
import com.wendao.run.feature.run.RunSummaryScreen
import com.wendao.run.feature.sect.SectScreen
import com.wendao.run.feature.spiritroot.SpiritRootIntroScreen
import com.wendao.run.feature.spiritroot.SpiritRootResultScreen
import com.wendao.run.feature.story.StoryIntroScreen
import com.wendao.run.feature.technique.TechniqueDetailScreen
import com.wendao.run.ui.navigation.MainTab
import com.wendao.run.ui.splash.AppSplashScreen
import com.wendao.run.ui.splash.splashHoldMillis
import com.wendao.run.ui.theme.PaoxiuColors

/**
 * 应用根导航：Keep 式底部四 Tab + 全屏跑步/灵根流程。
 */
@Composable
fun PaoxiuApp() {
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(splashHoldMillis.toLong())
        showSplash = false
    }

    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showTabs = currentRoute in MainTab.entries.map { it.route }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = PaoxiuColors.KeepCanvas,
        bottomBar = {
            if (showTabs) {
                NavigationBar(
                    containerColor = PaoxiuColors.NavBar,
                    tonalElevation = 0.dp,
                    windowInsets = NavigationBarDefaults.windowInsets,
                ) {
                    MainTab.entries.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    tab.icon,
                                    contentDescription = tab.label,
                                    tint = if (selected) PaoxiuColors.KeepGreen else PaoxiuColors.KeepTextSecondary,
                                )
                            },
                            label = {
                                Text(
                                    tab.label,
                                    color = if (selected) PaoxiuColors.KeepGreen else PaoxiuColors.KeepTextSecondary,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = PaoxiuColors.KeepGreen.copy(alpha = 0.12f),
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "login",
            modifier = Modifier.padding(padding),
        ) {
            composable("login") {
                LoginScreen(
                    onGuestLoggedIn = { user, needsStoryIntro ->
                        val destination = when {
                            needsStoryIntro -> "story_intro"
                            !user.spiritRootTestCompleted -> "spirit_root_intro"
                            else -> MainTab.Cultivate.route
                        }
                        navController.navigate(destination) {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                )
            }
            composable("story_intro") {
                StoryIntroScreen(
                    onContinue = { navController.navigate("spirit_root_intro") },
                    onSkip = { navController.navigate("spirit_root_intro") },
                )
            }
            composable("spirit_root_intro") {
                SpiritRootIntroScreen(
                    onStartTest = {
                        navController.navigate("run/${RunRecordEntity.RUN_TYPE_SPIRIT_ROOT}")
                    },
                )
            }
            composable(MainTab.Cultivate.route) {
                CultivateScreen(
                    onStartRun = {
                        navController.navigate("run/${RunRecordEntity.RUN_TYPE_NORMAL}")
                    },
                    onOpenRunDetail = { runId ->
                        navController.navigate("run_detail/$runId")
                    },
                    onOpenRunHistory = {
                        navController.navigate("run_history")
                    },
                    onOpenTechniqueDetail = { id ->
                        navController.navigate("technique_detail/$id")
                    },
                )
            }
            composable(MainTab.Grotto.route) {
                GrottoScreen(
                    onOpenTechniqueDetail = { id ->
                        navController.navigate("technique_detail/$id")
                    },
                )
            }
            composable(MainTab.Sect.route) { SectScreen() }
            composable(MainTab.Profile.route) {
                ProfileScreen(
                    onOpenMarket = { navController.navigate("market") },
                    onEditProfile = { navController.navigate("profile_edit") },
                    onOpenRunHistory = { navController.navigate("run_history") },
                    onLogout = {
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            }
            composable("profile_edit") {
                ProfileEditScreen(onBack = { navController.popBackStack() })
            }
            composable("run_history") {
                RunHistoryScreen(
                    onBack = { navController.popBackStack() },
                    onOpenRun = { runId -> navController.navigate("run_detail/$runId") },
                )
            }
            composable(
                route = "run_detail/{runId}",
                arguments = listOf(navArgument("runId") { type = NavType.StringType }),
            ) { entry ->
                val runId = entry.arguments?.getString("runId").orEmpty()
                RunDetailScreen(
                    runId = runId,
                    onBack = { navController.popBackStack() },
                )
            }
            composable("market") {
                MarketScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = "technique_detail/{techniqueId}",
                arguments = listOf(navArgument("techniqueId") { type = NavType.StringType }),
            ) { entry ->
                val techniqueId = entry.arguments?.getString("techniqueId").orEmpty()
                TechniqueDetailScreen(
                    techniqueId = techniqueId,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = "run/{runType}",
                arguments = listOf(navArgument("runType") { type = NavType.StringType }),
            ) {
                RunScreen(
                    onBack = { navController.popBackStack() },
                    onRunFinished = { runId ->
                        val isSpiritRoot = navController.currentBackStackEntry
                            ?.arguments
                            ?.getString("runType") == RunRecordEntity.RUN_TYPE_SPIRIT_ROOT
                        if (isSpiritRoot) {
                            navController.navigate("spirit_root_result/$runId") {
                                popUpTo("spirit_root_intro") { inclusive = true }
                            }
                        } else {
                            navController.navigate("run_summary/$runId") {
                                popUpTo("run/{runType}") { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    },
                )
            }
            composable(
                route = "run_summary/{runId}",
                arguments = listOf(navArgument("runId") { type = NavType.StringType }),
            ) { entry ->
                val runId = entry.arguments?.getString("runId").orEmpty()
                RunSummaryScreen(
                    runId = runId,
                    onBreakthrough = { id ->
                        navController.navigate("breakthrough/$id")
                    },
                    onOpenDetail = { id ->
                        navController.navigate("run_detail/$id")
                    },
                    onDone = {
                        navController.navigate(MainTab.Cultivate.route) {
                            popUpTo(MainTab.Cultivate.route) { inclusive = true }
                        }
                    },
                )
            }
            composable(
                route = "breakthrough/{runId}",
                arguments = listOf(navArgument("runId") { type = NavType.StringType }),
            ) { entry ->
                val runId = entry.arguments?.getString("runId").orEmpty()
                BreakthroughScreen(
                    runId = runId,
                    onDone = {
                        navController.navigate(MainTab.Cultivate.route) {
                            popUpTo(MainTab.Cultivate.route) { inclusive = true }
                        }
                    },
                )
            }
            composable(
                route = "spirit_root_result/{runId}",
                arguments = listOf(navArgument("runId") { type = NavType.StringType }),
            ) { entry ->
                val runId = entry.arguments?.getString("runId").orEmpty()
                SpiritRootResultScreen(
                    runId = runId,
                    onEnterCultivation = {
                        navController.navigate(MainTab.Cultivate.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            }
        }
    }

        AnimatedVisibility(
            visible = showSplash,
            exit = fadeOut(animationSpec = tween(600)),
        ) {
            AppSplashScreen()
        }
    }
}
