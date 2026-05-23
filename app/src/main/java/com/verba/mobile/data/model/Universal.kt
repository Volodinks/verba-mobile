package com.verba.mobile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Level {
    @SerialName("A1") A1,
    @SerialName("A2") A2,
    @SerialName("B1") B1,
    @SerialName("B2") B2,
    @SerialName("C1") C1,
    @SerialName("C2") C2,
}

@Serializable
enum class EnglishVariant {
    @SerialName("british") BRITISH,
    @SerialName("american") AMERICAN,
}

@Serializable
enum class Register {
    @SerialName("formal") FORMAL,
    @SerialName("neutral") NEUTRAL,
    @SerialName("informal") INFORMAL,
}

@Serializable
enum class ExplanationLanguage {
    @SerialName("english") ENGLISH,
    @SerialName("ukrainian") UKRAINIAN,
}

@Serializable
enum class DistractorType {
    @SerialName("easy") EASY,
    @SerialName("grammatical") GRAMMATICAL,
    @SerialName("semantic_close") SEMANTIC_CLOSE,
    @SerialName("common_mistake") COMMON_MISTAKE,
}

@Serializable
data class UniversalSettings(
    val level: Level? = null,
    val topic: String? = null,
    val custom_vocabulary: List<String>? = null,
    val grammar_focus: String? = null,
    val english_variant: EnglishVariant? = null,
    val register: Register? = null,
    val explanation_enabled: Boolean? = null,
    val explanation_language: ExplanationLanguage? = null,
    val interests: List<String>? = null,
    val distractor_types: List<DistractorType>? = null,
)

@Serializable
data class EffectiveSettings(
    val level: Level,
    val english_variant: EnglishVariant,
    val register: Register,
    val explanation_language: ExplanationLanguage,
    val distractor_types: List<DistractorType>,
    val topic: String? = null,
    val custom_vocabulary: List<String>? = null,
    val grammar_focus: String? = null,
    val explanation_enabled: Boolean? = null,
    val interests: List<String>? = null,
)

val REQUIRED_SETTINGS_KEYS = listOf(
    "level", "english_variant", "register", "explanation_language", "distractor_types",
)
