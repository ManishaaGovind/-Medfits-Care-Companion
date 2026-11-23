package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private const val ADMIN_BOOTSTRAP_EMAIL = "manishaaaaa.govind@gmail.com"

class MedFitsRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- User Profile ---

    fun observeProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Cross-user profile read denied." }
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
        )
    }

    suspend fun saveProfile(profile: UserProfile): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
            path = docRef.path
            val existing = docRef.get().await()
            val email = auth.currentUser?.email ?: profile.email
            if (existing.exists()) {
                val updates = buildMap<String, Any> {
                    put("email", email.take(200))
                    put("fullName", profile.fullName.trim().ifBlank { "MedFits Member" }.take(100))
                    put("ageRange", profile.ageRange.ifBlank { "25-34" }.take(40))
                    put("preferredLanguage", profile.preferredLanguage.ifBlank { "English" }.take(40))
                    put("dietaryPreference", profile.dietaryPreference.ifBlank { "Vegetarian" })
                    put("wellnessGoal", profile.wellnessGoal.trim().take(200))
                    put("activityLevel", profile.activityLevel.ifBlank { "Moderately Active" })
                    if (profile.preferredRoutine.isNotBlank()) {
                        put("preferredRoutine", profile.preferredRoutine.trim().take(300))
                    }
                    if (profile.wellnessPreferences.isNotBlank()) {
                        put("wellnessPreferences", profile.wellnessPreferences.trim().take(400))
                    }
                    put("waterGoalMl", profile.waterGoalMl.coerceIn(500L, 6000L))
                    put("sleepGoalHours", profile.sleepGoalHours.coerceIn(4.0, 12.0))
                    put("exerciseGoalMinutes", profile.exerciseGoalMinutes.coerceIn(10L, 300L))
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                docRef.update(updates).await()
            } else {
                val createPayload = buildMap<String, Any> {
                    put("userId", uid)
                    put("email", email.take(200))
                    put("fullName", profile.fullName.trim().ifBlank { auth.currentUser?.displayName ?: "MedFits Member" }.take(100))
                    put("ageRange", profile.ageRange.ifBlank { "25-34" }.take(40))
                    put("preferredLanguage", profile.preferredLanguage.ifBlank { "English" }.take(40))
                    put("dietaryPreference", profile.dietaryPreference.ifBlank { "Vegetarian" })
                    put("wellnessGoal", profile.wellnessGoal.trim().take(200))
                    put("activityLevel", profile.activityLevel.ifBlank { "Moderately Active" })
                    if (profile.preferredRoutine.isNotBlank()) {
                        put("preferredRoutine", profile.preferredRoutine.trim().take(300))
                    }
                    if (profile.wellnessPreferences.isNotBlank()) {
                        put("wellnessPreferences", profile.wellnessPreferences.trim().take(400))
                    }
                    put("waterGoalMl", profile.waterGoalMl.coerceIn(500L, 6000L))
                    put("sleepGoalHours", profile.sleepGoalHours.coerceIn(4.0, 12.0))
                    put("exerciseGoalMinutes", profile.exerciseGoalMinutes.coerceIn(10L, 300L))
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                docRef.set(createPayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- Chat Sessions & Messages ---

    fun observeChatSessions(userId: String): Flow<List<ChatSession>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        emitAll(
            db.collection("users").document(uid).collection("chatSessions")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        ChatSession::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
        )
    }

    fun observeChatMessages(userId: String, sessionId: String): Flow<List<ChatMessage>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        emitAll(
            db.collection("users").document(uid)
                .collection("chatSessions").document(sessionId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        ChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
        )
    }

    suspend fun createChatSession(title: String, category: String): Result<String> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/chatSessions"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("chatSessions").document()
            path = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "title" to title.trim().ifBlank { category }.take(120),
                "category" to category.ifBlank { "General Wellness" }.take(80),
                "lastMessagePreview" to "New conversation started",
                "messageCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun appendChatMessage(
        sessionId: String,
        role: String,
        content: String,
        category: String,
        newMessageCount: Long,
        sessionTitleUpdate: String? = null,
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/chatSessions/$sessionId/messages"
        return try {
            val uid = requireUserId()
            val sessionRef = db.collection("users").document(uid).collection("chatSessions").document(sessionId)
            val msgRef = sessionRef.collection("messages").document()
            path = msgRef.path
            val msgPayload = mapOf(
                "userId" to uid,
                "sessionId" to sessionId,
                "role" to role,
                "content" to content.take(15_900),
                "category" to category.take(80),
                "createdAt" to FieldValue.serverTimestamp(),
            )
            msgRef.set(msgPayload).await()

            val sessionUpdates = buildMap<String, Any> {
                put("category", category.take(80))
                put("lastMessagePreview", content.replace("\n", " ").trim().take(220))
                put("messageCount", newMessageCount.coerceIn(0L, 10000L))
                put("updatedAt", FieldValue.serverTimestamp())
                if (!sessionTitleUpdate.isNullOrBlank()) {
                    put("title", sessionTitleUpdate.trim().take(120))
                }
            }
            sessionRef.update(sessionUpdates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteChatSession(sessionId: String): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/chatSessions/$sessionId"
        return try {
            val uid = requireUserId()
            val sessionRef = db.collection("users").document(uid).collection("chatSessions").document(sessionId)
            path = sessionRef.path
            val msgs = sessionRef.collection("messages").get().await()
            msgs.documents.forEach { it.reference.delete().await() }
            sessionRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- Wellness Records ---

    fun observeWellnessRecords(userId: String): Flow<List<WellnessRecord>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        emitAll(
            db.collection("users").document(uid).collection("wellnessRecords")
                .orderBy("dateKey", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        WellnessRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
        )
    }

    suspend fun saveWellnessRecord(record: WellnessRecord): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/wellnessRecords"
        return try {
            val uid = requireUserId()
            val col = db.collection("users").document(uid).collection("wellnessRecords")
            val isUpdate = record.id.isNotBlank()
            val docRef = if (isUpdate) col.document(record.id) else col.document()
            path = docRef.path

            val cleanHabits = record.habitsCompleted.take(15).map { it.take(80) }
            if (isUpdate) {
                val updates = buildMap<String, Any> {
                    put("dateKey", record.dateKey)
                    put("waterMl", record.waterMl.coerceIn(0L, 10000L))
                    put("sleepHours", record.sleepHours.coerceIn(0.0, 24.0))
                    put("activityType", record.activityType.trim().ifBlank { "Walking" }.take(80))
                    put("exerciseMinutes", record.exerciseMinutes.coerceIn(0L, 600L))
                    put("habitsCompleted", cleanHabits)
                    if (record.notes.isNotBlank()) {
                        put("notes", record.notes.trim().take(500))
                    }
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                docRef.update(updates).await()
            } else {
                val createPayload = buildMap<String, Any> {
                    put("userId", uid)
                    put("dateKey", record.dateKey)
                    put("waterMl", record.waterMl.coerceIn(0L, 10000L))
                    put("sleepHours", record.sleepHours.coerceIn(0.0, 24.0))
                    put("activityType", record.activityType.trim().ifBlank { "Walking" }.take(80))
                    put("exerciseMinutes", record.exerciseMinutes.coerceIn(0L, 600L))
                    put("habitsCompleted", cleanHabits)
                    if (record.notes.isNotBlank()) {
                        put("notes", record.notes.trim().take(500))
                    }
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                docRef.set(createPayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteWellnessRecord(recordId: String): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/wellnessRecords/$recordId"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("wellnessRecords").document(recordId)
            path = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- Health Reports (PDF) ---

    fun observeHealthReports(userId: String): Flow<List<HealthReport>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        emitAll(
            db.collection("users").document(uid).collection("healthReports")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        HealthReport::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
        )
    }

    fun observeReportQuestions(userId: String, reportId: String): Flow<List<ReportQuestion>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        emitAll(
            db.collection("users").document(uid)
                .collection("healthReports").document(reportId)
                .collection("questions")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        ReportQuestion::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
        )
    }

    suspend fun saveHealthReport(
        fileName: String,
        fileSizeBytes: Long,
        pageCount: Long,
        extractedText: String,
        aiSummary: String,
    ): Result<String> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/healthReports"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("healthReports").document()
            path = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "fileName" to fileName.take(160),
                "fileSizeBytes" to fileSizeBytes.coerceIn(1L, 8388608L),
                "pageCount" to pageCount.coerceIn(1L, 200L),
                "extractedText" to extractedText.take(49_000),
                "aiSummary" to aiSummary.take(19_900),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveReportQuestion(reportId: String, question: String, answer: String): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/healthReports/$reportId/questions"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
                .collection("healthReports").document(reportId)
                .collection("questions").document()
            path = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "reportId" to reportId,
                "question" to question.trim().take(2000),
                "answer" to answer.take(15_900),
                "createdAt" to FieldValue.serverTimestamp(),
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteHealthReport(reportId: String): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/healthReports/$reportId"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("healthReports").document(reportId)
            path = docRef.path
            val questions = docRef.collection("questions").get().await()
            questions.documents.forEach { it.reference.delete().await() }
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- Specialist Support Requests ---

    fun observeUserSpecialistRequests(userId: String): Flow<List<SpecialistRequest>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        emitAll(
            db.collection("specialistRequests")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        SpecialistRequest::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
        )
    }

    suspend fun createSpecialistRequest(
        userName: String,
        category: String,
        concern: String,
    ): Result<Unit> {
        var path = "specialistRequests"
        return try {
            val uid = requireUserId()
            val email = auth.currentUser?.email ?: ""
            val docRef = db.collection("specialistRequests").document()
            path = docRef.path
            val trimmedConcern = concern.trim()
            require(trimmedConcern.length >= 5) { "Please provide at least 5 characters describing your concern." }
            val payload = mapOf(
                "userId" to uid,
                "userEmail" to email.take(200),
                "userName" to userName.ifBlank { auth.currentUser?.displayName ?: "MedFits Member" }.take(100),
                "category" to category.ifBlank { "General Wellness" }.take(80),
                "concern" to trimmedConcern.take(2000),
                "status" to "pending",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- Curated Wellness Resources ---

    fun observeWellnessResources(): Flow<List<WellnessResource>> = flow {
        requireUserId()
        emitAll(
            db.collection("wellnessResources")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        WellnessResource::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
        )
    }

    // --- Admin Operations ---

    suspend fun checkIsAdmin(): Boolean {
        return try {
            val user = auth.currentUser ?: return false
            if (user.email == ADMIN_BOOTSTRAP_EMAIL && user.isEmailVerified) {
                return true
            }
            val doc = db.collection("admins").document(user.uid).get().await()
            doc.exists()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
    }

    suspend fun fetchAdminDashboardSummary(): Result<AdminDashboardSummary> {
        return try {
            requireUserId()
            val usersSnap = db.collection("users").get().await()
            val chatsSnap = db.collectionGroup("chatSessions").get().await()
            val wellnessSnap = db.collectionGroup("wellnessRecords").get().await()
            val requestsSnap = db.collection("specialistRequests").get().await()

            val users = usersSnap.toObjects(
                UserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedByDescending { it.updatedAt?.seconds ?: 0L }

            val requests = requestsSnap.toObjects(
                SpecialistRequest::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedByDescending { it.createdAt?.seconds ?: 0L }

            Result.success(
                AdminDashboardSummary(
                    totalUsers = usersSnap.size(),
                    totalConversations = chatsSnap.size(),
                    totalWellnessRecords = wellnessSnap.size(),
                    totalSpecialistRequests = requestsSnap.size(),
                    recentUsers = users.take(10),
                    allRequests = requests,
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "admin_dashboard")
            Result.failure(e)
        }
    }

    suspend fun updateSpecialistRequestStatus(
        requestId: String,
        newStatus: String,
        adminNotes: String,
    ): Result<Unit> {
        val path = "specialistRequests/$requestId"
        return try {
            requireUserId()
            val updates = buildMap<String, Any> {
                put("status", newStatus)
                if (adminNotes.isNotBlank()) {
                    put("adminNotes", adminNotes.trim().take(1000))
                }
                put("updatedAt", FieldValue.serverTimestamp())
            }
            db.collection("specialistRequests").document(requestId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createWellnessResource(title: String, category: String, summary: String): Result<Unit> {
        var path = "wellnessResources"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("wellnessResources").document()
            path = docRef.path
            val payload = mapOf(
                "title" to title.trim().take(120),
                "category" to category.ifBlank { "Preventive Healthcare" }.take(80),
                "summary" to summary.trim().take(1000),
                "createdBy" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteWellnessResource(resourceId: String): Result<Unit> {
        val path = "wellnessResources/$resourceId"
        return try {
            requireUserId()
            db.collection("wellnessResources").document(resourceId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
