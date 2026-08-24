package com.example.taskmanager.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskmanager.components.ProgressCard
import com.example.taskmanager.components.TaskCard
import com.example.taskmanager.data.Task

@Composable
fun TodayScreen(
    tasks: List<Task>,
    onCompletedChange: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8F7FC)),

        contentPadding = PaddingValues(
            start = 20.dp,
            top = 20.dp,
            end = 20.dp,
            bottom = 100.dp
        ),

        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Header
        item {

            Text(
                text = "My Tasks",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Progress
        item {

            ProgressCard(
                completed = tasks.count { it.completed },
                total = tasks.size
            )
        }

        // Section title
        item {

            Text(
                text = "Today's tasks",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Tasks
        items(
            items = tasks,
            key = { task -> task.id }
        ) { task ->

            TaskCard(
                task = task,
                onCompletedChange = { completed ->

                    onCompletedChange(
                        task.id,
                        completed
                    )
                }
            )
        }

        // Extra space at bottom
        item {

            Spacer(
                modifier = Modifier.height(80.dp)
            )
        }
    }
}