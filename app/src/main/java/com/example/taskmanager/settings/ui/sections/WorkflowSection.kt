package com.example.taskmanager.settings.ui.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.taskmanager.settings.model.AutoArchiveDuration
import com.example.taskmanager.settings.model.DefaultViewType
import com.example.taskmanager.settings.model.PriorityLevel
import com.example.taskmanager.settings.model.WorkflowSettings
import com.example.taskmanager.settings.ui.components.SettingCard
import com.example.taskmanager.settings.ui.components.SettingDropdownItem
import com.example.taskmanager.settings.ui.components.SettingSectionHeader
import com.example.taskmanager.settings.ui.components.SettingSwitchItem

@Composable
fun WorkflowSection(
    workflow: WorkflowSettings,
    onWorkflowChange: (WorkflowSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingSectionHeader(
            title = "Tasks & Workflows",
            description = "Configure default attributes and focus timer."
        )

        SettingCard {
            SettingDropdownItem(
                title = "Default Startup View",
                selectedOption = workflow.defaultView,
                options = DefaultViewType.entries.toList(),
                optionLabel = { "${it.icon} ${it.displayName}" },
                onOptionSelected = { onWorkflowChange(workflow.copy(defaultView = it)) }
            )

            SettingDropdownItem(
                title = "Default Priority",
                selectedOption = workflow.defaultPriority,
                options = PriorityLevel.entries.toList(),
                optionLabel = { it.displayName },
                onOptionSelected = { onWorkflowChange(workflow.copy(defaultPriority = it)) }
            )

            SettingDropdownItem(
                title = "Auto-Archive Completed Tasks",
                selectedOption = workflow.autoArchiveDuration,
                options = AutoArchiveDuration.entries.toList(),
                optionLabel = { it.displayName },
                onOptionSelected = { onWorkflowChange(workflow.copy(autoArchiveDuration = it)) }
            )
        }

        SettingCard {
            Text(
                text = "Productivity & Timer",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White
            )

            SettingSwitchItem(
                title = "Pomodoro Timer Integration",
                description = "Enable 25-minute focus intervals attached to active tasks.",
                checked = workflow.pomodoro.enabled,
                onCheckedChange = {
                    onWorkflowChange(workflow.copy(pomodoro = workflow.pomodoro.copy(enabled = it)))
                }
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

            SettingSwitchItem(
                title = "Auto-Complete Parent Task",
                description = "Automatically complete parent tasks when all subtasks finish.",
                checked = workflow.autoCompleteParentTask,
                onCheckedChange = { onWorkflowChange(workflow.copy(autoCompleteParentTask = it)) }
            )
        }
    }
}
