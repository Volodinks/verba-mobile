package com.verba.mobile.ui.errors

import com.verba.mobile.data.api.ApiResult

/**
 * UI-side representation of a failure that's safe to expose from a ViewModel
 * (no Compose dependency). The screen converts this into a localized string via
 * `apiErrorMessage` / `networkErrorMessage`.
 */
sealed interface UiError {
    /** API returned a non-2xx response with optional `error` code in JSON body. */
    data class Api(val status: Int, val code: String?, val fieldsArg: String? = null, val raw: String = "") : UiError

    /** Network/IO/deserialization failure with original throwable for diagnostics. */
    data class Throwable(val cause: kotlin.Throwable) : UiError
}

fun ApiResult.Error.toUiError(): UiError.Api {
    // Best-effort extraction of `details.fields` for missing_required_setting → "{fields}" interpolation.
    val fields = if (code == "missing_required_setting") {
        runCatching {
            val obj = kotlinx.serialization.json.Json.parseToJsonElement(raw) as? kotlinx.serialization.json.JsonObject
            val details = obj?.get("details") as? kotlinx.serialization.json.JsonObject
            val arr = details?.get("fields") as? kotlinx.serialization.json.JsonArray
            arr?.mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }?.joinToString()
        }.getOrNull()
    } else null
    return UiError.Api(status = status, code = code, fieldsArg = fields, raw = raw)
}

fun ApiResult.Network.toUiError(): UiError.Throwable = UiError.Throwable(cause)
