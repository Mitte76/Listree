package com.mitte.listree.ui.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mitte.listree.LisTreeViewModel
import com.mitte.listree.R
import com.mitte.listree.navigation.Routes
import com.mitte.listree.ui.models.ListType
import com.mitte.listree.ui.models.TreeList
import com.mitte.listree.ui.theme.LisTreeTheme
import sh.calvin.reorderable.ReorderableColumn
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSection(
    list: TreeList,
    elevation: Dp,
    onListToEdit: (TreeList) -> Unit,
    onAddSubList: (TreeList) -> Unit,
    onMoveItem: (TreeList) -> Unit,
    viewModel: LisTreeViewModel,
    navController: NavController,
    iconButton: @Composable () -> Unit,
    showDeleted: Boolean,
    onTap: ((TreeList) -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    var showMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(start = 0.dp)) {

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
                            if (onTap != null) {
                                onTap(list)
                            } else {
                                viewModel.toggleExpanded(list.id)
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
                                iconButton()
                            }
                        }
                    }
                }
            }
        }
        AnimatedVisibility(list.isExpanded) {
            Column {
                ReorderableColumn(
                    list = list.children.filter { !it.deleted || showDeleted }.filterIsInstance<TreeList>(),
                    onSettle = { from, to ->
                        viewModel.moveSubList(list.id, from, to)
                    },
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp, start = 24.dp),

                    ) { _, item, isDragging ->
                    val elevation by animateDpAsState(
                        if (isDragging) 8.dp else 1.dp,
                        label = "elevation"
                    )

                    key(item.id) {
                        ReorderableItem {
                            if (item.type == ListType.GROUP_LIST) {
                                GroupSection(
                                    list = item,
                                    elevation = elevation,
                                    onListToEdit = onListToEdit,
                                    onAddSubList = onAddSubList,
                                    onMoveItem = onMoveItem,
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
                                                contentDescription = stringResource(R.string.reorder)
                                            )
                                        }
                                    },
                                    showDeleted = showDeleted,
                                    onTap = onTap
                                )

                            } else {
                                SingleSection(
                                    list = item,
                                    elevation = elevation,
                                    onListToEdit = onListToEdit,
                                    onMoveItem = onMoveItem,
                                    onTap = { navController.navigate(Routes.SHOPPING_ITEMS.replace(Routes.LIST_ID, item.id)) },
                                    viewModel = viewModel,
                                    modifier = Modifier.draggableHandle(),
                                    showDeleted = showDeleted
                                )
                            }
                        }
                    }
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, top = 8.dp)
                        .clickable { onAddSubList(list) },
                    shape = CardDefaults.shape,
                    shadowElevation = 1.dp,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.add_new_sub_list)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.add_new_sub_list),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
