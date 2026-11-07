package com.wishat12.app

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wishat12.app.data.entity.MessageTemplateEntity
import com.wishat12.app.data.entity.ReminderEntity
import com.wishat12.app.notifications.NotificationHelper
import com.wishat12.app.scheduling.ReminderProcessor
import com.wishat12.app.ui.screens.*
import com.wishat12.app.ui.theme.WishAt12Theme
import com.wishat12.app.util.BackupManager
import com.wishat12.app.viewmodel.WishAt12ViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createChannel(this)
        setContent {
            var darkTheme by remember { mutableStateOf(getSharedPreferences("settings", MODE_PRIVATE).getBoolean("dark_theme", true)) }
            WishAt12Theme(darkTheme = darkTheme) {
                WishAt12App(isDarkTheme = darkTheme, onThemeChange = { value ->
                    darkTheme = value
                    getSharedPreferences("settings", MODE_PRIVATE).edit().putBoolean("dark_theme", value).apply()
                })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { ReminderProcessor.recover(this@MainActivity) }
    }
}

private enum class AppTab(val label: String) {
    HOME("Home"), REMINDERS("Reminders"), CALENDAR("Calendar"), TEMPLATES("Templates"), SETTINGS("Settings")
}

@Composable
private fun WishAt12App(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    vm: WishAt12ViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val reminders by vm.reminders.collectAsState()
    val templates by vm.templates.collectAsState()
    val history by vm.history.collectAsState()
    val contacts by vm.contacts.collectAsState()
    var tab by remember { mutableStateOf(AppTab.HOME) }
    var editing by remember { mutableStateOf<ReminderEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var exactAllowed by remember { mutableStateOf(canScheduleExact(context)) }
    var notificationsAllowed by remember { mutableStateOf(hasNotificationPermission(context)) }
    var snackbar by remember { mutableStateOf<String?>(null) }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsAllowed = granted
        snackbar = if (granted) "Notifications enabled." else "Notifications are off. You can still view reminders in the app."
    }
    val exportBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) activity?.lifecycleScope?.launch {
            runCatching { BackupManager.export(context, uri) }
                .onSuccess { snackbar = "Backup exported." }
                .onFailure { snackbar = "Backup failed: ${it.message}" }
        }
    }
    val importBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) activity?.lifecycleScope?.launch {
            runCatching { BackupManager.import(context, uri) }
                .onSuccess { vm.rescheduleAll(); snackbar = "Backup restored." }
                .onFailure { snackbar = "Restore failed: ${it.message}" }
        }
    }

    LaunchedEffect(Unit) {
        vm.rescheduleAll()
        exactAllowed = canScheduleExact(context)
        notificationsAllowed = hasNotificationPermission(context)
    }

    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                exactAllowed = canScheduleExact(context)
                notificationsAllowed = hasNotificationPermission(context)
            }
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose { activity?.lifecycle?.removeObserver(observer) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = remember { SnackbarHostState() }) },
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { item ->
                    val icon = when (item) {
                        AppTab.HOME -> Icons.Default.Home
                        AppTab.REMINDERS -> Icons.Default.Cake
                        AppTab.CALENDAR -> Icons.Default.CalendarMonth
                        AppTab.TEMPLATES -> Icons.Default.Message
                        AppTab.SETTINGS -> Icons.Default.Settings
                    }
                    NavigationBarItem(selected = tab == item, onClick = { tab = item }, icon = { Icon(icon, null) }, label = { Text(item.label) })
                }
            }
        },
        floatingActionButton = {
            if (tab == AppTab.HOME || tab == AppTab.REMINDERS) FloatingActionButton(onClick = { editing = null; showEditor = true }) { Text("＋", style = MaterialTheme.typography.titleLarge) }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                AppTab.HOME -> HomeScreen(reminders, onAdd = { editing = null; showEditor = true }, onOpen = { editing = it; showEditor = true })
                AppTab.REMINDERS -> RemindersScreen(
                    reminders,
                    onEdit = { editing = it; showEditor = true },
                    onDelete = { vm.deleteReminder(it); snackbar = "Reminder deleted." },
                    onComplete = { vm.completeReminder(it); snackbar = "Marked as completed." },
                    onToggle = { item, active -> vm.toggleReminder(item, active); snackbar = if (active) "Reminder resumed." else "Reminder paused." },
                    onDuplicate = { vm.duplicateReminder(it); snackbar = "Reminder duplicated." }
                )
                AppTab.CALENDAR -> CalendarScreen(reminders) { editing = it; showEditor = true }
                AppTab.TEMPLATES -> TemplatesScreen(templates, onSave = { vm.saveTemplate(it); snackbar = "Template saved." }, onDelete = { vm.deleteTemplate(it); snackbar = "Template deleted." })
                AppTab.SETTINGS -> SettingsScreen(
                    exactAllowed = exactAllowed,
                    notificationsAllowed = notificationsAllowed,
                    onExactSettings = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            runCatching { context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))) }
                                .onFailure { snackbar = "Open Android Settings → Apps → Special app access → Alarms & reminders." }
                        } else exactAllowed = true
                    },
                    onNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    onNotificationSettings = {
                        val intent = if (Build.VERSION.SDK_INT >= 26) Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                        context.startActivity(intent)
                    },
                    onExport = { exportBackup.launch("wishat12-backup.json") },
                    onImport = { importBackup.launch(arrayOf("application/json", "text/json", "*/*")) },
                    onTestNotification = {
                        val demo = ReminderEntity(
                            id = -12L, recipientName = "WishAt12", occasionType = "Test notification", message = "Your notifications are working. You are all set! ✨",
                            scheduledDate = java.time.LocalDate.now().toString(), scheduledTime = "00:00", timezone = java.time.ZoneId.systemDefault().id,
                            scheduledAtMillis = System.currentTimeMillis()
                        )
                        NotificationHelper.showReminder(context, demo)
                        snackbar = "Test notification requested."
                    },
                    onTestAlarm = {
                        val due = System.currentTimeMillis() + 60_000L
                        val zone = java.time.ZoneId.systemDefault()
                        val local = java.time.Instant.ofEpochMilli(due).atZone(zone)
                        vm.saveReminder(ReminderEntity(
                            id = due, recipientName = "WishAt12 test", occasionType = "Test alarm",
                            message = "Your WishAt12 alarm is working. No WhatsApp message was sent.",
                            scheduledDate = local.toLocalDate().toString(), scheduledTime = local.toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                            timezone = zone.id, scheduledAtMillis = due, recurrenceAnchorMillis = due,
                            recurrenceType = "NEVER", notes = "Temporary one-minute alarm test"
                        ))
                        snackbar = "Test alarm scheduled for one minute from now."
                    },
                    isDarkTheme = isDarkTheme,
                    onThemeChange = onThemeChange,
                    history = history,
                    contacts = contacts,
                    onToggleFavorite = { vm.toggleFavorite(it) },
                    onAbout = { showAbout = true }
                )
            }
        }
    }

    if (showEditor) ReminderEditorDialog(existing = editing, templates = templates, favoriteContacts = contacts.filter { it.isFavorite }, onDismiss = { showEditor = false }, onSave = {
        vm.saveReminder(it); showEditor = false; snackbar = "Reminder saved."
    })
    if (showAbout) AlertDialog(
        onDismissRequest = { showAbout = false },
        title = { Text("WishAt12") },
        text = { Text("""A private, offline-first reminder app for birthdays, anniversaries and the moments worth remembering.

WishAt12 can open WhatsApp with a prepared message, but it never sends messages automatically.

Reminders can be delayed or restricted by Android battery, alarm and notification settings. Force-stopping the app prevents scheduled delivery until it is opened again.""") },
        confirmButton = {
            Row {
                TextButton(onClick = { showAbout = false; showPrivacy = true }) { Text("Privacy policy") }
                TextButton(onClick = { showAbout = false }) { Text("Got it") }
            }
        }
    )
    if (showPrivacy) AlertDialog(
        onDismissRequest = { showPrivacy = false },
        title = { Text("Privacy policy") },
        text = { Text("""WishAt12 does not require an account and does not send your reminders to a WishAt12 server. Reminder names, phone numbers, messages, templates, contacts and history are stored in the app's local Room database.

When you export a backup, the selected JSON file contains this data. Keep it private and share it only with people you trust.

When you choose Open WhatsApp, the phone number and message are passed to Android through a click-to-chat link so WhatsApp or your browser can open the conversation. WishAt12 does not send the message or verify delivery.""") },
        confirmButton = { TextButton(onClick = { showPrivacy = false }) { Text("Close") } }
    )
    if (snackbar != null) {
        LaunchedEffect(snackbar) {
            android.widget.Toast.makeText(context, snackbar, android.widget.Toast.LENGTH_SHORT).show()
            snackbar = null
        }
    }
}

private fun canScheduleExact(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms() else true
}

private fun hasNotificationPermission(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= 33) context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
    else context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
}
