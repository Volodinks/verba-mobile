package com.verba.mobile.data.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Polymorphic task hierarchy. The wire format does NOT carry a discriminator on each task;
 * the parent [LessonRun.lesson_type] (or the [Lesson.type] for a freshly fetched task) tells
 * us which subclass to decode.
 *
 * Use [Task.parse] at the boundary between the API layer and the rest of the app.
 */
sealed interface Task {
    val index: Int
    val generatedAt: String
    val answeredAt: String?
    val isCorrect: Boolean?
    val hasUserAnswer: Boolean

    companion object {
        fun parse(lessonType: LessonTypeId, json: Json, element: JsonElement): Task = when (lessonType) {
            LessonTypeId.MULTIPLE_CHOICE ->
                json.decodeFromJsonElement(MultipleChoiceTask.serializer(), element)
            LessonTypeId.INLINE_DROPDOWNS ->
                json.decodeFromJsonElement(InlineDropdownsTask.serializer(), element)
            LessonTypeId.TEXT_INPUT ->
                json.decodeFromJsonElement(TextInputTask.serializer(), element)
        }
    }
}

/**
 * Sealed shape of a user's pending answer; one variant per task type. The view-model
 * holds at most one of these at a time and serialises it into the wire `user_answer` object
 * when the learner submits.
 */
sealed interface PendingAnswer {
    val isComplete: Boolean

    data class MultipleChoice(val selectedIndex: Int?) : PendingAnswer {
        override val isComplete: Boolean get() = selectedIndex != null
    }

    data class InlineDropdowns(val selections: List<Int?>) : PendingAnswer {
        override val isComplete: Boolean get() = selections.all { it != null }
    }

    data class TextInput(val inputs: List<String>) : PendingAnswer {
        override val isComplete: Boolean get() = inputs.all { it.isNotBlank() }
    }
}
