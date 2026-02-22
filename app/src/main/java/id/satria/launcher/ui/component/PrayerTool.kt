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
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

// ── Data ──────────────────────────────────────────────────────────────────────
private data class PrayerEntry(val icon: String, val name: String, val time: String)

private data class PrayerTimes(
    val city     : String,
    val country  : String,
    val date     : String,   // "DD Mon YYYY"
    val hijri    : String,   // "DD MonthHijri YYYY"
    val method   : String,
    val entries  : List<PrayerEntry>,   // ordered display list
    val prayerNames: List<String>,      // subset used for "active" detection
)

// ── Network ───────────────────────────────────────────────────────────────────
private fun safeGet(url: String): String {
    val conn = URL(url).openConnection() as HttpURLConnection
    return try {
        conn.connectTimeout = 15_000
        conn.readTimeout    = 15_000
        conn.requestMethod  = "GET"
        conn.setRequestProperty("User-Agent", "SatriaLauncher/1.0 Android")
        conn.connect()
        if (conn.responseCode !in 200..299) throw Exception("HTTP ${conn.responseCode}")
        BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }
    } finally { conn.disconnect() }
}

// Trim detik dari "HH:mm (X)" atau "HH:mm:ss" → "HH:mm"
private fun cleanTime(raw: String): String =
    raw.trim().split(" ")[0].split(":").take(2).joinToString(":")

private fun fetchPrayer(city: String, country: String): PrayerTimes {
    val c  = URLEncoder.encode(city.trim(),    "UTF-8")
    val co = URLEncoder.encode(country.trim(), "UTF-8")
    // method=11 = Egyptian General Authority (umum dipakai, termasuk Indonesia)
    val url = "https://api.aladhan.com/v1/timingsByCity?city=$c&country=$co&method=11"
    val raw = safeGet(url)
    val root = JSONObject(raw)

    if (root.optInt("code", 0) != 200) throw Exception("City not found")

    val data    = root.getJSONObject("data")
    val timings = data.getJSONObject("timings")
    val dateObj = data.getJSONObject("date")
    val gregorian = dateObj.getJSONObject("gregorian")
    val hijriObj  = dateObj.getJSONObject("hijri")
    val meta    = data.getJSONObject("meta")

    val dateStr  = "${gregorian.getString("day")} ${gregorian.getJSONObject("month").getString("en")} ${gregorian.getString("year")}"
    val hijriStr = "${hijriObj.getString("day")} ${hijriObj.getJSONObject("month").getString("en")} ${hijriObj.getString("year")}"
    val methodStr = meta.getJSONObject("method").optString("name", "")

    fun t(key: String) = cleanTime(timings.optString(key, "--:--"))

    // Semua waktu yang tersedia dari AlAdhan
    val entries = listOf(
        PrayerEntry("🌑", "Imsak",     t("Imsak")),
        PrayerEntry("🌙", "Fajr",      t("Fajr")),
        PrayerEntry("🌅", "Sunrise",   t("Sunrise")),
        PrayerEntry("☀️", "Dhuhr",     t("Dhuhr")),
        PrayerEntry("🌤️", "Asr",       t("Asr")),
        PrayerEntry("🌇", "Sunset",    t("Sunset")),
        PrayerEntry("🌆", "Maghrib",   t("Maghrib")),
        PrayerEntry("🌃", "Isha",      t("Isha")),
        PrayerEntry("🌌", "Midnight",  t("Midnight")),
    )

    // Hanya waktu sholat wajib untuk deteksi "active"
    val prayerNames = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")

    return PrayerTimes(
        city        = city.trim().replaceFirstChar { it.uppercase() },
        country     = country.trim().replaceFirstChar { it.uppercase() },
        date        = dateStr,
        hijri       = hijriStr,
        method      = methodStr,
        entries     = entries,
        prayerNames = prayerNames,
    )
}

// ── Deteksi waktu sholat aktif sekarang ───────────────────────────────────────
private fun activePrayer(pt: PrayerTimes): String {
    val now = SimpleDateFormat("HH:mm", Locale.US).format(Date())
    var active = "Isha"
    for (e in pt.entries) {
        if (e.name in pt.prayerNames && now >= e.time) active = e.name
    }
    return active
}

