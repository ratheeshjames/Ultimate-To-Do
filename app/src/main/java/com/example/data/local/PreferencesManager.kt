package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ultimate_todo_prefs")

class PreferencesManager(private val context: Context) {

    companion object {
        private val KEY_CURRENT_LIST = stringPreferencesKey("cur_list_id")
        private val KEY_THEME = stringPreferencesKey("theme")
        private val KEY_ACCENT = stringPreferencesKey("accent")
        private val KEY_DENSE = booleanPreferencesKey("dense")
        private val KEY_SOUND = booleanPreferencesKey("sound")
        private val KEY_WEEK_START = intPreferencesKey("week_start")
        private val KEY_DATE_FMT = stringPreferencesKey("date_fmt")
        private val KEY_SHOW_TIPS = booleanPreferencesKey("show_tips")
        private val KEY_TOURED = booleanPreferencesKey("toured")
    }

    val currentListIdFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_CURRENT_LIST] ?: "l1"
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            theme = prefs[KEY_THEME] ?: "auto",
            accent = prefs[KEY_ACCENT] ?: "#6366f1",
            dense = prefs[KEY_DENSE] ?: false,
            sound = prefs[KEY_SOUND] ?: false,
            weekStart = prefs[KEY_WEEK_START] ?: 1,
            dateFormatLocale = prefs[KEY_DATE_FMT] ?: "en-US",
            showTips = prefs[KEY_SHOW_TIPS] ?: true,
            toured = prefs[KEY_TOURED] ?: false
        )
    }

    suspend fun setCurrentListId(listId: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CURRENT_LIST] = listId
        }
    }

    suspend fun updateSettings(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME] = settings.theme
            prefs[KEY_ACCENT] = settings.accent
            prefs[KEY_DENSE] = settings.dense
            prefs[KEY_SOUND] = settings.sound
            prefs[KEY_WEEK_START] = settings.weekStart
            prefs[KEY_DATE_FMT] = settings.dateFormatLocale
            prefs[KEY_SHOW_TIPS] = settings.showTips
            prefs[KEY_TOURED] = settings.toured
        }
    }

    suspend fun markTourCompleted() {
        context.dataStore.edit { prefs ->
            prefs[KEY_TOURED] = true
        }
    }
}
