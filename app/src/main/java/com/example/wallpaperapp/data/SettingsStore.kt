package com.example.wallpaperapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "wallpaper_settings")

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val hapticsEnabledKey = booleanPreferencesKey("haptics_enabled")
    private val dynamicColorsEnabledKey = booleanPreferencesKey("dynamic_colors_enabled")
    private val trustedExtensionsKey = stringSetPreferencesKey("trusted_extensions")
    private val extensionRepositoriesKey = stringSetPreferencesKey("extension_repositories")

    val hapticsEnabledFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[hapticsEnabledKey] != false
    }

    val dynamicColorsEnabledFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[dynamicColorsEnabledKey] != false
    }

    val trustedExtensionsFlow: Flow<Set<String>> = context.settingsDataStore.data.map { prefs ->
        prefs[trustedExtensionsKey] ?: emptySet()
    }

    val extensionRepositoriesFlow: Flow<Set<String>> = context.settingsDataStore.data.map { prefs ->
        prefs[extensionRepositoriesKey] ?: emptySet()
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[hapticsEnabledKey] = enabled
        }
    }

    suspend fun setDynamicColorsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[dynamicColorsEnabledKey] = enabled
        }
    }

    suspend fun setTrustedExtension(sourceId: String, trusted: Boolean) {
        context.settingsDataStore.edit { prefs ->
            val next = (prefs[trustedExtensionsKey] ?: emptySet()).toMutableSet()
            if (trusted) next += sourceId else next -= sourceId
            prefs[trustedExtensionsKey] = next
        }
    }

    suspend fun addRepository(url: String) {
        context.settingsDataStore.edit { prefs ->
            val next = (prefs[extensionRepositoriesKey] ?: emptySet()).toMutableSet()
            next += url
            prefs[extensionRepositoriesKey] = next
        }
    }
}
