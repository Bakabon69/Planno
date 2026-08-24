package com.example.taskmanager.settings.ui.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskmanager.settings.model.UserProfileSettings
import com.example.taskmanager.settings.ui.components.SettingCard
import com.example.taskmanager.settings.ui.components.SettingSectionHeader

@Composable
fun ProfileSection(
    profile: UserProfileSettings,
    onProfileChange: (UserProfileSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingSectionHeader(
            title = "Profile & Account",
            description = "Manage personal details and work schedule."
        )

        SettingCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4D7FFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile.fullName.take(2).uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                Column {
                    Text(
                        text = profile.fullName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "@${profile.username} • ${profile.role}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            OutlinedTextField(
                value = profile.fullName,
                onValueChange = { onProfileChange(profile.copy(fullName = it)) },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF4D7FFF),
                    unfocusedBorderColor = Color.DarkGray
                )
            )

            OutlinedTextField(
                value = profile.email,
                onValueChange = { onProfileChange(profile.copy(email = it)) },
                label = { Text("Email Address") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF4D7FFF),
                    unfocusedBorderColor = Color.DarkGray
                )
            )
        }

        SettingCard {
            Text(
                text = "Work Schedule",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = profile.workHoursStart,
                    onValueChange = { onProfileChange(profile.copy(workHoursStart = it)) },
                    label = { Text("Start") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF4D7FFF),
                        unfocusedBorderColor = Color.DarkGray
                    )
                )

                OutlinedTextField(
                    value = profile.workHoursEnd,
                    onValueChange = { onProfileChange(profile.copy(workHoursEnd = it)) },
                    label = { Text("End") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF4D7FFF),
                        unfocusedBorderColor = Color.DarkGray
                    )
                )
            }
        }
    }
}
