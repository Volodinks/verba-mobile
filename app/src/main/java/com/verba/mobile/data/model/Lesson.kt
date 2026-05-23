package com.verba.mobile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
enum class LessonTypeId {
    @SerialName("multiple_choice") MULTIPLE_CHOICE,
}

@Serializable
data class Lesson(
    val id: String,
    val folder_id: String,
    val type: LessonTypeId,
    val title: String,
    val description: String? = null,
    val universal_override: UniversalSettings? = null,
    /** Opaque per-type settings; parsed by the matching engine on the mobile side. */
    val type_settings: JsonObject,
    val createdBy: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)
