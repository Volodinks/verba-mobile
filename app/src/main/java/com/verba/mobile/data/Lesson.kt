package com.verba.mobile.data

data class Lesson(
    val id: String = "",
    val ownerUid: String = "",
    val body: String = "",
    val createdBy: String? = null,
    val createdAt: String? = null,
)
