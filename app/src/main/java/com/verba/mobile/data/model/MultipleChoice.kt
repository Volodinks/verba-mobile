package com.verba.mobile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class SkillTarget {
    @SerialName("vocabulary") VOCABULARY,
    @SerialName("grammar") GRAMMAR,
    @SerialName("collocations") COLLOCATIONS,
    @SerialName("word_formation") WORD_FORMATION,
    @SerialName("reading_comprehension") READING_COMPREHENSION,
}

@Serializable
enum class PresentationFormat {
    @SerialName("isolated_sentences") ISOLATED_SENTENCES,
    @SerialName("connected_text") CONNECTED_TEXT,
}

@Serializable
enum class QuestionStemStyle {
    @SerialName("gap_fill") GAP_FILL,
    @SerialName("direct_question") DIRECT_QUESTION,
}

@Serializable
data class MultipleChoiceSettings(
    val skill_target: SkillTarget,
    val options_count: Int,
    val presentation_format: PresentationFormat,
    val question_stem_style: QuestionStemStyle,
)

@Serializable
data class MultipleChoiceTaskMeta(
    val skill_target: SkillTarget,
    val distractor_type: DistractorType,
    val level: Level,
)

@Serializable
data class MultipleChoiceUserAnswer(
    val selected_index: Int,
)

/**
 * Task as returned by GET /api/runs/:id/next-task (correct_index and explanation hidden until answer
 * or completion). Same shape supports the post-answer enriched task too — extra fields just become null.
 */
@Serializable
data class MultipleChoiceTask(
    val index: Int,
    val stem: String,
    val options: List<String>,
    val correct_index: Int? = null,
    val explanation: String? = null,
    val meta: MultipleChoiceTaskMeta,
    val user_answer: MultipleChoiceUserAnswer? = null,
    val is_correct: Boolean? = null,
    val generatedAt: String,
    val answeredAt: String? = null,
)
