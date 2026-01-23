package com.mitte.shopper.ui.views

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.mitte.shopper.ui.models.ShoppingList
import com.mitte.shopper.ui.theme.ShopperTheme
import sh.calvin.reorderable.ReorderableCollectionItemScope
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReorderableCollectionItemScope.ShoppingListItem(
    list: ShoppingList,
    elevation: Dp,
    onListToEdit: (ShoppingList) -> Unit,
    onDeleteList: () -> Unit,
    onTap: () -> Unit,
    popupOffset: Dp
) {
    var showMenu by remember { mutableStateOf(false) }
    var pressOffset by remember { mutableStateOf(DpOffset.Zero) }
    val interactionSource = remember { MutableInteractionSource() }
    val density = LocalDensity.current

    Surface(
        shape = CardDefaults.shape,
        color = ShopperTheme.colors.groupCardContainer,
        contentColor = ShopperTheme.colors.groupCardContent,
        tonalElevation = 0.dp,
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
                    onTap = { onTap() },
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
                        .padding(vertical = 8.dp, horizontal = 8.dp)
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
                        onDeleteList()
                        showMenu = false
                    }
                )
            }
        }
    }
}