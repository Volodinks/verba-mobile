package com.verba.mobile.ui.navigation

object Routes {
    const val LESSON_LIST = "lessons"
    const val LESSON_DETAIL = "lessons/{lessonId}"
    fun lessonDetail(id: String) = "lessons/$id"
    const val ARG_LESSON_ID = "lessonId"
}
