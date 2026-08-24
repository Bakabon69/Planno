package com.example.taskmanager

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.example.taskmanager.data.Task
import com.example.taskmanager.data.TaskViewModel
import com.example.taskmanager.screens.AddTaskScreen
import com.example.taskmanager.screens.OnboardingScreen
import com.example.taskmanager.screens.TaskHistoryScreen
import com.example.taskmanager.settings.model.SoundEffectType
import com.example.taskmanager.settings.ui.SettingsScreen
import com.example.taskmanager.settings.viewmodel.SettingsEvent
import com.example.taskmanager.settings.viewmodel.SettingsTab
import com.example.taskmanager.settings.viewmodel.SettingsViewModel as AppSettingsViewModel
import com.example.taskmanager.ui.components.CalendarView
import com.example.taskmanager.ui.theme.TaskManagerTheme
import com.example.taskmanager.ui.utils.ReminderHelper
import com.example.taskmanager.ui.utils.SoundHelper
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsViewModel: AppSettingsViewModel = viewModel()
            val settingsState by settingsViewModel.uiState.collectAsState()

            TaskManagerTheme(
                themeMode = settingsState.settings.appearance.theme,
                accentColor = settingsState.settings.appearance.accentColor
            ) {
                TaskManagerApp(settingsViewModel = settingsViewModel)
            }
        }
    }
}

