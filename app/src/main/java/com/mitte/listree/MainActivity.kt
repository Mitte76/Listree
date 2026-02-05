package com.mitte.listree

import android.app.DownloadManager
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.mitte.listree.navigation.Routes
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

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        ContextCompat.registerReceiver(
            this,
            downloadCompletedReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )

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
            val currentRoute = navBackStackEntry?.destination?.route
            val themeChangedState =
                navBackStackEntry?.savedStateHandle?.getStateFlow("theme_changed", false)
                    ?.collectAsState()

            LaunchedEffect(themeChangedState?.value) {
                if (themeChangedState?.value == true) {
                    customColors = themePersistence.loadTheme(themeName, defaultColors)
                    navBackStackEntry?.savedStateHandle?.set("theme_changed", false)
                }
            }

            LaunchedEffect(lisTreeViewModel.shareListEvent) {
                lisTreeViewModel.shareListEvent.collect { listText ->
                    val sendIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, listText)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, null)
                    startActivity(shareIntent)
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
                                    UpdateManager.startUpdateDownload(
                                        this@MainActivity,
                                        showUpdateDialog.value!!
                                    )
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
                Scaffold(
                    topBar = {
                        println("currentRoute: $currentRoute")
                        val listId = if (currentRoute == Routes.SHOPPING_ITEMS) {
                            println("currentRoute: $currentRoute")
                            navBackStackEntry?.arguments?.getString("listId")
                        } else {
                            null
                        }

                        val tempTitle =
                            if (listId != null) lisTreeViewModel.getListById(listId)?.name else null

                        val title =
                            if (currentRoute == Routes.SETTINGS) stringResource(R.string.settings_title) else tempTitle
                                ?: stringResource(R.string.app_name)

                        println("listId: $listId")
                        TopAppBar(
                            title = { Text(title) },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = LisTreeTheme.colors.topAppBarContainer,
                                titleContentColor = LisTreeTheme.colors.topAppBarTitle
                            ),
                            navigationIcon = {
                                if (currentRoute != Routes.SHOPPING_LISTS) {
                                    IconButton(onClick = { navController.popBackStack() }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = stringResource(R.string.back_button_description)
                                        )
                                    }
                                }
                            },
                            actions = {
                                if (currentRoute != Routes.SETTINGS) {
                                    if (listId != null) {
                                        IconButton(onClick = {
                                            lisTreeViewModel.onShareListClicked(
                                                listId
                                            )
                                        }) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Send,
                                                contentDescription = "Share List"
                                            )
                                        }
                                    }

                                    IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = stringResource(R.string.settings_title)
                                        )
                                    }
                                }

                            })
                    },
                    floatingActionButton = {
                        when (currentRoute) {
                            Routes.SHOPPING_LISTS -> {
                                FloatingActionButton(
                                    onClick = { lisTreeViewModel.onAddListClicked() },
                                    containerColor = LisTreeTheme.colors.topAppBarContainer
                                ) {
                                    Icon(
                                        Icons.Filled.Add,
                                        contentDescription = stringResource(R.string.add_new_list)
                                    )
                                }
                            }

                            Routes.SHOPPING_ITEMS -> {
                                val listId = navBackStackEntry?.arguments?.getString("listId")
                                if (listId != null) {
                                    FloatingActionButton(
                                        onClick = { lisTreeViewModel.onAddItemClicked(listId) },
                                        containerColor = LisTreeTheme.colors.topAppBarContainer
                                    ) {
                                        Icon(
                                            Icons.Filled.Add,
                                            contentDescription = stringResource(R.string.add_new_list)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    bottomBar = {
                        if (currentRoute != Routes.SETTINGS) {
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Routes.SHOPPING_LISTS
                    ) {
                        composable(Routes.SHOPPING_LISTS) {
                            MainView(
                                modifier = Modifier.padding(innerPadding),
                                viewModel = lisTreeViewModel,
                                navController = navController
                            )
                        }

                        composable(
                            route = Routes.SHOPPING_ITEMS,
                            arguments = listOf(navArgument("listId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val listId = backStackEntry.arguments?.getString("listId") ?: ""
                            ListView(
                                modifier = Modifier.padding(innerPadding),
                                viewModel = lisTreeViewModel,
                                listId = listId,
                                navController = navController
                            )
                        }
                        composable(Routes.SETTINGS) {
                            SettingsScreen(
                                navController = navController,
                                mainActivity = this@MainActivity,
                                viewModel = lisTreeViewModel
                            )
                        }
                        composable(Routes.THEME_EDITOR) {
                            ThemeEditorScreen(navController = navController)
                        }
                        composable(
                            route = Routes.COMPONENT_THEME_EDITOR,
                            arguments = listOf(navArgument("componentName") {
                                type = NavType.StringType
                            })
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
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(downloadCompletedReceiver)
    }
}
