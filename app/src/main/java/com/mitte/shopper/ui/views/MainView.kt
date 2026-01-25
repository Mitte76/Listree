package com.mitte.shopper.ui.views

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.mitte.shopper.ShoppingViewModel
import com.mitte.shopper.ui.models.ListType
import com.mitte.shopper.ui.models.ShoppingList
import com.mitte.shopper.ui.theme.ShopperTheme
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainView(
    viewModel: ShoppingViewModel,
    navController: NavController
) {
    val shoppingLists by viewModel.shoppingLists.collectAsState()

    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var listToEditId by rememberSaveable { mutableStateOf<Int?>(null) }
    var showAddSubListDialog by rememberSaveable { mutableStateOf(false) }
    var parentGroupId by rememberSaveable { mutableStateOf<Int?>(null) }
    var listToMove by rememberSaveable { mutableStateOf<ShoppingList?>(null) }
    val lazyListState = rememberLazyListState()

    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        viewModel.moveList(from.index, to.index)
    }

    fun getAllLists(lists: List<ShoppingList>): List<ShoppingList> {
        return lists + lists.flatMap { getAllLists(it.subLists ?: emptyList()) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Lists") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ShopperTheme.colors.topAppBarContainer,
                    titleContentColor = ShopperTheme.colors.topAppBarTitle
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = ShopperTheme.colors.topAppBarContainer
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add new list")
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

        listToEditId?.let { id ->
            val listToEdit = viewModel.getListById(id)
            if (listToEdit != null) {
                EditListDialog(
                    list = listToEdit,
                    onDismissRequest = { listToEditId = null },
                    onConfirm = { newName ->
                        viewModel.editListName(id, newName)
                        listToEditId = null
                    }
                )
            }
        }

        if (showAddSubListDialog) {
            parentGroupId?.let { id ->
                val parentGroup = viewModel.getListById(id)
                if (parentGroup != null) {
                    AddSubListDialog(
                        onDismissRequest = {
                            showAddSubListDialog = false
                            parentGroupId = null
                        },
                        onConfirm = { subListName, isGroup ->
                            if (isGroup) {
                                viewModel.addSubGroup(id, subListName)
                                showAddSubListDialog = false
                                parentGroupId = null
                            } else {
                                viewModel.addSubList(id, subListName)
                                showAddSubListDialog = false
                                parentGroupId = null
                            }

                        }
                    )
                }
            }
        }

        listToMove?.let { list ->
            MoveListDialog(
                list = list,
                destinations = getAllLists(shoppingLists).filter { it.type == ListType.GROUP_LIST && it.id != list.parentId && it.id != list.id },
                onDismissRequest = { listToMove = null },
                onConfirm = { destinationId ->
                    list.parentId?.let {
                        viewModel.moveSubListToNewGroup(
                            list.id,
                            it,
                            destinationId
                        )
                    }
                    listToMove = null
                }
            )
        }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) { ->
            items(shoppingLists, key = { it.id }) { list ->
                ReorderableItem(reorderableState, key = list.id) { isDragging ->
                    val elevation by animateDpAsState(
                        if (isDragging) 8.dp else 2.dp,
                        label = "elevation"
                    )

                    if (list.type == ListType.GROUP_LIST) {
                        GroupTest(
                            list = list,
                            elevation = elevation,
                            onListToEdit = { listToEditId = it.id },
                            onAddSubList = {
                                println("Kamelsnopp ${it.id}")
                                parentGroupId = it.id
                                showAddSubListDialog = true
                            },
                            onMoveItem = { listToMove = it },
                            viewModel = viewModel,
                            navController = navController,

                            iconButton = {
                                IconButton(
                                    modifier = Modifier.draggableHandle(),
                                    onClick = {},
                                ) {
                                    Icon(Icons.Rounded.DragHandle, contentDescription = "Reorder")
                                }
                            }
                        )
                    } else {
                        SingleList(
                            list = list,
                            elevation = elevation,
                            onListToEdit = { listToEditId = it.id },
                            onMoveItem = { listToMove = it },
                            onTap = { navController.navigate("shoppingItems/${list.id}") },
                            viewModel = viewModel,
                            modifier = Modifier.draggableHandle()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MoveListDialog(
    list: ShoppingList,
    destinations: List<ShoppingList>,
    onDismissRequest: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = CardDefaults.shape,
            color = ShopperTheme.colors.groupCardContainer,
            contentColor = ShopperTheme.colors.groupCardContent,
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Move \"${list.name}\" to:", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn {
                    items(destinations) { destination ->
                        Text(
                            text = destination.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onConfirm(destination.id) }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddListDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (String, Boolean) -> Unit
) {
    var listName by rememberSaveable { mutableStateOf("") }
    var isGroup by rememberSaveable { mutableStateOf(false) }
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
private fun AddSubListDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (String, Boolean) -> Unit
) {
    var listName by rememberSaveable { mutableStateOf("") }
    var isGroup by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Create New Sub-List") },
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
    var listName by rememberSaveable { mutableStateOf(list.name) }
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
