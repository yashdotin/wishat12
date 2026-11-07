package com.wishat12.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.wishat12.app.data.entity.MessageTemplateEntity
import com.wishat12.app.ui.components.EmptyState
import com.wishat12.app.ui.components.SectionTitle

@Composable
fun TemplatesScreen(
    templates: List<MessageTemplateEntity>,
    onSave: (MessageTemplateEntity) -> Unit,
    onDelete: (MessageTemplateEntity) -> Unit
) {
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<MessageTemplateEntity?>(null) }
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Custom") }
    var content by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current

    fun startNew() {
        editing = null
        title = ""
        category = "Custom"
        content = ""
        showEditor = true
    }

    fun startEdit(template: MessageTemplateEntity) {
        editing = template
        title = template.title
        category = template.category
        content = template.content
        showEditor = true
    }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SectionTitle("Message studio", "Words ready for your next important moment")
            TextButton(onClick = { startNew() }) { Text("+ New") }
        }
        if (templates.isEmpty()) EmptyState("No templates yet", "Create a reusable message for any occasion.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(templates, key = { it.id }) { template ->
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(template.title, style = MaterialTheme.typography.titleMedium)
                                Text(template.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Row {
                                IconButton(onClick = { startEdit(template) }) { Icon(Icons.Default.Edit, "Edit template") }
                                IconButton(onClick = { onDelete(template) }) { Icon(Icons.Default.Delete, "Delete template") }
                            }
                        }
                        Text(template.content, style = MaterialTheme.typography.bodySmall, maxLines = 4)
                        OutlinedButton(onClick = { clipboard.setText(AnnotatedString(template.content)) }) {
                            Icon(Icons.Default.ContentCopy, null); Spacer(Modifier.width(6.dp)); Text("Copy message")
                        }
                    }
                }
            }
        }
    }
    if (showEditor) AlertDialog(
        onDismissRequest = { showEditor = false },
        title = { Text(if (editing == null) "New message template" else "Edit message template") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Template name") })
                OutlinedTextField(category, { category = it }, label = { Text("Category") })
                OutlinedTextField(content, { content = it }, label = { Text("Message") }, minLines = 4)
                Text("Use {name} where the recipient's name should appear.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
                Text("Preview: ${content.replace("{name}", "Alex")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isNotBlank() && content.isNotBlank()) {
                    onSave(MessageTemplateEntity(
                        id = editing?.id ?: 0L,
                        title = title.trim(),
                        category = category.trim().ifBlank { "Custom" },
                        content = content.trim(),
                        createdAt = editing?.createdAt ?: System.currentTimeMillis()
                    ))
                    showEditor = false
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = { showEditor = false }) { Text("Cancel") } }
    )
}
