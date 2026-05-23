package com.verba.mobile.ui.labels

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.verba.mobile.R
import com.verba.mobile.data.model.DistractorType
import com.verba.mobile.data.model.EnglishVariant
import com.verba.mobile.data.model.ExplanationLanguage
import com.verba.mobile.data.model.FeedbackMode
import com.verba.mobile.data.model.Level
import com.verba.mobile.data.model.LessonTypeId
import com.verba.mobile.data.model.PresentationFormat
import com.verba.mobile.data.model.QuestionStemStyle
import com.verba.mobile.data.model.Register
import com.verba.mobile.data.model.SkillTarget

@Composable
fun label(value: Level): String = stringResource(
    when (value) {
        Level.A1 -> R.string.label_level_a1
        Level.A2 -> R.string.label_level_a2
        Level.B1 -> R.string.label_level_b1
        Level.B2 -> R.string.label_level_b2
        Level.C1 -> R.string.label_level_c1
        Level.C2 -> R.string.label_level_c2
    }
)

@Composable
fun label(value: EnglishVariant): String = stringResource(
    when (value) {
        EnglishVariant.BRITISH -> R.string.label_english_variant_british
        EnglishVariant.AMERICAN -> R.string.label_english_variant_american
    }
)

@Composable
fun label(value: Register): String = stringResource(
    when (value) {
        Register.FORMAL -> R.string.label_register_formal
        Register.NEUTRAL -> R.string.label_register_neutral
        Register.INFORMAL -> R.string.label_register_informal
    }
)

@Composable
fun label(value: ExplanationLanguage): String = stringResource(
    when (value) {
        ExplanationLanguage.ENGLISH -> R.string.label_explanation_language_english
        ExplanationLanguage.UKRAINIAN -> R.string.label_explanation_language_ukrainian
    }
)

@Composable
fun label(value: DistractorType): String = stringResource(
    when (value) {
        DistractorType.EASY -> R.string.label_distractor_easy
        DistractorType.GRAMMATICAL -> R.string.label_distractor_grammatical
        DistractorType.SEMANTIC_CLOSE -> R.string.label_distractor_semantic_close
        DistractorType.COMMON_MISTAKE -> R.string.label_distractor_common_mistake
    }
)

@Composable
fun label(value: SkillTarget): String = stringResource(
    when (value) {
        SkillTarget.VOCABULARY -> R.string.label_skill_vocabulary
        SkillTarget.GRAMMAR -> R.string.label_skill_grammar
        SkillTarget.COLLOCATIONS -> R.string.label_skill_collocations
        SkillTarget.WORD_FORMATION -> R.string.label_skill_word_formation
        SkillTarget.READING_COMPREHENSION -> R.string.label_skill_reading_comprehension
    }
)

@Composable
fun label(value: PresentationFormat): String = stringResource(
    when (value) {
        PresentationFormat.ISOLATED_SENTENCES -> R.string.label_presentation_isolated_sentences
        PresentationFormat.CONNECTED_TEXT -> R.string.label_presentation_connected_text
    }
)

@Composable
fun label(value: QuestionStemStyle): String = stringResource(
    when (value) {
        QuestionStemStyle.GAP_FILL -> R.string.label_stem_gap_fill
        QuestionStemStyle.DIRECT_QUESTION -> R.string.label_stem_direct_question
    }
)

@Composable
fun label(value: FeedbackMode): String = stringResource(
    when (value) {
        FeedbackMode.IMMEDIATE -> R.string.label_feedback_immediate
        FeedbackMode.END -> R.string.label_feedback_end
    }
)

@Composable
fun label(value: LessonTypeId): String = stringResource(
    when (value) {
        LessonTypeId.MULTIPLE_CHOICE -> R.string.label_lesson_type_multiple_choice
    }
)

/**
 * Resolve an enum label by its raw serialized name (e.g. for keys coming from JSON / API
 * where we don't have the typed enum on hand). Returns null if unknown — caller can fall back
 * to the raw string.
 */
@Composable
fun labelForRawDistractor(raw: String): String? = when (raw) {
    "easy" -> stringResource(R.string.label_distractor_easy)
    "grammatical" -> stringResource(R.string.label_distractor_grammatical)
    "semantic_close" -> stringResource(R.string.label_distractor_semantic_close)
    "common_mistake" -> stringResource(R.string.label_distractor_common_mistake)
    else -> null
}

@Composable
fun labelForRawSkill(raw: String): String? = when (raw) {
    "vocabulary" -> stringResource(R.string.label_skill_vocabulary)
    "grammar" -> stringResource(R.string.label_skill_grammar)
    "collocations" -> stringResource(R.string.label_skill_collocations)
    "word_formation" -> stringResource(R.string.label_skill_word_formation)
    "reading_comprehension" -> stringResource(R.string.label_skill_reading_comprehension)
    else -> null
}
