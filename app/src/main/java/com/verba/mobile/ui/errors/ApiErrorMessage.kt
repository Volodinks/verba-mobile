package com.verba.mobile.ui.errors

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.verba.mobile.R

/**
 * Translate an API error (code + optional interpolation arg) into a localized message.
 * Unknown codes fall back to `error_unknown_error`.
 */
@Composable
fun apiErrorMessage(code: String?, fieldsArg: String? = null): String {
    val resId = errorResIdFor(code) ?: R.string.error_unknown_error
    return if (fieldsArg != null && needsArg(code)) stringResource(resId, fieldsArg)
    else stringResource(resId)
}

/** Render a UiError into a localized string. */
@Composable
fun uiErrorMessage(error: UiError): String = when (error) {
    is UiError.Api -> {
        val code = error.code
        if (code != null) apiErrorMessage(code, error.fieldsArg)
        else stringResource(R.string.error_http_generic, error.status)
    }
    is UiError.Throwable -> stringResource(
        R.string.error_network,
        error.cause.message ?: error.cause::class.simpleName ?: "",
    )
}

private fun needsArg(code: String?): Boolean = code == "missing_required_setting"

private fun errorResIdFor(code: String?): Int? = when (code) {
    "missing_token" -> R.string.error_missing_token
    "invalid_token" -> R.string.error_invalid_token
    "no_email" -> R.string.error_no_email
    "not_allowed" -> R.string.error_not_allowed
    "admin_required" -> R.string.error_admin_required
    "invalid_input" -> R.string.error_invalid_input
    "invalid_email" -> R.string.error_invalid_email
    "invalid_role" -> R.string.error_invalid_role
    "invalid_type_settings" -> R.string.error_invalid_type_settings
    "invalid_lesson_type_settings" -> R.string.error_invalid_lesson_type_settings
    "missing_required_setting" -> R.string.error_missing_required_setting
    "task_count_out_of_range" -> R.string.error_task_count_out_of_range
    "options_count_5_requires_c1_c2" -> R.string.error_options_count_5_requires_c1_c2
    "prompt_required" -> R.string.error_prompt_required
    "prompt_too_long" -> R.string.error_prompt_too_long
    "folder_not_found" -> R.string.error_folder_not_found
    "lesson_not_found" -> R.string.error_lesson_not_found
    "run_not_found" -> R.string.error_run_not_found
    "task_not_found" -> R.string.error_task_not_found
    "email_not_found" -> R.string.error_email_not_found
    "cycle_blocked" -> R.string.error_cycle_blocked
    "folder_not_empty" -> R.string.error_folder_not_empty
    "email_already_allowed" -> R.string.error_email_already_allowed
    "cannot_remove_self" -> R.string.error_cannot_remove_self
    "run_already_finished" -> R.string.error_run_already_finished
    "all_tasks_generated" -> R.string.error_all_tasks_generated
    "already_answered" -> R.string.error_already_answered
    "already_judged" -> R.string.error_already_judged
    "task_not_answered" -> R.string.error_task_not_answered
    "task_already_correct" -> R.string.error_task_already_correct
    "judge_unsupported_for_type" -> R.string.error_judge_unsupported_for_type
    "invalid_answer" -> R.string.error_invalid_answer
    "folder_id_required" -> R.string.error_folder_id_required
    "invalid_index_gap" -> R.string.error_invalid_index_gap
    "invalid_index_param" -> R.string.error_invalid_index_param
    "invalid_selected_index" -> R.string.error_invalid_selected_index
    "user_never_signed_in" -> R.string.error_user_never_signed_in
    "rate_limited" -> R.string.error_rate_limited
    "generation_failed" -> R.string.error_generation_failed
    "timeout" -> R.string.error_timeout
    "upstream_failed" -> R.string.error_upstream_failed
    "openai_not_configured" -> R.string.error_openai_not_configured
    "internal" -> R.string.error_internal
    "failed" -> R.string.error_failed
    "parse_error" -> R.string.error_parse
    "unknown_error" -> R.string.error_unknown_error
    else -> null
}
