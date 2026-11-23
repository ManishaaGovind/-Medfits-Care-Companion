package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ChatSession
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.data.WellnessCategories
import com.example.data.WellnessCategory
import com.example.data.WellnessRecord
import com.example.data.WellnessResource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    profileState: UiState<UserProfile?>,
    wellnessState: UiState<List<WellnessRecord>>,
    sessionsState: UiState<List<ChatSession>>,
    resourcesState: UiState<List<WellnessResource>>,
    onCategoryClick: (WellnessCategory) -> Unit,
    onOpenSession: (ChatSession) -> Unit,
    onOpenTracker: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val profile = (profileState as? UiState.Success)?.data
    val records = (wellnessState as? UiState.Success)?.data.orEmpty()
    val sessions = (sessionsState as? UiState.Success)?.data.orEmpty()
    val resources = (resourcesState as? UiState.Success)?.data.orEmpty()

    val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val todayRecord = records.firstOrNull { it.dateKey == todayKey } ?: records.firstOrNull()

    val waterGoal = profile?.waterGoalMl ?: 2500L
    val sleepGoal = profile?.sleepGoalHours ?: 8.0
    val exerciseGoal = profile?.exerciseGoalMinutes ?: 30L

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Welcome Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_wellness),
                        contentDescription = "Daily wellness hero banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(124.dp)
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    )
                    Column(modifier = Modifier.padding(16.dp)) {
                        val name = profile?.fullName?.takeIf { it.isNotBlank() } ?: "there"
                        Text(
                            text = "Welcome back, $name",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (profile != null) {
                                "Goal: ${profile.wellnessGoal.ifBlank { "Daily wellness" }} • ${profile.dietaryPreference} • ${profile.activityLevel}"
                            } else {
                                "Complete your wellness profile so MedFits AI can personalize every recommendation."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (profile == null && profileState !is UiState.Loading) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onOpenProfile,
                                modifier = Modifier.testTag("complete_profile_button")
                            ) {
                                Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Set Up Wellness Profile")
                            }
                        }
                    }
                }
            }
        }

        // Today's Wellness Overview
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Wellness Overview",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                OutlinedButton(
                    onClick = onOpenTracker,
                    modifier = Modifier.testTag("dashboard_log_wellness_button")
                ) {
                    Text("Log Today")
                }
            }
        }

        item {
            if (wellnessState is UiState.Loading) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricProgressCard(
                            title = "Hydration",
                            valueText = "${todayRecord?.waterMl ?: 0} / $waterGoal ml",
                            progress = ((todayRecord?.waterMl ?: 0L).toFloat() / waterGoal.coerceAtLeast(1L)).coerceIn(0f, 1f),
                            icon = Icons.Filled.WaterDrop,
                            accent = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        MetricProgressCard(
                            title = "Sleep",
                            valueText = "${String.format(Locale.US, "%.1f", todayRecord?.sleepHours ?: 0.0)} / ${String.format(Locale.US, "%.1f", sleepGoal)} hrs",
                            progress = (((todayRecord?.sleepHours ?: 0.0) / sleepGoal.coerceAtLeast(1.0)).toFloat()).coerceIn(0f, 1f),
                            icon = Icons.Filled.Bedtime,
                            accent = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricProgressCard(
                            title = "Activity",
                            valueText = "${todayRecord?.exerciseMinutes ?: 0} / $exerciseGoal min",
                            progress = ((todayRecord?.exerciseMinutes ?: 0L).toFloat() / exerciseGoal.coerceAtLeast(1L)).coerceIn(0f, 1f),
                            icon = Icons.AutoMirrored.Filled.DirectionsRun,
                            accent = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                        MetricProgressCard(
                            title = "Habits",
                            valueText = "${todayRecord?.habitsCompleted?.size ?: 0} / ${WellnessCategories.defaultDailyHabits.size} done",
                            progress = ((todayRecord?.habitsCompleted?.size ?: 0).toFloat() / WellnessCategories.defaultDailyHabits.size).coerceIn(0f, 1f),
                            icon = Icons.Filled.CheckCircle,
                            accent = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Weekly Progress Chart
        item {
            WeeklyHydrationAndActivityChart(records = records.take(7).reversed())
        }

        // 11 Core AI Categories
        item {
            Text(
                text = "11 Wellness AI Categories",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(WellnessCategories.all, key = { it.id }) { category ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(18.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .width(210.dp)
                            .clickable { onCategoryClick(category) }
                            .testTag("category_card_${category.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = category.shortTitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = category.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = category.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Ask AI",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent Conversations
        item {
            Text(
                text = "Recent AI Conversations",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (sessions.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Filled.ChatBubble, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "No conversations yet. Tap any of the 11 wellness categories above to start.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(sessions.take(4), key = { it.id }) { session ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenSession(session) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            AssistChip(
                                onClick = { onOpenSession(session) },
                                label = { Text(session.category, style = MaterialTheme.typography.labelSmall) }
                            )
                            Text(
                                text = session.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = session.lastMessagePreview,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open conversation",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        if (resources.isNotEmpty()) {
            item {
                Text(
                    text = "Wellness Guidance Library",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            items(resources.take(3), key = { it.id }) { res ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = res.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = res.title,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = res.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricProgressCard(
    title: String,
    valueText: String,
    progress: Float,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                Text(text = title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                text = valueText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            LinearProgressIndicator(
                progress = { progress },
                color = accent,
                trackColor = accent.copy(alpha = 0.16f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
fun WeeklyHydrationAndActivityChart(records: List<WellnessRecord>) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Weekly Progress (Water & Exercise)",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LegendDot("Water", primaryColor)
                    LegendDot("Exercise", secondaryColor)
                }
            }

            if (records.isEmpty()) {
                Text(
                    text = "Log daily entries in the Wellness Tracker to visualize your 7-day trend.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    val slotWidth = size.width / records.size.coerceAtLeast(1)
                    val barWidth = (slotWidth * 0.28f).coerceAtMost(28f)
                    val maxWater = (records.maxOfOrNull { it.waterMl } ?: 2500L).coerceAtLeast(2500L).toFloat()
                    val maxMin = (records.maxOfOrNull { it.exerciseMinutes } ?: 60L).coerceAtLeast(60L).toFloat()

                    records.forEachIndexed { idx, rec ->
                        val centerX = slotWidth * idx + slotWidth / 2f
                        val waterRatio = (rec.waterMl / maxWater).coerceIn(0.05f, 1f)
                        val exRatio = (rec.exerciseMinutes / maxMin).coerceIn(0.05f, 1f)

                        drawRoundRect(
                            color = trackColor,
                            topLeft = Offset(centerX - barWidth - 3f, 0f),
                            size = Size(barWidth, size.height),
                            cornerRadius = CornerRadius(8f, 8f)
                        )
                        drawRoundRect(
                            color = primaryColor,
                            topLeft = Offset(centerX - barWidth - 3f, size.height * (1f - waterRatio)),
                            size = Size(barWidth, size.height * waterRatio),
                            cornerRadius = CornerRadius(8f, 8f)
                        )

                        drawRoundRect(
                            color = trackColor,
                            topLeft = Offset(centerX + 3f, 0f),
                            size = Size(barWidth, size.height),
                            cornerRadius = CornerRadius(8f, 8f)
                        )
                        drawRoundRect(
                            color = secondaryColor,
                            topLeft = Offset(centerX + 3f, size.height * (1f - exRatio)),
                            size = Size(barWidth, size.height * exRatio),
                            cornerRadius = CornerRadius(8f, 8f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    records.forEach { rec ->
                        Text(
                            text = rec.dateKey.takeLast(5),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(color = color)
        }
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
