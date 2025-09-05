package com.example.dragit

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

class DragDropState internal constructor(
    val state: LazyListState,
    private val onSwap: (Int, Int) -> Unit
) {
    private var draggedDistance by mutableStateOf(0f)
    private var initialOffset by mutableStateOf(0)
    private var initialElement by mutableStateOf<LazyListItemInfo?>(null)
    
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
        }
    }

    fun onDragInterrupted() {
        initialOffset = 0
        draggedDistance = 0f
        currentDraggedIndex = null
        initialElement = null
    }

    fun onDrag(offset: Offset) {
        draggedDistance += offset.y
        
        val initialItem = initialElement ?: return
        val currentIndex = currentDraggedIndex ?: return
        
        val draggedStart = initialItem.offset + draggedDistance
        val draggedEnd = draggedStart + initialItem.size
        val draggedCenter = draggedStart + initialItem.size / 2
        
        // Find the item whose center the dragged item's center has crossed
        val targetItem = state.layoutInfo.visibleItemsInfo
            .firstOrNull { item ->
                item.index != currentIndex && 
                draggedCenter >= item.offset && 
                draggedCenter <= item.offset + item.size
            }
            
        targetItem?.let { target ->
            onSwap(currentIndex, target.index)
            currentDraggedIndex = target.index
        }
    }

}