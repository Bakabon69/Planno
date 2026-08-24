package com.example.taskmanager.settings.model

import kotlinx.serialization.Serializable

@Serializable
enum class ThemeMode {
    LIGHT, DARK, SYSTEM, HIGH_CONTRAST
}

@Serializable
enum class AccentColor(val hex: String, val displayName: String) {
    INDIGO("#6366F1", "Indigo"),
    EMERALD("#10B981", "Emerald"),
    VIOLET("#8B5CF6", "Violet"),
    ROSE("#EC4899", "Rose"),
    AMBER("#F59E0B", "Amber"),
    CYAN("#06B6D4", "Cyan"),
    ORANGE("#F97316", "Orange"),
    SLATE("#64748B", "Slate")
}

@Serializable
enum class DisplayDensity {
    COMFORTABLE, COMPACT, RELAXED
}

@Serializable
enum class DefaultViewType(val displayName: String, val icon: String) {
    INBOX("Inbox", "📥"),
    TODAY("Today's Focus", "⭐"),
    UPCOMING("Upcoming 7 Days", "📅"),
    KANBAN("Kanban Board", "📊"),
    MATRIX("Eisenhower Matrix", "🎯")
}

@Serializable
enum class PriorityLevel(val displayName: String, val colorHex: String) {
    NONE("None", "#94A3B8"),
    P3_LOW("Low (P3)", "#10B981"),
    P2_MEDIUM("Medium (P2)", "#F59E0B"),
    P1_HIGH("High (P1)", "#F97316"),
    P0_URGENT("Urgent (P0)", "#EF4444")
}

@Serializable
enum class AutoArchiveDuration(val displayName: String) {
    IMMEDIATELY("Immediately"),
    ONE_HOUR("After 1 hour"),
    TWENTY_FOUR_HOURS("After 24 hours"),
    SEVEN_DAYS("After 7 days"),
    NEVER("Never")
}

@Serializable
enum class SoundEffectType(val displayName: String) {
    ZEN_BELL("Zen Bell 🔔"),
    MARIMBA("Soft Marimba 🎵"),
    BUBBLE_POP("Bubble Pop 🫧"),
    NONE("Silent 🔕")
}

@Serializable
data class UserProfileSettings(
    val id: String = "usr_default_01",
    val fullName: String = "",
    val username: String = "",
    val email: String = "",
    val role: String = "",
    val avatarUrl: String? = null,
    val timezone: String = "Asia/Kolkata",
    val firstDayOfWeek: String = "Monday",
    val workHoursStart: String = "09:00",
    val workHoursEnd: String = "18:00",
    val hasCompletedOnboarding: Boolean = false
)

@Serializable
data class AppearanceSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: AccentColor = AccentColor.INDIGO,
    val density: DisplayDensity = DisplayDensity.COMFORTABLE,
    val confettiCelebration: Boolean = true,
    val reduceMotion: Boolean = false,
    val dynamicColor: Boolean = true
)

@Serializable
data class PomodoroSettings(
    val enabled: Boolean = true,
    val workMinutes: Int = 25,
    val breakMinutes: Int = 5,
    val longBreakMinutes: Int = 15
)

@Serializable
data class WorkflowSettings(
    val defaultView: DefaultViewType = DefaultViewType.TODAY,
    val defaultPriority: PriorityLevel = PriorityLevel.P2_MEDIUM,
    val autoArchiveDuration: AutoArchiveDuration = AutoArchiveDuration.TWENTY_FOUR_HOURS,
    val autoCompleteParentTask: Boolean = true,
    val showTaskIdentifiers: Boolean = false,
    val showPastTasksInCalendar: Boolean = true,
    val pomodoro: PomodoroSettings = PomodoroSettings()
)

@Serializable
data class NotificationSettings(
    val pushNotificationsEnabled: Boolean = true,
    val dailyDigestEmail: Boolean = true,
    val dailyDigestTime: String = "08:30",
    val reminderLeadMinutes: Int = 15,
    val quietHoursEnabled: Boolean = true,
    val quietHoursStart: String = "22:00",
    val quietHoursEnd: String = "07:00",
    val soundEffect: SoundEffectType = SoundEffectType.ZEN_BELL,
    val vibrationEnabled: Boolean = true
)

@Serializable
data class TagItem(
    val id: String,
    val name: String,
    val colorHex: String,
    val icon: String? = null
)

@Serializable
data class TaxonomySettings(
    val tags: List<TagItem> = listOf(
        TagItem("tag_1", "Work", "#4D7FFF"),
        TagItem("tag_2", "Personal", "#00BFA6"),
        TagItem("tag_3", "Fitness", "#FFD966"),
        TagItem("tag_4", "Design", "#B8A7FF"),
        TagItem("tag_5", "Urgent", "#E57373")
    )
)

@Serializable
data class IntegrationSettings(
    val googleCalendarSync: Boolean = false,
    val appleCalendarSync: Boolean = false,
    val slackNotifications: Boolean = false,
    val githubSync: Boolean = false,
    val webhookUrl: String = "",
    val apiKey: String = "sk_live_planno_demo_key_9942"
)

@Serializable
data class SecuritySettings(
    val enableScreenPrivacy: Boolean = false,
    val hardwareKeystoreEnabled: Boolean = true
)

@Serializable
data class AppSettings(
    val profile: UserProfileSettings = UserProfileSettings(),
    val appearance: AppearanceSettings = AppearanceSettings(),
    val workflow: WorkflowSettings = WorkflowSettings(),
    val notifications: NotificationSettings = NotificationSettings(),
    val taxonomy: TaxonomySettings = TaxonomySettings(),
    val integrations: IntegrationSettings = IntegrationSettings(),
    val security: SecuritySettings = SecuritySettings(),
    val appVersion: String = "2.4.0",
    val buildNumber: Int = 104,
    val lastUpdatedEpochMs: Long = System.currentTimeMillis()
)
