package com.example.taskmanager.settings.ui.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.taskmanager.settings.model.NotificationSettings
import com.example.taskmanager.settings.model.SoundEffectType
import com.example.taskmanager.settings.ui.components.SettingCard
import com.example.taskmanager.settings.ui.components.SettingDropdownItem
import com.example.taskmanager.settings.ui.components.SettingSectionHeader
import com.example.taskmanager.settings.ui.components.SettingSwitchItem

@Composable
fun NotificationSection(
    notifications: NotificationSettings,
    onNotificationsChange: (NotificationSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingSectionHeader(
            title = "Notifications & Audio",
            description = "Manage reminder alerts and completion sounds."
        )

        SettingCard {
            SettingSwitchItem(
                title = "Push Notifications",
                description = "Receive instant alerts when tasks become due.",
                checked = notifications.pushNotificationsEnabled,
                onCheckedChange = {
                    onNotificationsChange(notifications.copy(pushNotificationsEnabled = it))
                }
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

            SettingSwitchItem(
                title = "Daily Summary Email",
                description = "Receive daily priorities overview at ${notifications.dailyDigestTime}.",
                checked = notifications.dailyDigestEmail,
                onCheckedChange = {
                    onNotificationsChange(notifications.copy(dailyDigestEmail = it))
                }
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

            SettingDropdownItem(
                title = "Completion Sound Effect",
                selectedOption = notifications.soundEffect,
                options = SoundEffectType.entries.toList(),
                optionLabel = { it.displayName },
                onOptionSelected = {
                    onNotificationsChange(notifications.copy(soundEffect = it))
                }
            )
        }
    }
}
