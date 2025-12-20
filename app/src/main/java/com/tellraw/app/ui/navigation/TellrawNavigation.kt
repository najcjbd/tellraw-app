package com.tellraw.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tellraw.app.ui.screens.MainScreen
import com.tellraw.app.ui.screens.HelpScreen

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
                }
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