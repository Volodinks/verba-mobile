package com.verba.mobile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
enum class LessonTypeId {
    @SerialName("multiple_choice") MULTIPLE_CHOICE,
    @SerialName("inline_dropdowns") INLINE_DROPDOWNS,
    @SerialName("text_input") TEXT_INPUT,
}

@Serializable
data class Lesson(
    val id: String,
    val folder_id: String,
    val type: LessonTypeId,
    val title: String,
    val description: String? = null,
    /** Server-resolved full universal settings for this lesson. Required fields are filled when present. */
    val universal_settings: UniversalSettings? = null,
    /** Opaque per-type settings; shape depends on [type]. */
    val type_settings: JsonObject,
    val createdBy: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)