enum class TaskFilter(val title: String) {
    ALL("All"),
    PENDING("Pending"),
    COMPLETED("Completed"),
    TODAY("Today")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskManagerApp(
    viewModel: TaskViewModel = viewModel(),
    settingsViewModel: AppSettingsViewModel = viewModel()
) {
    val settingsState by settingsViewModel.uiState.collectAsState()
    val userProfile = settingsState.settings.profile
    val soundSetting = settingsState.settings.notifications.soundEffect
    val reduceMotion = settingsState.settings.appearance.reduceMotion
    val currentContext = LocalContext.current
    val currentView = LocalView.current

    // Enterprise Screen Privacy Protection (Anti-Screen Recording & Recent Apps Blurring)
    val activity = currentContext as? ComponentActivity
    val isScreenPrivacyActive = settingsState.settings.security.enableScreenPrivacy
    LaunchedEffect(isScreenPrivacyActive) {
        activity?.let {
            if (isScreenPrivacyActive) {
                it.window.setFlags(
                    android.view.WindowManager.LayoutParams.FLAG_SECURE,
                    android.view.WindowManager.LayoutParams.FLAG_SECURE
                )
            } else {
                it.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    // 🌟 Full First-Time User Onboarding Screen
    if (!userProfile.hasCompletedOnboarding) {
        OnboardingScreen(
            onCompleteOnboarding = { profile, selectedTags ->
                settingsViewModel.onEvent(SettingsEvent.CompleteOnboarding(profile, selectedTags))
            }
        )
        return
    }

    // Startup Tab is index 0 (Tasks Section), not Calendar!
    var selectedTab by remember { mutableIntStateOf(0) }
    var previousTab by remember { mutableIntStateOf(0) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    
    var selectedTaskIds by remember { mutableStateOf(setOf<Int>()) }
    val isSelectionMode by remember { derivedStateOf { selectedTaskIds.isNotEmpty() } }

    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var initialTaskTitlePrefill by remember { mutableStateOf<String?>(null) }

    // Pomodoro Timer State
    var activePomodoroTask by remember { mutableStateOf<Task?>(null) }

    // Sidebar Drawer State
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var drawerFilter by remember { mutableStateOf("All") } // "All", "Today", or a tag name

    // Exit selection mode on back press
    BackHandler(enabled = isSelectionMode) {
        selectedTaskIds = emptySet()
    }

    BackHandler(enabled = selectedTab == 4 && !isSelectionMode) {
        selectedTab = previousTab
        taskToEdit = null
        initialTaskTitlePrefill = null
    }

    // Close drawer on back press when open
    BackHandler(enabled = drawerState.isOpen && !isSelectionMode) {
        scope.launch { drawerState.close() }
    }

    val tasks = viewModel.tasks
    val userTags = settingsState.settings.taxonomy.tags

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = selectedTab == 0,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(0.82f)
            ) {
                // Sidebar Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 16.dp)
                ) {
                    // Profile Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!userProfile.avatarUrl.isNullOrBlank() && !userProfile.avatarUrl.startsWith("http")) {
                                Text(text = userProfile.avatarUrl, fontSize = 20.sp)
                            } else {
                                Text(
                                    text = if (userProfile.fullName.isNotBlank()) userProfile.fullName.take(2).uppercase() else "ME",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (userProfile.fullName.isNotBlank()) userProfile.fullName else "Planno User",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (userProfile.email.isNotBlank()) {
                                Text(
                                    text = userProfile.email,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    // Today
                    val todayCount = tasks.count { it.date == LocalDate.now() }
                    DrawerMenuItem(
                        icon = "📅",
                        label = "Today",
                        count = todayCount,
                        isSelected = selectedTab == 0 && drawerFilter == "Today",
                        onClick = {
                            drawerFilter = "Today"
                            selectedTab = 0
                            scope.launch { drawerState.close() }
                        }
                    )

                    // Calendar
                    DrawerMenuItem(
                        icon = "🗓️",
                        label = "Calendar",
                        count = tasks.size,
                        isSelected = selectedTab == 1,
                        onClick = {
                            previousTab = selectedTab
                            selectedTab = 1
                            scope.launch { drawerState.close() }
                        }
                    )

                    // Task History & Archive
                    val historyCount = tasks.count { it.completed || it.date.isBefore(LocalDate.now()) }
                    DrawerMenuItem(
                        icon = "📜",
                        label = "History & Archive",
                        count = historyCount,
                        isSelected = selectedTab == 3,
                        onClick = {
                            previousTab = selectedTab
                            selectedTab = 3
                            scope.launch { drawerState.close() }
                        }
                    )

                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    // Projects / Tags Header
                    Text(
                        text = "PROJECTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )

                    // User Tags / Projects
                    userTags.forEach { tag ->
                        val tagCount = tasks.count { it.tag == tag.name }
                        val tagEmoji = when (tag.name.lowercase()) {
                            "work" -> "💼"
                            "personal" -> "🏠"
                            "fitness" -> "💪"
                            "design" -> "🎨"
                            "urgent" -> "🔥"
                            "study" -> "📚"
                            else -> "📌"
                        }
                        DrawerMenuItem(
                            icon = tagEmoji,
                            label = tag.name,
                            count = tagCount,
                            isSelected = drawerFilter == tag.name,
                            onClick = {
                                drawerFilter = tag.name
                                scope.launch { drawerState.close() }
                            }
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    // Add Project Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { drawerState.close() }
                                settingsViewModel.onEvent(SettingsEvent.SelectTab(SettingsTab.TAXONOMY))
                                previousTab = selectedTab
                                selectedTab = 2 // Go to Settings → Tags
                            }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Project",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Add Project",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                if (isSelectionMode) {
                    TopAppBar(
                        title = { Text("${selectedTaskIds.size} selected", color = MaterialTheme.colorScheme.onSurface) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                    navigationIcon = {
                        IconButton(onClick = { selectedTaskIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            selectedTaskIds.forEach { id ->
                                ReminderHelper.cancelTaskReminder(currentContext, id)
                            }
                            viewModel.removeTasks(selectedTaskIds)
                            selectedTaskIds = emptySet()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444))
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (!isSelectionMode && selectedTab != 4) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { 
                            previousTab = selectedTab
                            selectedTab = 0 
                        },
                        icon = { Icon(Icons.Default.CheckCircle, contentDescription = "Tasks") },
                        label = { Text("Tasks", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { 
                            previousTab = selectedTab
                            selectedTab = 1 
                        },
                        icon = { Icon(Icons.Default.CalendarToday, contentDescription = "Calendar") },
                        label = { Text("Calendar", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { 
                            previousTab = selectedTab
                            selectedTab = 3 
                        },
                        icon = { Icon(Icons.Default.History, contentDescription = "History") },
                        label = { Text("History", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { 
                            previousTab = selectedTab
                            selectedTab = 2 
                        },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (!isSelectionMode && (selectedTab == 0 || selectedTab == 1)) {
                FloatingActionButton(
                    onClick = { 
                        taskToEdit = null
                        initialTaskTitlePrefill = null
                        previousTab = selectedTab
                        selectedTab = 4 
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 8.dp, end = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add task",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> {
                    if (tasks.isEmpty()) {
                        StartupEmptyStateScreen(
                            userName = userProfile.fullName,
                            reduceMotion = reduceMotion,
                            onAddNewTask = {
                                taskToEdit = null
                                initialTaskTitlePrefill = null
                                previousTab = selectedTab
                                selectedTab = 4
                            },
                            onSelectTemplate = { templateTitle, _ ->
                                taskToEdit = null
                                initialTaskTitlePrefill = templateTitle
                                previousTab = selectedTab
                                selectedTab = 4
                            }
                        )
                    } else {
                        MainTasksScreen(
                            userName = userProfile.fullName,
                            avatarUrl = userProfile.avatarUrl,
                            tasks = tasks,
                            selectedTaskIds = selectedTaskIds,
                            availableTags = settingsState.settings.taxonomy.tags.map { it.name },
                            reduceMotion = reduceMotion,
                            drawerFilter = drawerFilter,
                            onOpenDrawer = {
                                scope.launch { drawerState.open() }
                            },
                            onTaskClick = { id ->
                                if (isSelectionMode) {
                                    selectedTaskIds = if (id in selectedTaskIds) selectedTaskIds - id else selectedTaskIds + id
                                }
                            },
                            onTaskLongClick = { id ->
                                selectedTaskIds = if (id in selectedTaskIds) selectedTaskIds - id else selectedTaskIds + id
                            },
                            onEditClick = { task ->
                                taskToEdit = task
                                initialTaskTitlePrefill = null
                                previousTab = selectedTab
                                selectedTab = 4
                            },
                            onCompletedChange = { id, completed ->
                                viewModel.toggleTaskCompletion(id, completed)
                                if (completed) {
                                    ReminderHelper.cancelTaskReminder(currentContext, id)
                                    SoundHelper.playTaskCompletionSound(currentContext, currentView, soundSetting)
                                }
                            },
                            onStartPomodoro = { task ->
                                activePomodoroTask = task
                            },
                            onSwipeDeleteTask = { id ->
                                val toDelete = if (isSelectionMode && id in selectedTaskIds) selectedTaskIds else setOf(id)
                                viewModel.removeTasks(toDelete)
                                selectedTaskIds = emptySet()
                            }
                        )
                    }
                }
                1 -> CalendarScreen(
                    selectedDate = selectedDate,
                    currentMonth = currentMonth,
                    onDateSelected = { selectedDate = it },
                    onMonthChange = { currentMonth = it },
                    tasks = tasks,
                    selectedTaskIds = selectedTaskIds,
                    showPastTasks = settingsState.settings.workflow.showPastTasksInCalendar,
                    reduceMotion = reduceMotion,
                    onTaskClick = { id ->
                        if (isSelectionMode) {
                            selectedTaskIds = if (id in selectedTaskIds) selectedTaskIds - id else selectedTaskIds + id
                        }
                    },
                    onTaskLongClick = { id ->
                        selectedTaskIds = if (id in selectedTaskIds) selectedTaskIds - id else selectedTaskIds + id
                    },
                    onEditClick = { task ->
                        taskToEdit = task
                        initialTaskTitlePrefill = null
                        previousTab = selectedTab
                        selectedTab = 4
                    },
                    onCompletedChange = { id, completed ->
                        viewModel.toggleTaskCompletion(id, completed)
                        if (completed) {
                            ReminderHelper.cancelTaskReminder(currentContext, id)
                            SoundHelper.playTaskCompletionSound(currentContext, currentView, soundSetting)
                        }
                    },
                    onStartPomodoro = { task ->
                        activePomodoroTask = task
                    },
                    onAddNewTaskForDate = { date ->
                        selectedDate = date
                        taskToEdit = null
                        initialTaskTitlePrefill = null
                        previousTab = 1
                        selectedTab = 4
                    },
                    onSwipeDeleteTask = { id ->
                        val toDelete = if (isSelectionMode && id in selectedTaskIds) selectedTaskIds else setOf(id)
                        viewModel.removeTasks(toDelete)
                        selectedTaskIds = emptySet()
                    }
                )
                2 -> SettingsScreen(viewModel = settingsViewModel)
                3 -> TaskHistoryScreen(
                    tasks = tasks,
                    reduceMotion = reduceMotion,
                    onTaskClick = { task ->
                        taskToEdit = task
                        initialTaskTitlePrefill = null
                        previousTab = 3
                        selectedTab = 4
                    },
                    onDeleteTask = { taskId ->
                        viewModel.removeTasks(setOf(taskId))
                    },
                    onRestoreTask = { taskId ->
                        viewModel.toggleTaskCompletion(taskId, false)
                    }
                )
                4 -> AddTaskScreen(
                    initialTask = taskToEdit,
                    initialTitle = initialTaskTitlePrefill,
                    userName = userProfile.fullName,
                    availableTags = settingsState.settings.taxonomy.tags.map { it.name },
                    onAddTask = { title, description, time, location, color, tag, priority, hasReminder, reminderMinutesBefore ->
                        val task = if (taskToEdit != null) {
                            val updated = taskToEdit!!.copy(
                                title = title,
                                description = description,
                                time = time,
                                location = location,
                                color = color,
                                tag = tag,
                                priority = priority,
                                hasReminder = hasReminder,
                                reminderMinutesBefore = reminderMinutesBefore
                            )
                            viewModel.updateTask(updated)
                            updated
                        } else {
                            val newTask = Task(
                                id = (tasks.maxOfOrNull { it.id } ?: 0) + 1,
                                title = title,
                                description = description,
                                time = time,
                                date = selectedDate,
                                location = location,
                                color = color,
                                tag = tag,
                                priority = priority,
                                hasReminder = hasReminder,
                                reminderMinutesBefore = reminderMinutesBefore
                            )
                            viewModel.addTask(newTask)
                            newTask
                        }

                        ReminderHelper.scheduleTaskReminder(currentContext, task)

                        taskToEdit = null
                        initialTaskTitlePrefill = null
                        selectedTab = previousTab
                    }
                )
            }
        }
    }

    // Interactive Pomodoro Focus Dialog
    if (activePomodoroTask != null) {
        PomodoroTimerDialog(
            task = activePomodoroTask!!,
            onDismiss = { activePomodoroTask = null }
        )
    }
} }

/* ========================================================================= */
/* 📂 Sidebar Drawer Menu Item                                               */
/* ========================================================================= */
@Composable
fun DrawerMenuItem(
    icon: String,
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 18.sp)
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (count > 0) {
            Text(
                text = "$count",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/* ========================================================================= */
/* 🌟 Startup & Empty State Screen                                           */
/* ========================================================================= */
@Composable
fun StartupEmptyStateScreen(
    userName: String = "",
    reduceMotion: Boolean = false,
    onAddNewTask: () -> Unit,
    onSelectTemplate: (String, Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val firstName = if (userName.isNotBlank()) userName.trim().split(" ").firstOrNull() ?: userName else ""

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_animation")
    val animatedPulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val pulseScale = if (reduceMotion) 1f else animatedPulseScale

    val templates = listOf(
        "🚀 Launch a Project" to MaterialTheme.colorScheme.primary,
        "🎯 Daily Fitness Goal" to Color(0xFF00BFA6),
        "📚 Read 20 Pages" to Color(0xFFB8A7FF),
        "💼 Team Standup" to Color(0xFFFFD966),
        "🛒 Grocery Run" to Color(0xFFE57373)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(0.5f))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(150.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                        )
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                    .shadow(elevation = 14.dp, shape = CircleShape, spotColor = MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Checklist,
                    contentDescription = "Planno Logo",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (firstName.isNotBlank()) "Hey $firstName, ready to plan?" else "Welcome to Planno",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your workspace is clear and ready. Tap below to add your first goal for today!",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp),
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onAddNewTask,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(20.dp), spotColor = MaterialTheme.colorScheme.primary),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (firstName.isNotBlank()) "Create $firstName's First Task" else "Create Your First Task",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Or pick a quick template:",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            templates.forEach { (title, color) ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
                    modifier = Modifier.clickable { onSelectTemplate(title, color) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

/* ========================================================================= */
/* 📋 Main Task Screen (With Personalized Header, Progress & Tags)          */
/* ========================================================================= */
@Composable
fun MainTasksScreen(
    userName: String = "",
    avatarUrl: String? = null,
    tasks: List<Task>,
    selectedTaskIds: Set<Int>,
    availableTags: List<String>,
    reduceMotion: Boolean = false,
    drawerFilter: String = "All",
    onOpenDrawer: () -> Unit = {},
    onTaskClick: (Int) -> Unit,
    onTaskLongClick: (Int) -> Unit,
    onEditClick: (Task) -> Unit,
    onCompletedChange: (Int, Boolean) -> Unit,
    onStartPomodoro: (Task) -> Unit,
    onSwipeDeleteTask: (Int) -> Unit = {}
) {
    val todayDate = LocalDate.now()
    val completedCount = tasks.count { it.completed }
    val totalCount = tasks.size

    val firstName = if (userName.isNotBlank()) userName.trim().split(" ").firstOrNull() ?: userName else "Friend"

    // Time-based dynamic personalized greeting
    val currentHour = remember { LocalTime.now().hour }
    val greetingText = when (currentHour) {
        in 5..11 -> "☀️ Good morning, $firstName!"
        in 12..16 -> "🌤️ Good afternoon, $firstName!"
        in 17..21 -> "🌆 Good evening, $firstName!"
        else -> "🌙 Night check-in, $firstName!"
    }

    val motivationalNote = when {
        totalCount == 0 -> "Your workspace is ready. Let's plan your goals!"
        completedCount == totalCount -> "🎉 All done for today, $firstName! Great job."
        completedCount == 0 -> "You have $totalCount ${if (totalCount == 1) "task" else "tasks"} scheduled for today."
        else -> "${totalCount - completedCount} tasks left. Keep up the momentum!"
    }

    // Apply drawer-based sidebar filter
    val filteredTasks = remember(tasks, drawerFilter) {
        val baseFiltered = when (drawerFilter) {
            "Today" -> tasks.filter { it.date == todayDate }
            "All" -> tasks
            else -> tasks.filter { it.tag == drawerFilter }
        }
        baseFiltered.sortedWith(compareBy<Task> { it.completed }.thenBy { it.date })
    }

    // Subtitle showing current view
    val drawerSubtitle = when (drawerFilter) {
        "Today" -> "Today's Tasks"
        "All" -> "All Tasks"
        else -> "$drawerFilter Tasks"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        // 🌟 Personalized Dynamic Header Note with Sidebar Navigation & Filter indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open Sidebar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column {
                    Text(
                        text = greetingText,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$drawerSubtitle • ${todayDate.format(DateTimeFormatter.ofPattern("EEE, MMM d"))}",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = motivationalNote,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            val context = LocalContext.current
            val customAvatarBitmap = remember(avatarUrl) {
                avatarUrl?.takeIf { it.startsWith("content://") || it.startsWith("file://") }?.let { uriStr ->
                    runCatching {
                        context.contentResolver.openInputStream(Uri.parse(uriStr))?.use { stream ->
                            BitmapFactory.decodeStream(stream)?.asImageBitmap()
                        }
                    }.getOrNull()
                }
            }

            // User Profile Initials or Avatar Pill
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (customAvatarBitmap != null) {
                    Image(
                        bitmap = customAvatarBitmap,
                        contentDescription = "Profile Picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else if (!avatarUrl.isNullOrBlank() && !avatarUrl.startsWith("http")) {
                    Text(text = avatarUrl, fontSize = 22.sp)
                } else {
                    Text(
                        text = if (userName.isNotBlank()) userName.take(2).uppercase() else "ME",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tasks List
        if (filteredTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 40.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = when (drawerFilter) {
                            "Today" -> "No tasks scheduled for today"
                            "All" -> "No tasks in your inbox"
                            else -> "No tasks under #$drawerFilter"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            val isSelectionMode = selectedTaskIds.isNotEmpty()
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredTasks, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        isSelected = task.id in selectedTaskIds,
                        isSelectionMode = isSelectionMode,
                        reduceMotion = reduceMotion,
                        onTaskClick = onTaskClick,
                        onTaskLongClick = { onTaskLongClick(task.id) },
                        onEditClick = { onEditClick(task) },
                        onCompletedChange = { onCompletedChange(task.id, it) },
                        onStartPomodoro = { onStartPomodoro(task) },
                        onSwipeDelete = { onSwipeDeleteTask(task.id) }
                    )
                }
            }
        }
    }
}

/* ========================================================================= */
/* 📅 Calendar Screen with Selected Day's Tasks Below Grid                   */
/* ========================================================================= */
@Composable
fun CalendarScreen(
    selectedDate: LocalDate,
    currentMonth: YearMonth,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChange: (YearMonth) -> Unit,
    tasks: List<Task>,
    selectedTaskIds: Set<Int> = emptySet(),
    showPastTasks: Boolean = true,
    reduceMotion: Boolean = false,
    onTaskClick: (Int) -> Unit = {},
    onTaskLongClick: (Int) -> Unit = {},
    onEditClick: (Task) -> Unit = {},
    onCompletedChange: (Int, Boolean) -> Unit = { _, _ -> },
    onStartPomodoro: (Task) -> Unit = {},
    onAddNewTaskForDate: (LocalDate) -> Unit = {},
    onSwipeDeleteTask: (Int) -> Unit = {}
) {
    val today = remember { LocalDate.now() }
    val isPastDate = selectedDate.isBefore(today)
    val shouldHideDueToSetting = isPastDate && !showPastTasks

    val dayTasks = remember(tasks, selectedDate, showPastTasks) {
        if (shouldHideDueToSetting) {
            emptyList()
        } else {
            tasks.filter { it.date == selectedDate }
                .sortedWith(compareBy<Task> { it.completed }.thenBy { it.time })
        }
    }

    val isSelectionMode = selectedTaskIds.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Month Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = currentMonth.year.toString(),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onMonthChange(currentMonth.minusMonths(1)) }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { 
                    onMonthChange(YearMonth.now())
                    onDateSelected(LocalDate.now())
                }) {
                    Text("Today", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
                IconButton(onClick = { onMonthChange(currentMonth.plusMonths(1)) }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Calendar Grid View (with swipe gestures & bubble task indicators)
        CalendarView(
            selectedDate = selectedDate,
            currentMonth = currentMonth,
            onDateSelected = onDateSelected,
            onMonthChange = onMonthChange,
            tasks = tasks,
            showPastTasks = showPastTasks
        )

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 1.dp)

        Spacer(modifier = Modifier.height(12.dp))

        // Tasks for the Selected Bubble Day
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM dd")),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${dayTasks.size} ${if (dayTasks.size == 1) "task" else "tasks"}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            TextButton(
                onClick = { onAddNewTaskForDate(selectedDate) }
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Task", fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (dayTasks.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Outlined.EventNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "No tasks scheduled for ${selectedDate.format(DateTimeFormatter.ofPattern("MMM dd"))}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { onAddNewTaskForDate(selectedDate) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text("+ Plan a Task", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(dayTasks, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        isSelected = task.id in selectedTaskIds,
                        isSelectionMode = isSelectionMode,
                        reduceMotion = reduceMotion,
                        onTaskClick = onTaskClick,
                        onTaskLongClick = { onTaskLongClick(task.id) },
                        onEditClick = { onEditClick(task) },
                        onCompletedChange = { onCompletedChange(task.id, it) },
                        onStartPomodoro = { onStartPomodoro(task) },
                        onSwipeDelete = { onSwipeDeleteTask(task.id) }
                    )
                }
            }
        }
    }
}

/* ========================================================================= */
/* 🗂️ Task Row with Tags, Priority & Reminder Indicator                     */
/* ========================================================================= */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskRow(
    task: Task,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    reduceMotion: Boolean = false,
    onTaskClick: (Int) -> Unit = {},
    onTaskLongClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onCompletedChange: ((Boolean) -> Unit)? = null,
    onStartPomodoro: () -> Unit = {},
    onSwipeDelete: (() -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(false) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val brush = Brush.linearGradient(
        colors = listOf(task.color, task.color.copy(alpha = 0.6f))
    )
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy") }

    // Smooth spring animation when selected
    val cardScale by animateFloatAsState(
        targetValue = if (isSelected) 0.97f else 1f,
        animationSpec = if (reduceMotion) snap() else spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "selection_scale"
    )

    val elevation by animateDpAsState(
        targetValue = if (isSelected) 8.dp else 2.dp,
        animationSpec = if (reduceMotion) snap() else tween(200),
        label = "card_elevation"
    )

    val animatedOffsetY by animateFloatAsState(
        targetValue = offsetY,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
        label = "task_swipe_offset"
    )

    val isDeletingDown = offsetY > 80f
    val isDeletingUp = offsetY < -80f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        // Background Delete Indicator shown when swiped downwards or upwards
        if (offsetY != 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (isDeletingDown || isDeletingUp) Color(0xFFEF4444)
                        else Color(0xFFEF4444).copy(alpha = 0.4f)
                    )
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = if (offsetY > 0) Alignment.TopCenter else Alignment.BottomCenter
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Task",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isSelectionMode) "Release to Delete Selected Tasks" else "Release to Delete Task",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = animatedOffsetY.dp)
                .scale(cardScale)
                .pointerInput(task.id, isSelectionMode) {
                    detectVerticalDragGestures(
                        onDragStart = { offsetY = 0f },
                        onDragEnd = {
                            if (offsetY > 100f || offsetY < -100f) {
                                onSwipeDelete?.invoke()
                            }
                            offsetY = 0f
                        },
                        onDragCancel = { offsetY = 0f },
                        onVerticalDrag = { change: PointerInputChange, dragAmount: Float ->
                            change.consume()
                            // Allows smooth vertical dragging
                            offsetY = (offsetY + dragAmount * 0.6f).coerceIn(-140f, 140f)
                        }
                    )
                }
                .animateContentSize(
                    animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
                )
                .clip(RoundedCornerShape(18.dp))
                .combinedClickable(
                    onClick = {
                        if (isSelectionMode) {
                            onTaskClick(task.id)
                        } else {
                            isExpanded = !isExpanded
                        }
                    },
                    onLongClick = {
                        onTaskLongClick()
                    }
                ),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
            border = if (isSelected) {
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            } else {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            },
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
        ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Selection Indicator or Accent Bar
                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                2.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                } else {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(28.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(brush)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        color = if (task.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                        textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = if (isExpanded) 3 else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!isSelectionMode) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!isExpanded && !task.completed) {
                            IconButton(onClick = onStartPomodoro, modifier = Modifier.size(30.dp)) {
                                Icon(
                                    Icons.Outlined.Timer,
                                    contentDescription = "Focus Timer",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (!isExpanded) {
                            IconButton(onClick = onEditClick, modifier = Modifier.size(30.dp)) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        
                        IconButton(
                            onClick = { onCompletedChange?.invoke(!task.completed) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (task.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (task.completed) Color(0xFF00BFA6) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Task Meta Details (Time + Alarm + Tag + Priority)
            Row(
                modifier = Modifier.padding(start = if (isSelectionMode) 34.dp else 16.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${task.time} • ${task.date.format(DateTimeFormatter.ofPattern("MMM dd"))}",
                    color = task.color,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                if (task.hasReminder) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = "Alarm Set",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                }

                if (!task.tag.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "#${task.tag}",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (task.priority.isNotBlank() && task.priority != "None") {
                    val prioColor = when (task.priority) {
                        "Urgent" -> Color(0xFFEF4444)
                        "High" -> Color(0xFFF97316)
                        "Low" -> Color(0xFF10B981)
                        else -> Color(0xFFF59E0B)
                    }
                    Text(
                        text = "• ${task.priority}",
                        color = prioColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded && !isSelectionMode) {
                Column(modifier = Modifier.padding(start = 16.dp, top = 10.dp)) {
                    if (task.description.isNotBlank()) {
                        Text(
                            text = "Notes & Details",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = task.description,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 0.5.dp)
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Scheduled For", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            Text(task.date.format(dateFormatter), color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                        }

                        if (task.hasReminder) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Alarm Reminder", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    if (task.reminderMinutesBefore == 0) "At task time" else "${task.reminderMinutesBefore}m before",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onStartPomodoro,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Outlined.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Focus (25m)", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                        }

                        Button(
                            onClick = onEditClick,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Edit Details", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
} }

/* ========================================================================= */
/* 🍅 Interactive Pomodoro Focus Dialog                                      */
/* ========================================================================= */
@Composable
fun PomodoroTimerDialog(
    task: Task,
    onDismiss: () -> Unit
) {
    var totalSeconds by remember { mutableIntStateOf(25 * 60) }
    var isRunning by remember { mutableStateOf(false) }

    DisposableEffect(isRunning) {
        var timer: CountDownTimer? = null
        if (isRunning) {
            timer = object : CountDownTimer((totalSeconds * 1000).toLong(), 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    totalSeconds = (millisUntilFinished / 1000).toInt()
                }
                override fun onFinish() {
                    isRunning = false
                    totalSeconds = 0
                }
            }.start()
        }
        onDispose {
            timer?.cancel()
        }
    }

    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Focus Session", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            ) {
                Text(
                    text = task.title,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = timeFormatted,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { isRunning = !isRunning },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isRunning) "Pause" else "Start Focus")
                    }

                    OutlinedButton(
                        onClick = {
                            isRunning = false
                            totalSeconds = 25 * 60
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Reset")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
}

