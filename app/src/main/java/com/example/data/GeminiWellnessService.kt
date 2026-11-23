package com.example.data

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val AI_TAG = "GeminiWellnessService"
private const val MODEL_NAME = "gemini-3.8-flash"
private const val AI_TIMEOUT_MS = 60_000L

class GeminiWellnessService {

    private val model by lazy {
        Firebase.ai.generativeModel(modelName = MODEL_NAME)
    }

    private fun buildSafetyAndProfilePreamble(profile: UserProfile?, category: String): String {
        val profileSection = if (profile != null) {
            """
            USER WELLNESS PROFILE:
            - Name: ${profile.fullName.ifBlank { "Not specified" }}
            - Age Range: ${profile.ageRange.ifBlank { "Not specified" }}
            - Preferred Language: ${profile.preferredLanguage.ifBlank { "English" }}
            - Dietary Preference: ${profile.dietaryPreference.ifBlank { "Not specified" }}
            - Wellness Goal: ${profile.wellnessGoal.ifBlank { "Not specified" }}
            - Activity Level: ${profile.activityLevel.ifBlank { "Not specified" }}
            - Preferred Daily Routine: ${profile.preferredRoutine.ifBlank { "Not specified" }}
            - General Wellness Preferences: ${profile.wellnessPreferences.ifBlank { "Not specified" }}
            - Daily Targets: Water ${profile.waterGoalMl} ml, Sleep ${profile.sleepGoalHours} hrs, Exercise ${profile.exerciseGoalMinutes} min
            """.trimIndent()
        } else {
            "USER WELLNESS PROFILE: Not yet completed. Ask clarifying questions when personalization details are missing; never assume missing medical or lifestyle facts."
        }

        return """
            You are MedFits Care Companion, a supportive, evidence-informed healthcare education and lifestyle wellness assistant.
            Active Category Focus: $category

            $profileSection

            MANDATORY SAFETY & COMMUNICATION RULES:
            1. Respond in the user's preferred language (${profile?.preferredLanguage?.ifBlank { "English" } ?: "English"}).
            2. You are an educational wellness assistant, NOT a doctor. Never diagnose diseases, never prescribe medications, and never recommend changing or stopping prescribed medications.
            3. Do not claim any lab value or PDF report confirms a disease. Explain uncertainty clearly and encourage consulting qualified healthcare professionals.
            4. If the user mentions potentially urgent or emergency symptoms (e.g., chest pain, severe shortness of breath, sudden weakness/numbness, severe allergic reaction, suicidal thoughts, heavy bleeding), immediately advise seeking urgent medical evaluation or contacting local emergency services. Do not give false reassurance.
            5. Personalize advice using the user's profile when provided, avoid generic repetitive answers, use clear headings and bullet points, and ask helpful follow-up questions if key context is missing.
        """.trimIndent()
    }

    fun streamChatReply(
        profile: UserProfile?,
        category: String,
        history: List<ChatMessage>,
        userMessage: String,
    ): Flow<String> = flow {
        val preamble = buildSafetyAndProfilePreamble(profile, category)
        val chatModel = Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            systemInstruction = content { text(preamble) }
        )
        val chatHistory = history.takeLast(14).map { msg ->
            val role = if (msg.role == "user") "user" else "model"
            content(role) { text(msg.content) }
        }

        val chat = chatModel.startChat(history = chatHistory)
        val accumulated = StringBuilder()
        try {
            withTimeout(AI_TIMEOUT_MS) {
                chat.sendMessageStream(userMessage).collect { chunk ->
                    val chunkText = chunk.text.orEmpty()
                    if (chunkText.isNotEmpty()) {
                        accumulated.append(chunkText)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("AI returned an empty response.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(AI_TAG, "Chat streaming timed out", e)
            throw IllegalStateException("Response timed out after 60 seconds. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AI_TAG, "Gemini chat error", e)
            throw e
        }
    }

    suspend fun analyzeHealthReport(
        profile: UserProfile?,
        fileName: String,
        extractedText: String,
        pageBitmaps: List<Bitmap>,
    ): Pair<String, String> {
        val preamble = buildSafetyAndProfilePreamble(profile, "PDF Health Report Explanation")
        val prompt = """
            $preamble

            Task: Analyze the uploaded PDF health report "$fileName".
            Please provide your output in two clearly labeled sections:
            ## Document Findings & Values Listed
            (Summarize only what is explicitly visible in the report pages/text without inventing values.)

            ## Plain-Language Educational Explanation
            (Explain medical terms, reference concepts, and general wellness context in simple language. Clearly note this is educational information, does NOT confirm a diagnosis, and should be reviewed with a qualified clinician.)

            ${if (extractedText.isNotBlank()) "Extracted text snippet:\n$extractedText" else "Note: Extracted text was minimal; inspect the attached rendered PDF pages directly."}
        """.trimIndent()

        return try {
            val response = withTimeout(AI_TIMEOUT_MS) {
                model.generateContent(
                    content {
                        text(prompt)
                        pageBitmaps.take(3).forEach { bmp ->
                            image(bmp)
                        }
                    }
                )
            }
            val summary = response.text?.takeIf { it.isNotBlank() }
                ?: "Unable to generate an explanation for this PDF report."
            val cleanExtracted = if (extractedText.isNotBlank()) {
                extractedText
            } else {
                "Visual PDF pages (${pageBitmaps.size} page preview(s) inspected by Gemini multimodal analysis)."
            }
            cleanExtracted to summary
        } catch (e: TimeoutCancellationException) {
            Log.e(AI_TAG, "PDF analysis timed out", e)
            throw IllegalStateException("PDF explanation timed out after 60 seconds.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AI_TAG, "PDF analysis failed", e)
            throw e
        }
    }

    suspend fun askAboutHealthReport(
        profile: UserProfile?,
        report: HealthReport,
        previousQa: List<ReportQuestion>,
        question: String,
    ): String {
        val preamble = buildSafetyAndProfilePreamble(profile, "PDF Health Report Q&A")
        val qaContext = previousQa.takeLast(5).joinToString("\n") {
            "Q: ${it.question}\nA: ${it.answer}"
        }
        val prompt = """
            $preamble

            Document Name: ${report.fileName}
            Extracted Document Text:
            ${report.extractedText.take(12_000)}

            Prior Educational Summary:
            ${report.aiSummary.take(6_000)}

            Prior Q&A Context:
            $qaContext

            User Question: $question

            Clearly separate facts stated in the document from your plain-language educational explanation. Never diagnose or prescribe.
        """.trimIndent()

        return try {
            val response = withTimeout(AI_TIMEOUT_MS) {
                model.generateContent(prompt)
            }
            response.text?.takeIf { it.isNotBlank() }
                ?: "I could not generate an answer right now. Please try again."
        } catch (e: TimeoutCancellationException) {
            Log.e(AI_TAG, "Report Q&A timed out", e)
            throw IllegalStateException("Request timed out after 60 seconds.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AI_TAG, "Report Q&A failed", e)
            throw e
        }
    }
}
