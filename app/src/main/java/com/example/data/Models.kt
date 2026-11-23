package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val fullName: String = "",
    val ageRange: String = "25-34",
    val preferredLanguage: String = "English",
    val dietaryPreference: String = "Vegetarian",
    val wellnessGoal: String = "Build consistent daily wellness habits",
    val activityLevel: String = "Moderately Active",
    val preferredRoutine: String = "",
    val wellnessPreferences: String = "",
    val waterGoalMl: Long = 2500L,
    val sleepGoalHours: Double = 8.0,
    val exerciseGoalMinutes: Long = 30L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class ChatSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = "General Wellness",
    val lastMessagePreview: String = "",
    val messageCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sessionId: String = "",
    val role: String = "user",
    val content: String = "",
    val category: String = "General Wellness",
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class WellnessRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val waterMl: Long = 0L,
    val sleepHours: Double = 0.0,
    val activityType: String = "Walking",
    val exerciseMinutes: Long = 0L,
    val habitsCompleted: List<String> = emptyList(),
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class HealthReport(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fileName: String = "",
    val fileSizeBytes: Long = 0L,
    val pageCount: Long = 1L,
    val extractedText: String = "",
    val aiSummary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class ReportQuestion(
    @DocumentId val id: String = "",
    val userId: String = "",
    val reportId: String = "",
    val question: String = "",
    val answer: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class SpecialistRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val userName: String = "",
    val category: String = "General Wellness",
    val concern: String = "",
    val status: String = "pending",
    val adminNotes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class WellnessResource(
    @DocumentId val id: String = "",
    val title: String = "",
    val category: String = "",
    val summary: String = "",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

data class AdminDashboardSummary(
    val totalUsers: Int = 0,
    val totalConversations: Int = 0,
    val totalWellnessRecords: Int = 0,
    val totalSpecialistRequests: Int = 0,
    val recentUsers: List<UserProfile> = emptyList(),
    val allRequests: List<SpecialistRequest> = emptyList(),
)

data class WellnessCategory(
    val id: String,
    val title: String,
    val shortTitle: String,
    val description: String,
    val samplePrompt: String,
)

object WellnessCategories {
    val all: List<WellnessCategory> = listOf(
        WellnessCategory(
            id = "nutrition",
            title = "Nutrition and Diet",
            shortTitle = "Nutrition",
            description = "Balanced meals, healthy food swaps, and personalized meal ideas.",
            samplePrompt = "Can you suggest a balanced, nutrient-rich meal plan for today based on my dietary preference?"
        ),
        WellnessCategory(
            id = "weight",
            title = "Healthy Weight Management",
            shortTitle = "Weight",
            description = "Sustainable habits and evidence-informed weight education.",
            samplePrompt = "What sustainable daily habits support healthy weight management without crash dieting?"
        ),
        WellnessCategory(
            id = "fitness",
            title = "Fitness and Physical Activity",
            shortTitle = "Fitness",
            description = "Beginner workouts, walking plans, and daily movement routines.",
            samplePrompt = "Create a gentle 20-minute beginner workout plan tailored to my activity level."
        ),
        WellnessCategory(
            id = "sleep",
            title = "Sleep Management",
            shortTitle = "Sleep",
            description = "Sleep hygiene, calming wind-down routines, and restorative rest.",
            samplePrompt = "Help me design a relaxing 30-minute bedtime wind-down routine for better sleep."
        ),
        WellnessCategory(
            id = "mental",
            title = "Mental Wellness and Stress Management",
            shortTitle = "Stress & Mind",
            description = "Mindfulness, breathing techniques, and everyday stress relief.",
            samplePrompt = "Guide me through a 5-minute breathing and mindfulness exercise to manage daily stress."
        ),
        WellnessCategory(
            id = "hydration",
            title = "Hydration",
            shortTitle = "Hydration",
            description = "Hydration education, fluid balance, and smart water reminders.",
            samplePrompt = "How can I space my water intake throughout the day to hit my hydration target comfortably?"
        ),
        WellnessCategory(
            id = "skincare",
            title = "Skin and Personal Care",
            shortTitle = "Skin Care",
            description = "Simple skincare routines, sun protection, and daily hygiene.",
            samplePrompt = "What is a simple, gentle morning and evening skincare routine for everyday skin health?"
        ),
        WellnessCategory(
            id = "routine",
            title = "Daily Lifestyle and Routine Planning",
            shortTitle = "Daily Routine",
            description = "Morning structure, balanced schedules, and work-life harmony.",
            samplePrompt = "Help me build a balanced daily schedule that includes movement, meals, and rest."
        ),
        WellnessCategory(
            id = "womens",
            title = "Women's Wellness Education",
            shortTitle = "Women's Health",
            description = "Menstrual & reproductive wellness education and care guidance.",
            samplePrompt = "What everyday lifestyle habits support menstrual wellness, and when should someone consult a doctor?"
        ),
        WellnessCategory(
            id = "preventive",
            title = "Preventive Healthcare",
            shortTitle = "Preventive",
            description = "Routine checkup awareness, screenings, and proactive care.",
            samplePrompt = "What routine preventive health checkups and wellness screenings should adults discuss with their clinician?"
        ),
        WellnessCategory(
            id = "habits",
            title = "Healthy Habit Formation",
            shortTitle = "Habits",
            description = "Goal setting, consistency strategies, and positive momentum.",
            samplePrompt = "How do I use habit stacking to stay consistent with my wellness goals this week?"
        ),
    )

    val defaultDailyHabits = listOf(
        "Morning Hydration",
        "Balanced Whole-Food Meal",
        "20+ Min Movement",
        "Mindful Breathing Break",
        "Screen-Free Wind Down",
    )
}
