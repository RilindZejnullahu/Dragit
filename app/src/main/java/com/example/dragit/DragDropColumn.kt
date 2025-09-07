package com.example.dragit

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex

/**
 * A LazyColumn that supports drag-and-drop reordering via overlay drag handles.
 * 
 * Only the drag handle areas are draggable, allowing normal scrolling elsewhere.
 * Items use full width with drag handles overlaid on the right side.
 * Items smoothly animate to their new positions when reordered.
 *
 * @param items List of items to display and reorder
 * @param onSwap Callback invoked when items need to be swapped (fromIndex, toIndex)
 * @param modifier Modifier for the LazyColumn
 * @param allowDrag Whether drag functionality is enabled
 * @param verticalSpacing Spacing between items
 * @param contentPadding Padding for the LazyColumn content
 * @param animationSpec Animation specification for item reordering animations
 * @param key Function to generate stable keys for items (must return Bundle-serializable types)
 * @param dragHandleContent Custom drag handle content
 * @param itemContent Content for each item
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T : Any> DragDropColumn(
    items: List<T>,
    onSwap: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    allowDrag: Boolean = true,
    verticalSpacing: Dp = 8.dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    animationSpec: FiniteAnimationSpec<IntOffset> = DragDropDefaults.DEFAULT_ANIMATION_SPEC,
    key: ((item: T) -> Any)? = null,
    dragHandleContent: @Composable () -> Unit = { DefaultDragHandle() },
    itemContent: @Composable LazyItemScope.(item: T) -> Unit
) {
    val listState = rememberLazyListState()
    val dragDropState = rememberDragDropState(listState, onSwap)
    var dragHandleXRange by remember { mutableStateOf(0f..0f) }

    LazyColumn(
        modifier = if (allowDrag) {
            modifier.pointerInput(dragDropState) {
                var isDragging = false
                var lastPosition = Offset.Zero

                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)

                        when {
                            event.changes.any { it.pressed } && !isDragging -> {
                                val change = event.changes.first()
                                lastPosition = change.position
                                
                                // Only start dragging if touch is within drag handle area
                                if (change.position.x in dragHandleXRange) {
                                    isDragging = true
                                    dragDropState.onDragStart(lastPosition)
                                    change.consume()
                                }
                            }

                            isDragging && event.changes.any { it.pressed } -> {
                                val change = event.changes.first()
                                val currentPosition = change.position
                                val dragOffset = currentPosition - lastPosition

                                if (dragOffset != Offset.Zero) {
                                    dragDropState.onDrag(dragOffset)
                                    lastPosition = currentPosition
                                }
                                change.consume()
                            }

                            isDragging && !event.changes.any { it.pressed } -> {
                                isDragging = false
                                dragDropState.onDragInterrupted()
                            }
                        }
                    }
                }
            }
        } else {
            modifier
        },
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(verticalSpacing)
    ) {
        if (key != null) {
            itemsIndexed(items = items, key = { _, item -> key(item) }) { index, item ->
                DragDropItem(
                    item = item,
                    index = index,
                    dragDropState = dragDropState,
                    allowDrag = allowDrag,
                    animationSpec = animationSpec,
                    dragHandleXRange = dragHandleXRange,
                    onDragHandlePositioned = { range -> dragHandleXRange = range },
                    dragHandleContent = dragHandleContent,
                    itemContent = itemContent
                )
            }
        } else {
            itemsIndexed(items = items) { index, item ->
                DragDropItem(
                    item = item,
                    index = index,
                    dragDropState = dragDropState,
                    allowDrag = allowDrag,
                    animationSpec = animationSpec,
                    dragHandleXRange = dragHandleXRange,
                    onDragHandlePositioned = { range -> dragHandleXRange = range },
                    dragHandleContent = dragHandleContent,
                    itemContent = itemContent
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun <T : Any> LazyItemScope.DragDropItem(
    item: T,
    index: Int,
    dragDropState: DragDropState,
    allowDrag: Boolean,
    animationSpec: FiniteAnimationSpec<IntOffset>,
    dragHandleXRange: ClosedFloatingPointRange<Float>,
    onDragHandlePositioned: (ClosedFloatingPointRange<Float>) -> Unit,
    dragHandleContent: @Composable () -> Unit,
    itemContent: @Composable LazyItemScope.(item: T) -> Unit
) {
    val isDragging = allowDrag && index == dragDropState.currentDraggedIndex
    val itemModifier = if (isDragging) {
        Modifier
            .zIndex(1f)
            .graphicsLayer {
                translationY = dragDropState.draggingItemOffset
                scaleX = DragDropDefaults.DRAG_SCALE
                scaleY = DragDropDefaults.DRAG_SCALE
                shadowElevation = DragDropDefaults.DRAG_ELEVATION
                alpha = DragDropDefaults.DRAG_ALPHA
            }
    } else {
        Modifier
            .animateItem(placementSpec = animationSpec)
    }

    Box(
        modifier = itemModifier.fillMaxWidth()
    ) {
        // Item content fills entire width
        itemContent(item)
        
        // Drag handle overlaid on top-right
        if (allowDrag) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(DragDropDefaults.DRAG_HANDLE_PADDING)
                    .onGloballyPositioned { coordinates ->
                        val position = coordinates.positionInParent()
                        val size = coordinates.size
                        onDragHandlePositioned(position.x..(position.x + size.width))
                    }
            ) {
                dragHandleContent()
            }
        }
    }
}

/**
 * Default drag handle implementation using Material Menu icon.
 * Provides a clean, unobtrusive drag handle with good touch target size.
 */
@Composable
fun DefaultDragHandle() {
    Icon(
        imageVector = Icons.Default.Menu,
        contentDescription = "Drag to reorder",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .size(DragDropDefaults.DRAG_HANDLE_SIZE)
            .clickable { /* No action needed - drag is handled by pointer input */ }
    )
}

/**
 * Default values and constants for DragDropColumn behavior and appearance.
 */
object DragDropDefaults {
    /** Scale factor applied to items while being dragged */
    const val DRAG_SCALE = 1.02f
    
    /** Alpha transparency for items while being dragged */
    const val DRAG_ALPHA = 0.9f
    
    /** Shadow elevation for items while being dragged */
    const val DRAG_ELEVATION = 8f
    
    /** Size of the default drag handle */
    val DRAG_HANDLE_SIZE = 32.dp
    
    /** Padding around the drag handle */
    val DRAG_HANDLE_PADDING = 8.dp
    
    /** Default animation spec for smooth item reordering transitions */
    val DEFAULT_ANIMATION_SPEC: FiniteAnimationSpec<IntOffset> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
}