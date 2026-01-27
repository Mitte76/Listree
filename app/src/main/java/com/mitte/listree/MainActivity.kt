package com.mitte.listree

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mitte.listree.ui.theme.LisTreeTheme
import com.mitte.listree.ui.views.ListView
import com.mitte.listree.ui.views.MainView

class MainActivity : ComponentActivity() {
    private val lisTreeViewModel by viewModels<LisTreeViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LisTreeTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "shoppingLists"
                ) {
                    composable("shoppingLists") {
                        MainView(
                            viewModel = lisTreeViewModel,
                            navController = navController
                        )
                    }

                    composable(
                        route = "shoppingItems/{listId}",
                        arguments = listOf(navArgument("listId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val listId = backStackEntry.arguments?.getString("listId") ?: ""
                        ListView(
                            viewModel = lisTreeViewModel,
                            listId = listId
                        )
                    }
                }
            }
        }
    }
}
