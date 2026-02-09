package com.mitte.listree.ui.views.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.mitte.listree.ui.theme.LisTreeColors
import com.mitte.listree.ui.theme.LisTreeTheme
import com.mitte.listree.ui.views.lists.GroupSection
import com.mitte.listree.ui.views.lists.NormalItem
import com.mitte.listree.ui.views.lists.SingleSection
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.reflect.full.memberProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentThemeEditorScreen(
    navController: NavController,
    componentName: String?
) {
    val context = LocalContext.current
    val themePersistence = ThemePersistence(context)
    val isDarkTheme = isSystemInDarkTheme()
    val themeName = if (isDarkTheme) "dark" else "light"
    val defaultColors = if (isDarkTheme) DarkLisTreeColors else LightLisTreeColors
    val dummyViewModel: LisTreeViewModel = viewModel()
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { _, _ -> }
    var isItemChecked by remember { mutableStateOf(false) }
    var hasChanges by remember { mutableStateOf(false) }

    val backAction: () -> Unit = {
        if (hasChanges) {
            navController.previousBackStackEntry?.savedStateHandle?.set("theme_changed", true)
        }
        navController.popBackStack()
    }

    BackHandler { backAction() }


    val (customColors, setCustomColors) = remember {
        mutableStateOf(
            themePersistence.loadTheme(themeName, defaultColors)
        )
    }

    val colorNames = when (componentName) {
        "topAppBar" -> listOf("topAppBarContainer", "topAppBarTitle")
        "groupSection" -> listOf("groupCardContainer", "groupCardContent")
        "groupSectionDeleted" -> listOf("groupCardDeletedContainer", "groupCardDeletedContent")
        "singleSection" -> listOf("singleCardContainer", "singleCardContent")
        "singleSectionDeleted" -> listOf("singleCardDeletedContainer", "singleCardDeletedContent")
        "headerItem" -> listOf("headerItemContainer", "headerItemContent")
        "headerItemDeleted" -> listOf("headerItemDeletedContainer", "headerItemDeletedContent")
        "normalItem", "normalItemChecked" -> listOf(
            "normalItemContainer",
            "normalItemContent",
            "normalItemCheckedContainer",
            "normalItemCheckedContent",
            "strikethrough"
        )

        "normalItemDeleted" -> listOf("normalItemDeletedContainer", "normalItemDeletedContent")
        else -> emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit $componentName") },
                navigationIcon = {
                    IconButton(onClick = backAction) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            item {
                LisTreeTheme(customColors = customColors) {
                    when (componentName) {

                        "groupSection" -> {
                            GroupSection(
                                list = TreeList(
                                    "1",
                                    "Group List",
                                    ListType.GROUP_LIST,
                                    isExpanded = false
                                ),
                                elevation = 2.dp,
                                onListToEdit = {},
                                onAddSubList = {},
                                onMoveItem = {},
                                viewModel = dummyViewModel,
                                navController = navController,
                                iconButton = {},
                                showDeleted = false,
                            )
                        }

                        "groupSectionDeleted" -> {
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
                            )
                        }

                        "singleSection" -> {
                            SingleSection(
                                list = TreeList("4", "Single List", ListType.ITEM_LIST),
                                elevation = 2.dp,
                                onListToEdit = {},
                                onMoveItem = {},
                                viewModel = dummyViewModel,
                                showDeleted = false,
                                onTap = {}
                            )
                        }

                        "singleSectionDeleted" -> {
                            SingleSection(
                                list = TreeList(
                                    "4",
                                    "Deleted Single List",
                                    ListType.ITEM_LIST,
                                    deleted = true
                                ),
                                elevation = 2.dp,
                                onListToEdit = {},
                                onMoveItem = {},
                                viewModel = dummyViewModel,
                                showDeleted = true,
                                onTap = {}
                            )
                        }

                        "normalItem", "normalItemChecked" -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Text("Show as checked")
                                Checkbox(checked = isItemChecked, onCheckedChange = { isItemChecked = it })
                            }
                            NormalItem(
                                reorderableState = reorderableState,
                                item = ListItem(id = "6", name = "Normal Item", isChecked = isItemChecked),
                                swipeDirection = null,
                                density = LocalDensity.current,
                                itemHeights = remember { mutableStateMapOf() },
                                viewModel = dummyViewModel,
                                listId = "4",
                                onEditItem = {},
                                onStartPendingDelete = {},
                                onUndoPendingDelete = {},
                                onTap = {}
                            )
                        }

                        "normalItemDeleted" -> {
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
                            )
                        }
                    }
                }
            }
            items(colorNames) { colorName ->
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    val prop = LisTreeColors::class.memberProperties.find { it.name == colorName }
                    if (prop != null) {
                        val color = prop.get(customColors) as Color
                        ColorPicker(label = colorName, color = color, onColorChange = { newColor ->
                            themePersistence.saveColor(themeName, colorName, newColor, defaultColors)
                            setCustomColors(
                                themePersistence.loadTheme(themeName, defaultColors)
                            )
                            hasChanges = true
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun ColorPicker(label: String, color: Color, onColorChange: (Color) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        ColorSliderDialog(
            color = color,
            onColorChange = onColorChange,
            onDismiss = { showDialog = false })
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color)
        )
        Spacer(modifier = Modifier.padding(horizontal = 8.dp))
        Text(text = label)
    }
}


@Composable
fun ColorSliderDialog(
    color: Color,
    onColorChange: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var red by remember { mutableFloatStateOf(color.red) }
    var green by remember { mutableFloatStateOf(color.green) }
    var blue by remember { mutableFloatStateOf(color.blue) }
    var alpha by remember { mutableFloatStateOf(color.alpha) }
    var hexText by remember { mutableStateOf("") }

    LaunchedEffect(color) {
        red = color.red
        green = color.green
        blue = color.blue
        alpha = color.alpha
        hexText = String.format("%02X%02X%02X%02X", (alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())
    }

    LaunchedEffect(red, green, blue, alpha) {
        hexText = String.format("%02X%02X%02X%02X", (alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Color") },
        text = {
            Column {
                Text("Red")
                Slider(value = red, onValueChange = { red = it })
                Text("Green")
                Slider(value = green, onValueChange = { green = it })
                Text("Blue")
                Slider(value = blue, onValueChange = { blue = it })
                Text("Alpha")
                Slider(value = alpha, onValueChange = { alpha = it })

                OutlinedTextField(
                    value = hexText,
                    onValueChange = { newText ->
                        if (newText.length <= 8) {
                            hexText = newText.uppercase()
                            if (newText.length == 8) {
                                newText.toLongOrNull(16)?.let { colorLong ->
                                    alpha = ((colorLong shr 24) and 0xFF) / 255f
                                    red = ((colorLong shr 16) and 0xFF) / 255f
                                    green = ((colorLong shr 8) and 0xFF) / 255f
                                    blue = (colorLong and 0xFF) / 255f
                                }
                            }
                        }
                    },
                    label = { Text("Hex (AARRGGBB)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(Color(red, green, blue, alpha))
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onColorChange(Color(red, green, blue, alpha))
                    onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
