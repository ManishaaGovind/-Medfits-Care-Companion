package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.GeminiWellnessService
import com.example.data.MedFitsRepository
import com.example.data.UiState
import com.example.data.attemptAutoSignIn
import com.example.data.signOutUser
import com.example.firebase.configureAppCheck
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CareHubScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.WellnessScreen
import com.example.ui.theme.MedFitsTheme
import com.example.ui.viewmodel.MedFitsViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private sealed class MedFitsRoute(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
    val testTag: String,
) {
    data object Dashboard : MedFitsRoute("dashboard", R.string.nav_dashboard, Icons.Filled.Home, "nav_dashboard")
    data object Chat : MedFitsRoute("chat", R.string.nav_chat, Icons.Filled.Psychology, "nav_chat")
    data object Tracker : MedFitsRoute("tracker", R.string.nav_tracker, Icons.Filled.Favorite, "nav_tracker")
    data object Reports : MedFitsRoute("reports", R.string.nav_reports, Icons.Filled.Description, "nav_reports")
    data object CareHub : MedFitsRoute("care_hub", R.string.nav_care, Icons.Filled.Person, "nav_care_hub")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            MedFitsTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppRoot()
                }
            }
        }
    }
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(Unit) {
        if (currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { currentUser = Firebase.auth.currentUser },
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
    } else {
        val uid = user.uid
        key(uid) {
            AuthenticatedMedFitsShell(
                currentUserId = uid,
                userEmail = user.email.orEmpty(),
                userDisplayName = user.displayName.orEmpty(),
                onSignOut = {
                    signOutUser(credentialManager = credentialManager, scope = scope)
                }
            )
        }
    }
}

