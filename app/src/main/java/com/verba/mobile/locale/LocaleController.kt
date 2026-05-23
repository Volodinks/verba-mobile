package com.verba.mobile.locale

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/** Supported UI locales (matches xml/locales_config). */
val SUPPORTED_LOCALES: List<String> = listOf("uk", "en")
const val DEFAULT_LOCALE: String = "uk"

object LocaleController {

    fun resolveStartupLocale(saved: String?, systemLanguage: String): String {
        if (saved != null && saved in SUPPORTED_LOCALES) return saved
        if (systemLanguage in SUPPORTED_LOCALES) return systemLanguage
        return DEFAULT_LOCALE
    }

    /**
     * Apply locale. On API 33+ uses platform LocaleManager directly (works across vendors,
     * including Samsung, where AppCompatDelegate's locale-managing layer needs extra wiring).
     * On older API levels falls back to AppCompat.
     */
    fun apply(context: Context, tag: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val lm = context.getSystemService(LocaleManager::class.java)
            lm?.applicationLocales = LocaleList.forLanguageTags(tag)
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        }
    }

    /** Currently active app locale tag or null. */
    fun current(context: Context): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val lm = context.getSystemService(LocaleManager::class.java) ?: return null
            val list = lm.applicationLocales
            if (list.isEmpty) return null
            return list.get(0)?.language
        }
        val list = AppCompatDelegate.getApplicationLocales()
        if (list.isEmpty) return null
        return list.get(0)?.language
    }
}
