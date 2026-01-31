package com.mitte.listree.ui.views

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.DragHandle
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
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
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
import androidx.compose.material3.ripple
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mitte.listree.LisTreeViewModel
import com.mitte.listree.R
import com.mitte.listree.ui.models.ListItem
import com.mitte.listree.ui.models.TreeList
import com.mitte.listree.ui.theme.LisTreeTheme
import kotlinx.coroutines.delay
import sh.calvin.reorderable.ReorderableColumn
import sh.calvin.reorderable.ReorderableColumnScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ListView(
    modifier: Modifier = Modifier,
    viewModel: LisTreeViewModel,
    listId: String,
    navController: NavController
) {
    val allLists by viewModel.treeLists.collectAsState()
    val showDeleted by viewModel.showDeleted.collectAsState()

    fun findListById(lists: List<TreeList>, id: String): TreeList? {
        for (list in lists) {
            if (list.id == id) return list
            val found = findListById(list.children.filterIsInstance<TreeList>(), id)
            if (found != null) return found
        }
        return null
    }

    val currentList = findListById(allLists, listId)
        ?: TreeList(id = listId, name = stringResource(R.string.not_found), children = emptyList())
    val items = currentList.children.filter { !it.deleted || showDeleted }.sortedBy { it.order }

    var listIdForNewItem by remember { mutableStateOf<String?>(null) }
    var itemToEdit by remember { mutableStateOf<ListItem?>(null) }
    var itemsPendingDeletion by remember {
        mutableStateOf<Map<String, SwipeToDismissBoxValue>>(
            emptyMap()
        )
    }
    val itemHeights = remember { mutableStateMapOf<String, Dp>() }
    val density = LocalDensity.current
    val lazyListState = rememberLazyListState()

    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        println("from.index: ${from.index}, to.index: ${to.index}")
        viewModel.moveItem(listId, from.index, to.index)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentList.name) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LisTreeTheme.colors.topAppBarContainer,
                    titleContentColor = LisTreeTheme.colors.topAppBarTitle
                ),
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings_title)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { listIdForNewItem = listId },
                containerColor = LisTreeTheme.colors.topAppBarContainer
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_new_item))
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->

        if (listIdForNewItem != null) {
            AddItemDialog(
                onDismissRequest = { listIdForNewItem = null },
                onConfirm = { itemName, isHeader ->

                    if (isHeader) {
                        viewModel.addList(itemName, listIdForNewItem!!)
                    } else {
                        viewModel.addItem(listIdForNewItem!!, itemName, false)
                    }
                    listIdForNewItem = null
                },
                showIsHeaderCheckbox = listIdForNewItem == listId // Only show for top-level list
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
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)

        ) {
            items(
                items = items,
                key = { item -> item.id }
            ) { item ->
                when (item) {
                    is ListItem -> {
                        val swipeDirection = itemsPendingDeletion[item.id]
                        // Call the LazyItemScope extension function
                        NormalItem(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            reorderableState = reorderableState,
                            item = item,
                            swipeDirection = swipeDirection,
                            density = density,
                            itemHeights = itemHeights,
                            viewModel = viewModel,
                            listId = listId,
                            onEditItem = { selectedItem ->
                                itemToEdit = selectedItem
                            },
                            onStartPendingDelete = { direction ->
                                itemsPendingDeletion = itemsPendingDeletion + (item.id to direction)
                            },
                            onUndoPendingDelete = {
                                itemsPendingDeletion = itemsPendingDeletion - item.id
                            },
                        )
                    }

                    is TreeList -> {
                        ExpandableItemList(
                            list = item,
                            viewModel = viewModel,
                            reorderableState = reorderableState,
                            onAddItem = { listIdForNewItem = it }
                        )
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun LazyItemScope.ExpandableItemList(
    list: TreeList,
    viewModel: LisTreeViewModel,
    reorderableState: ReorderableLazyListState,
    onAddItem: (String) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    var showMenu by remember { mutableStateOf(false) }
    var childItemsPendingDeletion by remember { mutableStateOf<Map<String, SwipeToDismissBoxValue>>(emptyMap()) }

    ReorderableItem(
        reorderableState,
        key = list.id,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) { isDragging ->

        val elevation by animateDpAsState(
            if (isDragging) 8.dp else 2.dp,
            label = "elevation"
        )

        Column {

            Surface(
                shape = CardDefaults.shape,
                color = if (list.deleted) LisTreeTheme.colors.groupCardDeletedContainer else LisTreeTheme.colors.groupCardContainer,
                contentColor = if (list.deleted) LisTreeTheme.colors.groupCardDeletedContent else LisTreeTheme.colors.groupCardContent,
                shadowElevation = elevation,
                modifier = Modifier
                    .indication(interactionSource, ripple())
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = { offset ->
                                val press = PressInteraction.Press(offset)
                                interactionSource.emit(press)
                                try {
                                    awaitRelease()
                                    interactionSource.emit(PressInteraction.Release(press))
                                } catch (_: CancellationException) {
                                    interactionSource.emit(PressInteraction.Cancel(press))
                                }
                            },
                            onTap = {
                                viewModel.toggleExpanded(list.id)
                            }
                        )
                    },
            ) {
                Box {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 8.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (list.isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(bottom = 2.dp)
                            )

                            Text(
                                text = list.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = pluralStringResource(
                                    R.plurals.item_count,
                                    list.children.size,
                                    list.children.size
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = LisTreeTheme.colors.listMetaCount
                            )
                        }
                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                            if (list.deleted) {
                                Box(modifier = Modifier.padding(end = 8.dp)) {

                                    IconButton(
                                        onClick = { viewModel.undeleteList(list.id) },
                                        modifier = Modifier
                                            .height(30.dp)
                                            .width(30.dp),
                                    ) {
                                        Icon(
                                            Icons.Default.Restore,
                                            contentDescription = stringResource(
                                                R.string.restore_item,
                                                list.name
                                            )
                                        )
                                    }
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onAddItem(list.id) },
                                        modifier = Modifier
                                            .height(30.dp)
                                            .width(30.dp),
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = stringResource(
                                                R.string.item_settings,
                                                list.name
                                            )
                                        )
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
                                                contentDescription = stringResource(
                                                    R.string.item_settings,
                                                    list.name
                                                )
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = showMenu,
                                            onDismissRequest = { showMenu = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.edit)) },
                                                onClick = {
//                                                    onListToEdit(list)
//                                                    showMenu = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.move_to)) },
                                                onClick = {
//                                                    onMoveItem(list)
//                                                    showMenu = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.delete)) },
                                                onClick = {
                                                    viewModel.deleteList(list.id)
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
            AnimatedVisibility(list.isExpanded) {
                Column {
                    ReorderableColumn(
                        list = list.children.filterIsInstance<ListItem>(),
                        onSettle = { from, to ->
                            viewModel.moveSubList(list.id, from, to)
                        },
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)

                    ) { _, item, isDragging ->
                        val swipeDirection = childItemsPendingDeletion[item.id]
                        NormalItem(
                            modifier = Modifier.padding(start = 12.dp),
                            isDragging = isDragging,
                            item = item,
                            swipeDirection = swipeDirection,
                            density = LocalDensity.current,
                            itemHeights = remember { mutableStateMapOf() },
                            viewModel = viewModel,
                            listId = list.id,
                            onEditItem = {},
                            onStartPendingDelete = { direction ->
                                childItemsPendingDeletion = childItemsPendingDeletion + (item.id to direction)
                            },
                            onUndoPendingDelete = {
                                childItemsPendingDeletion = childItemsPendingDeletion - item.id
                            },
                        )
                    }
                }
            }
        }

    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun NormalItemContent(
    modifier: Modifier = Modifier,
    isDragging: Boolean,
    item: ListItem,
    swipeDirection: SwipeToDismissBoxValue?,
    density: Density,
    itemHeights: SnapshotStateMap<String, Dp>,
    viewModel: LisTreeViewModel,
    listId: String,
    onEditItem: (ListItem) -> Unit,
    onStartPendingDelete: (SwipeToDismissBoxValue) -> Unit,
    onUndoPendingDelete: () -> Unit,
    onTap: (() -> Unit)? = null,
    iconButton: @Composable () -> Unit,

    ) {
    var showMenu by remember { mutableStateOf(false) }
    var pressOffset by remember { mutableStateOf(DpOffset.Zero) }
    val interactionSource = remember { MutableInteractionSource() }

    val elevation by animateDpAsState(
        if (isDragging) 8.dp else 2.dp,
        label = "elevation"
    )

    LaunchedEffect(swipeDirection, item.id) {
        if (swipeDirection != null) {
            delay(2500)

            viewModel.removeItem(listId, item.id)
            onUndoPendingDelete()
        }
    }

    AnimatedContent(
        targetState = swipeDirection,
        label = "UndoAnimation",
        transitionSpec = {
            if (targetState != null) {
                val slideDirection =
                    if (targetState == SwipeToDismissBoxValue.StartToEnd) 1 else -1
                slideInHorizontally { width -> -width * slideDirection } togetherWith slideOutHorizontally { width -> width * slideDirection }
            } else {
                val slideDirection =
                    if (initialState == SwipeToDismissBoxValue.StartToEnd) 1 else -1
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
            val dismissState =
                rememberSwipeToDismissBoxState(positionalThreshold = { distance -> distance * 0.5f })

            SwipeToDismissBox(
                state = dismissState,
                onDismiss = { dismissValue ->
                    when (dismissValue) {
                        SwipeToDismissBoxValue.StartToEnd -> {
                            onStartPendingDelete(dismissValue)
                        }

                        SwipeToDismissBoxValue.EndToStart -> {
                            onStartPendingDelete(dismissValue)
                        }

                        SwipeToDismissBoxValue.Settled -> {
                            // no action
                        }
                    }
                },
                enableDismissFromStartToEnd = !item.deleted,
                enableDismissFromEndToStart = !item.deleted,
                backgroundContent = {
                    if (!item.deleted) {
                        val color by animateColorAsState(
                            targetValue = when (dismissState.dismissDirection) {
                                SwipeToDismissBoxValue.EndToStart, SwipeToDismissBoxValue.StartToEnd -> LisTreeTheme.colors.undoRowBackground

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
                                contentDescription = stringResource(R.string.delete_item),
                                tint = Color.White,
                                modifier = Modifier.scale(scale)
                            )
                        }
                    }
                }
            ) {
                Surface(
                    shape = CardDefaults.shape,
                    color = if (item.deleted) LisTreeTheme.colors.normalItemDeletedContainer else if (item.isChecked) LisTreeTheme.colors.normalItemCheckedContainer else LisTreeTheme.colors.normalItemContainer,
                    contentColor = if (item.deleted) LisTreeTheme.colors.normalItemDeletedContent else if (item.isChecked) LisTreeTheme.colors.normalItemCheckedContent else LisTreeTheme.colors.normalItemContent,
                    shadowElevation = elevation,
                    modifier = modifier
                        .alpha(if (item.deleted) 0.6f else 1f)
                        .onSizeChanged { size ->
                            with(density) {
                                itemHeights[item.id] = size.height.toDp()
                            }
                        }
                        .indication(interactionSource, ripple())
                        .pointerInput(item.deleted) {
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
                                    } catch (_: CancellationException) {
                                        interactionSource.emit(PressInteraction.Cancel(press))
                                    }
                                },
                                onTap = {
                                    if (onTap != null) {
                                        onTap()
                                    } else {
                                        if (!item.deleted) viewModel.toggleChecked(
                                            listId,
                                            item.id
                                        )
                                    }
                                },
                                onLongPress = { offset ->
                                    if (onTap == null) {
                                        if (!item.deleted) {
                                            showMenu = true
                                            pressOffset = with(density) {
                                                DpOffset(
                                                    offset.x.toDp(),
                                                    offset.y.toDp()
                                                )
                                            }
                                        }
                                    }
                                }
                            )
                        }
                ) {
                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {

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
                                            color = if (item.deleted) LisTreeTheme.colors.normalItemDeletedContent else if (item.isChecked) LisTreeTheme.colors.normalItemCheckedContent else LisTreeTheme.colors.normalItemContent
                                        )
                                        if (item.isChecked && !item.deleted) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(1.dp)
                                                    .background(LisTreeTheme.colors.strikethrough)
                                            )
                                        }
                                    }
                                }
                                if (item.deleted) {
                                    Box(modifier = Modifier.padding(end = 8.dp)) {

                                        IconButton(
                                            onClick = {
                                                viewModel.undeleteItem(
                                                    listId,
                                                    item.id
                                                )
                                            },
                                            modifier = Modifier
                                                .height(30.dp)
                                                .width(30.dp),
                                        ) {
                                            Icon(
                                                Icons.Default.Restore,
                                                contentDescription = stringResource(
                                                    R.string.restore_item,
                                                    item.name
                                                )
                                            )
                                        }
                                    }
                                } else {
                                    Box {
                                        IconButton(
                                            onClick = { showMenu = true },
                                            modifier = Modifier
                                                .height(30.dp)
                                                .width(30.dp),
                                        ) {
                                            Icon(
                                                Icons.Default.MoreVert,
                                                contentDescription = stringResource(
                                                    R.string.item_settings,
                                                    item.name
                                                )
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = showMenu,
                                            onDismissRequest = { showMenu = false },
                                            offset = pressOffset
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.edit)) },
                                                onClick = {
                                                    onEditItem(item)
                                                    showMenu = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.delete)) },
                                                onClick = {
                                                    viewModel.removeItem(listId, item.id)
                                                    showMenu = false
                                                }
                                            )
                                        }
                                    }
                                    Box(modifier = Modifier.padding(end = 8.dp)) {

                                        iconButton()
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
@OptIn(ExperimentalMaterial3Api::class)
fun LazyItemScope.NormalItem(
    modifier: Modifier = Modifier,
    reorderableState: ReorderableLazyListState,
    item: ListItem,
    swipeDirection: SwipeToDismissBoxValue?,
    density: Density,
    itemHeights: SnapshotStateMap<String, Dp>,
    viewModel: LisTreeViewModel,
    listId: String,
    onEditItem: (ListItem) -> Unit,
    onStartPendingDelete: (SwipeToDismissBoxValue) -> Unit,
    onUndoPendingDelete: () -> Unit,
    onTap: (() -> Unit)? = null,
) {
    ReorderableItem(
        reorderableState,
        key = item.id,
        modifier = modifier
    ) { isDragging ->
        NormalItemContent(
            modifier = Modifier,
            isDragging = isDragging,
            item = item,
            swipeDirection = swipeDirection,
            density = density,
            itemHeights = itemHeights,
            viewModel = viewModel,
            listId = listId,
            onEditItem = onEditItem,
            onStartPendingDelete = onStartPendingDelete,
            onUndoPendingDelete = onUndoPendingDelete,
            onTap = onTap,
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
                        contentDescription = stringResource(R.string.reorder)
                    )
                }
            }
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ReorderableColumnScope.NormalItem(
    modifier: Modifier = Modifier,
    isDragging: Boolean,
    item: ListItem,
    swipeDirection: SwipeToDismissBoxValue?,
    density: Density,
    itemHeights: SnapshotStateMap<String, Dp>,
    viewModel: LisTreeViewModel,
    listId: String,
    onEditItem: (ListItem) -> Unit,
    onStartPendingDelete: (SwipeToDismissBoxValue) -> Unit,
    onUndoPendingDelete: () -> Unit,
    onTap: (() -> Unit)? = null,
) {

    ReorderableItem() {
        NormalItemContent(
            modifier = modifier,
            isDragging = isDragging,
            item = item,
            swipeDirection = swipeDirection,
            density = density,
            itemHeights = itemHeights,
            viewModel = viewModel,
            listId = listId,
            onEditItem = onEditItem,
            onStartPendingDelete = onStartPendingDelete,
            onUndoPendingDelete = onUndoPendingDelete,
            onTap = onTap,
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
                        contentDescription = stringResource(R.string.reorder)
                    )
                }
            }
        )


    }
}


