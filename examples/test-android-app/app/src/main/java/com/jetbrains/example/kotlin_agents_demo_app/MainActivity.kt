package com.jetbrains.example.kotlin_agents_demo_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jetbrains.example.kotlin_agents_demo_app.screens.settings.SettingsScreen
import com.jetbrains.example.kotlin_agents_demo_app.screens.start.StartScreen
import com.jetbrains.example.kotlin_agents_demo_app.theme.AppTheme
import kotlinx.serialization.Serializable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController)
                }
            }
        }
    }
}

/**
 * Navigation routes for the app
 */
@Serializable
sealed interface NavRoute {
    @Serializable
    data object StartScreen : NavRoute

    @Serializable
    data object SettingsScreen : NavRoute
}

/**
 * Main navigation graph for the app
 */
@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = NavRoute.StartScreen,
    ) {

        composable<NavRoute.StartScreen> {
            StartScreen(
                onNavigateToSettings = {
                    navController.navigate(NavRoute.SettingsScreen)
                },
                onNavigateToAgentDemo = { }
            )
        }

        composable<NavRoute.SettingsScreen> {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSaveSettings = {
                    navController.popBackStack()
                }
            )
        }
    }
}