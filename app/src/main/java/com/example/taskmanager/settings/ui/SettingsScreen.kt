package com.example.taskmanager.settings.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskmanager.security.SecurityCheckHelper
import com.example.taskmanager.settings.model.*
import com.example.taskmanager.settings.ui.components.*
import com.example.taskmanager.settings.viewmodel.*
import com.example.taskmanager.ui.components.MacColorPicker
import com.example.taskmanager.ui.utils.SoundHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.toastEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            Text(
                text = "Settings",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Tabs Horizontal Scroll
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingsTab.entries.forEach { tab ->
                    val isSelected = uiState.activeTab == tab
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onEvent(SettingsEvent.SelectTab(tab)) },
                        label = { Text(tab.title, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            labelColor = MaterialTheme.colorScheme.onSurface,
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                            selectedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // Tab Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (uiState.activeTab) {
                    SettingsTab.PROFILE -> AestheticProfileSection(uiState, viewModel)
                    SettingsTab.APPEARANCE -> AppearanceSection(uiState, viewModel)
                    SettingsTab.WORKFLOWS -> WorkflowSection(uiState, viewModel)
                    SettingsTab.NOTIFICATIONS -> NotificationSection(uiState, viewModel)
                    SettingsTab.TAXONOMY -> TaxonomySection(uiState, viewModel)
                    SettingsTab.INTEGRATIONS -> IntegrationSection(uiState, viewModel)
                    SettingsTab.DATA -> DataSection(uiState, viewModel)
                    SettingsTab.DEVELOPER -> DeveloperSection(uiState, viewModel)
                }
            }
        }
    }
}

