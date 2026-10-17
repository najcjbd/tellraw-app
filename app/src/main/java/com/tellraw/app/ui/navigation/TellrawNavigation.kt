package com.tellraw.app.ui.navigation

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
        startDestination = "main"
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