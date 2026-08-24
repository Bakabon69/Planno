package com.example.taskmanager.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskmanager.data.Task
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarView(
    selectedDate: LocalDate,
    currentMonth: YearMonth,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChange: (YearMonth) -> Unit = {},
    tasks: List<Task>,
    showPastTasks: Boolean = true,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    
    // Filter task dates according to showPastTasks setting
    val taskDates = remember(tasks, showPastTasks) {
        val filtered = if (showPastTasks) tasks else tasks.filter { !it.date.isBefore(today) }
        filtered.map { it.date }.toSet()
    }
    
    val gridData = remember(currentMonth) {
        val firstDayOfMonth = currentMonth.atDay(1)
        val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value // 1 (Mon) to 7 (Sun)
        val daysInMonth = currentMonth.lengthOfMonth()
        val emptyDaysBefore = firstDayOfWeek - 1
        
        val items = mutableListOf<LocalDate?>()
        repeat(emptyDaysBefore) { items.add(null) }
        for (i in 1..daysInMonth) {
            items.add(currentMonth.atDay(i))
        }
        while (items.size % 7 != 0) {
            items.add(null)
        }
        items
    }

    var totalDrag by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(currentMonth) {
                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onDragEnd = {
                        if (totalDrag > 50) {
                            // Swiped right -> go to previous month
                            onMonthChange(currentMonth.minusMonths(1))
                        } else if (totalDrag < -50) {
                            // Swiped left -> go to next month
                            onMonthChange(currentMonth.plusMonths(1))
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        totalDrag += dragAmount
                    }
                )
            }
    ) {
        // Day Labels (Mon, Tue, ...)
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Month Days Grid with Smooth Slide Animation
        androidx.compose.animation.AnimatedContent(
            targetState = currentMonth,
            transitionSpec = {
                if (targetState > initialState) {
                    // Moving forward into next month: slide in from right, slide out to left
                    (androidx.compose.animation.slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth },
                        animationSpec = androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                    ) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(200)))
                        .togetherWith(
                            androidx.compose.animation.slideOutHorizontally(
                                targetOffsetX = { fullWidth -> -fullWidth },
                                animationSpec = androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                            ) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(150))
                        )
                } else {
                    // Moving backward into previous month: slide in from left, slide out to right
                    (androidx.compose.animation.slideInHorizontally(
                        initialOffsetX = { fullWidth -> -fullWidth },
                        animationSpec = androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                    ) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(200)))
                        .togetherWith(
                            androidx.compose.animation.slideOutHorizontally(
                                targetOffsetX = { fullWidth -> fullWidth },
                                animationSpec = androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                            ) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(150))
                        )
                }
            },
            label = "month_slide_transition"
        ) { targetMonth ->
            val monthGridData = remember(targetMonth) {
                val firstDayOfMonth = targetMonth.atDay(1)
                val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value // 1 (Mon) to 7 (Sun)
                val daysInMonth = targetMonth.lengthOfMonth()
                val emptyDaysBefore = firstDayOfWeek - 1
                
                val items = mutableListOf<LocalDate?>()
                repeat(emptyDaysBefore) { items.add(null) }
                for (i in 1..daysInMonth) {
                    items.add(targetMonth.atDay(i))
                }
                while (items.size % 7 != 0) {
                    items.add(null)
                }
                items
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                monthGridData.chunked(7).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEach { date ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (date != null) {
                                    val isSelected = date == selectedDate
                                    val isToday = date == today
                                    val hasTasks = taskDates.contains(date)

                                    val bubbleColor = when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        else -> Color.Transparent
                                    }

                                    val textColor = when {
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        isToday -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize(0.85f)
                                            .clip(CircleShape)
                                            .background(bubbleColor)
                                            .then(
                                                if (isToday && !isSelected) {
                                                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                                } else Modifier
                                            )
                                            .clickable { onDateSelected(date) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = date.dayOfMonth.toString(),
                                                color = textColor,
                                                fontSize = 15.sp,
                                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                            )
                                            
                                            // Task Indicator Dot
                                            if (hasTasks) {
                                                Box(
                                                    modifier = Modifier
                                                        .padding(top = 2.dp)
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                            else Color(0xFF00BFA6)
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CalendarViewPreview() {
    CalendarView(
        selectedDate = LocalDate.now(),
        currentMonth = YearMonth.now(),
        onDateSelected = {},
        tasks = emptyList()
    )
}
