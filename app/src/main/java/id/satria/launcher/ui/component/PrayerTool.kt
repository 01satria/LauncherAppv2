package id.satria.launcher.ui.component

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.satria.launcher.ui.theme.SatriaColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

// ── Data ──────────────────────────────────────────────────────────────────────
private data class PrayerTimes(
    val city      : String,
    val country   : String,
    val date      : String,
    val fajr      : String,
    val sunrise   : String,
    val dhuhr     : String,
    val asr       : String,
    val maghrib   : String,
    val isha      : String,
)

// ── HTTP ──────────────────────────────────────────────────────────────────────
private fun fetchPrayer(citySlug: String): PrayerTimes {
    val slug = citySlug.trim().lowercase()
        .replace(" ", "_")
        .replace(Regex("[^a-z0-9_]"), "")
    val url  = "https://muslimsalat.com/$slug.json"
    val conn = URL(url).openConnection() as HttpURLConnection
    val raw  = try {
        conn.apply {
            connectTimeout = 15_000
            readTimeout    = 15_000
            requestMethod  = "GET"
            setRequestProperty("User-Agent", "SatriaLauncher/1.0 Android")
        }
        conn.connect()
        if (conn.responseCode !in 200..299) throw Exception("HTTP ${conn.responseCode}")
        BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }
    } finally {
        conn.disconnect()
    }

    val j     = JSONObject(raw)
    val title = j.optString("title", citySlug)
    val country = j.optString("country", "")
    val items = j.getJSONArray("items")
    val today = items.getJSONObject(0)
    val dateParts = j.optJSONObject("query")?.optString("for", "") ?: ""

    fun t(key: String): String {
        val raw = today.optString(key, "--:--")
        // convert "6:15 AM" → "06:15" 24h
        return try {
            val sdf12 = SimpleDateFormat("h:mm a", Locale.US)
            val sdf24 = SimpleDateFormat("HH:mm", Locale.US)
            sdf24.format(sdf12.parse(raw)!!)
        } catch (_: Exception) { raw }
    }

    return PrayerTimes(
        city    = title,
        country = country,
        date    = dateParts,
        fajr    = t("fajr"),
        sunrise = t("shurooq"),
        dhuhr   = t("dhuhr"),
        asr     = t("asr"),
        maghrib = t("maghrib"),
        isha    = t("isha"),
    )
}

// ── Helper: waktu sholat aktif sekarang ───────────────────────────────────────
private fun currentPrayer(pt: PrayerTimes): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.US)
    val now = sdf.format(Date())
    val order = listOf(
        "Fajr" to pt.fajr, "Dhuhr" to pt.dhuhr,
        "Asr"  to pt.asr,  "Maghrib" to pt.maghrib, "Isha" to pt.isha
    )
    var current = "Isha"
    for ((name, time) in order) {
        if (now >= time) current = name
    }
    return current
}

