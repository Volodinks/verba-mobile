package com.verba.mobile.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Folder(
    val id: String,
    val parent_id: String? = null,
    val name: String,
    val position: Double = 0.0,
    val universal_settings: UniversalSettings? = null,
    val createdBy: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val lesson_count: Int? = null,
)
