package com.example.taskmanager.data

import androidx.compose.ui.graphics.Color
import java.time.LocalDate

data class Task(
    val id: Int,
    val title: String,
    val description: String,
    val time: String,
    val date: LocalDate = LocalDate.now(),
    val location: String = "",
    val color: Color,
    val completed: Boolean = false,
    val tag: String? = null,
    val priority: String = "Medium",
    val completedAtEpochMs: Long? = null,
    val hasReminder: Boolean = false,
    val reminderMinutesBefore: Int = 0
)