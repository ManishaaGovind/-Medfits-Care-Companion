package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.data.WellnessCategories
import com.example.data.WellnessRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun WellnessScreen(
    wellnessState: UiState<List<WellnessRecord>>,
    profile: UserProfile?,
    onSaveRecord: (WellnessRecord, () -> Unit) -> Unit,
    onDeleteRecord: (String) -> Unit,
) {
    var editingRecord by remember { mutableStateOf<WellnessRecord?>(null) }
    var showEditorDialog by remember { mutableStateOf(false) }

    val records = (wellnessState as? UiState.Success)?.data.orEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Daily Wellness Tracker",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Targets: ${profile?.waterGoalMl ?: 2500} ml water • ${profile?.sleepGoalHours ?: 8.0}h sleep • ${profile?.exerciseGoalMinutes ?: 30}m active",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = {
                        editingRecord = null
                        showEditorDialog = true
                    },
                    modifier = Modifier.testTag("add_wellness_entry_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Log Day")
                }
            }
        }

        item {
            WeeklyHydrationAndActivityChart(records = records.take(7).reversed())
        }

        if (wellnessState is UiState.Loading) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (records.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("No wellness entries yet", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Tap 'Log Day' to record your hydration, sleep duration, physical activity, and daily habits.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(records, key = { it.id }) { rec ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = rec.dateKey,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row {
                                IconButton(
                                    onClick = {
                                        editingRecord = rec
                                        showEditorDialog = true
                                    }
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit entry")
                                }
                                IconButton(onClick = { onDeleteRecord(rec.id) }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Delete entry",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Water: ${rec.waterMl} ml  •  Sleep: ${String.format(Locale.US, "%.1f", rec.sleepHours)} hrs  •  ${rec.activityType} (${rec.exerciseMinutes} min)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (rec.habitsCompleted.isNotEmpty()) {
                            Text(
                                text = "Habits: ${rec.habitsCompleted.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        if (rec.notes.isNotBlank()) {
                            Text(
                                text = rec.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEditorDialog) {
        WellnessRecordDialog(
            initial = editingRecord,
            onDismiss = { showEditorDialog = false },
            onSave = { updated ->
                onSaveRecord(updated) { showEditorDialog = false }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WellnessRecordDialog(
    initial: WellnessRecord?,
    onDismiss: () -> Unit,
    onSave: (WellnessRecord) -> Unit,
) {
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var dateKey by remember { mutableStateOf(initial?.dateKey ?: today) }
    var waterMl by remember { mutableLongStateOf(initial?.waterMl ?: 2000L) }
    var sleepHours by remember { mutableDoubleStateOf(initial?.sleepHours ?: 7.5) }
    var activityType by remember { mutableStateOf(initial?.activityType ?: "Brisk Walking") }
    var exerciseMinutes by remember { mutableLongStateOf(initial?.exerciseMinutes ?: 30L) }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    val selectedHabits = remember {
        mutableStateListOf<String>().apply {
            addAll(initial?.habitsCompleted ?: listOf("Morning Hydration"))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Log Daily Wellness" else "Edit Wellness Entry") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = dateKey,
                        onValueChange = { dateKey = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text("Water Intake: $waterMl ml", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { waterMl = (waterMl - 250L).coerceAtLeast(0L) }) { Text("-250") }
                        OutlinedButton(onClick = { waterMl = (waterMl + 250L).coerceAtMost(10000L) }) { Text("+250") }
                        OutlinedButton(onClick = { waterMl = (waterMl + 500L).coerceAtMost(10000L) }) { Text("+500") }
                    }
                }
                item {
                    Text(
                        "Sleep Duration: ${String.format(Locale.US, "%.1f", sleepHours)} hrs",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Slider(
                        value = sleepHours.toFloat(),
                        onValueChange = { sleepHours = (it * 2f).roundToInt() / 2.0 },
                        valueRange = 0f..14f
                    )
                }
                item {
                    OutlinedTextField(
                        value = activityType,
                        onValueChange = { activityType = it },
                        label = { Text("Physical Activity") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Exercise Duration: $exerciseMinutes min", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = exerciseMinutes.toFloat(),
                        onValueChange = { exerciseMinutes = it.roundToInt().toLong() },
                        valueRange = 0f..180f
                    )
                }
                item {
                    Text("Daily Habits Completed", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        WellnessCategories.defaultDailyHabits.forEach { habit ->
                            val isSelected = habit in selectedHabits
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) selectedHabits.remove(habit) else selectedHabits.add(habit)
                                },
                                label = { Text(habit) }
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (optional)") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanDate = if (Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$").matches(dateKey.trim())) {
                        dateKey.trim()
                    } else today
                    onSave(
                        WellnessRecord(
                            id = initial?.id.orEmpty(),
                            userId = initial?.userId.orEmpty(),
                            dateKey = cleanDate,
                            waterMl = waterMl,
                            sleepHours = sleepHours,
                            activityType = activityType,
                            exerciseMinutes = exerciseMinutes,
                            habitsCompleted = selectedHabits.toList(),
                            notes = notes
                        )
                    )
                },
                modifier = Modifier.testTag("save_wellness_record_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
