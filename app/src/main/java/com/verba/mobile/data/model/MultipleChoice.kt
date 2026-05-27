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
    val tense: EnglishTense? = null,
    val conditional: Conditional? = null,
    val sentence_type: SentenceType? = null,
    val validated_sentence: String? = null,
    val translation_uk: String? = null,
    val pipeline_attempts: Int? = null,
    val question_form: QuestionStemStyle? = null,
    val assembled_sentence: String? = null,
)

@Serializable
data class MultipleChoiceUserAnswer(
    val selected_index: Int,
)

/**
 * Task as returned by POST /api/runs/:id/next-task. Reveal fields (correct_index, explanation, certain
 * meta keys) are stripped server-side when the visibility policy hides them — both are nullable.
 */
@Serializable
data class MultipleChoiceTask(
    override val index: Int,
    val stem: String,
    val options: List<String>,
    val correct_index: Int? = null,
    val explanation: String? = null,
    val meta: MultipleChoiceTaskMeta,
    val user_answer: MultipleChoiceUserAnswer? = null,
    val is_correct: Boolean? = null,
    override val generatedAt: String,
    override val answeredAt: String? = null,
) : Task {
    override val isCorrect: Boolean? get() = is_correct
    override val hasUserAnswer: Boolean get() = user_answer != null
}
