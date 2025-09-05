package com.example.dragit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.StrokeCap
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
import androidx.compose.ui.zIndex

@Composable
fun <T : Any> DragDropColumn(
    items: List<T>,
    onSwap: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    allowDrag: Boolean = true,
    verticalSpacing: Dp = 8.dp,
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
        contentPadding = PaddingValues(0.dp),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing)
    ) {
        itemsIndexed(items = items) { index, item ->
            val isDragging = allowDrag && index == dragDropState.currentDraggedIndex
            val itemModifier = if (isDragging) {
                Modifier
                    .zIndex(1f)
                    .graphicsLayer {
                        translationY = dragDropState.draggingItemOffset
                        scaleX = 1.02f
                        scaleY = 1.02f
                        shadowElevation = 8f
                        alpha = 0.9f
                    }
            } else {
                Modifier
            }

            Row(
                modifier = itemModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    itemContent(item)
                }
                
                if (allowDrag) {
                    Box(
                        modifier = Modifier
                            .onGloballyPositioned { coordinates ->
                                val position = coordinates.positionInParent()
                                val size = coordinates.size
                                dragHandleXRange = position.x..(position.x + size.width)
                            }
                    ) {
                        dragHandleContent()
                    }
                }
            }
        }
    }
}

@Composable
fun DefaultDragHandle() {
    val handleColor = MaterialTheme.colorScheme.onSurfaceVariant
    
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable { /* No action needed - drag is handled by pointer input */ }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.size(24.dp)
        ) {
            val strokeWidth = 3.dp.toPx()
            val lineLength = size.width * 0.6f
            val lineSpacing = size.height / 4f
            val startX = (size.width - lineLength) / 2f
            
            // Draw three horizontal lines for drag handle
            repeat(3) { index ->
                val y = size.height / 2f + (index - 1) * lineSpacing
                drawLine(
                    color = handleColor,
                    start = androidx.compose.ui.geometry.Offset(startX, y),
                    end = androidx.compose.ui.geometry.Offset(startX + lineLength, y),
                    strokeWidth = strokeWidth,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }
    }
}