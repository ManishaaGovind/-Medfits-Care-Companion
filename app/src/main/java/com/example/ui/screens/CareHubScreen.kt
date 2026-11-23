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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AdminDashboardSummary
import com.example.data.SpecialistRequest
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.data.WellnessCategories
import com.example.data.WellnessResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareHubScreen(
    profileState: UiState<UserProfile?>,
    specialistState: UiState<List<SpecialistRequest>>,
    resourcesState: UiState<List<WellnessResource>>,
    isAdmin: Boolean,
    adminDashboardState: UiState<AdminDashboardSummary>,
    userEmail: String,
    userDisplayName: String,
    onSaveProfile: (UserProfile) -> Unit,
    onSubmitSpecialistRequest: (String, String, String, () -> Unit) -> Unit,
    onRefreshAdmin: () -> Unit,
    onUpdateRequestStatus: (String, String, String) -> Unit,
    onCreateResource: (String, String, String, () -> Unit) -> Unit,
    onDeleteResource: (String) -> Unit,
    onSignOut: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = if (isAdmin) listOf("Profile", "Specialist", "Admin") else listOf("Profile", "Specialist")
    val profile = (profileState as? UiState.Success)?.data

    Column(modifier = Modifier.fillMaxSize()) {
        SecondaryTabRow(selectedTabIndex = selectedTab.coerceAtMost(tabs.lastIndex)) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) },
                    icon = {
                        Icon(
                            imageVector = when (title) {
                                "Profile" -> Icons.Filled.Person
                                "Specialist" -> Icons.Filled.SupportAgent
                                else -> Icons.Filled.AdminPanelSettings
                            },
                            contentDescription = null
                        )
                    }
                )
            }
        }

        when (tabs.getOrElse(selectedTab) { "Profile" }) {
            "Profile" -> ProfileSection(
                profile = profile,
                userEmail = userEmail,
                userDisplayName = userDisplayName,
                onSaveProfile = onSaveProfile,
                onSignOut = onSignOut
            )
            "Specialist" -> SpecialistSection(
                requestsState = specialistState,
                userName = profile?.fullName?.ifBlank { userDisplayName } ?: userDisplayName,
                onSubmit = onSubmitSpecialistRequest
            )
            "Admin" -> AdminSection(
                adminState = adminDashboardState,
                resourcesState = resourcesState,
                onRefresh = onRefreshAdmin,
                onUpdateRequestStatus = onUpdateRequestStatus,
                onCreateResource = onCreateResource,
                onDeleteResource = onDeleteResource
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileSection(
    profile: UserProfile?,
    userEmail: String,
    userDisplayName: String,
    onSaveProfile: (UserProfile) -> Unit,
    onSignOut: () -> Unit,
) {
    var fullName by remember(profile) { mutableStateOf(profile?.fullName ?: userDisplayName) }
    var ageRange by remember(profile) { mutableStateOf(profile?.ageRange ?: "25-34") }
    var preferredLanguage by remember(profile) { mutableStateOf(profile?.preferredLanguage ?: "English") }
    var dietaryPreference by remember(profile) { mutableStateOf(profile?.dietaryPreference ?: "Vegetarian") }
    var wellnessGoal by remember(profile) { mutableStateOf(profile?.wellnessGoal ?: "Improve daily hydration and sleep") }
    var activityLevel by remember(profile) { mutableStateOf(profile?.activityLevel ?: "Moderately Active") }
    var preferredRoutine by remember(profile) { mutableStateOf(profile?.preferredRoutine ?: "") }
    var wellnessPreferences by remember(profile) { mutableStateOf(profile?.wellnessPreferences ?: "") }
    var waterGoal by remember(profile) { mutableStateOf((profile?.waterGoalMl ?: 2500L).toString()) }
    var sleepGoal by remember(profile) { mutableStateOf((profile?.sleepGoalHours ?: 8.0).toString()) }
    var exerciseGoal by remember(profile) { mutableStateOf((profile?.exerciseGoalMinutes ?: 30L).toString()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(fullName.ifBlank { "MedFits Member" }, style = MaterialTheme.typography.titleMedium)
                        Text(userEmail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sign Out")
                    }
                }
            }
        }

        item {
            Text("Personalized Wellness Profile", style = MaterialTheme.typography.titleLarge)
        }

        item {
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = ageRange,
                    onValueChange = { ageRange = it },
                    label = { Text("Age / Age Range") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = preferredLanguage,
                    onValueChange = { preferredLanguage = it },
                    label = { Text("Preferred Language") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Text("Dietary Preference", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Vegetarian", "Non-vegetarian", "Vegan").forEach { option ->
                    FilterChip(
                        selected = dietaryPreference == option,
                        onClick = { dietaryPreference = option },
                        label = { Text(option) }
                    )
                }
            }
        }

        item {
            Text("Activity Level", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Sedentary", "Lightly Active", "Moderately Active", "Very Active").forEach { level ->
                    FilterChip(
                        selected = activityLevel == level,
                        onClick = { activityLevel = level },
                        label = { Text(level) }
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = wellnessGoal,
                onValueChange = { wellnessGoal = it },
                label = { Text("Primary Wellness Goal") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = preferredRoutine,
                onValueChange = { preferredRoutine = it },
                label = { Text("Preferred Daily Routine") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = wellnessPreferences,
                onValueChange = { wellnessPreferences = it },
                label = { Text("General Wellness Preferences") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = waterGoal,
                    onValueChange = { waterGoal = it },
                    label = { Text("Water (ml)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = sleepGoal,
                    onValueChange = { sleepGoal = it },
                    label = { Text("Sleep (hrs)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = exerciseGoal,
                    onValueChange = { exerciseGoal = it },
                    label = { Text("Active (min)") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Button(
                onClick = {
                    onSaveProfile(
                        UserProfile(
                            fullName = fullName,
                            email = userEmail,
                            ageRange = ageRange,
                            preferredLanguage = preferredLanguage,
                            dietaryPreference = dietaryPreference,
                            wellnessGoal = wellnessGoal,
                            activityLevel = activityLevel,
                            preferredRoutine = preferredRoutine,
                            wellnessPreferences = wellnessPreferences,
                            waterGoalMl = waterGoal.toLongOrNull()?.coerceIn(500L, 6000L) ?: 2500L,
                            sleepGoalHours = sleepGoal.toDoubleOrNull()?.coerceIn(4.0, 12.0) ?: 8.0,
                            exerciseGoalMinutes = exerciseGoal.toLongOrNull()?.coerceIn(10L, 300L) ?: 30L,
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_profile_button")
            ) {
                Text("Save Wellness Profile")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpecialistSection(
    requestsState: UiState<List<SpecialistRequest>>,
    userName: String,
    onSubmit: (String, String, String, () -> Unit) -> Unit,
) {
    var selectedCategory by remember { mutableStateOf(WellnessCategories.all.first().title) }
    var concernText by remember { mutableStateOf("") }
    val requests = (requestsState as? UiState.Success)?.data.orEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Request Specialist Guidance", style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(R.string.specialist_disclaimer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        WellnessCategories.all.take(6).forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat.title,
                                onClick = { selectedCategory = cat.title },
                                label = { Text(cat.shortTitle) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = concernText,
                        onValueChange = { concernText = it },
                        label = { Text("Describe your wellness concern or question") },
                        minLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("specialist_concern_input")
                    )
                    Button(
                        onClick = {
                            onSubmit(userName, selectedCategory, concernText) {
                                concernText = ""
                            }
                        },
                        enabled = concernText.trim().length >= 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("submit_specialist_request_button")
                    ) {
                        Text("Submit Support Inquiry")
                    }
                }
            }
        }

        item {
            Text("Your Previous Inquiries", style = MaterialTheme.typography.titleMedium)
        }

        if (requests.isEmpty()) {
            item {
                Text(
                    "You have not submitted any specialist inquiries yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(requests, key = { it.id }) { req ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(req.category, style = MaterialTheme.typography.titleSmall)
                            AssistChip(onClick = {}, label = { Text(req.status.uppercase()) })
                        }
                        Text(req.concern, style = MaterialTheme.typography.bodyMedium)
                        if (req.adminNotes.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Specialist Note: ${req.adminNotes}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminSection(
    adminState: UiState<AdminDashboardSummary>,
    resourcesState: UiState<List<WellnessResource>>,
    onRefresh: () -> Unit,
    onUpdateRequestStatus: (String, String, String) -> Unit,
    onCreateResource: (String, String, String, () -> Unit) -> Unit,
    onDeleteResource: (String) -> Unit,
) {
    var newResTitle by remember { mutableStateOf("") }
    var newResCategory by remember { mutableStateOf("Preventive Healthcare") }
    var newResSummary by remember { mutableStateOf("") }
    val resources = (resourcesState as? UiState.Success)?.data.orEmpty()

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
                Text("Admin Operations Dashboard", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh admin metrics")
                }
            }
        }

        when (adminState) {
            is UiState.Loading -> {
                item { CircularProgressIndicator() }
            }
            is UiState.Error -> {
                item {
                    Text(adminState.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is UiState.Success -> {
                val data = adminState.data
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        AdminStatTile("Users", data.totalUsers.toString(), Modifier.weight(1f))
                        AdminStatTile("Chats", data.totalConversations.toString(), Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        AdminStatTile("Wellness Logs", data.totalWellnessRecords.toString(), Modifier.weight(1f))
                        AdminStatTile("Support Reqs", data.totalSpecialistRequests.toString(), Modifier.weight(1f))
                    }
                }

                item {
                    Text("Specialist Support Management", style = MaterialTheme.typography.titleMedium)
                }

                items(data.allRequests, key = { it.id }) { req ->
                    var noteDraft by remember(req.id) { mutableStateOf(req.adminNotes) }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${req.userName} (${req.userEmail})", style = MaterialTheme.typography.titleSmall)
                            Text("${req.category} • Status: ${req.status}", style = MaterialTheme.typography.labelSmall)
                            Text(req.concern, style = MaterialTheme.typography.bodySmall)
                            OutlinedTextField(
                                value = noteDraft,
                                onValueChange = { noteDraft = it },
                                label = { Text("Admin response note") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onUpdateRequestStatus(req.id, "in_review", noteDraft) }) {
                                    Text("In Review")
                                }
                                Button(onClick = { onUpdateRequestStatus(req.id, "resolved", noteDraft) }) {
                                    Text("Resolve")
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Text("Wellness Resource Management", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newResTitle,
                        onValueChange = { newResTitle = it },
                        label = { Text("Resource Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newResCategory,
                        onValueChange = { newResCategory = it },
                        label = { Text("Category") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newResSummary,
                        onValueChange = { newResSummary = it },
                        label = { Text("Summary") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            onCreateResource(newResTitle, newResCategory, newResSummary) {
                                newResTitle = ""
                                newResSummary = ""
                            }
                        },
                        enabled = newResTitle.isNotBlank() && newResSummary.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Publish Resource")
                    }
                }
            }
        }

        items(resources, key = { it.id }) { res ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(res.title, style = MaterialTheme.typography.titleSmall)
                        Text(res.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { onDeleteResource(res.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete resource", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminStatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