// ── UI ────────────────────────────────────────────────────────────────────────
@Composable
fun PrayerTool(
    savedCities   : List<String>,
    onAddCity     : (String) -> Unit,
    onRemoveCity  : (String) -> Unit,
) {
    val scope   = rememberCoroutineScope()
    var query   by remember { mutableStateOf("") }
    var result  by remember { mutableStateOf<PrayerTimes?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error   by remember { mutableStateOf("") }
    var showSaved by remember { mutableStateOf(false) }

    fun fetch(city: String = query.trim()) {
        if (city.isBlank()) return
        scope.launch {
            loading = true; error = ""; result = null
            try {
                val pt = withContext(Dispatchers.IO) { fetchPrayer(city) }
                result  = pt
                query   = city
            } catch (e: Exception) {
                error = "City not found. Try English name (e.g. yogyakarta)"
            } finally {
                loading = false
            }
        }
    }

    // Auto-load kota pertama yang disimpan
    LaunchedEffect(savedCities) {
        if (result == null && savedCities.isNotEmpty()) fetch(savedCities.first())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SatriaColors.ScreenBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {

        // ── Search bar ────────────────────────────────────────────────────
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            TextField(
                value         = query,
                onValueChange = { query = it },
                placeholder   = { Text("City name…", color = SatriaColors.TextTertiary) },
                singleLine    = true,
                colors        = toolTextFieldColors(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction      = ImeAction.Search,
                ),
                keyboardActions = KeyboardActions(onSearch = { fetch() }),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp)),
            )
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SatriaColors.Accent)
                    .clickable { fetch() },
                contentAlignment = Alignment.Center,
            ) {
                Text("🔍", fontSize = 18.sp)
            }
        }

        // ── Saved cities ──────────────────────────────────────────────────
        AnimatedVisibility(savedCities.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SatriaColors.CardBg),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSaved = !showSaved }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text("🕌 Saved (${savedCities.size}/8)",
                        color = SatriaColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(if (showSaved) "▲" else "▼", color = SatriaColors.TextTertiary, fontSize = 11.sp)
                }
                AnimatedVisibility(showSaved, enter = expandVertically(), exit = shrinkVertically()) {
                    Column {
                        savedCities.forEachIndexed { idx, city ->
                            if (idx > 0)
                                Box(Modifier.fillMaxWidth().height(1.dp).padding(start = 16.dp).background(SatriaColors.Divider))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { fetch(city); showSaved = false }
                                    .padding(horizontal = 16.dp, vertical = 11.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically,
                            ) {
                                Text(city.replaceFirstChar { it.uppercase() },
                                    color = SatriaColors.TextSecondary, fontSize = 14.sp)
                                Text("✕", color = SatriaColors.TextTertiary, fontSize = 13.sp,
                                    modifier = Modifier.clickable { onRemoveCity(city) })
                            }
                        }
                    }
                }
            }
        }

        // ── Loading ───────────────────────────────────────────────────────
        if (loading) {
            Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SatriaColors.Accent, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
            }
        }

        // ── Error ─────────────────────────────────────────────────────────
        if (error.isNotEmpty()) {
            Text(error, color = SatriaColors.Danger, fontSize = 13.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
        }

        // ── Result ────────────────────────────────────────────────────────
        result?.let { pt ->
            val active = remember(pt) { currentPrayer(pt) }

            // Header kota
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SatriaColors.Accent)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Text(pt.city, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                if (pt.country.isNotBlank())
                    Text(pt.country, color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Text("Now: $active", color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            // Grid waktu sholat
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SatriaColors.CardBg),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                listOf(
                    Triple("🌙", "Fajr",    pt.fajr),
                    Triple("🌅", "Sunrise", pt.sunrise),
                    Triple("☀️", "Dhuhr",   pt.dhuhr),
                    Triple("🌤️", "Asr",     pt.asr),
                    Triple("🌇", "Maghrib", pt.maghrib),
                    Triple("🌃", "Isha",    pt.isha),
                ).forEachIndexed { i, (icon, name, time) ->
                    val isActive = name == active
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isActive) Modifier.background(SatriaColors.Accent.copy(alpha = 0.12f))
                                else Modifier
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(icon, fontSize = 20.sp, modifier = Modifier.width(26.dp))
                        Text(
                            name,
                            color      = if (isActive) SatriaColors.Accent else SatriaColors.TextPrimary,
                            fontSize   = 15.sp,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                            modifier   = Modifier.weight(1f),
                        )
                        Text(
                            time,
                            color      = if (isActive) SatriaColors.Accent else SatriaColors.TextSecondary,
                            fontSize   = 15.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        )
                        if (isActive)
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(SatriaColors.Accent)
                            )
                    }
                    if (i < 5)
                        Box(Modifier.fillMaxWidth().height(1.dp).padding(start = 52.dp).background(SatriaColors.Divider))
                }
            }

            // Save button
            val alreadySaved = savedCities.contains(query.trim().lowercase())
            val canSave      = !alreadySaved && savedCities.size < 8
            if (canSave || alreadySaved) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (alreadySaved) SatriaColors.SurfaceMid else SatriaColors.Accent)
                        .clickable(enabled = canSave) { onAddCity(query.trim().lowercase()) }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (alreadySaved) "✓ Saved" else "📌 Save City",
                        color      = if (alreadySaved) SatriaColors.TextTertiary else Color.White,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
        }
    }
}
