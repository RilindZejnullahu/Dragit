package com.example.dragit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dragit.ui.theme.DragItTheme

data class ListItem(
    val id: Int,
    val title: String,
    val color: Color
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DragItTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DragDropDemo(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun DragDropDemo(modifier: Modifier = Modifier) {
    var items by remember {
        mutableStateOf(
            (1..10).map { index ->
                ListItem(
                    id = index,
                    title = "Item $index", 
                    color = colors[(index - 1) % colors.size]
                )
            }
        )
    }

    DragDropColumn(
        items = items,
        onSwap = { fromIndex, toIndex ->
            items = items.toMutableList().apply {
                add(toIndex, removeAt(fromIndex))
            }
        },
        modifier = modifier.padding(16.dp),
        verticalSpacing = 12.dp
    ) { item ->
        ItemCard(item = item)
    }
}

@Composable
fun ItemCard(item: ListItem) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(item.color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = item.title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

private val colors = listOf(
    Color(0xFF6200EE), Color(0xFF3700B3), Color(0xFF03DAC6),
    Color(0xFFFF6200), Color(0xFFFF5722), Color(0xFF4CAF50),
    Color(0xFF2196F3), Color(0xFF9C27B0), Color(0xFFFF9800),
    Color(0xFF607D8B)
)

@Preview(showBackground = true)
@Composable
fun DragDropDemoPreview() {
    DragItTheme {
        DragDropDemo()
    }
}