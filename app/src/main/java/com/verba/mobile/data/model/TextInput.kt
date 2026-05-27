package com.verba.mobile.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TextInputSettings(
    val skill_target: SkillTarget,
    val min_gaps: Int,
    val max_gaps: Int,
    val presentation_format: PresentationFormat,
)

/**
 * One gap inside a text-input task.
 * - [correct_answers] and [explanation] are stripped when reveals are hidden.
 * - [hint] is a base-form placeholder shown in the empty input (e.g. infinitive of the verb the
 *   learner must inflect). Null when the gap tests word choice rather than inflection.
 */
@Serializable
data class TextInputGap(
    val correct_answers: List<String>? = null,
    val explanation: String? = null,
    val hint: String? = null,
)

@Serializable
data class TextInputTaskMeta(
    val skill_target: SkillTarget,
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
data class TextInputUserAnswer(
    val inputs: List<String>,
)

/**
 * Verdict from the AI judge after a text-input appeal. Once set, the appeal flow is closed —
 * the verdict is final and the client never re-issues the request.
 */
@Serializable
data class JudgeVerdict(
    val is_actually_correct: Boolean,
    val reason: String,
    val judged_at: String,
)

@Serializable
data class TextInputTask(
    override val index: Int,
    val stem_template: String,
    val gaps: List<TextInputGap>,
    val meta: TextInputTaskMeta,
    val user_answer: TextInputUserAnswer? = null,
    val is_correct: Boolean? = null,
    override val generatedAt: String,
    override val answeredAt: String? = null,
    val judge_verdict: JudgeVerdict? = null,
) : Task {
    override val isCorrect: Boolean? get() = is_correct
    override val hasUserAnswer: Boolean get() = user_answer != null
}