// ── UI ────────────────────────────────────────────────────────────────────────
@Composable
fun PrayerTool(
    savedCities  : List<String>,   // format "city|country"
    onAddCity    : (String) -> Unit,
    onRemoveCity : (String) -> Unit,
) {
    val scope     = rememberCoroutineScope()
    var city      by remember { mutableStateOf("") }
    var country   by remember { mutableStateOf("") }
    var result    by remember { mutableStateOf<PrayerTimes?>(null) }
    var loading   by remember { mutableStateOf(false) }
    var error     by remember { mutableStateOf("") }
    var showSaved by remember { mutableStateOf(false) }

    // Key yang disimpan: "city|country"
    val currentKey = "${city.trim()}|${country.trim()}"

    fun fetch(c: String = city.trim(), co: String = country.trim()) {
        if (c.isBlank()) return
        scope.launch {
            loading = true; error = ""; result = null
            try {
                val pt = withContext(Dispatchers.IO) { fetchPrayer(c, co) }
                result  = pt
                city    = c
                country = co
            } catch (_: Exception) {
                error = "City not found. Try e.g. city: Yogyakarta, country: Indonesia"
            } finally {
                loading = false
            }
        }
    }

    // Auto-load kota pertama yang tersimpan
    LaunchedEffect(savedCities) {
        if (result == null && savedCities.isNotEmpty()) {
            val parts = savedCities.first().split("|")
            fetch(parts.getOrElse(0) { "" }, parts.getOrElse(1) { "" })
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SatriaColors.ScreenBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {

        // ── Search ────────────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TextField(
                value         = city,
                onValueChange = { city = it },
                placeholder   = { Text("City  (e.g. Yogyakarta)", color = SatriaColors.TextTertiary) },
                singleLine    = true,
                colors        = toolTextFieldColors(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction      = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value         = country,
                    onValueChange = { country = it },
                    placeholder   = { Text("Country  (e.g. Indonesia)", color = SatriaColors.TextTertiary) },
                    singleLine    = true,
                    colors        = toolTextFieldColors(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction      = ImeAction.Search,
                    ),
                    keyboardActions = KeyboardActions(onSearch = { fetch() }),
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)),
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SatriaColors.Accent)
                        .clickable { fetch() },
                    contentAlignment = Alignment.Center,
                ) { Text("🔍", fontSize = 18.sp) }
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
                        savedCities.forEachIndexed { idx, key ->
                            val parts   = key.split("|")
                            val label   = parts.getOrElse(0) { key }.replaceFirstChar { it.uppercase() }
                            val ctry    = parts.getOrElse(1) { "" }.replaceFirstChar { it.uppercase() }
                            if (idx > 0)
                                Box(Modifier.fillMaxWidth().height(1.dp).padding(start = 16.dp).background(SatriaColors.Divider))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        fetch(parts.getOrElse(0) { "" }, parts.getOrElse(1) { "" })
                                        showSaved = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 11.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(label, color = SatriaColors.TextSecondary, fontSize = 14.sp)
                                    if (ctry.isNotBlank())
                                        Text(ctry, color = SatriaColors.TextTertiary, fontSize = 11.sp)
                                }
                                Text("✕", color = SatriaColors.TextTertiary, fontSize = 13.sp,
                                    modifier = Modifier.clickable { onRemoveCity(key) })
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
            Text(error, color = SatriaColors.Danger, fontSize = 13.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }

        // ── Result ────────────────────────────────────────────────────────
        result?.let { pt ->
            val active = remember(pt) { activePrayer(pt) }

            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SatriaColors.Accent)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Text("${pt.city}, ${pt.country}",
                    color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(pt.date, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                Text(pt.hijri, color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(7.dp).clip(RoundedCornerShape(50))
                            .background(Color.White)
                    )
                    Text("Now: $active", color = Color.White,
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Waktu lengkap
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SatriaColors.CardBg),
            ) {
                pt.entries.forEachIndexed { i, entry ->
                    val isActive = entry.name == active
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (isActive) Modifier.background(SatriaColors.Accent.copy(alpha = 0.10f)) else Modifier)
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(entry.icon, fontSize = 18.sp, modifier = Modifier.width(24.dp))
                        Text(
                            entry.name,
                            color      = if (isActive) SatriaColors.Accent else SatriaColors.TextPrimary,
                            fontSize   = 14.sp,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                            modifier   = Modifier.weight(1f),
                        )
                        // Label "wajib" / "sunnah"
                        if (entry.name in pt.prayerNames) {
                            Text("wajib",
                                color    = SatriaColors.Accent.copy(alpha = 0.6f),
                                fontSize = 10.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SatriaColors.Accent.copy(alpha = 0.08f))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            entry.time,
                            color      = if (isActive) SatriaColors.Accent else SatriaColors.TextSecondary,
                            fontSize   = 14.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        )
                        if (isActive)
                            Box(Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(SatriaColors.Accent))
                    }
                    if (i < pt.entries.lastIndex)
                        Box(Modifier.fillMaxWidth().height(1.dp).padding(start = 50.dp).background(SatriaColors.Divider))
                }
            }

            // Method info
            if (pt.method.isNotBlank()) {
                Text("Method: ${pt.method}",
                    color = SatriaColors.TextTertiary, fontSize = 10.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }

            // Save button
            val alreadySaved = savedCities.contains(currentKey)
            val canSave      = !alreadySaved && savedCities.size < 8
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (alreadySaved) SatriaColors.SurfaceMid else SatriaColors.Accent)
                    .clickable(enabled = canSave) { onAddCity(currentKey) }
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

            Spacer(Modifier.height(4.dp))
        }
    }
}
