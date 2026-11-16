package org.cemetery.app.data
import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
private val Context.preferences by preferencesDataStore("settings")
data class Settings(val language: String = "system", val theme: String = "system", val reminders: Boolean = false, val leadMinutes: Int = 60, val style: String = "neutral")
@Singleton class Preferences @Inject constructor(@ApplicationContext private val context: Context) {
    val settings: Flow<Settings> = context.preferences.data.map { p -> Settings(p[stringPreferencesKey("language")] ?: "system", p[stringPreferencesKey("theme")] ?: "system", p[booleanPreferencesKey("reminders")] ?: false, p[intPreferencesKey("lead")] ?: 60, p[stringPreferencesKey("style")] ?: "neutral") }
    suspend fun text(key: String, value: String) { context.preferences.edit { it[stringPreferencesKey(key)] = value } }
    suspend fun reminders(value: Boolean) { context.preferences.edit { it[booleanPreferencesKey("reminders")] = value } }
    suspend fun lead(value: Int) { context.preferences.edit { it[intPreferencesKey("lead")] = value } }
}
