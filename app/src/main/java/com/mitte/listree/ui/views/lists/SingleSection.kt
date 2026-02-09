package com.mitte.listree.ui.views.lists

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mitte.listree.viewmodels.LisTreeViewModel
import com.mitte.listree.R
import com.mitte.listree.ui.models.ListContent
import com.mitte.listree.ui.models.ListItem
import com.mitte.listree.ui.models.TreeList
import com.mitte.listree.ui.theme.LisTreeTheme
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SingleSection(
    list: TreeList,
    elevation: Dp,
    onListToEdit: (TreeList) -> Unit,
    onMoveItem: (TreeList) -> Unit,
    onTap: () -> Unit,
    viewModel: LisTreeViewModel,
    modifier: Modifier = Modifier,
    showDeleted: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }
    var showMenu by remember { mutableStateOf(false) }

    fun countAllItems(items: List<ListContent>): Int {
        var count = 0
        for (item in items) {
            if (!item.deleted || showDeleted) {
                if (item is ListItem) {
                    count++
                } else if (item is TreeList) {
                    count += countAllItems(item.children)
                }
            }
        }
        return count
    }

    Surface(
        shape = CardDefaults.shape,
        color = if (list.deleted) LisTreeTheme.colors.singleCardDeletedContainer else LisTreeTheme.colors.singleCardContainer,
        contentColor = if (list.deleted) LisTreeTheme.colors.singleCardDeletedContent else LisTreeTheme.colors.singleCardContent,
        tonalElevation = 0.dp,
        shadowElevation = elevation,
        modifier = Modifier
            .indication(interactionSource, ripple())
            .pointerInput(list.deleted) {
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
                    onTap = { onTap() }
                )
            },
    ) {
        Box(modifier = Modifier.alpha(if (list.deleted) 0.6f else 1f)) {
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
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
                        Text(
                            text = list.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = pluralStringResource(
                                R.plurals.item_count,
                                countAllItems(list.children),
                                countAllItems(list.children)
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = LisTreeTheme.colors.listMetaCount
                        )
                    }
                    if (list.deleted) {
                        IconButton(onClick = { viewModel.undeleteList(list.id) }) {
                            Icon(
                                Icons.Default.Restore,
                                contentDescription = stringResource(R.string.restore_list, list.name)
                            )
                        }
                    } else {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
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
                                        onListToEdit(list)
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.move_to)) },
                                    onClick = {
                                        onMoveItem(list)
                                        showMenu = false
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
                        IconButton(
                            modifier = modifier
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
