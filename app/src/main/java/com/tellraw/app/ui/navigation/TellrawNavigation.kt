package com.tellraw.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tellraw.app.ui.screens.GridEditorScreen
import com.tellraw.app.ui.screens.MainScreen
import com.tellraw.app.ui.screens.HelpScreen
import com.tellraw.app.ui.viewmodel.TellrawViewModel

@Composable
fun TellrawNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = "main",
        // D6：页面切换淡入 + 轻微横向位移（比瞬间切换更顺滑，也不拖沓）
        enterTransition = {
            fadeIn(tween(200)) + slideInHorizontally(tween(200)) { it / 10 }
        },
        exitTransition = { fadeOut(tween(140)) },
        popEnterTransition = {
            fadeIn(tween(200)) + slideInHorizontally(tween(200)) { -it / 10 }
        },
        popExitTransition = {
            fadeOut(tween(140)) + slideOutHorizontally(tween(140)) { it / 10 }
        }
    ) {
        composable("main") {
            MainScreen(
                onNavigateToHelp = {
                    navController.navigate("help")
                },
                onNavigateToGrid = {
                    navController.navigate("grid")
                }
            )
        }

        composable("grid") {
            // 与主界面共用同一个 ViewModel（选择器 / 消息 / 目标版本）
            val mainEntry = remember(it) { navController.getBackStackEntry("main") }
            GridEditorScreen(
                viewModel = hiltViewModel(mainEntry),
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable("help") {
            HelpScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}