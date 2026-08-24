package com.example.taskmanager.settings.ui.sections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.taskmanager.settings.model.TaxonomySettings
import com.example.taskmanager.settings.ui.components.SettingCard
import com.example.taskmanager.settings.ui.components.SettingSectionHeader
import com.example.taskmanager.settings.ui.components.TagChip

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaxonomySection(
    taxonomy: TaxonomySettings,
    onAddTag: (name: String, colorHex: String) -> Unit,
    onRemoveTag: (tagId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var newTagName by remember { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingSectionHeader(
            title = "Tags & Labels",
            description = "Organize custom tags across your tasks."
        )

        SettingCard {
            Text(
                text = "Manage Tags",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    placeholder = { Text("New tag name...", color = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF4D7FFF),
                        unfocusedBorderColor = Color.DarkGray
                    )
                )

                Button(
                    onClick = {
                        if (newTagName.isNotBlank()) {
                            onAddTag(newTagName.trim(), "#4D7FFF")
                            newTagName = ""
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4D7FFF))
                ) {
                    Text("+ Add")
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                taxonomy.tags.forEach { tag ->
                    TagChip(
                        name = tag.name,
                        colorHex = tag.colorHex,
                        onRemove = { onRemoveTag(tag.id) }
                    )
                }
            }
        }
    }
}
