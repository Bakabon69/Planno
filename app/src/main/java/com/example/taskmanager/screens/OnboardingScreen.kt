package com.example.taskmanager.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskmanager.settings.model.TagItem
import com.example.taskmanager.settings.model.UserProfileSettings
import com.example.taskmanager.ui.components.MacColorPicker

data class StarterTag(
    val id: String,
    val name: String,
    var colorHex: String,
    val icon: String,
    val defaultSelected: Boolean = false
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    onCompleteOnboarding: (UserProfileSettings, List<TagItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // User Form State (Completely Independent Fields - NO AUTO-OVERWRITE)
    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var selectedAvatarEmoji by remember { mutableStateOf("⚡") }
    var customImageUri by remember { mutableStateOf<Uri?>(null) }

    // Tag Customization State
    var activeEditingTagId by remember { mutableStateOf<String?>("tag_1") }
    var customTagColorHex by remember { mutableStateOf("#007AFF") }
    var newCustomTagName by remember { mutableStateOf("") }

    val customBitmap = remember(customImageUri) {
        customImageUri?.let { uri ->
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            customImageUri = uri
            selectedAvatarEmoji = ""
        }
    }

    // Avatar Presets
    val avatarPresets = listOf("⚡", "🚀", "🦊", "🎨", "🧘", "💼", "🤖", "🌟", "💡", "🎯")

    // Starter Tags List
    var starterTags by remember {
        mutableStateOf(
            listOf(
                StarterTag("tag_1", "Work", "#007AFF", "💼", defaultSelected = true),
                StarterTag("tag_2", "Personal", "#34C759", "🏠", defaultSelected = true),
                StarterTag("tag_3", "Fitness", "#FFCC00", "🏋️", defaultSelected = true),
                StarterTag("tag_4", "Design", "#AF52DE", "🎨", defaultSelected = true),
                StarterTag("tag_5", "Study", "#30B0C7", "📚", defaultSelected = false),
                StarterTag("tag_6", "Coding", "#00C7BE", "💻", defaultSelected = true),
                StarterTag("tag_7", "Finance", "#FF9500", "💰", defaultSelected = false),
                StarterTag("tag_8", "Health", "#FF2D55", "🧘", defaultSelected = false),
                StarterTag("tag_9", "Side Project", "#5856D6", "🚀", defaultSelected = true),
                StarterTag("tag_10", "Urgent", "#FF3B30", "🔥", defaultSelected = false)
            )
        )
    }

    var selectedTagIds by remember {
        mutableStateOf(starterTags.filter { it.defaultSelected }.map { it.id }.toSet())
    }

    // Skip Action
    fun handleSkip() {
        val defaultTags = starterTags
            .filter { it.id in selectedTagIds }
            .map { TagItem(id = it.id, name = it.name, colorHex = it.colorHex) }

        onCompleteOnboarding(
            UserProfileSettings(
                fullName = "",
                username = "",
                email = "",
                role = "",
                avatarUrl = null,
                hasCompletedOnboarding = true
            ),
            defaultTags
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // 🌟 Top Navigation Bar with Prominent Skip Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✨ First-Time Setup",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // High-visibility Skip Button
            OutlinedButton(
                onClick = { handleSkip() },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Skip",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = "Skip",
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Welcome Logo & Header
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                    )
                )
                .shadow(12.dp, CircleShape, spotColor = MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Checklist,
                contentDescription = "Planno Logo",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Welcome to Planno",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Personalize your workspace or skip to explore directly.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
            lineHeight = 20.sp
        )

        // 1. Profile Picture & Avatar Selector Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Profile Avatar",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Choose an avatar or upload a photo (Optional)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                // Avatar Preview Swatch
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (customBitmap != null) {
                        Image(
                            bitmap = customBitmap,
                            contentDescription = "Profile Picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else if (selectedAvatarEmoji.isNotBlank()) {
                        Text(
                            text = selectedAvatarEmoji,
                            fontSize = 36.sp
                        )
                    } else {
                        Text(
                            text = if (fullName.isNotBlank()) fullName.take(2).uppercase() else "ME",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Avatar Presets Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    avatarPresets.forEach { emoji ->
                        val isSelected = selectedAvatarEmoji == emoji && customImageUri == null
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable {
                                    selectedAvatarEmoji = emoji
                                    customImageUri = null
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = emoji, fontSize = 18.sp)
                            }
                        }
                    }

                    // Gallery Photo Picker
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { imagePickerLauncher.launch("image/*") }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.AddAPhoto,
                                contentDescription = "Upload Photo",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Personal Information Fields (COMPLETELY INDEPENDENT)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Personal Information",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Full Name Input (Typing here DOES NOT change username)
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Your Full Name (Optional)") },
                    placeholder = { Text("e.g. Robin or Jack") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                // Username Input (Completely Independent)
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username (@handle) (Optional)") },
                    placeholder = { Text("e.g. robin_99") },
                    leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                // Email Input
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address (Optional)") },
                    placeholder = { Text("e.g. robin@example.com") },
                    leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                // Role / Title Input
                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Role / Primary Focus (Optional)") },
                    placeholder = { Text("e.g. Designer, Developer, Student") },
                    leadingIcon = { Icon(Icons.Default.Work, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Apple/Mac Style Color Palette & Tags Selector Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column {
                    Text(
                        text = "Tags & Color Customization",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Pick active tags. Tap any tag to customize its color.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Starter Tag Chips Flow
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    starterTags.forEach { tag ->
                        val isSelected = tag.id in selectedTagIds
                        val isEditing = activeEditingTagId == tag.id
                        val tagColor = runCatching { Color(android.graphics.Color.parseColor(tag.colorHex)) }.getOrDefault(Color(0xFF007AFF))
                        
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) tagColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                width = if (isEditing) 2.dp else if (isSelected) 1.dp else 0.5.dp,
                                color = if (isEditing) MaterialTheme.colorScheme.onSurface else if (isSelected) tagColor else Color.Transparent
                            ),
                            modifier = Modifier.clickable {
                                activeEditingTagId = tag.id
                                customTagColorHex = tag.colorHex
                                selectedTagIds = if (isSelected) {
                                    if (selectedTagIds.size > 1) selectedTagIds - tag.id else selectedTagIds
                                } else {
                                    selectedTagIds + tag.id
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(tagColor)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "${tag.icon} ${tag.name}",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) tagColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isSelected) {
                                    Spacer(Modifier.width(6.dp))
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = tagColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Add Custom Tag Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newCustomTagName,
                        onValueChange = { newCustomTagName = it },
                        placeholder = { Text("Add custom tag (e.g. Health)...", fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (newCustomTagName.isNotBlank()) {
                                val newId = "tag_custom_${System.currentTimeMillis()}"
                                val newTag = StarterTag(newId, newCustomTagName.trim(), customTagColorHex, "🏷️", defaultSelected = true)
                                starterTags = starterTags + newTag
                                selectedTagIds = selectedTagIds + newId
                                activeEditingTagId = newId
                                newCustomTagName = ""
                            }
                        },
                        enabled = newCustomTagName.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("+ Add")
                    }
                }

                // Rainbow Spectrum & Color Picker
                val editingTagName = starterTags.find { it.id == activeEditingTagId }?.name ?: "Tag"
                MacColorPicker(
                    selectedColorHex = customTagColorHex,
                    title = "🎨 Customize '$editingTagName' Color",
                    onColorSelected = { hex ->
                        customTagColorHex = hex
                        activeEditingTagId?.let { id ->
                            starterTags = starterTags.map {
                                if (it.id == id) it.copy(colorHex = hex) else it
                            }
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Get Started CTA Button
        Button(
            onClick = {
                val finalTags = starterTags
                    .filter { it.id in selectedTagIds }
                    .map { TagItem(id = it.id, name = it.name, colorHex = it.colorHex) }

                val profile = UserProfileSettings(
                    fullName = fullName.trim(),
                    username = username.trim(),
                    email = email.trim(),
                    role = if (role.isNotBlank()) role.trim() else "Productivity Explorer",
                    avatarUrl = customImageUri?.toString() ?: selectedAvatarEmoji,
                    hasCompletedOnboarding = true
                )

                onCompleteOnboarding(profile, finalTags)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(18.dp),
                    spotColor = MaterialTheme.colorScheme.primary
                ),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🚀 Launch Workspace",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Prominent Secondary Skip Button
        OutlinedButton(
            onClick = { handleSkip() },
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                text = "Skip for now & setup later",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(36.dp))
    }
}
