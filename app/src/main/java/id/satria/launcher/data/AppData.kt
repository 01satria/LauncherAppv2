package id.satria.launcher.data

import android.graphics.drawable.Drawable
import kotlinx.serialization.Serializable

// Pengganti interface AppData di types/index.ts
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
