package com.mitte.shopper.ui.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mitte.shopper.ShoppingViewModel
import com.mitte.shopper.ui.models.ListType
import com.mitte.shopper.ui.models.ShoppingList
import com.mitte.shopper.ui.theme.ShopperTheme
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableColumn
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainView(
    viewModel: ShoppingViewModel,
    navController: NavController
) {
    val shoppingLists by viewModel.shoppingLists.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var listToEdit by remember { mutableStateOf<ShoppingList?>(null) }
    var showAddSubListDialog by remember { mutableStateOf(false) }
    var parentGroup by remember { mutableStateOf<ShoppingList?>(null) }
    val lazyListState = rememberLazyListState()

    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        viewModel.moveList(from.index, to.index)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Shopping Lists") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ShopperTheme.colors.topAppBarContainer,
                    titleContentColor = ShopperTheme.colors.topAppBarTitle
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add new shopping list")
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        if (showAddDialog) {
            AddListDialog(
                onDismissRequest = { showAddDialog = false },
                onConfirm = { listName, isGroup ->
                    if (isGroup) {
                        viewModel.addGroupList(listName)
                    } else {
                        viewModel.addList(listName)
                    }
                    showAddDialog = false
                }
            )
        }

        listToEdit?.let { list ->
            EditListDialog(
                list = list,
                onDismissRequest = { listToEdit = null },
                onConfirm = { newName ->
                    viewModel.editListName(list.id, newName)
                    listToEdit = null
                }
            )
        }

        if (showAddSubListDialog) {
            parentGroup?.let { group ->
                AddSubListDialog(
                    onDismissRequest = {
                        showAddSubListDialog = false
                        parentGroup = null
                    },
                    onConfirm = { subListName ->
                        viewModel.addSubList(group.id, subListName)
                        showAddSubListDialog = false
                        parentGroup = null
                    }
                )
            }
        }

        val popupOffset = innerPadding.calculateTopPadding()

        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(shoppingLists, key = { it.id }) { list ->
                ReorderableItem(reorderableState, key = list.id) { isDragging ->
                    val elevation by animateDpAsState(
                        if (isDragging) 8.dp else 1.dp,
                        label = "elevation"
                    )
                    var showMenu by remember { mutableStateOf(false) }
                    var pressOffset by remember { mutableStateOf(DpOffset.Zero) }
                    val density = LocalDensity.current
                    val interactionSource = remember { MutableInteractionSource() }

                    if (list.type == ListType.GROUP_LIST) {
                        GroupListItem(
                            list = list,
                            elevation = elevation,
                            interactionSource = interactionSource,
                            pressOffset = pressOffset,
                            showMenu = showMenu,
                            onShowMenuChange = { showMenu = it },
                            onPressOffsetChange = { pressOffset = it },
                            onListToEdit = { listToEdit = it },
                            onAddSubList = {
                                parentGroup = it
                                showAddSubListDialog = true
                            },
                            viewModel = viewModel,
                            navController = navController,
                            density = density,
                            popupOffset = popupOffset
                        )
                    } else {
                        ShoppingListItem(
                            list = list,
                            elevation = elevation,
                            interactionSource = interactionSource,
                            pressOffset = pressOffset,
                            showMenu = showMenu,
                            onShowMenuChange = { showMenu = it },
                            onPressOffsetChange = { pressOffset = it },
                            onListToEdit = { listToEdit = it },
                            onDeleteList = { viewModel.deleteList(list.id) },
                            onTap = { navController.navigate("shoppingItems/${list.id}") },
                            density = density,
                            popupOffset = popupOffset
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReorderableCollectionItemScope.GroupListItem(
    list: ShoppingList,
    elevation: Dp,
    interactionSource: MutableInteractionSource,
    pressOffset: DpOffset,
    showMenu: Boolean,
    onShowMenuChange: (Boolean) -> Unit,
    onPressOffsetChange: (DpOffset) -> Unit,
    onListToEdit: (ShoppingList) -> Unit,
    onAddSubList: (ShoppingList) -> Unit,
    viewModel: ShoppingViewModel,
    navController: NavController,
    density: androidx.compose.ui.unit.Density,
    popupOffset: Dp
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column {
        Card(
            modifier = Modifier
                .clip(CardDefaults.shape)
                .indication(interactionSource, rememberRipple())
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { offset ->
                            val press = PressInteraction.Press(offset)
                            interactionSource.emit(press)
                            try {
                                awaitRelease()
                                interactionSource.emit(PressInteraction.Release(press))
                            } catch (c: CancellationException) {
                                interactionSource.emit(PressInteraction.Cancel(press))
                            }
                        },
                        onTap = { isExpanded = !isExpanded },
                        onLongPress = { offset ->
                            onShowMenuChange(true)
                            onPressOffsetChange(
                                with(density) {
                                    DpOffset(offset.x.toDp(), offset.y.toDp())
                                }
                            )
                        }
                    )
                },
            elevation = CardDefaults.cardElevation(defaultElevation = elevation)
        ) {
            Box {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        modifier = Modifier.draggableHandle(),
                        onClick = {},
                    ) {
                        Icon(Icons.Rounded.DragHandle, contentDescription = "Reorder")
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = list.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${list.subLists?.size ?: 0} lists",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ShopperTheme.colors.listMetaCount
                        )
                    }
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { onShowMenuChange(false) },
                    offset = pressOffset.copy(y = pressOffset.y - popupOffset)
                ) {
                    DropdownMenuItem(
                        text = { Text("Add sub-list") },
                        onClick = {
                            onAddSubList(list)
                            onShowMenuChange(false)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            onListToEdit(list)
                            onShowMenuChange(false)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            viewModel.deleteList(list.id)
                            onShowMenuChange(false)
                        }
                    )
                }
            }
        }
        AnimatedVisibility(isExpanded) {

            ReorderableColumn(
                list = list.subLists ?: emptyList(),
                onSettle = { from, to ->
                    viewModel.moveSubList(list.id, from, to)
                },
            ) { index, item, isDragging ->
                val elevation by animateDpAsState(
                    if (isDragging) 8.dp else 1.dp,
                    label = "elevation"
                )

                key(item.id) {
                    ReorderableItem {

                        val interactionSource = remember { MutableInteractionSource() }
                        var showMenu by remember { mutableStateOf(false) }
                        var pressOffset by remember { mutableStateOf(DpOffset.Zero) }
                        val density = LocalDensity.current

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 32.dp, top = 8.dp)
                                .clip(CardDefaults.shape)
                                .indication(interactionSource, rememberRipple())
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = { offset ->
                                            val press = PressInteraction.Press(offset)
                                            interactionSource.emit(press)
                                            try {
                                                awaitRelease()
                                                interactionSource.emit(PressInteraction.Release(press))
                                            } catch (c: CancellationException) {
                                                interactionSource.emit(PressInteraction.Cancel(press))
                                            }
                                        },
                                        onTap = { navController.navigate("shoppingItems/${item.id}") },
                                        onLongPress = { offset ->
                                            showMenu = true
                                            pressOffset = with(density) {
                                                DpOffset(offset.x.toDp(), offset.y.toDp())
                                            }
                                        }
                                    )
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = elevation)
                        ) {
                            Box {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        modifier = Modifier.draggableHandle(),
                                        onClick = {},
                                    ) {
                                        Icon(Icons.Rounded.DragHandle, contentDescription = "Reorder")
                                    }
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(vertical = 8.dp, horizontal = 16.dp)
                                    ) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${item.items?.size ?: 0} items",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ShopperTheme.colors.listMetaCount
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                    offset = pressOffset.copy(y = pressOffset.y - popupOffset)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Edit") },
                                        onClick = {
                                            onListToEdit(list)
                                            showMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete") },
                                        onClick = {
                                            viewModel.deleteList(item.id)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReorderableCollectionItemScope.ShoppingListItem(
    list: ShoppingList,
    elevation: Dp,
    interactionSource: MutableInteractionSource,
    pressOffset: DpOffset,
    showMenu: Boolean,
    onShowMenuChange: (Boolean) -> Unit,
    onPressOffsetChange: (DpOffset) -> Unit,
    onListToEdit: (ShoppingList) -> Unit,
    onDeleteList: () -> Unit,
    onTap: () -> Unit,
    density: androidx.compose.ui.unit.Density,
    popupOffset: Dp
) {
    Card(
        modifier = Modifier
            .clip(CardDefaults.shape)
            .indication(interactionSource, rememberRipple())
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        val press = PressInteraction.Press(offset)
                        interactionSource.emit(press)
                        try {
                            awaitRelease()
                            interactionSource.emit(PressInteraction.Release(press))
                        } catch (c: CancellationException) {
                            interactionSource.emit(PressInteraction.Cancel(press))
                        }
                    },
                    onTap = { onTap() },
                    onLongPress = { offset ->
                        onShowMenuChange(true)
                        onPressOffsetChange(
                            with(density) {
                                DpOffset(offset.x.toDp(), offset.y.toDp())
                            }
                        )
                    }
                )
            },
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Box {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    modifier = Modifier.draggableHandle(),
                    onClick = {},
                ) {
                    Icon(Icons.Rounded.DragHandle, contentDescription = "Reorder")
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = list.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${list.items?.size ?: 0} items",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ShopperTheme.colors.listMetaCount
                    )
                }
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { onShowMenuChange(false) },
                offset = pressOffset.copy(y = pressOffset.y - popupOffset)
            ) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = {
                        onListToEdit(list)
                        onShowMenuChange(false)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        onDeleteList()
                        onShowMenuChange(false)
                    }
                )
            }
        }
    }
}


@Composable
private fun AddListDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (String, Boolean) -> Unit
) {
    var listName by remember { mutableStateOf("") }
    var isGroup by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Create New List") },
        text = {
            Column {
                OutlinedTextField(
                    value = listName,
                    onValueChange = { listName = it },
                    label = { Text("List Name") },
                    singleLine = true
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isGroup, onCheckedChange = { isGroup = it })
                    Text("Is a group")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (listName.isNotBlank()) onConfirm(listName, isGroup) },
                enabled = listName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancel") }
        }
    )
}

@Composable
private fun EditListDialog(
    list: ShoppingList,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var listName by remember { mutableStateOf(list.name) }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Edit List Name") },
        text = {
            OutlinedTextField(
                value = listName,
                onValueChange = { listName = it },
                label = { Text("List Name") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { if (listName.isNotBlank()) onConfirm(listName) },
                enabled = listName.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancel") }
        }
    )
}

@Composable
private fun AddSubListDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var listName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Create New Sub-List") },
        text = {
            OutlinedTextField(
                value = listName,
                onValueChange = { listName = it },
                label = { Text("Sub-List Name") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { if (listName.isNotBlank()) onConfirm(listName) },
                enabled = listName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancel") }
        }
    )
}
