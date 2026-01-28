package com.mitte.listree.ui.views

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitte.listree.LisTreeViewModel
import com.mitte.listree.R
import com.mitte.listree.ui.models.ListItem
import com.mitte.listree.ui.models.TreeList
import com.mitte.listree.ui.theme.LisTreeTheme
import kotlinx.coroutines.delay
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
    listId: String
) {
    val allLists by viewModel.treeLists.collectAsState()
    val showDeleted by viewModel.showDeleted.collectAsState()

    fun findListById(lists: List<TreeList>, id: String): TreeList? {
        for (list in lists) {
            if (list.id == id) return list
            val found = findListById(list.subLists ?: emptyList(), id)
            if (found != null) return found
        }
        return null
    }

    val currentList = findListById(allLists, listId)
        ?: TreeList(id = listId, name = stringResource(R.string.not_found), items = emptyList())
    val items = currentList.items?.filter { !it.deleted || showDeleted }?.sortedBy { it.order } ?: emptyList()

    var showAddItemDialog by remember { mutableStateOf(false) }
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
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddItemDialog = true },
                containerColor = LisTreeTheme.colors.topAppBarContainer
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_new_item))
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
                .padding(vertical = 8.dp),
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
                            itemsPendingDeletion = itemsPendingDeletion - item.id
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
                        listId,
                        showDeleted = showDeleted
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
                        },
                        showDeleted = showDeleted
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
    item: ListItem,
    density: Density,
    onEditItem: (ListItem) -> Unit,
    viewModel: LisTreeViewModel,
    listId: String,
    showDeleted: Boolean
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
            color = if (item.deleted) LisTreeTheme.colors.deletedCardContainer else LisTreeTheme.colors.sectionContainer,
            contentColor = LisTreeTheme.colors.sectionContent,
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
                                interactionSource.emit(
                                    PressInteraction.Release(
                                        press
                                    )
                                )
                            } catch (_: CancellationException) {
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
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {

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
                                    contentDescription = stringResource(R.string.reorder),
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
    item: ListItem,
    swipeDirection: SwipeToDismissBoxValue?,
    density: Density,
    itemHeights: SnapshotStateMap<String, Dp>,
    viewModel: LisTreeViewModel,
    listId: String,
    onEditItem: (ListItem) -> Unit,
    onStartPendingDelete: (SwipeToDismissBoxValue) -> Unit,
    onUndoPendingDelete: () -> Unit,
    showDeleted: Boolean
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
                val dismissState = rememberSwipeToDismissBoxState(positionalThreshold = { distance -> distance * 0.5f })

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
                                contentDescription = stringResource(R.string.delete_item),
                                tint = Color.White,
                                modifier = Modifier.scale(scale)
                            )
                        }
                    }
                ) {
                    Surface(
                        shape = CardDefaults.shape,
                        color = if (item.deleted) LisTreeTheme.colors.deletedCardContainer else LisTreeTheme.colors.itemContainer,
                        contentColor = LisTreeTheme.colors.itemContent,
                        shadowElevation = elevation,
                        modifier = Modifier
                            .onSizeChanged { size ->
                                with(density) {
                                    itemHeights[item.id] = size.height.toDp()
                                }
                            }
                            .indication(interactionSource, ripple())
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
                                        } catch (_: CancellationException) {
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
                                                color = if (item.isChecked || item.deleted) Color.Gray else LisTreeTheme.colors.itemContent
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
                                                contentDescription = stringResource(R.string.reorder)
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
        Text(
            stringResource(R.string.undo),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun AddItemDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (String, Boolean) -> Unit
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
            Column() {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isHeader, onCheckedChange = { isHeader = it })
                    Text(stringResource(R.string.is_a_header))
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
