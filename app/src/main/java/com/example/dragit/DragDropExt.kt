package com.example.dragit

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

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

