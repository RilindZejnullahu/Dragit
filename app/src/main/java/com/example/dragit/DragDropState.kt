package com.example.dragit

import android.util.Log
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class DragDropState internal constructor(
    val state: LazyListState,
    private val scope: CoroutineScope,
    private val onSwap: (Int, Int) -> Unit
) {
    private var draggedDistance by mutableStateOf(0f)
    private var draggingItemInitialOffset by mutableStateOf(0)
    internal val draggingItemOffset: Float
        get() = draggingItemLayoutInfo?.let { item ->
            draggingItemInitialOffset + draggedDistance - item.offset
        } ?: 0f
    private val draggingItemLayoutInfo: LazyListItemInfo?
        get() = state.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == currentIndexOfDraggedItem }

    internal var previousIndexOfDraggedItem by mutableStateOf<Int?>(null)
        private set

    // used to obtain initial offsets on drag start
    private var initiallyDraggedElement by mutableStateOf<LazyListItemInfo?>(null)

    var currentIndexOfDraggedItem by mutableStateOf<Int?>(null)

    private val initialOffsets: Pair<Int, Int>?
        get() = initiallyDraggedElement?.let { Pair(it.offset, it.offsetEnd) }

    private val currentElement: LazyListItemInfo?
        get() = currentIndexOfDraggedItem?.let {
            state.getVisibleItemInfoFor(absoluteIndex = it)
        }


    fun onDragStart(offset: Offset) {
        Log.d("DragDropState", "onDragStart - offset: $offset")
        val hitItem = state.layoutInfo.visibleItemsInfo
            .firstOrNull { item -> offset.y.toInt() in item.offset..(item.offset + item.size) }

        if (hitItem != null) {
            Log.d("DragDropState", "Hit item at index ${hitItem.index}, bounds: ${hitItem.offset}..${hitItem.offset + hitItem.size}")
            currentIndexOfDraggedItem = hitItem.index
            initiallyDraggedElement = hitItem
            draggingItemInitialOffset = hitItem.offset
        } else {
            Log.d("DragDropState", "No item hit at offset $offset")
            Log.d("DragDropState", "Available items: ${state.layoutInfo.visibleItemsInfo.map { "${it.index}: ${it.offset}..${it.offset + it.size}" }}")
        }
    }

    fun onDragInterrupted() {
        draggingItemInitialOffset = 0
        draggedDistance = 0f
        currentIndexOfDraggedItem = null
        initiallyDraggedElement = null
        previousIndexOfDraggedItem = null
    }

    fun onDrag(offset: Offset) {
        draggedDistance += offset.y
        Log.d("DragDropState", "onDrag - offset: $offset, total draggedDistance: $draggedDistance")

        initialOffsets?.let { (topOffset, bottomOffset) ->
            val startOffset = topOffset + draggedDistance
            val endOffset = bottomOffset + draggedDistance
            Log.d("DragDropState", "Dragged element bounds: $startOffset..$endOffset")

            currentElement?.let { hovered ->
                val candidateItem = state.layoutInfo.visibleItemsInfo
                    .filterNot { item -> item.offsetEnd < startOffset || item.offset > endOffset || hovered.index == item.index }
                    .firstOrNull { item ->
                        val delta = (startOffset - hovered.offset)
                        when {
                            delta > 0 -> (endOffset > item.offsetEnd)
                            else -> (startOffset < item.offset)
                        }
                    }

                candidateItem?.let { item ->
                    Log.d("DragDropState", "Swapping $currentIndexOfDraggedItem with ${item.index}")
                    currentIndexOfDraggedItem?.let { current ->
                        onSwap.invoke(current, item.index)
                    }
                    currentIndexOfDraggedItem = item.index
                }
            }
        }
    }

}