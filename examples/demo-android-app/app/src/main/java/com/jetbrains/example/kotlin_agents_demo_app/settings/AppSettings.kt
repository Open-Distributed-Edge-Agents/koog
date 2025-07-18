package com.jetbrains.example.kotlin_agents_demo_app.settings

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Define the DataStore at the app level
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// Data stored in the settings
data class AppSettingsData(
    val openAiToken: String,
    val anthropicToken: String,
    val liteRtModelPath: String
)

/**
 * Class to handle settings interaction
 */
class AppSettings(val application: Application) {

    // Define keys for the preferences
    companion object {
        val OPENAI_TOKEN_KEY = stringPreferencesKey("openai_token")
        val ANTHROPIC_TOKEN_KEY = stringPreferencesKey("anthropic_token")
        val LITERT_MODEL_PATH_KEY = stringPreferencesKey("litert_model_path")
    }


    suspend fun getCurrentSettings(): AppSettingsData {
        return application.settingsDataStore.data.map { preferences ->
            AppSettingsData(
                openAiToken = preferences[OPENAI_TOKEN_KEY].orEmpty(),
                anthropicToken = preferences[ANTHROPIC_TOKEN_KEY].orEmpty(),
                liteRtModelPath = preferences[LITERT_MODEL_PATH_KEY].orEmpty()
            )
        }.first()
    }

    suspend fun setCurrentSettings(settings: AppSettingsData) {
        application.settingsDataStore.edit { preferences ->
            preferences[OPENAI_TOKEN_KEY] = settings.openAiToken
            preferences[ANTHROPIC_TOKEN_KEY] = settings.anthropicToken
            preferences[LITERT_MODEL_PATH_KEY] = settings.liteRtModelPath
        }
    }
}