package com.example.taskmanager.settings.ui.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskmanager.settings.model.AppearanceSettings
import com.example.taskmanager.settings.model.ThemeMode
import com.example.taskmanager.settings.ui.components.*

@Composable
fun AppearanceSection(
    appearance: AppearanceSettings,
    onAppearanceChange: (AppearanceSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingSectionHeader(
            title = "Appearance & Theming",
            description = "Customize colors and animations."
        )

        SettingCard {
            SettingDropdownItem(
                title = "Theme Mode",
                description = "Choose your preferred color theme.",
                selectedOption = appearance.theme,
                options = ThemeMode.entries.toList(),
                optionLabel = { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } },
                onOptionSelected = { onAppearanceChange(appearance.copy(theme = it)) }
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Accent Color",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White
                )
                ColorPalettePicker(
                    selectedColor = appearance.accentColor,
                    onColorSelected = { onAppearanceChange(appearance.copy(accentColor = it)) }
                )
            }
        }

        SettingCard {
            Text(
                text = "Animations",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White
            )

            SettingSwitchItem(
                title = "Confetti Celebrations",
                description = "Particle effects on task completion.",
                checked = appearance.confettiCelebration,
                onCheckedChange = { onAppearanceChange(appearance.copy(confettiCelebration = it)) }
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

            SettingSwitchItem(
                title = "Reduce Motion",
                description = "Simpler transitions for performance.",
                checked = appearance.reduceMotion,
                onCheckedChange = { onAppearanceChange(appearance.copy(reduceMotion = it)) }
            )
        }
    }
}