@Composable
private fun AuthenticatedMedFitsShell(
    currentUserId: String,
    userEmail: String,
    userDisplayName: String,
    onSignOut: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: MedFitsViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                MedFitsViewModel(
                    repository = MedFitsRepository(db),
                    aiService = GeminiWellnessService(),
                    currentUserId = currentUserId,
                )
            }
        }
    )

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val chatSessionsState by viewModel.chatSessionsState.collectAsStateWithLifecycle()
    val wellnessState by viewModel.wellnessRecordsState.collectAsStateWithLifecycle()
    val reportsState by viewModel.healthReportsState.collectAsStateWithLifecycle()
    val specialistState by viewModel.specialistRequestsState.collectAsStateWithLifecycle()
    val resourcesState by viewModel.wellnessResourcesState.collectAsStateWithLifecycle()
    val activeSessionId by viewModel.activeSessionId.collectAsStateWithLifecycle()
    val activeMessagesState by viewModel.activeMessagesState.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val streamingReply by viewModel.streamingReply.collectAsStateWithLifecycle()
    val isAiBusy by viewModel.isAiBusy.collectAsStateWithLifecycle()
    val selectedReportId by viewModel.selectedReportId.collectAsStateWithLifecycle()
    val reportQuestionsState by viewModel.reportQuestionsState.collectAsStateWithLifecycle()
    val isPdfUploading by viewModel.isPdfUploading.collectAsStateWithLifecycle()
    val isReportQaBusy by viewModel.isReportQaBusy.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val adminDashboardState by viewModel.adminDashboardState.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(bannerMessage) {
        val msg = bannerMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBannerMessage()
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: MedFitsRoute.Dashboard.route

    val navItems = listOf(
        MedFitsRoute.Dashboard,
        MedFitsRoute.Chat,
        MedFitsRoute.Tracker,
        MedFitsRoute.Reports,
        MedFitsRoute.CareHub,
    )

    val profile = (profileState as? UiState.Success)?.data

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        navItems.forEach { dest ->
                            val label = stringResource(dest.labelRes)
                            NavigationBarItem(
                                selected = currentRoute == dest.route,
                                onClick = {
                                    navController.navigate(dest.route) {
                                        popUpTo(MedFitsRoute.Dashboard.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(dest.icon, contentDescription = label) },
                                label = { Text(label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail {
                        navItems.forEach { dest ->
                            val label = stringResource(dest.labelRes)
                            NavigationRailItem(
                                selected = currentRoute == dest.route,
                                onClick = {
                                    navController.navigate(dest.route) {
                                        popUpTo(MedFitsRoute.Dashboard.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(dest.icon, contentDescription = label) },
                                label = { Text(label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = MedFitsRoute.Dashboard.route
                    ) {
                        composable(MedFitsRoute.Dashboard.route) {
                            DashboardScreen(
                                profileState = profileState,
                                wellnessState = wellnessState,
                                sessionsState = chatSessionsState,
                                resourcesState = resourcesState,
                                onCategoryClick = { cat ->
                                    viewModel.startNewSession(cat.title)
                                    navController.navigate(MedFitsRoute.Chat.route) { launchSingleTop = true }
                                },
                                onOpenSession = { session ->
                                    viewModel.selectCategory(session.category)
                                    viewModel.selectSession(session.id)
                                    navController.navigate(MedFitsRoute.Chat.route) { launchSingleTop = true }
                                },
                                onOpenTracker = {
                                    navController.navigate(MedFitsRoute.Tracker.route) { launchSingleTop = true }
                                },
                                onOpenProfile = {
                                    navController.navigate(MedFitsRoute.CareHub.route) { launchSingleTop = true }
                                }
                            )
                        }
                        composable(MedFitsRoute.Chat.route) {
                            ChatScreen(
                                selectedCategory = selectedCategory,
                                sessionsState = chatSessionsState,
                                activeSessionId = activeSessionId,
                                messagesState = activeMessagesState,
                                streamingReply = streamingReply,
                                isAiBusy = isAiBusy,
                                profile = profile,
                                onSelectCategory = viewModel::selectCategory,
                                onSelectSession = viewModel::selectSession,
                                onNewChat = { viewModel.startNewSession() },
                                onDeleteSession = viewModel::deleteChatSession,
                                onSendMessage = { text, history ->
                                    viewModel.sendChatMessage(text, profile, history)
                                }
                            )
                        }
                        composable(MedFitsRoute.Tracker.route) {
                            WellnessScreen(
                                wellnessState = wellnessState,
                                profile = profile,
                                onSaveRecord = viewModel::saveWellnessRecord,
                                onDeleteRecord = viewModel::deleteWellnessRecord
                            )
                        }
                        composable(MedFitsRoute.Reports.route) {
                            ReportsScreen(
                                reportsState = reportsState,
                                selectedReportId = selectedReportId,
                                questionsState = reportQuestionsState,
                                isPdfUploading = isPdfUploading,
                                isReportQaBusy = isReportQaBusy,
                                profile = profile,
                                onSelectReport = viewModel::selectReport,
                                onUploadPdf = { uri -> viewModel.uploadAndAnalyzePdf(context, uri, profile) },
                                onAskQuestion = { report, question, history, onDone ->
                                    viewModel.askReportQuestion(report, question, history, profile, onDone)
                                },
                                onDeleteReport = viewModel::deleteHealthReport
                            )
                        }
                        composable(MedFitsRoute.CareHub.route) {
                            CareHubScreen(
                                profileState = profileState,
                                specialistState = specialistState,
                                resourcesState = resourcesState,
                                isAdmin = isAdmin,
                                adminDashboardState = adminDashboardState,
                                userEmail = userEmail,
                                userDisplayName = userDisplayName,
                                onSaveProfile = { updated -> viewModel.saveProfile(updated) },
                                onSubmitSpecialistRequest = viewModel::submitSpecialistRequest,
                                onRefreshAdmin = viewModel::refreshAdminDashboard,
                                onUpdateRequestStatus = viewModel::updateSpecialistRequestStatus,
                                onCreateResource = viewModel::createWellnessResource,
                                onDeleteResource = viewModel::deleteWellnessResource,
                                onSignOut = onSignOut
                            )
                        }
                    }
                }
            }
        }
    }
}
