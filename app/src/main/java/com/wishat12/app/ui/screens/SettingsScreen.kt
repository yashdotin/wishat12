package com.wishat12.app.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import com.wishat12.app.data.entity.MessageHistoryEntity
import com.wishat12.app.data.entity.ContactEntity
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    exactAllowed: Boolean,
    notificationsAllowed: Boolean,
    onExactSettings: () -> Unit,
    onNotificationPermission: () -> Unit,
    onNotificationSettings: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onTestNotification: () -> Unit,
    onTestAlarm: () -> Unit,
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    history: List<MessageHistoryEntity>,
    contacts: List<ContactEntity>,
    onToggleFavorite: (ContactEntity) -> Unit,
    onAbout: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Text("Make sure WishAt12 has what it needs to remember your moments.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Permissions", style = MaterialTheme.typography.titleMedium)
                SettingsRow(Icons.Default.Alarm, "Exact alarms", if (exactAllowed) "Allowed · precise reminder timing" else "Not enabled · using inexact fallback", !exactAllowed, onExactSettings)
                Text("Allow exact alarms for reminders to fire close to the selected time. If this is off, Android may delay reminders. After changing this setting, return to WishAt12 so it can restore your alarms.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .62f))
                if (Build.VERSION.SDK_INT >= 33) SettingsRow(Icons.Default.Notifications, "Notifications", if (notificationsAllowed) "Enabled" else "Permission not granted", !notificationsAllowed, onNotificationPermission)
                else SettingsRow(Icons.Default.Notifications, "Notifications", if (notificationsAllowed) "Enabled" else "Check notification settings", !notificationsAllowed, onNotificationSettings)
                Text("Allow WishAt12 notifications and keep the Scheduled wishes notification channel enabled. If notifications are blocked, an alarm can fire without a visible reminder.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .62f))
                TextButton(onClick = onNotificationSettings) { Text("Open notification settings") }
            }
        }
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Your data", style = MaterialTheme.typography.titleMedium)
                Text("Your reminders and messages stay on this phone. Export a JSON backup before changing devices.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .62f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onExport, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Backup, null); Spacer(Modifier.width(6.dp)); Text("Export") }
                    OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) { Text("Restore") }
                }
            }
        }
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Appearance", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (isDarkTheme) "Dark theme" else "Light theme")
                    Switch(checked = isDarkTheme, onCheckedChange = onThemeChange)
                }
            }
        }
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Try it out", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = onTestNotification, modifier = Modifier.fillMaxWidth()) { Text("Send a test notification") }
                OutlinedButton(onClick = onTestAlarm, modifier = Modifier.fillMaxWidth()) { Text("Schedule a test alarm in 1 minute") }
                Text("First use Send a test notification. Then schedule the one-minute alarm, close WishAt12 normally (do not Force stop it), lock the phone and wait. The test creates a temporary reminder. Tap Open chat & send to review a WhatsApp message; you must press Send yourself.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .58f))
            }
        }
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Favorite contacts", style = MaterialTheme.typography.titleMedium)
                if (contacts.isEmpty()) Text("Contacts are saved automatically when you add a WhatsApp number to a reminder.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .58f))
                else contacts.take(12).forEach { contact ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(contact.name, style = MaterialTheme.typography.bodyMedium)
                            Text(contact.phoneNumber, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .58f))
                        }
                        IconButton(onClick = { onToggleFavorite(contact) }) {
                            Icon(if (contact.isFavorite) Icons.Default.Star else Icons.Default.StarBorder, if (contact.isFavorite) "Remove favorite" else "Add favorite", tint = if (contact.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Recent reminder history", style = MaterialTheme.typography.titleMedium)
                if (history.isEmpty()) Text("Your triggered reminder history will appear here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .58f))
                else history.take(5).forEach { item ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("${item.occasion} · ${item.recipientName}", style = MaterialTheme.typography.bodyMedium)
                        Text(Instant.ofEpochMilli(item.firedAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM yyyy · hh:mm a")), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
                    }
                }
            }
        }
        ListItem(leadingContent = { Icon(Icons.Default.Info, null) }, headlineContent = { Text("About WishAt12") }, supportingContent = { Text("Version 1.0.0 · Free · Offline-first") }, modifier = Modifier.fillMaxWidth(), trailingContent = { TextButton(onClick = onAbout) { Text("View") } })
    }
}

@Composable
private fun SettingsRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, actionNeeded: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .58f))
        }
        if (actionNeeded) TextButton(onClick = onClick) { Text("Fix") }
    }
}
