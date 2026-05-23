package com.verba.mobile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
enum class FeedbackMode {
    @SerialName("immediate") IMMEDIATE,
    @SerialName("end") END,
}

@Serializable
enum class LessonRunStatus {
    @SerialName("pending") PENDING,
    @SerialName("in_progress") IN_PROGRESS,
    @SerialName("completed") COMPLETED,
    @SerialName("abandoned") ABANDONED,
}

@Serializable
data class SkillBreakdownEntry(
    val total: Int,
    val correct: Int,
    val accuracy: Double,
)

@Serializable
data class StatisticsMistake(
    val index: Int,
    val stem: String,
    val user_selected_text: String,
    val correct_text: String,
    val explanation: String? = null,
)

@Serializable
data class Statistics(
    val total_tasks: Int,
    val correct: Int,
    val incorrect: Int,
    val accuracy: Double,
    val avg_time_per_task_ms: Double,
    val total_time_ms: Double,
    val breakdown_by_skill: Map<String, SkillBreakdownEntry> = emptyMap(),
    val breakdown_by_distractor: Map<String, SkillBreakdownEntry> = emptyMap(),
    val mistakes: List<StatisticsMistake> = emptyList(),
)

/**
 * Server-side LessonRun document. `tasks` is opaque JSON because shape depends on `lesson_type`
 * (currently always MultipleChoiceTask). Parse to MultipleChoiceTask in the client when type matches.
 */
@Serializable
data class LessonRun(
    val id: String,
    val lesson_id: String,
    val lesson_type: LessonTypeId,
    val user_uid: String,
    val effective_settings: EffectiveSettings,
    val type_settings: JsonObject,
    val feedback_mode: FeedbackMode,
    val task_count: Int,
    val status: LessonRunStatus,
    val tasks: List<JsonObject> = emptyList(),
    val statistics: Statistics? = null,
    val startedAt: String,
    val completedAt: String? = null,
)
