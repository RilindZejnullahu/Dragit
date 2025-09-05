package com.example.dragit

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun <T : Any> DragDropColumn(
    items: List<T>,
    onSwap: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    allowDrag: Boolean = true,
    itemContent: @Composable LazyItemScope.(item: T) -> Unit
) {
    val listState = rememberLazyListState()
    val dragDropState = rememberDragDropState(listState) { fromIndex, toIndex ->
        onSwap(fromIndex, toIndex)
    }

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
                                // Touch down - potential drag start
                                val change = event.changes.first()
                                lastPosition = change.position
                                Log.d("DragDrop", "Touch down at: $lastPosition")

                                // Start dragging immediately on touch
                                isDragging = true
                                dragDropState.onDragStart(lastPosition)
                                Log.d("DragDrop", "onDragStart called at offset: $lastPosition")
                                change.consume()
                            }

                            isDragging && event.changes.any { it.pressed } -> {
                                // Continue dragging
                                val change = event.changes.first()
                                val currentPosition = change.position
                                val dragOffset = currentPosition - lastPosition

                                if (dragOffset != Offset.Zero) {
                                    Log.d("DragDrop", "onDrag called - offset: $dragOffset")
                                    dragDropState.onDrag(dragOffset)
                                }

                                lastPosition = currentPosition
                                change.consume()
                            }

                            isDragging && !event.changes.any { it.pressed } -> {
                                // Touch up - end dragging
                                Log.d("DragDrop", "onDragEnd called")
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
        contentPadding = PaddingValues(0.dp),
        verticalArrangement = Arrangement.spacedBy(40.dp)
    ) {
        itemsIndexed(items = items) { index, item ->
            val isDragging = allowDrag && index == dragDropState.currentIndexOfDraggedItem

            // Apply drag transformation directly to the item
            val itemModifier = if (isDragging) {
                Modifier
                    .zIndex(1f)
                    .graphicsLayer {
                        translationY = dragDropState.draggingItemOffset
                        scaleX = 1.02f
                        scaleY = 1.02f
                        shadowElevation = 8f
                        alpha = 0.95f
                    }
            } else {
                Modifier
            }

            // Debug logging
            if (isDragging) {
                Log.d("DragAnimation", "Item $index is being dragged, offset: ${dragDropState.draggingItemOffset}")
            }
            Log.d("DragAnimation", "LazyColumn item: index=$index, dragging=$isDragging")

            Box(modifier = itemModifier) {
                itemContent(item)
            }
        }
    }
}