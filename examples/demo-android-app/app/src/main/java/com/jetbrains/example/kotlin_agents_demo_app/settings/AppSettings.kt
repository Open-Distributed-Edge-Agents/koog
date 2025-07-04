package com.jetbrains.example.kotlin_agents_demo_app.settings

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
    val selectedProvider: String = PROVIDER_OPENAI, // Default provider
    val liteRTModelId: String = "", // e.g., LiteRTModels.Gemma3n.E2B.id
    val liteRTModelPath: String = ""
)

/**
 * Class to handle settings interaction
 */
class AppSettings(val context: Context) { // Made context public

    // Define keys for the preferences
    companion object {
        const val PROVIDER_OPENAI = "OpenAI"
        const val PROVIDER_LITERT = "LiteRT"
        // Add PROVIDER_ANTHROPIC if it becomes a selectable option

        val OPENAI_TOKEN_KEY = stringPreferencesKey("openai_token")
        val ANTHROPIC_TOKEN_KEY = stringPreferencesKey("anthropic_token") // Keep for now, though not selectable yet
        val SELECTED_PROVIDER_KEY = stringPreferencesKey("selected_provider")
        val LITERT_MODEL_ID_KEY = stringPreferencesKey("litert_model_id")
        val LITERT_MODEL_PATH_KEY = stringPreferencesKey("litert_model_path")
    }


    suspend fun getCurrentSettings(): AppSettingsData {
        return context.settingsDataStore.data.map { preferences ->
            AppSettingsData(
                openAiToken = preferences[OPENAI_TOKEN_KEY].orEmpty(),
                anthropicToken = preferences[ANTHROPIC_TOKEN_KEY].orEmpty(),
                selectedProvider = preferences[SELECTED_PROVIDER_KEY] ?: PROVIDER_OPENAI,
                liteRTModelId = preferences[LITERT_MODEL_ID_KEY].orEmpty(),
                liteRTModelPath = preferences[LITERT_MODEL_PATH_KEY].orEmpty()
            )
        }.first()
    }

    suspend fun setCurrentSettings(settings: AppSettingsData) {
        context.settingsDataStore.edit { preferences ->
            preferences[OPENAI_TOKEN_KEY] = settings.openAiToken
            preferences[ANTHROPIC_TOKEN_KEY] = settings.anthropicToken // Keep for now
            preferences[SELECTED_PROVIDER_KEY] = settings.selectedProvider
            preferences[LITERT_MODEL_ID_KEY] = settings.liteRTModelId
            preferences[LITERT_MODEL_PATH_KEY] = settings.liteRTModelPath
        }
    }
}