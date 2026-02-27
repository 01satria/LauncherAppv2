package id.satria.launcher.data

import kotlinx.serialization.Serializable

// ── Money Management ──────────────────────────────────────────────────────────
@Serializable
data class MoneyTransaction(
    val id         : String,
    val walletId   : String,
    val type       : String,       // "expense" | "income" | "transfer"
    val amount     : Double,
    val categoryKey: String,
    val note       : String,
    val date       : String,       // "yyyy-MM-dd"
    val toWalletId : String = "",  // for transfers
)

@Serializable
data class MoneyWallet(
    val id       : String,
    val name     : String,
    val emoji    : String,
    val currency : String = "IDR",
    val color    : String = "#27AE60",
)

// icon Drawable tidak disimpan di AppData untuk mencegah kebocoran memori.
// Bitmap sudah di-pre-cache di LauncherRepository via iconCache (LruCache RGB_565).
data class AppData(
    val label: String,
    val packageName: String,
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