@Composable
private fun UndoRow(height: Dp, onUndo: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(CardDefaults.shape)
            .background(LisTreeTheme.colors.undoRowBackground)
            .clickable(onClick = onUndo),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(R.string.undo),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = LisTreeTheme.colors.undoRowContent
        )
    }
}

@Composable
private fun AddItemDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (String, Boolean) -> Unit,
    showIsHeaderCheckbox: Boolean
) {
    var itemName by rememberSaveable { mutableStateOf("") }
    var isHeader by rememberSaveable { mutableStateOf(false) }
    val voicePrompt = stringResource(R.string.voice_prompt_add)

    val voiceRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (!results.isNullOrEmpty()) {
                    itemName =
                        results[0].replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                }
            }
        }
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.add_new_item_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text(stringResource(R.string.item_name)) },
                    singleLine = false,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
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
                                    voicePrompt
                                )
                            }
                            try {
                                voiceRecognizerLauncher.launch(intent)
                            } catch (_: ActivityNotFoundException) {
                                println("Voice recognition not available on this device.")
                            }
                        }) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = stringResource(R.string.voice_input)
                            )
                        }
                    }
                )
                if (showIsHeaderCheckbox) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isHeader, onCheckedChange = { isHeader = it })
                        Text(stringResource(R.string.is_a_header))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (itemName.isNotBlank()) onConfirm(itemName, isHeader) },
                enabled = itemName.isNotBlank()
            ) { Text(stringResource(R.string.add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun EditItemDialog(
    item: ListItem,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var itemName by remember { mutableStateOf(item.name) }
    val voicePrompt = stringResource(R.string.voice_prompt_edit)

    val voiceRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (!results.isNullOrEmpty()) {
                    itemName =
                        results[0].replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                }
            }
        }
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.edit_item_name)) },
        text = {
            OutlinedTextField(
                value = itemName,
                onValueChange = { itemName = it },
                label = { Text(stringResource(R.string.item_name)) },
                singleLine = false,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
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
                                voicePrompt
                            )
                        }
                        try {
                            voiceRecognizerLauncher.launch(intent)
                        } catch (_: ActivityNotFoundException) {
                            println("Voice recognition not available on this device.")
                        }
                    }) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = stringResource(R.string.voice_input)
                        )
                    }
                }
            )
        },
        confirmButton = {
            Button(
                onClick = { if (itemName.isNotBlank()) onConfirm(itemName) },
                enabled = itemName.isNotBlank()
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel)) }
        }
    )
}