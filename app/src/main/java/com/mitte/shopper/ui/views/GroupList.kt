package com.mitte.shopper.ui.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.coerceAtLeast
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
    viewModel: ShoppingViewModel,
    navController: NavController,
) {
    var showMenu by remember { mutableStateOf(false) }
    var pressOffset by remember { mutableStateOf(DpOffset.Zero) }
    val interactionSource = remember { MutableInteractionSource() }
    val density = LocalDensity.current
    var menuWidth by remember { mutableStateOf(0.dp) }

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
                        onTap = { viewModel.toggleExpanded(list.id) },
                        onLongPress = { offset ->
                            showMenu = true
                            pressOffset = with(density) {
                                DpOffset(offset.x.toDp(), offset.y.toDp())
                            }
                        }
                    )
                },
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .clip(shape = CardDefaults.shape)
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
                            text = "${list.subLists?.size ?: 0} lists",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ShopperTheme.colors.listMetaCount
                        )
                    }
                    IconButton(
                        modifier = Modifier.draggableHandle(),
                        onClick = {},
                    ) {
                        Icon(Icons.Rounded.DragHandle, contentDescription = "Reorder")
                    }
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.onSizeChanged {
                        menuWidth = with(density) { it.width.toDp() }
                    },
                    offset = DpOffset((pressOffset.x - menuWidth).coerceAtLeast(0.dp), 0.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("Add sub-list") },
                        onClick = {
                            onAddSubList(list)
                            showMenu = false
                        }
                    )
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
        }
        AnimatedVisibility(list.isExpanded) {

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
                        var subMenuWidth by remember { mutableStateOf(0.dp) }

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
                                        onLongPress = { offset ->
                                            showMenu = true
                                            pressOffset = with(density) {
                                                DpOffset(offset.x.toDp(), offset.y.toDp())
                                            }
                                        }
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
                                    IconButton(
                                        modifier = Modifier.draggableHandle(),
                                        onClick = {},
                                    ) {
                                        Icon(Icons.Rounded.DragHandle, contentDescription = "Reorder")
                                    }
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                    modifier = Modifier.onSizeChanged {
                                        subMenuWidth = with(density) { it.width.toDp() }
                                    },
                                    offset = DpOffset((pressOffset.x - subMenuWidth).coerceAtLeast(0.dp), 0.dp)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Edit") },
                                        onClick = {
                                            onListToEdit(item)
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
