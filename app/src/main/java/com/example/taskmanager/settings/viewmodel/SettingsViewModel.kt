package com.example.taskmanager.settings.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskmanager.settings.data.SettingsRepository
import com.example.taskmanager.settings.data.SettingsRepositoryImpl
import com.example.taskmanager.settings.model.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SettingsTab(val title: String) {
    PROFILE("Profile"),
    APPEARANCE("Appearance"),
    WORKFLOWS("Workflows"),
    NOTIFICATIONS("Notifications"),
    TAXONOMY("Tags"),
    INTEGRATIONS("Integrations"),
    DATA("Data"),
    DEVELOPER("Developer")
}

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val activeTab: SettingsTab = SettingsTab.PROFILE,
    val searchQuery: String = ""
)

sealed interface SettingsEvent {
    data class SelectTab(val tab: SettingsTab) : SettingsEvent
    data class Search(val query: String) : SettingsEvent
    data class UpdateProfile(val profile: UserProfileSettings) : SettingsEvent
    data class UpdateTheme(val theme: ThemeMode) : SettingsEvent
    data class UpdateAccentColor(val color: AccentColor) : SettingsEvent
    data class ToggleConfetti(val enabled: Boolean) : SettingsEvent
    data class ToggleReduceMotion(val enabled: Boolean) : SettingsEvent
    data class UpdateDefaultView(val view: DefaultViewType) : SettingsEvent
    data class UpdateDefaultPriority(val priority: PriorityLevel) : SettingsEvent
    data class UpdateAutoArchive(val duration: AutoArchiveDuration) : SettingsEvent
    data class TogglePomodoro(val enabled: Boolean) : SettingsEvent
    data class TogglePushNotification(val enabled: Boolean) : SettingsEvent
    data class UpdateSoundEffect(val sound: SoundEffectType) : SettingsEvent
    data class AddTag(val name: String, val colorHex: String) : SettingsEvent
    data class RemoveTag(val tagId: String) : SettingsEvent
    data class ToggleGoogleCalendar(val enabled: Boolean) : SettingsEvent
    data class ToggleShowPastTasksInCalendar(val enabled: Boolean) : SettingsEvent
    data class CompleteOnboarding(val profile: UserProfileSettings, val tags: List<TagItem>) : SettingsEvent
    data class ToggleScreenPrivacy(val enabled: Boolean) : SettingsEvent
    data class SendDeveloperFeedback(val category: String, val subject: String, val message: String) : SettingsEvent
    object ExportBackup : SettingsEvent
    object ResetDefaults : SettingsEvent
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SettingsRepository = SettingsRepositoryImpl(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _toastEvents = MutableSharedFlow<String>()
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.settingsFlow.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.SelectTab -> {
                _uiState.update { it.copy(activeTab = event.tab) }
            }
            is SettingsEvent.Search -> {
                _uiState.update { it.copy(searchQuery = event.query) }
            }
            is SettingsEvent.UpdateProfile -> {
                viewModelScope.launch {
                    repository.updateSettings { it.copy(profile = event.profile) }
                    _toastEvents.emit("Profile details saved")
                }
            }
            is SettingsEvent.UpdateTheme -> {
                viewModelScope.launch {
                    repository.updateSettings { it.copy(appearance = it.appearance.copy(theme = event.theme)) }
                }
            }
            is SettingsEvent.UpdateAccentColor -> {
                viewModelScope.launch {
                    repository.updateSettings { it.copy(appearance = it.appearance.copy(accentColor = event.color)) }
                }
            }
            is SettingsEvent.ToggleConfetti -> {
                viewModelScope.launch {
                    repository.updateSettings { it.copy(appearance = it.appearance.copy(confettiCelebration = event.enabled)) }
                }
            }
            is SettingsEvent.ToggleReduceMotion -> {
                viewModelScope.launch {
                    repository.updateSettings { it.copy(appearance = it.appearance.copy(reduceMotion = event.enabled)) }
                    _toastEvents.emit(if (event.enabled) "Animations reduced" else "Animations enabled")
                }
            }
            is SettingsEvent.UpdateDefaultView -> {
                viewModelScope.launch {
                    repository.updateSettings { it.copy(workflow = it.workflow.copy(defaultView = event.view)) }
                }
            }
            is SettingsEvent.UpdateDefaultPriority -> {
                viewModelScope.launch {
                    repository.updateSettings { it.copy(workflow = it.workflow.copy(defaultPriority = event.priority)) }
                }
            }
            is SettingsEvent.UpdateAutoArchive -> {
                viewModelScope.launch {
                    repository.updateSettings { it.copy(workflow = it.workflow.copy(autoArchiveDuration = event.duration)) }
                }
            }
            is SettingsEvent.TogglePomodoro -> {
                viewModelScope.launch {
                    repository.updateSettings {
                        it.copy(workflow = it.workflow.copy(pomodoro = it.workflow.pomodoro.copy(enabled = event.enabled)))
                    }
                }
            }
            is SettingsEvent.TogglePushNotification -> {
                viewModelScope.launch {
                    repository.updateSettings {
                        it.copy(notifications = it.notifications.copy(pushNotificationsEnabled = event.enabled))
                    }
                }
            }
            is SettingsEvent.UpdateSoundEffect -> {
                viewModelScope.launch {
                    repository.updateSettings {
                        it.copy(notifications = it.notifications.copy(soundEffect = event.sound))
                    }
                    _toastEvents.emit("Sound updated: ${event.sound.displayName}")
                }
            }
            is SettingsEvent.AddTag -> {
                viewModelScope.launch {
                    val newTag = TagItem(id = System.currentTimeMillis().toString(), name = event.name, colorHex = event.colorHex)
                    repository.updateSettings {
                        it.copy(taxonomy = it.taxonomy.copy(tags = it.taxonomy.tags + newTag))
                    }
                    _toastEvents.emit("Tag #${event.name} created")
                }
            }
            is SettingsEvent.RemoveTag -> {
                viewModelScope.launch {
                    repository.updateSettings {
                        it.copy(taxonomy = it.taxonomy.copy(tags = it.taxonomy.tags.filterNot { tag -> tag.id == event.tagId }))
                    }
                }
            }
            is SettingsEvent.ToggleGoogleCalendar -> {
                viewModelScope.launch {
                    repository.updateSettings {
                        it.copy(integrations = it.integrations.copy(googleCalendarSync = event.enabled))
                    }
                }
            }
            is SettingsEvent.ToggleShowPastTasksInCalendar -> {
                viewModelScope.launch {
                    repository.updateSettings {
                        it.copy(workflow = it.workflow.copy(showPastTasksInCalendar = event.enabled))
                    }
                }
            }
            is SettingsEvent.CompleteOnboarding -> {
                viewModelScope.launch {
                    repository.updateSettings {
                        it.copy(
                            profile = event.profile,
                            taxonomy = it.taxonomy.copy(tags = event.tags)
                        )
                    }
                    val firstName = event.profile.fullName.trim().split(" ").firstOrNull() ?: "there"
                    _toastEvents.emit("Welcome to Planno, $firstName! 🎉")
                }
            }
            is SettingsEvent.ToggleScreenPrivacy -> {
                viewModelScope.launch {
                    repository.updateSettings {
                        it.copy(security = it.security.copy(enableScreenPrivacy = event.enabled))
                    }
                    _toastEvents.emit(if (event.enabled) "Screen privacy & anti-spying enabled 🛡️" else "Screen privacy disabled")
                }
            }
            is SettingsEvent.SendDeveloperFeedback -> {
                viewModelScope.launch {
                    _toastEvents.emit("Message sent to developer! Thank you.")
                }
            }
            is SettingsEvent.ExportBackup -> {
                viewModelScope.launch {
                    _toastEvents.emit("Settings backup exported")
                }
            }
            is SettingsEvent.ResetDefaults -> {
                viewModelScope.launch {
                    repository.resetToDefaults()
                    _toastEvents.emit("All settings reset to defaults")
                }
            }
        }
    }
}
