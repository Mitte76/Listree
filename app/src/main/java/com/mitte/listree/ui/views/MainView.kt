package com.mitte.listree.ui.views

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mitte.listree.LisTreeViewModel
import com.mitte.listree.R
import com.mitte.listree.ui.models.ListType
import com.mitte.listree.ui.models.TreeList
import com.mitte.listree.ui.theme.LisTreeTheme
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainView(
    viewModel: LisTreeViewModel,
    navController: NavController
) {
    val shoppingLists by viewModel.treeLists.collectAsState()
    val showDeleted by viewModel.showDeleted.collectAsState()

    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var listToEditId by rememberSaveable { mutableStateOf<String?>(null) }
    var showAddSubListDialog by rememberSaveable { mutableStateOf(false) }
    var parentGroupId by rememberSaveable { mutableStateOf<String?>(null) }

    var listToMove by rememberSaveable { mutableStateOf<TreeList?>(null) }
    val bottomSheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    fun closeMoveSheet() {
        scope.launch {
            bottomSheetState.hide()
            listToMove = null
        }
    }

    val lazyListState = rememberLazyListState()

    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        viewModel.moveList(from.index, to.index)
    }

    fun getFlatShoppingListWithDepth(
        lists: List<TreeList>,
        depth: Int = 0
    ): List<Pair<TreeList, Int>> {
        return lists.flatMap { list ->
            listOf(Pair(list, depth)) + getFlatShoppingListWithDepth(
                list.subLists ?: emptyList(),
                depth + 1
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.your_lists)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LisTreeTheme.colors.topAppBarContainer,
                    titleContentColor = LisTreeTheme.colors.topAppBarTitle
                ),
                actions = {
                    Box {
                        var showMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.more_options)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings_title)) },
                                onClick = {
                                    showMenu = false
                                    navController.navigate("settings")
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = LisTreeTheme.colors.topAppBarContainer
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_new_list))
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
                            } else {
                                viewModel.addSubList(id, subListName)
                            }
                            showAddSubListDialog = false
                            parentGroupId = null
                        }
                    )
                }
            }
        }

        listToMove?.let { list ->
            ModalBottomSheet(
                onDismissRequest = { listToMove = null },
                sheetState = bottomSheetState
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.move_list_to, list.name),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    LazyColumn {
                        if (list.parentId != null) {
                            item {
                                Text(
                                    text = stringResource(R.string.top_level),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.moveList(
                                                listId = list.id,
                                                newParentId = null
                                            )
                                            closeMoveSheet()
                                        }
                                        .padding(vertical = 12.dp)
                                )
                            }
                        }

                        val destinations =
                            getFlatShoppingListWithDepth(shoppingLists).filter { (it, _) ->
                                it.type == ListType.GROUP_LIST && it.id != list.parentId && it.id != list.id
                            }

                        items(destinations) { (destination, depth) ->
                            Text(
                                text = destination.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.moveList(
                                            listId = list.id,
                                            newParentId = destination.id
                                        )
                                        closeMoveSheet()
                                    }
                                    .padding(
                                        start = (16 * depth).dp,
                                        end = 16.dp,
                                        top = 12.dp,
                                        bottom = 12.dp
                                    )
                            )
                        }
                    }
                }
            }
        }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) { ->
            items(shoppingLists.filter { !it.deleted || showDeleted }, key = { it.id }) { list ->
                ReorderableItem(reorderableState, key = list.id) { isDragging ->
                    val elevation by animateDpAsState(
                        if (isDragging) 8.dp else 2.dp,
                        label = "elevation"
                    )

                    if (list.type == ListType.GROUP_LIST) {
                        GroupSection(
                            list = list,
                            elevation = elevation,
                            onListToEdit = { listToEditId = it.id },
                            onAddSubList = {
                                parentGroupId = it.id
                                showAddSubListDialog = true
                            },
                            onMoveItem = { listToMove = it },
                            viewModel = viewModel,
                            navController = navController,

                            iconButton = {
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
                            },
                            showDeleted = showDeleted
                        )
                    } else {
                        SingleSection(
                            list = list,
                            elevation = elevation,
                            onListToEdit = { listToEditId = it.id },
                            onMoveItem = { listToMove = it },
                            onTap = { navController.navigate("shoppingItems/${list.id}") },
                            viewModel = viewModel,
                            modifier = Modifier.draggableHandle(),
                            showDeleted = showDeleted
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
        title = { Text(stringResource(R.string.create_new_list)) },
        text = {
            Column {
                OutlinedTextField(
                    value = listName,
                    onValueChange = { listName = it },
                    label = { Text(stringResource(R.string.list_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isGroup, onCheckedChange = { isGroup = it })
                    Text(stringResource(R.string.is_a_group))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (listName.isNotBlank()) onConfirm(listName, isGroup) },
                enabled = listName.isNotBlank()
            ) {
                Text(stringResource(R.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel)) }
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
        title = { Text(stringResource(R.string.create_new_sub_list)) },
        text = {
            Column {
                OutlinedTextField(
                    value = listName,
                    onValueChange = { listName = it },
                    label = { Text(stringResource(R.string.list_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isGroup, onCheckedChange = { isGroup = it })
                    Text(stringResource(R.string.is_a_group))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (listName.isNotBlank()) onConfirm(listName, isGroup) },
                enabled = listName.isNotBlank()
            ) {
                Text(stringResource(R.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun EditListDialog(
    list: TreeList,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var listName by rememberSaveable(list) { mutableStateOf(list.name) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.edit_list_name)) },
        text = {
            OutlinedTextField(
                value = listName,
                onValueChange = { listName = it },
                label = { Text(stringResource(R.string.list_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )
        },
        confirmButton = {
            Button(
                onClick = { if (listName.isNotBlank()) onConfirm(listName) },
                enabled = listName.isNotBlank()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel)) }
        }
    )
}
