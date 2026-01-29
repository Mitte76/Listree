package com.mitte.listree.ui.views

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mitte.listree.LisTreeViewModel
import com.mitte.listree.data.ThemePersistence
import com.mitte.listree.ui.models.ListItem
import com.mitte.listree.ui.models.ListType
import com.mitte.listree.ui.models.TreeList
import com.mitte.listree.ui.theme.DarkLisTreeColors
import com.mitte.listree.ui.theme.LightLisTreeColors
import com.mitte.listree.ui.theme.LisTreeTheme
import sh.calvin.reorderable.rememberReorderableLazyListState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeEditorContent(
    navController: NavController,
    onNavigateBack: () -> Unit,
    onResetToDefault: () -> Unit
) {
    val dummyViewModel: LisTreeViewModel = viewModel()
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { _, _ -> }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Theme Editor") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                modifier = Modifier.clickable {
                    Log.d("ThemeEditor", "TopAppBar clicked")
                    navController.navigate("componentThemeEditor/topAppBar")
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LisTreeTheme.colors.topAppBarContainer,
                    titleContentColor = LisTreeTheme.colors.topAppBarTitle,
                ),
                actions = {
                    IconButton(onClick = onResetToDefault) {
                        Icon(Icons.Default.Restore, contentDescription = "Reset to Default")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text("Group Section")
            }
            item {
                GroupSection(
                    list = TreeList("1", "Group List", ListType.GROUP_LIST, isExpanded = false),
                    elevation = 2.dp,
                    onListToEdit = {},
                    onAddSubList = {},
                    onMoveItem = {},
                    viewModel = dummyViewModel,
                    navController = navController,
                    iconButton = {},
                    showDeleted = true,
                    onTap = {
                        Log.d("ThemeEditor", "GroupSection clicked")
                        navController.navigate("componentThemeEditor/groupSection")
                    }
                )
            }
            item {
                GroupSection(
                    list = TreeList(
                        "1",
                        "Deleted Group List",
                        ListType.GROUP_LIST,
                        isExpanded = false,
                        deleted = true
                    ),
                    elevation = 2.dp,
                    onListToEdit = {},
                    onAddSubList = {},
                    onMoveItem = {},
                    viewModel = dummyViewModel,
                    navController = navController,
                    iconButton = {},
                    showDeleted = true,
                    onTap = {
                        Log.d("ThemeEditor", "GroupSectionDeleted clicked")
                        navController.navigate("componentThemeEditor/groupSectionDeleted")
                    }
                )
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Single Section")
            }
            item {
                SingleSection(
                    list = TreeList("4", "Single List", ListType.ITEM_LIST),
                    elevation = 2.dp,
                    onListToEdit = {},
                    onMoveItem = {},
                    onTap = {
                        Log.d("ThemeEditor", "SingleSection clicked")
                        navController.navigate("componentThemeEditor/singleSection")
                    },
                    viewModel = dummyViewModel,
                    showDeleted = false
                )
            }
            item {
                SingleSection(
                    list = TreeList("4", "Deleted Single List", ListType.ITEM_LIST, deleted = true),
                    elevation = 2.dp,
                    onListToEdit = {},
                    onMoveItem = {},
                    onTap = {
                        Log.d("ThemeEditor", "SingleSectionDeleted clicked")
                        navController.navigate("componentThemeEditor/singleSectionDeleted")
                    },
                    viewModel = dummyViewModel,
                    showDeleted = true
                )
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Header Item")
            }
            item {
                HeaderItem(
                    reorderableState = reorderableState,
                    item = ListItem(id = "5", name = "Header Item", isHeader = true),
                    density = LocalDensity.current,
                    onEditItem = {},
                    viewModel = dummyViewModel,
                    listId = "4",
                    onTap = {
                        Log.d("ThemeEditor", "HeaderItem clicked")
                        navController.navigate("componentThemeEditor/headerItem")
                    }
                )
            }
            item {
                HeaderItem(
                    reorderableState = reorderableState,
                    item = ListItem(id = "5", name = "Deleted Header Item", isHeader = true, deleted = true),
                    density = LocalDensity.current,
                    onEditItem = {},
                    viewModel = dummyViewModel,
                    listId = "4",
                    onTap = {
                        Log.d("ThemeEditor", "HeaderItemDeleted clicked")
                        navController.navigate("componentThemeEditor/headerItemDeleted")
                    }
                )
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Normal/Checked Item")
            }
            item {
                NormalItem(
                    reorderableState = reorderableState,
                    item = ListItem(id = "6", name = "Normal/Checked Item", isChecked = false),
                    swipeDirection = null,
                    density = LocalDensity.current,
                    itemHeights = remember { mutableStateMapOf() },
                    viewModel = dummyViewModel,
                    listId = "4",
                    onEditItem = {},
                    onStartPendingDelete = {},
                    onUndoPendingDelete = {},
                    onTap = {
                        Log.d("ThemeEditor", "NormalItem clicked")
                        navController.navigate("componentThemeEditor/normalItem")
                    }
                )
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Deleted Item")
            }
            item {
                NormalItem(
                    reorderableState = reorderableState,
                    item = ListItem(id = "8", name = "Deleted Item", deleted = true),
                    swipeDirection = null,
                    density = LocalDensity.current,
                    itemHeights = remember { mutableStateMapOf() },
                    viewModel = dummyViewModel,
                    listId = "4",
                    onEditItem = {},
                    onStartPendingDelete = {},
                    onUndoPendingDelete = {},
                    onTap = {
                        Log.d("ThemeEditor", "NormalItemDeleted clicked")
                        navController.navigate("componentThemeEditor/normalItemDeleted")
                    }
                )
            }
        }
    }
}

@Composable
fun ThemeEditorScreen(navController: NavController) {
    val context = LocalContext.current
    val themePersistence = ThemePersistence(context)
    val isDarkTheme = isSystemInDarkTheme()
    val themeName = if (isDarkTheme) "dark" else "light"
    val defaultColors = if (isDarkTheme) DarkLisTreeColors else LightLisTreeColors
    var hasChanges by remember { mutableStateOf(false) }
    val (customColors, setCustomColors) = remember {
        mutableStateOf(
            themePersistence.loadTheme(themeName, defaultColors)
        )
    }

    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    if (savedStateHandle != null) {
        val themeChanged by savedStateHandle.getStateFlow("theme_changed", false).collectAsState()

        LaunchedEffect(themeChanged) {
            if (themeChanged) {
                hasChanges = true
                setCustomColors(
                    themePersistence.loadTheme(themeName, defaultColors)
                )
                savedStateHandle["theme_changed"] = false
            }
        }
    }

    val backAction: () -> Unit = {
        if (hasChanges) {
            navController.previousBackStackEntry?.savedStateHandle?.set("theme_changed", true)
        }
        navController.popBackStack()
    }

    BackHandler { backAction() }

    LisTreeTheme(customColors = customColors) {
        ThemeEditorContent(
            navController = navController,
            onNavigateBack = backAction,
            onResetToDefault = {
                themePersistence.saveTheme(
                    themeName,
                    defaultColors
                )
                setCustomColors(
                    defaultColors
                )
                hasChanges = true
            }
        )
    }
}
