package com.example.taskmanager.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.taskmanager.security.CryptoSecurityHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate

data class TaskDTO(
    val id: Int,
    val title: String,
    val description: String = "",
    val time: String = "10:00 AM",
    val date: String,
    val location: String = "",
    val color: Int,
    val completed: Boolean = false,
    val tag: String? = null,
    val priority: String? = "Medium",
    val completedAtEpochMs: Long? = null,
    val hasReminder: Boolean? = false,
    val reminderMinutesBefore: Int? = 0
)

class TaskRepository(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("tasks_vault_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveTasks(tasks: List<Task>) {
        val dtos = tasks.map { 
            TaskDTO(
                id = it.id,
                title = it.title,
                description = it.description,
                time = it.time,
                date = it.date.toString(),
                location = it.location,
                color = it.color.toArgb(),
                completed = it.completed,
                tag = it.tag,
                priority = it.priority,
                completedAtEpochMs = it.completedAtEpochMs,
                hasReminder = it.hasReminder,
                reminderMinutesBefore = it.reminderMinutesBefore
            ) 
        }
        val plainJson = gson.toJson(dtos)
        // Encrypt with AES-256-GCM before saving to disk
        val encryptedPayload = CryptoSecurityHelper.encrypt(plainJson)
        sharedPreferences.edit().putString("encrypted_tasks_payload", encryptedPayload).apply()
    }

    fun loadTasks(): List<Task> {
        val rawPayload = sharedPreferences.getString("encrypted_tasks_payload", null)
            ?: sharedPreferences.getString("tasks_list", null) // fallback to old key if migrating
            ?: return emptyList()

        // Decrypt payload with Keystore
        val decryptedJson = CryptoSecurityHelper.decrypt(rawPayload)

        return try {
            val type = object : TypeToken<List<TaskDTO>>() {}.type
            val dtos: List<TaskDTO> = gson.fromJson(decryptedJson, type) ?: return emptyList()
            dtos.map { 
                Task(
                    id = it.id,
                    title = it.title,
                    description = it.description,
                    time = it.time,
                    date = LocalDate.parse(it.date),
                    location = it.location,
                    color = Color(it.color),
                    completed = it.completed,
                    tag = it.tag,
                    priority = it.priority ?: "Medium",
                    completedAtEpochMs = it.completedAtEpochMs,
                    hasReminder = it.hasReminder ?: false,
                    reminderMinutesBefore = it.reminderMinutesBefore ?: 0
                ) 
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
