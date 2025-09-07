package com.example.dragit

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

/**
 * State holder for drag-and-drop operations in a LazyColumn.
 * 
 * Manages drag gestures, collision detection, and item swapping.
 * Uses center-point collision detection for intuitive drag behavior.
 *
 * @param state The LazyListState of the associated LazyColumn
 * @param onSwap Callback invoked when items should be swapped
 */
class DragDropState internal constructor(
    val state: LazyListState,
    private val onSwap: (Int, Int) -> Unit
) {
    private var draggedDistance by mutableStateOf(0f)
    private var initialOffset by mutableStateOf(0)
    private var initialElement by mutableStateOf<LazyListItemInfo?>(null)
    private var lastSwapTargetBounds by mutableStateOf<IntRange?>(null)
    
    var currentDraggedIndex by mutableStateOf<Int?>(null)
    
    internal val draggingItemOffset: Float
        get() = currentDraggedIndex?.let { index ->
            state.layoutInfo.visibleItemsInfo
                .firstOrNull { it.index == index }
                ?.let { item -> initialOffset + draggedDistance - item.offset }
        } ?: 0f


    fun onDragStart(offset: Offset) {
        val hitItem = state.layoutInfo.visibleItemsInfo
            .firstOrNull { item -> 
                offset.y.toInt() in item.offset..(item.offset + item.size) 
            }
            
        hitItem?.let { item ->
            currentDraggedIndex = item.index
            initialElement = item
            initialOffset = item.offset
            lastSwapTargetBounds = null
        }
    }

    fun onDragInterrupted() {
        initialOffset = 0
        draggedDistance = 0f
        currentDraggedIndex = null
        initialElement = null
        lastSwapTargetBounds = null
    }

    fun onDrag(offset: Offset) {
        draggedDistance += offset.y
        
        val initialItem = initialElement ?: return
        val currentIndex = currentDraggedIndex ?: return
        
        val draggedStart = initialItem.offset + draggedDistance
        val draggedEnd = draggedStart + initialItem.size
        val draggedCenter = draggedStart + initialItem.size / 2
        
        // Find the item whose center the dragged item's center has crossed
        // Exclude items with the same bounds as the last swap target to prevent cascading
        val targetItem = state.layoutInfo.visibleItemsInfo
            .firstOrNull { item ->
                val itemBounds = item.offset..(item.offset + item.size)
                item.index != currentIndex &&
                itemBounds != lastSwapTargetBounds &&
                draggedCenter >= item.offset && 
                draggedCenter <= item.offset + item.size
            }
            
        targetItem?.let { target ->
            onSwap(currentIndex, target.index)
            lastSwapTargetBounds = target.offset..(target.offset + target.size)
            currentDraggedIndex = target.index
        }
    }
}

/**
 * Creates and remembers a [DragDropState] for the given [LazyListState].
 *
 * @param lazyListState The LazyListState to associate with drag-and-drop operations
 * @param onSwap Callback invoked when items should be swapped (fromIndex, toIndex)
 * @return A remembered DragDropState instance
 */
@Composable
fun rememberDragDropState(
    lazyListState: LazyListState,
    onSwap: (Int, Int) -> Unit
): DragDropState {
    return remember(lazyListState) {
        DragDropState(
            state = lazyListState,
            onSwap = onSwap
        )
    }
}