package com.mitte.listree

import android.app.DownloadManager
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mitte.listree.data.ThemePersistence
import com.mitte.listree.model.UpdateInfo
import com.mitte.listree.ui.theme.DarkLisTreeColors
import com.mitte.listree.ui.theme.LightLisTreeColors
import com.mitte.listree.ui.theme.LisTreeTheme
import com.mitte.listree.ui.views.ComponentThemeEditorScreen
import com.mitte.listree.ui.views.ListView
import com.mitte.listree.ui.views.MainView
import com.mitte.listree.ui.views.SettingsScreen
import com.mitte.listree.ui.views.ThemeEditorScreen
import com.mitte.listree.update.DownloadCompletedReceiver
import com.mitte.listree.update.UpdateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private val lisTreeViewModel by viewModels<LisTreeViewModel>()
    private val downloadCompletedReceiver = DownloadCompletedReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        ContextCompat.registerReceiver(this, downloadCompletedReceiver, filter, ContextCompat.RECEIVER_EXPORTED)

        val showUpdateDialog = mutableStateOf<UpdateInfo?>(null)

        CoroutineScope(Dispatchers.IO).launch {
            val updateInfo = UpdateManager.checkForUpdate(this@MainActivity)
            if (updateInfo != null) {
                withContext(Dispatchers.Main) {
                    showUpdateDialog.value = updateInfo
                }
            }
        }

        setContent {
            val isDarkTheme = isSystemInDarkTheme()
            val themeName = if (isDarkTheme) "dark" else "light"
            val defaultColors = if (isDarkTheme) DarkLisTreeColors else LightLisTreeColors
            val themePersistence = remember { ThemePersistence(this) }
            var customColors by remember {
                mutableStateOf(
                    themePersistence.loadTheme(themeName, defaultColors)
                )
            }
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val themeChangedState = navBackStackEntry?.savedStateHandle?.getStateFlow("theme_changed", false)?.collectAsState()

            LaunchedEffect(themeChangedState?.value) {
                if (themeChangedState?.value == true) {
                    customColors = themePersistence.loadTheme(themeName, defaultColors)
                    navBackStackEntry?.savedStateHandle?.set("theme_changed", false)
                }
            }

            LisTreeTheme(customColors = customColors) {
                if (showUpdateDialog.value != null) {
                    AlertDialog(
                        onDismissRequest = { showUpdateDialog.value = null },
                        title = { Text(stringResource(R.string.update_available_title)) },
                        text = { Text(stringResource(R.string.update_available_text)) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    UpdateManager.startUpdateDownload(this@MainActivity, showUpdateDialog.value!!)
                                    showUpdateDialog.value = null
                                }
                            ) {
                                Text(stringResource(R.string.update_button))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showUpdateDialog.value = null }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    )
                }

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
                            listId = listId,
                            navController = navController
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            navController = navController,
                            mainActivity = this@MainActivity,
                            viewModel = lisTreeViewModel
                        )
                    }
                    composable("themeEditor") {
                        ThemeEditorScreen(navController = navController)
                    }
                    composable(
                        route = "componentThemeEditor/{componentName}",
                        arguments = listOf(navArgument("componentName") { type = NavType.StringType })
                    ) { backStackEntry ->
                        ComponentThemeEditorScreen(
                            navController = navController,
                            componentName = backStackEntry.arguments?.getString("componentName")
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(downloadCompletedReceiver)
    }
}
