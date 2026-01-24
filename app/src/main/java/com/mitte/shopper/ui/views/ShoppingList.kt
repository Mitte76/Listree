package com.mitte.shopper.ui.views

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
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
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ShoppingList(
    modifier: Modifier = Modifier,
    viewModel: ShoppingViewModel,
    listId: Int
) {
    val allLists by viewModel.shoppingLists.collectAsState()
    val currentList = allLists.firstOrNull { it.id == listId } 
        ?: allLists.flatMap { it.subLists ?: emptyList() }.firstOrNull { it.id == listId } 
        ?: ShoppingList(id = listId, name = "Not Found", items = emptyList())
    val items = currentList.items ?: emptyList()

    var showAddItemDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<ShoppingItem?>(null) }
    var itemsPendingDeletion by remember { mutableStateOf<Set<Int>>(emptySet()) }
    val itemHeights = remember { mutableStateMapOf<Int, Dp>() }
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
            FloatingActionButton(onClick = { showAddItemDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add new item")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->

        if (showAddItemDialog) {
            AddItemDialog(
                onDismissRequest = { showAddItemDialog = false },
                onConfirm = { itemName ->
                    viewModel.addItem(listId, itemName)
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
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)

        ) {
            items(
                items = items,
                key = { item -> item.id }
            ) { item ->
                val isPendingDeletion = itemsPendingDeletion.contains(item.id)

                LaunchedEffect(isPendingDeletion, item.id) {
                    if (isPendingDeletion) {
                        delay(5000)
                        if (itemsPendingDeletion.contains(item.id)) {
                            viewModel.removeItem(listId, item)
                            itemsPendingDeletion = itemsPendingDeletion - item.id
                        }
                    }
                }

                if (isPendingDeletion) {
                    val itemHeight = itemHeights[item.id]
                    if (itemHeight != null) {
                        UndoRow(
                            height = itemHeight,
                            onUndo = { itemsPendingDeletion = itemsPendingDeletion - item.id }
                        )
                    }
                } else {
                    var showMenu by remember { mutableStateOf(false) }
                    var pressOffset by remember { mutableStateOf(DpOffset.Zero) }
                    val interactionSource = remember { MutableInteractionSource() }

                    ReorderableItem(reorderableState, key = item.id) { isDragging ->
                        val elevation by animateDpAsState(
                            if (isDragging) 8.dp else 0.dp,
                            label = "elevation"
                        )
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = {
                                if (it == SwipeToDismissBoxValue.EndToStart || it == SwipeToDismissBoxValue.StartToEnd) {
                                    itemsPendingDeletion = itemsPendingDeletion + item.id
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
                                color = ShopperTheme.colors.groupCardContainer,
                                contentColor = ShopperTheme.colors.groupCardContent,
                                tonalElevation = 0.dp,
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
                                                    DpOffset(offset.x.toDp(), offset.y.toDp() - innerPadding.calculateTopPadding())
                                                }
                                            }
                                        )
                                    }
                            ) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 0.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                                text = item.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                                                color = if (item.isChecked) Color.Gray else Color.Unspecified,
                                            )
                                        }
                                        IconButton(
                                            modifier = Modifier.draggableHandle(),
                                            onClick = {},
                                        ) {
                                            Icon(
                                                Icons.Rounded.DragHandle,
                                                contentDescription = "Reorder"
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false },
                                        offset = pressOffset
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Edit") },
                                            onClick = {
                                                itemToEdit = item
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
    onConfirm: (String) -> Unit
) {
    var itemName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Add New Item") },
        text = {
            OutlinedTextField(
                value = itemName,
                onValueChange = { itemName = it },
                label = { Text("Item Name") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { if (itemName.isNotBlank()) onConfirm(itemName) },
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
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Edit Item Name") },
        text = {
            OutlinedTextField(
                value = itemName,
                onValueChange = { itemName = it },
                label = { Text("Item Name") },
                singleLine = false
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
