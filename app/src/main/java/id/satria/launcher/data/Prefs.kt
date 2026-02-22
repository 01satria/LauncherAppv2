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
    val ICON_SIZE         = intPreferencesKey("icon_size")
    val DOCK_ICON_SIZE    = intPreferencesKey("dock_icon_size")
    val NOTES             = stringPreferencesKey("notes")
    val HABITS            = stringPreferencesKey("habits")
    // ── Theme palette ─────────────────────────────────────────────────────
    val THEME_ACCENT      = stringPreferencesKey("theme_accent")
    val THEME_BG          = stringPreferencesKey("theme_bg")
    val THEME_BORDER      = stringPreferencesKey("theme_border")
    val THEME_FONT        = stringPreferencesKey("theme_font")
}

const val DEFAULT_ICON_SIZE      = 54
const val DEFAULT_DOCK_ICON_SIZE = 56
const val MIN_ICON_SIZE          = 36
const val MAX_ICON_SIZE          = 72
const val MIN_DOCK_ICON_SIZE     = 40
const val MAX_DOCK_ICON_SIZE     = 72

class Prefs(private val context: Context) {

    private val ds   = context.dataStore
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

    private fun <T> decode(s: String?, default: T, block: (String) -> T?): T =
        s?.let { runCatching { block(it) }.getOrNull() } ?: default

    val hiddenPackages   = ds.data.map { decode(it[PrefKeys.HIDDEN_PACKAGES],   emptyList()) { s -> json.decodeFromString<List<String>>(s) } }
    val dockPackages     = ds.data.map { decode(it[PrefKeys.DOCK_PACKAGES],     emptyList()) { s -> json.decodeFromString<List<String>>(s) } }
    val todos            = ds.data.map { decode(it[PrefKeys.TODOS],             emptyList()) { s -> json.decodeFromString<List<TodoItem>>(s) } }
    val countdowns       = ds.data.map { decode(it[PrefKeys.COUNTDOWNS],        emptyList()) { s -> json.decodeFromString<List<CountdownItem>>(s) } }
    val weatherLocations = ds.data.map { decode(it[PrefKeys.WEATHER_LOCATIONS], emptyList()) { s -> json.decodeFromString<List<String>>(s) } }
    val notes            = ds.data.map { decode(it[PrefKeys.NOTES],             emptyList()) { s -> json.decodeFromString<List<NoteItem>>(s) } }
    val habits           = ds.data.map { decode(it[PrefKeys.HABITS],            emptyList()) { s -> json.decodeFromString<List<HabitItem>>(s) } }

    // ── Theme palette — stored as AARRGGBB hex string ──────────────────────
    val themeAccent  = ds.data.map { it[PrefKeys.THEME_ACCENT]  ?: "FF27AE60" }
    val themeBg      = ds.data.map { it[PrefKeys.THEME_BG]      ?: "FF000000" }
    val themeBorder  = ds.data.map { it[PrefKeys.THEME_BORDER]  ?: "FF1A1A1A" }
    val themeFont    = ds.data.map { it[PrefKeys.THEME_FONT]    ?: "FFFFFFFF" }

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
    suspend fun setNotes(v: List<NoteItem>)           = ds.edit { it[PrefKeys.NOTES]             = json.encodeToString(v) }
    suspend fun setHabits(v: List<HabitItem>)         = ds.edit { it[PrefKeys.HABITS]            = json.encodeToString(v) }
    suspend fun setThemeAccent(v: String)                 = ds.edit { it[PrefKeys.THEME_ACCENT]      = v }
    suspend fun setThemeBg(v: String)                     = ds.edit { it[PrefKeys.THEME_BG]          = v }
    suspend fun setThemeBorder(v: String)                 = ds.edit { it[PrefKeys.THEME_BORDER]      = v }
    suspend fun setThemeFont(v: String)                   = ds.edit { it[PrefKeys.THEME_FONT]        = v }
}
