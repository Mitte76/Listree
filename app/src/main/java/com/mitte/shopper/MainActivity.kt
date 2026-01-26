package com.mitte.shopper

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
import com.mitte.shopper.ui.theme.ShopperTheme
import com.mitte.shopper.ui.views.ListView
import com.mitte.shopper.ui.views.MainView

class MainActivity : ComponentActivity() {
    private val shoppingViewModel by viewModels<ShoppingViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShopperTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "shoppingLists"
                ) {
                    composable("shoppingLists") {
                        MainView(
                            viewModel = shoppingViewModel,
                            navController = navController
                        )
                    }

                    composable(
                        route = "shoppingItems/{listId}",
                        arguments = listOf(navArgument("listId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val listId = backStackEntry.arguments?.getInt("listId") ?: -1
                        ListView(
                            viewModel = shoppingViewModel,
                            listId = listId
                        )
                    }
                }
            }
        }
    }
}
