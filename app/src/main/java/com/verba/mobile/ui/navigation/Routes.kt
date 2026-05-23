package com.verba.mobile.ui.navigation

object Routes {
    const val FOLDER_TREE = "folders"
    const val LESSON_DETAIL = "lessons/{lessonId}"
    const val RUN_SETUP = "lessons/{lessonId}/setup"
    const val RUN_PLAY = "runs/{runId}/play"
    const val RUN_RESULT = "runs/{runId}/result"

    fun lessonDetail(id: String) = "lessons/$id"
    fun runSetup(lessonId: String) = "lessons/$lessonId/setup"
    fun runPlay(runId: String) = "runs/$runId/play"
    fun runResult(runId: String) = "runs/$runId/result"

    const val ARG_LESSON_ID = "lessonId"
    const val ARG_RUN_ID = "runId"
}
