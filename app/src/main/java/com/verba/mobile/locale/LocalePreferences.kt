package com.verba.mobile.locale

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.localeDataStore by preferencesDataStore(name = "verba_locale")
private val KEY = stringPreferencesKey("verba.ui_locale")

class LocalePreferences(private val context: Context) {

    /** Emits the saved locale tag (`"uk"`/`"en"`) or null if user has not chosen yet. */
    val savedLocale: Flow<String?> = context.localeDataStore.data.map { it[KEY] }

    suspend fun get(): String? = savedLocale.first()

    suspend fun set(tag: String?) {
        context.localeDataStore.edit { prefs ->
            if (tag == null) prefs.remove(KEY) else prefs[KEY] = tag
        }
    }
}
