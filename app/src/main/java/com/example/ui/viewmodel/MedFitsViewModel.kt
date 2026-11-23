package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AdminDashboardSummary
import com.example.data.ChatMessage
import com.example.data.ChatSession
import com.example.data.GeminiWellnessService
import com.example.data.HealthReport
import com.example.data.MedFitsRepository
import com.example.data.PdfExtractor
import com.example.data.ReportQuestion
import com.example.data.SpecialistRequest
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.data.WellnessRecord
import com.example.data.WellnessResource
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val VM_TAG = "MedFitsVM"
private const val STOP_TIMEOUT_MS = 5000L

@OptIn(ExperimentalCoroutinesApi::class)
class MedFitsViewModel(
    private val repository: MedFitsRepository,
    private val aiService: GeminiWellnessService,
    private val currentUserId: String,
) : ViewModel() {

    private fun <T> Flow<T>.toUiState(defaultError: String): StateFlow<UiState<T>> {
        return map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(VM_TAG, defaultError, root)
                emit(UiState.Error(root.message ?: defaultError))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = UiState.Loading
            )
    }

    val profileState: StateFlow<UiState<UserProfile?>> =
        repository.observeProfile(currentUserId).toUiState("Unable to load profile")

    val chatSessionsState: StateFlow<UiState<List<ChatSession>>> =
        repository.observeChatSessions(currentUserId).toUiState("Unable to load chat sessions")

    val wellnessRecordsState: StateFlow<UiState<List<WellnessRecord>>> =
        repository.observeWellnessRecords(currentUserId).toUiState("Unable to load wellness records")

    val healthReportsState: StateFlow<UiState<List<HealthReport>>> =
        repository.observeHealthReports(currentUserId).toUiState("Unable to load health reports")

    val specialistRequestsState: StateFlow<UiState<List<SpecialistRequest>>> =
        repository.observeUserSpecialistRequests(currentUserId).toUiState("Unable to load specialist requests")

    val wellnessResourcesState: StateFlow<UiState<List<WellnessResource>>> =
        repository.observeWellnessResources().toUiState("Unable to load wellness resources")

    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    val activeMessagesState: StateFlow<UiState<List<ChatMessage>>> =
        _activeSessionId.flatMapLatest { sessionId ->
            if (sessionId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeChatMessages(currentUserId, sessionId)
                    .map<List<ChatMessage>, UiState<List<ChatMessage>>> { UiState.Success(it) }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        emit(UiState.Error(firestoreError?.message ?: error.message ?: "Unable to load messages"))
                    }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Success(emptyList()))

    private val _selectedReportId = MutableStateFlow<String?>(null)
    val selectedReportId: StateFlow<String?> = _selectedReportId.asStateFlow()

    val reportQuestionsState: StateFlow<UiState<List<ReportQuestion>>> =
        _selectedReportId.flatMapLatest { reportId ->
            if (reportId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeReportQuestions(currentUserId, reportId)
                    .map<List<ReportQuestion>, UiState<List<ReportQuestion>>> { UiState.Success(it) }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        emit(UiState.Error(firestoreError?.message ?: error.message ?: "Unable to load report Q&A"))
                    }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Success(emptyList()))

    private val _selectedCategory = MutableStateFlow("Nutrition and Diet")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _streamingReply = MutableStateFlow<String?>(null)
    val streamingReply: StateFlow<String?> = _streamingReply.asStateFlow()

    private val _isAiBusy = MutableStateFlow(false)
    val isAiBusy: StateFlow<Boolean> = _isAiBusy.asStateFlow()

    private val _isPdfUploading = MutableStateFlow(false)
    val isPdfUploading: StateFlow<Boolean> = _isPdfUploading.asStateFlow()

    private val _isReportQaBusy = MutableStateFlow(false)
    val isReportQaBusy: StateFlow<Boolean> = _isReportQaBusy.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _adminDashboardState = MutableStateFlow<UiState<AdminDashboardSummary>>(UiState.Loading)
    val adminDashboardState: StateFlow<UiState<AdminDashboardSummary>> = _adminDashboardState.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    init {
        viewModelScope.launch {
            val admin = repository.checkIsAdmin()
            _isAdmin.value = admin
            if (admin) {
                refreshAdminDashboard()
            }
        }
    }

    fun clearBannerMessage() {
        _bannerMessage.value = null
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun selectSession(sessionId: String?) {
        _activeSessionId.value = sessionId
    }

    fun startNewSession(category: String? = null) {
        if (!category.isNullOrBlank()) {
            _selectedCategory.value = category
        }
        _activeSessionId.value = null
    }

    fun selectReport(reportId: String?) {
        _selectedReportId.value = reportId
    }

    fun saveProfile(profile: UserProfile, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.saveProfile(profile)
            result.onSuccess {
                _bannerMessage.value = "Wellness profile saved."
                onDone()
            }.onFailure { err ->
                _bannerMessage.value = err.localizedMessage ?: "Failed to save profile."
            }
        }
    }

    fun sendChatMessage(
        userText: String,
        profile: UserProfile?,
        history: List<ChatMessage>,
    ) {
        val trimmed = userText.trim()
        if (trimmed.isBlank() || _isAiBusy.value) return
        val activeCategory = _selectedCategory.value

        viewModelScope.launch {
            _isAiBusy.value = true
            _streamingReply.value = ""
            try {
                var sessionId = _activeSessionId.value
                var isFirstMessage = false
                if (sessionId.isNullOrBlank()) {
                    val created = repository.createChatSession(
                        title = trimmed.take(60),
                        category = activeCategory
                    )
                    sessionId = created.getOrElse { throw it }
                    _activeSessionId.value = sessionId
                    isFirstMessage = true
                }

                val currentCount = history.size.toLong()
                repository.appendChatMessage(
                    sessionId = sessionId,
                    role = "user",
                    content = trimmed,
                    category = activeCategory,
                    newMessageCount = currentCount + 1L,
                    sessionTitleUpdate = if (isFirstMessage) trimmed.take(60) else null
                ).getOrElse { throw it }

                val finalReply = StringBuilder()
                aiService.streamChatReply(
                    profile = profile,
                    category = activeCategory,
                    history = history,
                    userMessage = trimmed
                ).collect { accumulated ->
                    finalReply.clear()
                    finalReply.append(accumulated)
                    _streamingReply.value = accumulated
                }

                repository.appendChatMessage(
                    sessionId = sessionId,
                    role = "assistant",
                    content = finalReply.toString(),
                    category = activeCategory,
                    newMessageCount = currentCount + 2L
                ).getOrElse { throw it }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(VM_TAG, "Chat message failed", e)
                _bannerMessage.value = e.localizedMessage ?: "Unable to complete AI response."
            } finally {
                _streamingReply.value = null
                _isAiBusy.value = false
            }
        }
    }

    fun deleteChatSession(sessionId: String) {
        viewModelScope.launch {
            if (_activeSessionId.value == sessionId) {
                _activeSessionId.value = null
            }
            repository.deleteChatSession(sessionId)
                .onSuccess { _bannerMessage.value = "Conversation deleted." }
                .onFailure { _bannerMessage.value = it.localizedMessage ?: "Failed to delete conversation." }
        }
    }

    fun saveWellnessRecord(record: WellnessRecord, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveWellnessRecord(record)
                .onSuccess {
                    _bannerMessage.value = "Wellness entry saved."
                    onDone()
                }
                .onFailure { _bannerMessage.value = it.localizedMessage ?: "Failed to save wellness entry." }
        }
    }

    fun deleteWellnessRecord(recordId: String) {
        viewModelScope.launch {
            repository.deleteWellnessRecord(recordId)
                .onSuccess { _bannerMessage.value = "Wellness entry deleted." }
                .onFailure { _bannerMessage.value = it.localizedMessage ?: "Failed to delete entry." }
        }
    }

    fun uploadAndAnalyzePdf(context: Context, uri: Uri, profile: UserProfile?) {
        if (_isPdfUploading.value) return
        viewModelScope.launch {
            _isPdfUploading.value = true
            try {
                val extracted = withContext(Dispatchers.IO) {
                    PdfExtractor.extract(context.applicationContext, uri)
                }.getOrElse { throw it }

                val (cleanExtracted, aiSummary) = aiService.analyzeHealthReport(
                    profile = profile,
                    fileName = extracted.fileName,
                    extractedText = extracted.extractedText,
                    pageBitmaps = extracted.pageBitmaps,
                )

                val savedId = repository.saveHealthReport(
                    fileName = extracted.fileName,
                    fileSizeBytes = extracted.fileSizeBytes,
                    pageCount = extracted.pageCount.toLong(),
                    extractedText = cleanExtracted,
                    aiSummary = aiSummary,
                ).getOrElse { throw it }

                _selectedReportId.value = savedId
                _bannerMessage.value = "Report analyzed and saved securely."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(VM_TAG, "PDF upload/analysis failed", e)
                _bannerMessage.value = e.localizedMessage ?: "Unable to analyze PDF report."
            } finally {
                _isPdfUploading.value = false
            }
        }
    }

    fun askReportQuestion(
        report: HealthReport,
        question: String,
        history: List<ReportQuestion>,
        profile: UserProfile?,
        onDone: () -> Unit = {},
    ) {
        val trimmed = question.trim()
        if (trimmed.isBlank() || _isReportQaBusy.value) return
        viewModelScope.launch {
            _isReportQaBusy.value = true
            try {
                val answer = aiService.askAboutHealthReport(profile, report, history, trimmed)
                repository.saveReportQuestion(report.id, trimmed, answer).getOrElse { throw it }
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(VM_TAG, "Report question failed", e)
                _bannerMessage.value = e.localizedMessage ?: "Failed to answer question."
            } finally {
                _isReportQaBusy.value = false
            }
        }
    }

    fun deleteHealthReport(reportId: String) {
        viewModelScope.launch {
            if (_selectedReportId.value == reportId) {
                _selectedReportId.value = null
            }
            repository.deleteHealthReport(reportId)
                .onSuccess { _bannerMessage.value = "Report deleted." }
                .onFailure { _bannerMessage.value = it.localizedMessage ?: "Failed to delete report." }
        }
    }

    fun submitSpecialistRequest(userName: String, category: String, concern: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.createSpecialistRequest(userName, category, concern)
                .onSuccess {
                    _bannerMessage.value = "Support inquiry submitted."
                    onDone()
                }
                .onFailure { _bannerMessage.value = it.localizedMessage ?: "Failed to submit inquiry." }
        }
    }

    fun refreshAdminDashboard() {
        viewModelScope.launch {
            _adminDashboardState.value = UiState.Loading
            repository.fetchAdminDashboardSummary()
                .onSuccess { _adminDashboardState.value = UiState.Success(it) }
                .onFailure { _adminDashboardState.value = UiState.Error(it.localizedMessage ?: "Admin access denied or failed.") }
        }
    }

    fun updateSpecialistRequestStatus(requestId: String, status: String, adminNotes: String) {
        viewModelScope.launch {
            repository.updateSpecialistRequestStatus(requestId, status, adminNotes)
                .onSuccess {
                    _bannerMessage.value = "Request status updated."
                    refreshAdminDashboard()
                }
                .onFailure { _bannerMessage.value = it.localizedMessage ?: "Failed to update request." }
        }
    }

    fun createWellnessResource(title: String, category: String, summary: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.createWellnessResource(title, category, summary)
                .onSuccess {
                    _bannerMessage.value = "Wellness resource published."
                    onDone()
                }
                .onFailure { _bannerMessage.value = it.localizedMessage ?: "Failed to publish resource." }
        }
    }

    fun deleteWellnessResource(resourceId: String) {
        viewModelScope.launch {
            repository.deleteWellnessResource(resourceId)
                .onSuccess { _bannerMessage.value = "Resource removed." }
                .onFailure { _bannerMessage.value = it.localizedMessage ?: "Failed to remove resource." }
        }
    }
}
