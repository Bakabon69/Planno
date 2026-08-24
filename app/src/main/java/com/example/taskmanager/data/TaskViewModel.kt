package com.example.taskmanager.data

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import java.time.LocalDate

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TaskRepository(application)
    
    var tasks by mutableStateOf<List<Task>>(emptyList())
        private set

    init {
        val loadedTasks = repository.loadTasks()
        val cleanedTasks = loadedTasks.filterNot { it.title == "Gym Workout" && it.description == "Chest and shoulders session" }
        tasks = cleanedTasks
        if (loadedTasks.size != cleanedTasks.size) {
            repository.saveTasks(tasks)
        }
    }

    fun addTask(task: Task) {
        tasks = tasks + task
        repository.saveTasks(tasks)
    }

    fun updateTask(updatedTask: Task) {
        tasks = tasks.map { if (it.id == updatedTask.id) updatedTask else it }
        repository.saveTasks(tasks)
    }

    fun removeTasks(ids: Set<Int>) {
        tasks = tasks.filter { it.id !in ids }
        repository.saveTasks(tasks)
    }

    fun toggleTaskCompletion(id: Int, completed: Boolean) {
        tasks = tasks.map { 
            if (it.id == id) {
                it.copy(
                    completed = completed,
                    completedAtEpochMs = if (completed) System.currentTimeMillis() else null
                )
            } else it 
        }
        repository.saveTasks(tasks)
    }

    fun getActiveTasks(autoArchiveDurationHours: Long = 24): List<Task> {
        val now = System.currentTimeMillis()
        val archiveThresholdMs = autoArchiveDurationHours * 60 * 60 * 1000
        return tasks.filter {
            if (!it.completed) true
            else {
                val completedTime = it.completedAtEpochMs ?: now
                (now - completedTime) < archiveThresholdMs
            }
        }
    }
}
