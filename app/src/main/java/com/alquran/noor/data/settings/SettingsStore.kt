package com.alquran.noor.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.alquran.noor.data.Reciters
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    /** 0 = النظام، 1 = فاتح، 2 = داكن */
    val themeMode: Int = 0,
    val dynamicColor: Boolean = true,
    val fontSize: Float = 30f,
    val reciterId: String = Reciters.default.id,
    val showTafsir: Boolean = true,
    val keepScreenOn: Boolean = true,
    val lastSurah: Int = 0,
    val lastAyah: Int = 1,
) {
    val hasLastRead: Boolean get() = lastSurah > 0
}

class SettingsStore(private val context: Context) {

    private object Keys {
        val THEME = intPreferencesKey("theme_mode")
        val DYNAMIC = booleanPreferencesKey("dynamic_color")
        val FONT = floatPreferencesKey("font_size")
        val RECITER = stringPreferencesKey("reciter")
        val TAFSIR = booleanPreferencesKey("show_tafsir")
        val KEEP_ON = booleanPreferencesKey("keep_screen_on")
        val LAST_SURAH = intPreferencesKey("last_surah")
        val LAST_AYAH = intPreferencesKey("last_ayah")
    }

    val settings: Flow<AppSettings> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p ->
            val d = AppSettings()
            AppSettings(
                themeMode = p[Keys.THEME] ?: d.themeMode,
                dynamicColor = p[Keys.DYNAMIC] ?: d.dynamicColor,
                fontSize = p[Keys.FONT] ?: d.fontSize,
                reciterId = p[Keys.RECITER] ?: d.reciterId,
                showTafsir = p[Keys.TAFSIR] ?: d.showTafsir,
                keepScreenOn = p[Keys.KEEP_ON] ?: d.keepScreenOn,
                lastSurah = p[Keys.LAST_SURAH] ?: d.lastSurah,
                lastAyah = p[Keys.LAST_AYAH] ?: d.lastAyah,
            )
        }

    suspend fun setThemeMode(value: Int) = context.dataStore.edit { it[Keys.THEME] = value }
    suspend fun setDynamicColor(value: Boolean) = context.dataStore.edit { it[Keys.DYNAMIC] = value }
    suspend fun setFontSize(value: Float) = context.dataStore.edit { it[Keys.FONT] = value }
    suspend fun setReciter(id: String) = context.dataStore.edit { it[Keys.RECITER] = id }
    suspend fun setShowTafsir(value: Boolean) = context.dataStore.edit { it[Keys.TAFSIR] = value }
    suspend fun setKeepScreenOn(value: Boolean) = context.dataStore.edit { it[Keys.KEEP_ON] = value }

    suspend fun setLastRead(surah: Int, ayah: Int) {
        context.dataStore.edit {
            it[Keys.LAST_SURAH] = surah
            it[Keys.LAST_AYAH] = ayah
        }
    }
}
