package com.friday.assistant.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "friday_preferences")

class PreferencesManager(private val context: Context) {

    companion object {
        val KEY_BACKEND_URL = stringPreferencesKey("backend_url")
        val KEY_VOICE_PITCH = floatPreferencesKey("voice_pitch")
        val KEY_VOICE_RATE = floatPreferencesKey("voice_rate")
        val KEY_VOICE_LANGUAGE = stringPreferencesKey("voice_language")
        val KEY_CONTINUOUS_LISTENING = booleanPreferencesKey("continuous_listening")
        val KEY_WAKE_WORD = booleanPreferencesKey("wake_word")
        val KEY_MASTER_AUTOMATION = booleanPreferencesKey("master_automation")
        val KEY_AI_MODEL = stringPreferencesKey("ai_model")
        val KEY_FORCE_OFFLINE = booleanPreferencesKey("force_offline")
        val KEY_AUTO_FALLBACK = booleanPreferencesKey("auto_fallback")
        val KEY_GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")

        const val DEFAULT_BACKEND_URL = "https://api.friday-assistant.internal/"
        const val DEFAULT_VOICE_LANG = "en-US"
    }

    val backendUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_BACKEND_URL] ?: DEFAULT_BACKEND_URL
    }

    val voicePitch: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_VOICE_PITCH] ?: 1.0f
    }

    val voiceRate: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_VOICE_RATE] ?: 1.0f
    }

    val voiceLanguage: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_VOICE_LANGUAGE] ?: DEFAULT_VOICE_LANG
    }

    val continuousListening: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_CONTINUOUS_LISTENING] ?: false
    }

    val wakeWordEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_WAKE_WORD] ?: true
    }

    val masterAutomation: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_MASTER_AUTOMATION] ?: true
    }

    val selectedModel: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_AI_MODEL] ?: "auto"
    }

    val forceOffline: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_FORCE_OFFLINE] ?: false
    }

    val autoFallback: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_FALLBACK] ?: true
    }

    val geminiApiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_GEMINI_API_KEY] ?: ""
    }

    suspend fun setBackendUrl(url: String) {
        context.dataStore.edit { prefs -> prefs[KEY_BACKEND_URL] = url }
    }

    suspend fun setVoicePitch(pitch: Float) {
        context.dataStore.edit { prefs -> prefs[KEY_VOICE_PITCH] = pitch }
    }

    suspend fun setVoiceRate(rate: Float) {
        context.dataStore.edit { prefs -> prefs[KEY_VOICE_RATE] = rate }
    }

    suspend fun setVoiceLanguage(lang: String) {
        context.dataStore.edit { prefs -> prefs[KEY_VOICE_LANGUAGE] = lang }
    }

    suspend fun setContinuousListening(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_CONTINUOUS_LISTENING] = enabled }
    }

    suspend fun setWakeWordEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_WAKE_WORD] = enabled }
    }

    suspend fun setMasterAutomation(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_MASTER_AUTOMATION] = enabled }
    }

    suspend fun setSelectedModel(model: String) {
        context.dataStore.edit { prefs -> prefs[KEY_AI_MODEL] = model }
    }

    suspend fun setForceOffline(force: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_FORCE_OFFLINE] = force }
    }

    suspend fun setAutoFallback(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_AUTO_FALLBACK] = enabled }
    }

    suspend fun setGeminiApiKey(key: String) {
        context.dataStore.edit { prefs -> prefs[KEY_GEMINI_API_KEY] = key }
    }
}
