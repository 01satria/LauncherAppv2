package id.satria.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore("satria_prefs")

object PrefKeys {
    val USER_NAME         = stringPreferencesKey("user_name")
    val ASSISTANT_NAME    = stringPreferencesKey("assistant_name")
    val HIDDEN_PACKAGES   = stringPreferencesKey("hidden_packages")
    val DOCK_PACKAGES     = stringPreferencesKey("dock_packages")
    val SHOW_HIDDEN       = booleanPreferencesKey("show_hidden")
    val SHOW_NAMES        = booleanPreferencesKey("show_names")
    val LAYOUT_MODE       = stringPreferencesKey("layout_mode")
    val AVATAR_PATH       = stringPreferencesKey("avatar_path")
    val TODOS             = stringPreferencesKey("todos")
    val COUNTDOWNS        = stringPreferencesKey("countdowns")
    val WEATHER_LOCATIONS = stringPreferencesKey("weather_locations")
    // ── Icon size (stored as Int dp) ──────────────────────────────────────
    val ICON_SIZE         = intPreferencesKey("icon_size")       // home screen icon
    val DOCK_ICON_SIZE    = intPreferencesKey("dock_icon_size")  // dock icon
}

// Default icon sizes (dp)
const val DEFAULT_ICON_SIZE      = 54
const val DEFAULT_DOCK_ICON_SIZE = 56
const val MIN_ICON_SIZE          = 36
const val MAX_ICON_SIZE          = 72
const val MIN_DOCK_ICON_SIZE     = 40
const val MAX_DOCK_ICON_SIZE     = 72

class Prefs(private val context: Context) {

    private val ds = context.dataStore
    private val json = Json { ignoreUnknownKeys = true }

    // ── Read flows ─────────────────────────────────────────────────────────
    val userName         = ds.data.map { it[PrefKeys.USER_NAME] ?: "User" }
    val assistantName    = ds.data.map { it[PrefKeys.ASSISTANT_NAME] ?: "Assistant" }
    val showHidden       = ds.data.map { it[PrefKeys.SHOW_HIDDEN] ?: false }
    val showNames        = ds.data.map { it[PrefKeys.SHOW_NAMES] ?: true }
    val layoutMode       = ds.data.map { it[PrefKeys.LAYOUT_MODE] ?: "grid" }
    val avatarPath       = ds.data.map { it[PrefKeys.AVATAR_PATH] }
    val iconSize         = ds.data.map { it[PrefKeys.ICON_SIZE] ?: DEFAULT_ICON_SIZE }
    val dockIconSize     = ds.data.map { it[PrefKeys.DOCK_ICON_SIZE] ?: DEFAULT_DOCK_ICON_SIZE }

    val hiddenPackages  = ds.data.map {
        it[PrefKeys.HIDDEN_PACKAGES]
            ?.let { s -> runCatching { json.decodeFromString<List<String>>(s) }.getOrNull() }
            ?: emptyList()
    }
    val dockPackages    = ds.data.map {
        it[PrefKeys.DOCK_PACKAGES]
            ?.let { s -> runCatching { json.decodeFromString<List<String>>(s) }.getOrNull() }
            ?: emptyList()
    }
    val todos           = ds.data.map {
        it[PrefKeys.TODOS]
            ?.let { s -> runCatching { json.decodeFromString<List<TodoItem>>(s) }.getOrNull() }
            ?: emptyList()
    }
    val countdowns      = ds.data.map {
        it[PrefKeys.COUNTDOWNS]
            ?.let { s -> runCatching { json.decodeFromString<List<CountdownItem>>(s) }.getOrNull() }
            ?: emptyList()
    }
    val weatherLocations = ds.data.map {
        it[PrefKeys.WEATHER_LOCATIONS]
            ?.let { s -> runCatching { json.decodeFromString<List<String>>(s) }.getOrNull() }
            ?: emptyList()
    }

    // ── Write helpers ──────────────────────────────────────────────────────
    suspend fun setUserName(v: String)      = ds.edit { it[PrefKeys.USER_NAME] = v }
    suspend fun setAssistantName(v: String) = ds.edit { it[PrefKeys.ASSISTANT_NAME] = v }
    suspend fun setShowHidden(v: Boolean)   = ds.edit { it[PrefKeys.SHOW_HIDDEN] = v }
    suspend fun setShowNames(v: Boolean)    = ds.edit { it[PrefKeys.SHOW_NAMES] = v }
    suspend fun setLayoutMode(v: String)    = ds.edit { it[PrefKeys.LAYOUT_MODE] = v }
    suspend fun setAvatarPath(v: String)    = ds.edit { it[PrefKeys.AVATAR_PATH] = v }
    suspend fun setIconSize(v: Int)         = ds.edit { it[PrefKeys.ICON_SIZE] = v }
    suspend fun setDockIconSize(v: Int)     = ds.edit { it[PrefKeys.DOCK_ICON_SIZE] = v }

    suspend fun setHiddenPackages(v: List<String>)   = ds.edit { it[PrefKeys.HIDDEN_PACKAGES]   = json.encodeToString(v) }
    suspend fun setDockPackages(v: List<String>)     = ds.edit { it[PrefKeys.DOCK_PACKAGES]     = json.encodeToString(v) }
    suspend fun setTodos(v: List<TodoItem>)           = ds.edit { it[PrefKeys.TODOS]             = json.encodeToString(v) }
    suspend fun setCountdowns(v: List<CountdownItem>) = ds.edit { it[PrefKeys.COUNTDOWNS]       = json.encodeToString(v) }
    suspend fun setWeatherLocations(v: List<String>)  = ds.edit { it[PrefKeys.WEATHER_LOCATIONS] = json.encodeToString(v) }
}