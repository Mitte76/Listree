package com.mitte.shopper.ui.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mitte.shopper.ShoppingViewModel
import com.mitte.shopper.ui.models.ShoppingList
import com.mitte.shopper.ui.theme.ShopperTheme
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableColumn
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReorderableCollectionItemScope.GroupList(
    list: ShoppingList,
    elevation: Dp,
    onListToEdit: (ShoppingList) -> Unit,
    onAddSubList: (ShoppingList) -> Unit,
    onMoveItem: (ShoppingList) -> Unit,
    viewModel: ShoppingViewModel,
    navController: NavController
) {
    val interactionSource = remember { MutableInteractionSource() }
    var showMenu by remember { mutableStateOf(false) }

    Column {
        Surface(
            shape = CardDefaults.shape,
            color = ShopperTheme.colors.groupCardContainer,
            contentColor = ShopperTheme.colors.groupCardContent,
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
                                interactionSource.emit(PressInteraction.Release(press))
                            } catch (c: CancellationException) {
                                interactionSource.emit(PressInteraction.Cancel(press))
                            }
                        },
                        onTap = { viewModel.toggleExpanded(list.id) }
                    )
                },
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 8.dp, horizontal = 8.dp),
                    ) {
                        Text(
                            text = list.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${list.subLists?.size ?: 0} sub-lists",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ShopperTheme.colors.listMetaCount
                        )
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Settings for ${list.name}")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
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
                                    viewModel.deleteList(list.id)
                                    showMenu = false
                                }
                            )
                        }
                    }
                    IconButton(
                        modifier = Modifier.draggableHandle(),
                        onClick = {},
                    ) {
                        Icon(Icons.Rounded.DragHandle, contentDescription = "Reorder")
                    }
                }
                Icon(
                    imageVector = if (list.isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 2.dp)
                )
            }
        }
        AnimatedVisibility(list.isExpanded) {
            Column {
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
                            var showSubMenu by remember { mutableStateOf(false) }
                            
                            Surface(
                                shape = CardDefaults.shape,
                                color = ShopperTheme.colors.singleCardContainer,
                                contentColor = ShopperTheme.colors.singleCardContent,
                                tonalElevation = 0.dp,
                                shadowElevation = elevation,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 32.dp, top = 8.dp)
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
                                            onTap = { navController.navigate("shoppingItems/${item.id}") },
                                        )
                                    },
                            ) {
                                Box {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
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
                                        Box {
                                            IconButton(onClick = { showSubMenu = true }) {
                                                Icon(Icons.Default.MoreVert, contentDescription = "Settings for ${item.name}")
                                            }
                                            DropdownMenu(
                                                expanded = showSubMenu,
                                                onDismissRequest = { showSubMenu = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Edit") },
                                                    onClick = {
                                                        onListToEdit(item)
                                                        showSubMenu = false
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Move to...") },
                                                    onClick = {
                                                        onMoveItem(item)
                                                        showSubMenu = false
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Delete") },
                                                    onClick = {
                                                        viewModel.deleteList(item.id)
                                                        showSubMenu = false
                                                    }
                                                )
                                            }
                                        }
                                        IconButton(
                                            modifier = Modifier.draggableHandle(),
                                            onClick = {},
                                        ) {
                                            Icon(Icons.Rounded.DragHandle, contentDescription = "Reorder")
                                        }
                                    }
                                }
                            }

                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp, top = 8.dp)
                        .clickable { onAddSubList(list) }
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add new sub-list"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add new sub-list", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
