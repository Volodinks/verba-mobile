package com.verba.mobile.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Folder(
    val id: String,
    val parent_id: String? = null,
    /**
     * Denormalised ancestor chain from root → self (inclusive). Always ends with [id].
     * Powers the breadcrumb without N+1 lookups across separate folder docs.
     */
    val path: List<String> = emptyList(),
    val name: String,
    val position: Double = 0.0,
    val createdBy: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    /** Recursive count of lessons in this folder and its descendants, computed by the server. */
    val lesson_count: Int? = null,
)
