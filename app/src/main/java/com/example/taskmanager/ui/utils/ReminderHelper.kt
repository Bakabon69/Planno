package com.example.taskmanager.ui.utils

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.taskmanager.data.Task
import com.example.taskmanager.receiver.TaskAlarmReceiver
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object ReminderHelper {

    fun scheduleTaskReminder(context: Context, task: Task) {
        if (!task.hasReminder) {
            cancelTaskReminder(context, task.id)
            return
        }

        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Parse time string e.g. "10:00 AM" or "17:30"
            val time = parseTimeString(task.time)
            var triggerDateTime = task.date.atTime(time).minusMinutes(task.reminderMinutesBefore.toLong())
            val now = LocalDateTime.now()

            // If the calculated lead time is in the past, but the actual task time is in the future,
            // schedule for right now + 5 seconds so the user is immediately alerted!
            if (triggerDateTime.isBefore(now)) {
                val actualTaskDateTime = task.date.atTime(time)
                if (actualTaskDateTime.isAfter(now)) {
                    triggerDateTime = now.plusSeconds(3)
                } else {
                    // Task time is entirely in the past
                    return
                }
            }

            val triggerEpochMs = triggerDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
                putExtra("task_id", task.id)
                putExtra("task_title", task.title)
                putExtra("task_description", if (task.description.isNotBlank()) task.description else "Scheduled for ${task.time}")
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                task.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerEpochMs, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerEpochMs, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerEpochMs, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerEpochMs, pendingIntent)
            }
        } catch (e: Exception) {
            Log.e("ReminderHelper", "Error scheduling reminder", e)
        }
    }

    fun snoozeTask(context: Context, taskId: Int, taskTitle: String, taskDescription: String, snoozeMinutes: Int = 5) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val triggerEpochMs = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000)

            val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
                putExtra("task_id", taskId)
                putExtra("task_title", taskTitle)
                putExtra("task_description", taskDescription)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerEpochMs, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerEpochMs, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerEpochMs, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerEpochMs, pendingIntent)
            }
        } catch (e: Exception) {
            Log.e("ReminderHelper", "Error snoozing task alarm", e)
        }
    }

    fun dismissAlarmNotification(context: Context, taskId: Int) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(taskId)
        } catch (_: Exception) {}
    }

    fun cancelTaskReminder(context: Context, taskId: Int) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, TaskAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
            dismissAlarmNotification(context, taskId)
        } catch (_: Exception) {}
    }

    fun parseTimeString(timeStr: String): LocalTime {
        return try {
            val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
            LocalTime.parse(timeStr.trim().uppercase(), formatter)
        } catch (e: Exception) {
            try {
                val formatter2 = DateTimeFormatter.ofPattern("H:mm", Locale.US)
                LocalTime.parse(timeStr.trim(), formatter2)
            } catch (e2: Exception) {
                LocalTime.of(9, 0)
            }
        }
    }
}
