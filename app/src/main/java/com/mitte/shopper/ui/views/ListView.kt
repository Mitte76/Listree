package com.mitte.shopper.ui.views

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitte.shopper.ShoppingViewModel
import com.mitte.shopper.ui.models.ShoppingItem
import com.mitte.shopper.ui.models.ShoppingList
import com.mitte.shopper.ui.theme.ShopperTheme
import kotlinx.coroutines.delay
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ListView(
    modifier: Modifier = Modifier,
    viewModel: ShoppingViewModel,
    listId: String
) {
    val allLists by viewModel.shoppingLists.collectAsState()
    val currentList = allLists.firstOrNull { it.id == listId }
        ?: allLists.flatMap { it.subLists ?: emptyList() }.firstOrNull { it.id == listId }
        ?: ShoppingList(id = listId, name = "Not Found", items = emptyList())
    val items = currentList.items ?: emptyList()

    var showAddItemDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<ShoppingItem?>(null) }
    var itemsPendingDeletion by remember { mutableStateOf<Map<String, SwipeToDismissBoxValue>>(emptyMap()) }
    val itemHeights = remember { mutableStateMapOf<String, Dp>() }
    val density = LocalDensity.current
    val lazyListState = rememberLazyListState()

    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        viewModel.moveItem(listId, from.index, to.index)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentList.name) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ShopperTheme.colors.topAppBarContainer,
                    titleContentColor = ShopperTheme.colors.topAppBarTitle
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddItemDialog = true },
                containerColor = ShopperTheme.colors.topAppBarContainer
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add new item")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->

        if (showAddItemDialog) {
            AddItemDialog(
                onDismissRequest = { showAddItemDialog = false },
                onConfirm = { itemName, isHeader ->
                    viewModel.addItem(listId, itemName, isHeader)
                    showAddItemDialog = false
                }
            )
        }

        itemToEdit?.let { item ->
            EditItemDialog(
                item = item,
                onDismissRequest = { itemToEdit = null },
                onConfirm = { newItemName ->
                    viewModel.editItemName(listId, item.id, newItemName)
                    itemToEdit = null
                }
            )
        }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(vertical = 8.dp/*, horizontal = 8.dp*/),
            verticalArrangement = Arrangement.spacedBy(8.dp)

        ) {
            items(
                items = items,
                key = { item -> item.id }
            ) { item ->
                val swipeDirection = itemsPendingDeletion[item.id]
                LaunchedEffect(swipeDirection, item.id) {
                    if (swipeDirection != null) {
                        delay(2500)
                        if (itemsPendingDeletion.containsKey(item.id)) {
                            viewModel.removeItem(listId, item)
                        }
                    }
                }

                if (item.isHeader) {
                    HeaderItem(
                        reorderableState, item, density,
                        onEditItem = { selectedItem ->
                            itemToEdit = selectedItem
                        },
                        viewModel,
                        listId
                    )

                } else {

                    NormalItem(
                        reorderableState,
                        item,
                        swipeDirection,
                        density,
                        itemHeights,
                        viewModel,
                        listId,
                        onEditItem = { selectedItem ->
                            itemToEdit = selectedItem
                        },
                        onStartPendingDelete = { direction ->
                            itemsPendingDeletion = itemsPendingDeletion + (item.id to direction)
                        },
                        onUndoPendingDelete = {
                            itemsPendingDeletion = itemsPendingDeletion - item.id
                        }
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LazyItemScope.HeaderItem(
    reorderableState: ReorderableLazyListState,
    item: ShoppingItem,
    density: Density,
    onEditItem: (ShoppingItem) -> Unit,
    viewModel: ShoppingViewModel,
    listId: String

) {
    ReorderableItem(reorderableState, key = item.id) { isDragging ->

        val interactionSource = remember { MutableInteractionSource() }
        var showMenu by remember { mutableStateOf(false) }
        var pressOffset by remember { mutableStateOf(DpOffset.Zero) }

        val elevation by animateDpAsState(
            if (isDragging) 8.dp else 2.dp,
            label = "elevation"
        )

        Surface(
            color = ShopperTheme.colors.sectionContainer,
            contentColor = ShopperTheme.colors.sectionContent,
            shadowElevation = elevation,
            modifier = Modifier
                .indication(interactionSource, rememberRipple())
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { offset ->
                            val press = PressInteraction.Press(offset)
                            interactionSource.emit(press)
                            try {
                                awaitRelease()
                                interactionSource.emit(
                                    PressInteraction.Release(
                                        press
                                    )
                                )
                            } catch (c: CancellationException) {
                                interactionSource.emit(PressInteraction.Cancel(press))
                            }
                        },
                        onTap = { },
                        onLongPress = { offset ->
                            showMenu = true
                            pressOffset = with(density) {
                                DpOffset(
                                    offset.x.toDp(),
                                    offset.y.toDp()
                                )
                            }
                        }
                    )
                }
        )
        {
            CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {

                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = item.name,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier
                                    .height(30.dp)
                                    .width(30.dp),
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "Settings for ${item.name}"
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                offset = pressOffset
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Edit") },
                                    onClick = {
                                        onEditItem(item)
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    onClick = {
                                        viewModel.removeItem(listId, item)
                                        showMenu = false
                                    }
                                )
                            }
                        }

                        Box(modifier = Modifier.padding(end = 8.dp)) {
                            IconButton(
                                modifier = Modifier
                                    .draggableHandle()
                                    .height(30.dp)
                                    .width(30.dp),
                                onClick = {},

                                ) {
                                Icon(
                                    Icons.Rounded.DragHandle,
                                    contentDescription = "Reorder",
                                    modifier = Modifier.size(24.dp)


                                )
                            }
                        }
                    }
                }
            }
        }

    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LazyItemScope.NormalItem(
    reorderableState: ReorderableLazyListState,
    item: ShoppingItem,
    swipeDirection: SwipeToDismissBoxValue?,
    density: Density,
    itemHeights: SnapshotStateMap<String, Dp>,
    viewModel: ShoppingViewModel,
    listId: String,
    onEditItem: (ShoppingItem) -> Unit,
    onStartPendingDelete: (SwipeToDismissBoxValue) -> Unit,
    onUndoPendingDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var pressOffset by remember { mutableStateOf(DpOffset.Zero) }
    val interactionSource = remember { MutableInteractionSource() }

    ReorderableItem(
        reorderableState,
        key = item.id,
        modifier = Modifier.padding(start = 8.dp)
    ) { isDragging ->
        val elevation by animateDpAsState(
            if (isDragging) 8.dp else 2.dp,
            label = "elevation"
        )

        AnimatedContent(
            targetState = swipeDirection,
            label = "UndoAnimation",
            transitionSpec = {
                if (targetState != null) {
                    val slideDirection = if (targetState == SwipeToDismissBoxValue.StartToEnd) 1 else -1
                    slideInHorizontally { width -> -width * slideDirection } togetherWith slideOutHorizontally { width -> width * slideDirection }
                } else {
                    val slideDirection = if (initialState == SwipeToDismissBoxValue.StartToEnd) 1 else -1
                    slideInHorizontally { width -> width * slideDirection } togetherWith slideOutHorizontally { width -> -width * slideDirection }
                }
            }
        ) { currentDirection ->
            if (currentDirection != null) {
                val itemHeight = itemHeights[item.id]
                if (itemHeight != null) {
                    UndoRow(
                        height = itemHeight,
                        onUndo = onUndoPendingDelete
                    )
                }
            } else {
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = {
                        if (it == SwipeToDismissBoxValue.EndToStart || it == SwipeToDismissBoxValue.StartToEnd) {
                            onStartPendingDelete(it)
                            true
                        } else {
                            false
                        }
                    },
                    positionalThreshold = { it * .50f }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = true,
                    enableDismissFromEndToStart = true,
                    backgroundContent = {
                        val color by animateColorAsState(
                            targetValue = when (dismissState.dismissDirection) {
                                SwipeToDismissBoxValue.EndToStart, SwipeToDismissBoxValue.StartToEnd -> Color.Red.copy(
                                    alpha = 0.8f
                                )

                                else -> Color.Transparent
                            }, label = "background color"
                        )
                        val scale by animateFloatAsState(
                            targetValue = if (dismissState.dismissDirection != SwipeToDismissBoxValue.Settled) 1f else 0.8f,
                            label = "icon scale"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CardDefaults.shape)
                                .background(color)
                                .padding(horizontal = 20.dp),
                            contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Item",
                                tint = Color.White,
                                modifier = Modifier.scale(scale)
                            )
                        }
                    }
                ) {
                    Surface(
                        shape = CardDefaults.shape,
                        color = ShopperTheme.colors.itemContainer,
                        contentColor = ShopperTheme.colors.itemContent,
                        shadowElevation = elevation,
                        modifier = Modifier
                            .onSizeChanged { size ->
                                with(density) {
                                    itemHeights[item.id] = size.height.toDp()
                                }
                            }
                            .indication(interactionSource, rememberRipple())
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = { offset ->
                                        val press = PressInteraction.Press(offset)
                                        interactionSource.emit(press)
                                        try {
                                            awaitRelease()
                                            interactionSource.emit(
                                                PressInteraction.Release(
                                                    press
                                                )
                                            )
                                        } catch (c: CancellationException) {
                                            interactionSource.emit(PressInteraction.Cancel(press))
                                        }
                                    },
                                    onTap = { viewModel.toggleChecked(listId, item) },
                                    onLongPress = { offset ->
                                        showMenu = true
                                        pressOffset = with(density) {
                                            DpOffset(
                                                offset.x.toDp(),
                                                offset.y.toDp()
                                            )
                                        }
                                    }
                                )
                            }
                    ) {
                        CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {

                            Box(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 8.dp, vertical = 8.dp)
                                                .fillMaxWidth(),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            Text(
                                                text = item.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = if (item.isChecked) Color.Gray else ShopperTheme.colors.itemContent
                                            )
                                            if (item.isChecked) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(1.dp)
                                                        .background(ShopperTheme.colors.strikethrough)
                                                )
                                            }
                                        }
                                    }

                                    Box {
                                        IconButton(
                                            onClick = { showMenu = true },
                                            modifier = Modifier
                                                .height(30.dp)
                                                .width(30.dp),
                                        ) {
                                            Icon(
                                                Icons.Default.MoreVert,
                                                contentDescription = "Settings for ${item.name}"
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = showMenu,
                                            onDismissRequest = { showMenu = false },
                                            offset = pressOffset
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Edit") },
                                                onClick = {
                                                    onEditItem(item)
                                                    showMenu = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Delete") },
                                                onClick = {
                                                    viewModel.removeItem(listId, item)
                                                    showMenu = false
                                                }
                                            )
                                        }
                                    }
                                    Box(modifier = Modifier.padding(end = 8.dp)) {

                                        IconButton(
                                            modifier = Modifier
                                                .draggableHandle()
                                                .height(30.dp)
                                                .width(30.dp),
                                            onClick = {},
                                        ) {
                                            Icon(
                                                Icons.Rounded.DragHandle,
                                                contentDescription = "Reorder"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UndoRow(height: Dp, onUndo: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(CardDefaults.shape)
            .background(Color.Red.copy(alpha = 0.8f))
            .clickable(onClick = onUndo),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("UNDO", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun AddItemDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (String, Boolean) -> Unit
) {
    var itemName by rememberSaveable { mutableStateOf("") }
    var isHeader by rememberSaveable { mutableStateOf(false) }

    val voiceRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (!results.isNullOrEmpty()) {
                    itemName = results[0]
                }
            }
        }
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Add New Item") },
        text = {
            Column() {
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Item Name") },
                    singleLine = false,
                    trailingIcon = {
                        IconButton(onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                )
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "sv-SE")
                                putExtra(
                                    RecognizerIntent.EXTRA_PROMPT,
                                    "Prata för att lägga till en vara"
                                )
                            }
                            try {
                                voiceRecognizerLauncher.launch(intent)
                            } catch (e: ActivityNotFoundException) {
                                println("Voice recognition not available on this device.")
                            }
                        }) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice Input")
                        }
                    }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isHeader, onCheckedChange = { isHeader = it })
                    Text("Is a header")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (itemName.isNotBlank()) onConfirm(itemName, isHeader) },
                enabled = itemName.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancel") }
        }
    )
}

@Composable
private fun EditItemDialog(
    item: ShoppingItem,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var itemName by remember { mutableStateOf(item.name) }
    val voiceRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (!results.isNullOrEmpty()) {
                    itemName = results[0]
                }
            }
        }
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Edit Item Name") },
        text = {
            OutlinedTextField(
                value = itemName,
                onValueChange = { itemName = it },
                label = { Text("Item Name") },
                singleLine = false,
                trailingIcon = {
                    IconButton(onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(
                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                            )
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "sv-SE")
                            putExtra(
                                RecognizerIntent.EXTRA_PROMPT,
                                "Prata för att ändra en vara"
                            )
                        }
                        try {
                            voiceRecognizerLauncher.launch(intent)
                        } catch (e: ActivityNotFoundException) {
                            println("Voice recognition not available on this device.")
                        }
                    }) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice Input")
                    }
                }
            )
        },
        confirmButton = {
            Button(
                onClick = { if (itemName.isNotBlank()) onConfirm(itemName) },
                enabled = itemName.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancel") }
        }
    )
}
