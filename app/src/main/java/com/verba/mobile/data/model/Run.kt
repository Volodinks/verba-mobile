package com.verba.mobile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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
 * Server-side LessonRun document. [tasks] stays as raw JSON because shape depends on [lesson_type];
 * use [parsedTasks] to get the typed [Task] hierarchy.
 *
 * [task_count] is nullable — null means "open-ended: the learner ends the run manually".
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
    val task_count: Int? = null,
    val status: LessonRunStatus,
    val tasks: List<JsonObject> = emptyList(),
    val statistics: Statistics? = null,
    val startedAt: String,
    val completedAt: String? = null,
) {
    fun parsedTasks(json: Json): List<Task> =
        tasks.map { Task.parse(lesson_type, json, it) }
}
