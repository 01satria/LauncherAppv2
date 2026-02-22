package id.satria.launcher.data

import android.graphics.drawable.Drawable
import kotlinx.serialization.Serializable

data class AppData(
    val label: String,
    val packageName: String,
    val icon: Drawable,
)

@Serializable
data class TodoItem(
    val id: String,
    val text: String,
    val done: Boolean,
)

@Serializable
data class CountdownItem(
    val id: String,
    val name: String,
    val targetDate: String, // ISO string
)

@Serializable
data class WeatherForecast(
    val label: String,
    val temp: Int,
    val icon: String,
    val pop: Int,
    val isNow: Boolean,
)

data class WeatherResult(
    val city: String,
    val temp: Int,
    val desc: String,
    val wind: Int,
    val humidity: Int,
    val icon: String,
    val rawQuery: String,
    val forecast: List<WeatherForecast>,
)

// ── Notes ─────────────────────────────────────────────────────────────────────
@Serializable
data class NoteItem(
    val id        : String,
    val text      : String,
    val createdAt : Long,
    val updatedAt : Long,
)

// ── Habits ────────────────────────────────────────────────────────────────────
@Serializable
data class HabitItem(
    val id          : String,
    val name        : String,
    val emoji       : String,
    val doneDates   : List<String>, // "yyyy-MM-dd" strings
    val streak      : Int,
    val createdAt   : Long,
) {
    fun doneToday(todayKey: String): Boolean = doneDates.contains(todayKey)
}

// ── Home layout items ─────────────────────────────────────────────────────────
// type: "app" | "widget_clock" | "widget_date" | "widget_battery" | "widget_steps"
@Serializable
data class HomeItemData(
    val id       : String,
    val type     : String,          // "app" or "widget_*"
    val pkg      : String = "",     // only for type=="app"
    val row      : Int    = 0,
    val col      : Int    = 0,
    val spanCols : Int    = 1,      // 1..4
    val spanRows : Int    = 1,
)