/* ========================================================================= */
/* 1. Aesthetic Profile & Account Section (Redesigned)                      */
/* ========================================================================= */
@Composable
fun AestheticProfileSection(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }
    val profile = uiState.settings.profile

    var editName by remember(profile.fullName) { mutableStateOf(profile.fullName) }
    var editUsername by remember(profile.username) { mutableStateOf(profile.username) }
    var editEmail by remember(profile.email) { mutableStateOf(profile.email) }
    var editRole by remember(profile.role) { mutableStateOf(profile.role) }
    var editAvatar by remember(profile.avatarUrl) { mutableStateOf(profile.avatarUrl ?: "⚡") }
    var editWorkStart by remember(profile.workHoursStart) { mutableStateOf(profile.workHoursStart) }
    var editWorkEnd by remember(profile.workHoursEnd) { mutableStateOf(profile.workHoursEnd) }

    val avatarPresets = listOf("⚡", "🚀", "🦊", "🎨", "🧘", "💼", "🤖", "🌟", "💡", "🎯")

    // Image Picker Launcher for Custom Photo
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            editAvatar = uri.toString()
        }
    }

    // Decode Custom Image Bitmap if present
    val customBitmap = remember(profile.avatarUrl) {
        profile.avatarUrl?.takeIf { it.startsWith("content://") || it.startsWith("file://") }?.let { uriStr ->
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(uriStr))?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }

    val editCustomBitmap = remember(editAvatar) {
        editAvatar.takeIf { it.startsWith("content://") || it.startsWith("file://") }?.let { uriStr ->
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(uriStr))?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    "Edit Profile & Avatar",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                ) {
                    // Avatar / Photo Selection Row
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (editCustomBitmap != null) {
                            Image(
                                bitmap = editCustomBitmap,
                                contentDescription = "Profile Picture",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        } else if (editAvatar.isNotBlank() && !editAvatar.startsWith("http")) {
                            Text(text = editAvatar, fontSize = 32.sp)
                        } else {
                            Text(
                                text = if (editName.isNotBlank()) editName.take(2).uppercase() else "ME",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Avatar Emoji Presets & Photo Picker
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        avatarPresets.forEach { emoji ->
                            val isSelected = editAvatar == emoji
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable { editAvatar = emoji }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = emoji, fontSize = 16.sp)
                                }
                            }
                        }

                        // Upload Photo Button
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .size(36.dp)
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

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Display Name") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editUsername,
                        onValueChange = { editUsername = it },
                        label = { Text("Username (@handle)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editRole,
                        onValueChange = { editRole = it },
                        label = { Text("Role / Title") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editWorkStart,
                            onValueChange = { editWorkStart = it },
                            label = { Text("Work Start") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = editWorkEnd,
                            onValueChange = { editWorkEnd = it },
                            label = { Text("Work End") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onEvent(
                            SettingsEvent.UpdateProfile(
                                profile.copy(
                                    fullName = editName.trim(),
                                    username = editUsername.trim(),
                                    email = editEmail.trim(),
                                    role = editRole.trim(),
                                    avatarUrl = editAvatar,
                                    workHoursStart = editWorkStart.trim(),
                                    workHoursEnd = editWorkEnd.trim(),
                                    hasCompletedOnboarding = true
                                )
                            )
                        )
                        showEditDialog = false
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    SettingSectionHeader(
        title = "Profile & Account",
        description = "Personal identity, avatar, work hours, and preferences."
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val displayName = if (profile.fullName.isNotBlank()) profile.fullName else "Welcome User"
            val displayRole = if (profile.role.isNotBlank()) profile.role else "Productivity Explorer"
            val initials = if (profile.fullName.isNotBlank()) profile.fullName.take(2).uppercase() else "ME"

            // Interactive Avatar Badge
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable { showEditDialog = true },
                contentAlignment = Alignment.Center
            ) {
                if (customBitmap != null) {
                    Image(
                        bitmap = customBitmap,
                        contentDescription = "Profile Picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else if (!profile.avatarUrl.isNullOrBlank() && !profile.avatarUrl.startsWith("http")) {
                    Text(
                        text = profile.avatarUrl,
                        fontSize = 36.sp
                    )
                } else {
                    Text(
                        text = initials,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = displayName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = if (profile.username.isNotBlank()) "$displayRole • @${profile.username}" else displayRole,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )

            if (profile.email.isNotBlank()) {
                Text(
                    text = profile.email,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⏰ Work Schedule: ${profile.workHoursStart} - ${profile.workHoursEnd} (Mon - Fri)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = { showEditDialog = true },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Edit Profile & Avatar",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

/* ========================================================================= */
/* 2. Appearance Section (With Working Reduce Motion)                        */
/* ========================================================================= */
@Composable
fun AppearanceSection(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    SettingSectionHeader(
        title = "Appearance & Theming",
        description = "Switch between Dark, Light, or System theme and choose accent colors."
    )

    SettingCard {
        Text(
            "Theme Mode",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                ThemeMode.DARK to "🌙 Dark",
                ThemeMode.LIGHT to "☀️ Light",
                ThemeMode.SYSTEM to "⚙️ System"
            ).forEach { (theme, label) ->
                val isSelected = uiState.settings.appearance.theme == theme
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.onEvent(SettingsEvent.UpdateTheme(theme)) }
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        Column {
            Text(
                "Accent Color Palette",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(10.dp))
            ColorPalettePicker(
                selectedColor = uiState.settings.appearance.accentColor,
                onColorSelected = { viewModel.onEvent(SettingsEvent.UpdateAccentColor(it)) }
            )
        }
    }

    SettingCard {
        Text(
            "Motion & Accessibility",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )

        SettingSwitchItem(
            title = "Confetti Celebrations",
            description = "Show visual celebratory particle animation on finishing tasks.",
            checked = uiState.settings.appearance.confettiCelebration,
            onCheckedChange = { viewModel.onEvent(SettingsEvent.ToggleConfetti(it)) }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        // Working Reduce Animations Toggle
        SettingSwitchItem(
            title = "Reduce Animations",
            description = "Disables spring transitions & pulse effects for faster performance.",
            checked = uiState.settings.appearance.reduceMotion,
            onCheckedChange = { viewModel.onEvent(SettingsEvent.ToggleReduceMotion(it)) }
        )
    }
}

/* ========================================================================= */
/* 3. Tasks & Workflows Section                                              */
/* ========================================================================= */
@Composable
fun WorkflowSection(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    SettingSectionHeader(
        title = "Tasks & Workflows",
        description = "Configure default priority, auto-archive rules, and focus timer."
    )

    SettingCard {
        Text(
            "Default Startup View",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DefaultViewType.entries.forEach { view ->
                val isSelected = uiState.settings.workflow.defaultView == view
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable { viewModel.onEvent(SettingsEvent.UpdateDefaultView(view)) }
                ) {
                    Text(
                        text = "${view.icon} ${view.displayName}",
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        Text(
            "Default Task Priority",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PriorityLevel.entries.forEach { priority ->
                val isSelected = uiState.settings.workflow.defaultPriority == priority
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable { viewModel.onEvent(SettingsEvent.UpdateDefaultPriority(priority)) }
                ) {
                    Text(
                        text = priority.displayName,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        Text(
            "Auto-Archive Completed Tasks",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AutoArchiveDuration.entries.forEach { duration ->
                val isSelected = uiState.settings.workflow.autoArchiveDuration == duration
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable { viewModel.onEvent(SettingsEvent.UpdateAutoArchive(duration)) }
                ) {
                    Text(
                        text = duration.displayName,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }

    SettingCard {
        Text(
            "Productivity Automations",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )

        SettingSwitchItem(
            title = "Pomodoro Focus Timer",
            description = "Attach 25-minute focus intervals and countdowns to tasks.",
            checked = uiState.settings.workflow.pomodoro.enabled,
            onCheckedChange = { viewModel.onEvent(SettingsEvent.TogglePomodoro(it)) }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        SettingSwitchItem(
            title = "Auto-Complete Parent Task",
            description = "Mark parent task done when all subtasks finish.",
            checked = uiState.settings.workflow.autoCompleteParentTask,
            onCheckedChange = { viewModel.onEvent(SettingsEvent.TogglePomodoro(!it)) }
        )
    }

    SettingCard {
        Text(
            "Calendar Preferences",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )

        SettingSwitchItem(
            title = "Show Past Tasks in Calendar",
            description = "Display completed and previous date tasks on the calendar days.",
            checked = uiState.settings.workflow.showPastTasksInCalendar,
            onCheckedChange = { viewModel.onEvent(SettingsEvent.ToggleShowPastTasksInCalendar(it)) }
        )
    }
}

/* ========================================================================= */
/* 4. Notifications & Audio Section (With Instant Live Audio Preview)        */
/* ========================================================================= */
@Composable
fun NotificationSection(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val view = LocalView.current

    SettingSectionHeader(
        title = "Notifications & Audio",
        description = "Manage reminder alerts, daily digests, and audio feedback."
    )

    SettingCard {
        SettingSwitchItem(
            title = "Push Notifications",
            description = "Receive instant alerts when scheduled tasks are due.",
            checked = uiState.settings.notifications.pushNotificationsEnabled,
            onCheckedChange = { viewModel.onEvent(SettingsEvent.TogglePushNotification(it)) }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        SettingSwitchItem(
            title = "Daily Summary Email",
            description = "Receive scheduled morning briefing at 08:30 AM.",
            checked = uiState.settings.notifications.dailyDigestEmail,
            onCheckedChange = { viewModel.onEvent(SettingsEvent.TogglePushNotification(!it)) }
        )
    }

    SettingCard {
        Text(
            "Task Completion Audio Chime",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Text(
            "Tap any sound to test and choose your preferred audio feedback.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SoundEffectType.entries.forEach { sound ->
                val isSelected = uiState.settings.notifications.soundEffect == sound
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.onEvent(SettingsEvent.UpdateSoundEffect(sound))
                            SoundHelper.playTaskCompletionSound(context, view, sound)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = sound.displayName,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }

                        if (isSelected) {
                            Text(
                                text = "Active",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ========================================================================= */
/* 5. Projects & Tags (Taxonomy) Section (Active Tag Manager)                */
/* ========================================================================= */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaxonomySection(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    var newTagName by remember { mutableStateOf("") }
    var selectedTagColor by remember { mutableStateOf("#4D7FFF") }

    val colors = listOf("#4D7FFF", "#00BFA6", "#B8A7FF", "#FFD966", "#E57373", "#F06292")

    SettingSectionHeader(
        title = "Projects, Tags & Statuses",
        description = "Create and organize custom tags used when adding and filtering tasks."
    )

    SettingCard {
        Text(
            "Manage Tags & Categories",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newTagName,
                onValueChange = { newTagName = it },
                placeholder = { Text("New tag (e.g. Work, Study)...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = {
                    if (newTagName.isNotBlank()) {
                        viewModel.onEvent(SettingsEvent.AddTag(newTagName.trim(), selectedTagColor))
                        newTagName = ""
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("+ Add")
            }
        }

        MacColorPicker(
            selectedColorHex = selectedTagColor,
            title = "Tag Color",
            onColorSelected = { selectedTagColor = it }
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            uiState.settings.taxonomy.tags.forEach { tag ->
                TagChip(
                    name = tag.name,
                    colorHex = tag.colorHex,
                    onRemove = { viewModel.onEvent(SettingsEvent.RemoveTag(tag.id)) }
                )
            }
        }
    }
}

/* ========================================================================= */
/* 6. Integrations & API Section                                             */
/* ========================================================================= */
@Composable
fun IntegrationSection(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    SettingSectionHeader(
        title = "Integrations & API",
        description = "Connect external calendars, chat apps, and developer tools."
    )

    SettingCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Google Calendar 2-Way Sync",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    "Sync tasks with scheduled due dates directly to Google Calendar.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = uiState.settings.integrations.googleCalendarSync,
                onCheckedChange = { viewModel.onEvent(SettingsEvent.ToggleGoogleCalendar(it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedBorderColor = Color.Transparent,
                    uncheckedThumbColor = Color(0xFFF1F5F9),
                    uncheckedTrackColor = Color(0xFF334155).copy(alpha = 0.8f),
                    uncheckedBorderColor = Color(0xFF64748B)
                )
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Slack Workspace Bot",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    "Post daily task lists and reminders to your team channels.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            Button(
                onClick = { /* Connect slack */ },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Connect", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
            }
        }
    }
}

/* ========================================================================= */
/* 7. Data, Backup & Danger Zone Section                                     */
/* ========================================================================= */
@Composable
fun DataSection(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset All Settings?") },
            text = { Text("This will restore all preferences and tags to their initial defaults. Your created tasks will remain safe.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onEvent(SettingsEvent.ResetDefaults)
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset Defaults")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    SettingSectionHeader(
        title = "Data & Security Vault",
        description = "Hardware encryption, anti-spying privacy shield, and backups."
    )

    // Security & Cryptographic Vault Card
    SettingCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Hardware Encryption Vault",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    "All tasks and settings are encrypted at rest using AES-256-GCM via Android Keystore.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF10B981).copy(alpha = 0.15f)
            ) {
                Text(
                    text = "🔒 AES-256 ACTIVE",
                    color = Color(0xFF10B981),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        SettingSwitchItem(
            title = "🛡️ Screen Privacy & Anti-Spying",
            description = "Blocks external screenshot malware, screen recording, and blurs task details in the Recent Apps switcher.",
            checked = uiState.settings.security.enableScreenPrivacy,
            onCheckedChange = { viewModel.onEvent(SettingsEvent.ToggleScreenPrivacy(it)) }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        val isRooted = remember { SecurityCheckHelper.isDeviceRooted() }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Device Environment Integrity",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (!isRooted) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)
            ) {
                Text(
                    text = if (!isRooted) "✅ Secure & Verified" else "⚠️ Root Detected",
                    color = if (!isRooted) Color(0xFF10B981) else Color(0xFFEF4444),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }

    SettingCard {
        Text(
            "Backup & Export",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Text(
            "Download a full JSON snapshot of all your settings and configurations.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )

        Button(
            onClick = { viewModel.onEvent(SettingsEvent.ExportBackup) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Export Settings (JSON)")
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "⚠️ Danger Zone",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            )

            Text(
                text = "Resetting settings will discard any customized theme, preferences, and custom tags.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            OutlinedButton(
                onClick = { showResetDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset Settings to Defaults")
            }
        }
    }
}

/* ========================================================================= */
/* 8. Developer & Message the Developer Section                              */
/* ========================================================================= */
@Composable
fun DeveloperSection(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    val context = LocalContext.current
    var showMessageDialog by remember { mutableStateOf(false) }

    var feedbackCategory by remember { mutableStateOf("Feature Request") }
    var feedbackSubject by remember { mutableStateOf("") }
    var feedbackMessage by remember { mutableStateOf("") }

    if (showMessageDialog) {
        AlertDialog(
            onDismissRequest = { showMessageDialog = false },
            title = {
                Text("Message the Developer", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Send feedback, report an issue, or suggest a new feature directly:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Category Chips
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("💡 Feature Request", "🐞 Bug Report", "❤️ Praise / Note").forEach { cat ->
                            val isSelected = feedbackCategory == cat
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { feedbackCategory = cat }
                            ) {
                                Text(
                                    text = cat,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = feedbackSubject,
                        onValueChange = { feedbackSubject = it },
                        label = { Text("Subject") },
                        placeholder = { Text("Short summary...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = feedbackMessage,
                        onValueChange = { feedbackMessage = it },
                        label = { Text("Your Message") },
                        placeholder = { Text("Describe your idea or report here...") },
                        modifier = Modifier.fillMaxWidth().height(110.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (feedbackMessage.isNotBlank()) {
                            viewModel.onEvent(
                                SettingsEvent.SendDeveloperFeedback(
                                    category = feedbackCategory,
                                    subject = feedbackSubject,
                                    message = feedbackMessage
                                )
                            )

                            // Also launch real email intent if available
                            try {
                                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:dhananjayydv@proton.me")
                                    putExtra(Intent.EXTRA_SUBJECT, "[$feedbackCategory] $feedbackSubject")
                                    putExtra(Intent.EXTRA_TEXT, feedbackMessage)
                                }
                                context.startActivity(Intent.createChooser(emailIntent, "Send Email"))
                            } catch (_: Exception) {}

                            showMessageDialog = false
                            feedbackSubject = ""
                            feedbackMessage = ""
                        }
                    },
                    enabled = feedbackMessage.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Send Message")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMessageDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    SettingSectionHeader(
        title = "Developer & About",
        description = "App creator info, tech stack, and direct developer communication."
    )

    // Developer Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Crafted by Yadav",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Planno v${uiState.settings.appVersion} (Build ${uiState.settings.buildNumber})",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )

            Text(
                text = "dhananjayydv@proton.me",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Text(
                    text = "Kotlin • Jetpack Compose • Material 3 • Coroutines",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = { showMessageDialog = true },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Mail,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "✉️ Message the Developer",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
