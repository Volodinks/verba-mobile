package com.verba.mobile.data.model

import kotlinx.serialization.Serializable

@Serializable
data class InlineDropdownsSettings(
    val skill_target: SkillTarget,
    val min_gaps: Int,
    val max_gaps: Int,
    val options_per_gap: Int,
    val presentation_format: PresentationFormat,
)

/**
 * One gap inside an inline-dropdowns task. Reveal fields (correct_index, explanation) are absent
 * when the visibility policy hides them — both are nullable.
 */
@Serializable
data class InlineDropdownsGap(
    val options: List<String>,
    val correct_index: Int? = null,
    val explanation: String? = null,
)

@Serializable
data class InlineDropdownsTaskMeta(
    val skill_target: SkillTarget,
    val distractor_type: DistractorType,
    val level: Level,
    val gap_count: Int,
    val tense: EnglishTense? = null,
    val conditional: Conditional? = null,
    val sentence_type: SentenceType? = null,
    val validated_sentence: String? = null,
    val translation_uk: String? = null,
    val pipeline_attempts: Int? = null,
)

@Serializable
data class InlineDropdownsUserAnswer(
    val selections: List<Int>,
)

@Serializable
data class InlineDropdownsTask(
    override val index: Int,
    val stem_template: String,
    val gaps: List<InlineDropdownsGap>,
    val meta: InlineDropdownsTaskMeta,
    val user_answer: InlineDropdownsUserAnswer? = null,
    val is_correct: Boolean? = null,
    override val generatedAt: String,
    override val answeredAt: String? = null,
) : Task {
    override val isCorrect: Boolean? get() = is_correct
    override val hasUserAnswer: Boolean get() = user_answer != null
}
