package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ChatMessage
import com.example.data.ChatSession
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.data.WellnessCategories

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    selectedCategory: String,
    sessionsState: UiState<List<ChatSession>>,
    activeSessionId: String?,
    messagesState: UiState<List<ChatMessage>>,
    streamingReply: String?,
    isAiBusy: Boolean,
    profile: UserProfile?,
    onSelectCategory: (String) -> Unit,
    onSelectSession: (String?) -> Unit,
    onNewChat: () -> Unit,
    onDeleteSession: (String) -> Unit,
    onSendMessage: (String, List<ChatMessage>) -> Unit,
) {
    var inputText by remember { mutableStateOf("") }
    var showHistorySheet by remember { mutableStateOf(false) }

    val messages = (messagesState as? UiState.Success)?.data.orEmpty()
    val sessions = (sessionsState as? UiState.Success)?.data.orEmpty()
    val listState = rememberLazyListState()

    val currentCategoryObj = WellnessCategories.all.firstOrNull { it.title == selectedCategory }

    LaunchedEffect(messages.size, streamingReply) {
        val targetIndex = (messages.size + if (streamingReply != null) 1 else 0) - 1
        if (targetIndex >= 0) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header bar with New Chat + History
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "MedFits AI Guide",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = selectedCategory,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Row {
                        IconButton(
                            onClick = { showHistorySheet = true },
                            modifier = Modifier.testTag("chat_history_button")
                        ) {
                            Icon(Icons.Filled.History, contentDescription = "Conversation history")
                        }
                        IconButton(
                            onClick = onNewChat,
                            modifier = Modifier.testTag("new_chat_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "New conversation")
                        }
                    }
                }

                // 11 Core Categories Selector + Free-Form
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == "General Wellness",
                            onClick = { onSelectCategory("General Wellness") },
                            label = { Text("Free-Form") }
                        )
                    }
                    items(WellnessCategories.all, key = { it.id }) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat.title,
                            onClick = { onSelectCategory(cat.title) },
                            label = { Text(cat.shortTitle) }
                        )
                    }
                }
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty() && streamingReply == null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Psychology,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = currentCategoryObj?.title ?: "Free-Form Wellness Chat",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Text(
                                text = currentCategoryObj?.description
                                    ?: "Ask any everyday health education, lifestyle, or habit question.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.safety_disclaimer),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (currentCategoryObj != null) {
                                AssistChip(
                                    onClick = { onSendMessage(currentCategoryObj.samplePrompt, messages) },
                                    label = {
                                        Text(
                                            text = "Try: ${currentCategoryObj.samplePrompt}",
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            items(messages, key = { it.id.ifBlank { "${it.role}_${it.content.hashCode()}" } }) { msg ->
                ChatBubble(role = msg.role, content = msg.content)
            }

            if (streamingReply != null) {
                item {
                    ChatBubble(
                        role = "assistant",
                        content = streamingReply.ifBlank { "Thinking..." },
                        isStreaming = streamingReply.isBlank()
                    )
                }
            }
        }

        // Composer
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            if (profile != null) "Ask ${profile.fullName.ifBlank { "MedFits" }}'s wellness guide..."
                            else "Ask a wellness or lifestyle question..."
                        )
                    },
                    maxLines = 4,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_message_input")
                )
                IconButton(
                    onClick = {
                        val msg = inputText
                        inputText = ""
                        onSendMessage(msg, messages)
                    },
                    enabled = inputText.isNotBlank() && !isAiBusy,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("send_chat_button")
                ) {
                    if (isAiBusy) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send message",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    if (showHistorySheet) {
        ModalBottomSheet(onDismissRequest = { showHistorySheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Conversation History", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = {
                        onNewChat()
                        showHistorySheet = false
                    }) {
                        Text("New Chat")
                    }
                }
                if (sessions.isEmpty()) {
                    Text(
                        "No saved conversations yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.height(340.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sessions, key = { it.id }) { session ->
                            Surface(
                                color = if (session.id == activeSessionId) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = RoundedCornerShape(14.dp),
                                onClick = {
                                    onSelectSession(session.id)
                                    onSelectCategory(session.category)
                                    showHistorySheet = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            session.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "${session.category} • ${session.messageCount} msgs",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { onDeleteSession(session.id) }) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "Delete conversation",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ChatBubble(
    role: String,
    content: String,
    isStreaming: Boolean = false,
) {
    val isUser = role == "user"
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            contentColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            tonalElevation = if (isUser) 0.dp else 1.dp,
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isStreaming) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                }
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
