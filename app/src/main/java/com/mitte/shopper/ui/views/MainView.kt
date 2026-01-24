package com.mitte.shopper.ui.views

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(shoppingLists, key = { it.id }) { list ->
                ReorderableItem(reorderableState, key = list.id) { isDragging ->
                    val elevation by animateDpAsState(
                        if (isDragging) 8.dp else 2.dp,
                        label = "elevation"
                    )

                    if (list.type == ListType.GROUP_LIST) {
                        GroupList(
                            list = list,
                            elevation = elevation,
                            onListToEdit = { listToEdit = it },
                            onAddSubList = {
                                parentGroup = it
                                showAddSubListDialog = true
                            },
                            viewModel = viewModel,
                            navController = navController,
                            popupOffset = popupOffset
                        )
                    } else {
                        SingleList(
                            list = list,
                            elevation = elevation,
                            onListToEdit = { listToEdit = it },
                            onDeleteList = { viewModel.deleteList(list.id) },
                            onTap = { navController.navigate("shoppingItems/${list.id}") },
                            popupOffset = popupOffset
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
