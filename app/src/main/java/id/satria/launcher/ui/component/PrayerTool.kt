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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.satria.launcher.ui.theme.LocalAppTheme
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

private data class PrayerResult(
    val city      : String,
    val country   : String,
    val dateGreg  : String,   // "DD Mon YYYY"
    val dateHijri : String,   // "DD MonthHijri YYYY"
    val entries   : List<PrayerEntry>,
    val activeNow : String,   // current prayer name
    val cacheJson : String,   // raw timings JSON to store
)

// ── Network — AlAdhan v1 ───────────────────────────────────────────────────────
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

private fun trimTime(raw: String) = raw.trim().split(" ")[0].split(":").take(2).joinToString(":")

private fun buildEntries(timings: JSONObject): List<PrayerEntry> = listOf(
    PrayerEntry("🌑", "Imsak",    trimTime(timings.optString("Imsak",    "--:--"))),
    PrayerEntry("🌙", "Fajr",     trimTime(timings.optString("Fajr",     "--:--"))),
    PrayerEntry("🌅", "Sunrise",  trimTime(timings.optString("Sunrise",  "--:--"))),
    PrayerEntry("☀️", "Dhuhr",    trimTime(timings.optString("Dhuhr",    "--:--"))),
    PrayerEntry("🌤️","Asr",      trimTime(timings.optString("Asr",      "--:--"))),
    PrayerEntry("🌇", "Sunset",   trimTime(timings.optString("Sunset",   "--:--"))),
    PrayerEntry("🌆", "Maghrib",  trimTime(timings.optString("Maghrib",  "--:--"))),
    PrayerEntry("🌃", "Isha",     trimTime(timings.optString("Isha",     "--:--"))),
    PrayerEntry("🌌", "Midnight", trimTime(timings.optString("Midnight", "--:--"))),
)

private fun currentPrayer(entries: List<PrayerEntry>): String {
    val now = SimpleDateFormat("HH:mm", Locale.US).format(Date())
    val salah = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
    var active = "Isha"
    for (e in entries) {
        if (e.name in salah && now >= e.time) active = e.name
    }
    return active
}

// Fetch dengan tanggal hari ini eksplisit (DD-MM-YYYY) agar sinkron
private fun fetchPrayer(city: String): PrayerResult {
    val today    = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date())
    val cityEnc  = URLEncoder.encode(city.trim(), "UTF-8")
    // method=11 = Egyptian General Authority (umum, mencakup Asia Tenggara)
    val url = "https://api.aladhan.com/v1/timingsByCity/$today?city=$cityEnc&country=&method=11"
    val raw = safeGet(url)
    val root = JSONObject(raw)
    if (root.optInt("code", 0) != 200) throw Exception("City not found")

    val data      = root.getJSONObject("data")
    val timings   = data.getJSONObject("timings")
    val dateObj   = data.getJSONObject("date")
    val meta      = data.getJSONObject("meta")
    val greg      = dateObj.getJSONObject("gregorian")
    val hijri     = dateObj.getJSONObject("hijri")
    val locInfo   = meta.optJSONObject("timezone") // opsional
    val cityName  = city.trim().replaceFirstChar { it.uppercase() }
    val country   = meta.optString("timezone", "")   // fallback
        .let { tz -> if (tz.contains("/")) tz.split("/")[0] else "" }

    val dateGreg  = "${greg.getString("day")} ${greg.getJSONObject("month").getString("en")} ${greg.getString("year")}"
    val dateHij   = "${hijri.getString("day")} ${hijri.getJSONObject("month").getString("en")} ${hijri.getString("year")} H"

    val entries   = buildEntries(timings)
    val active    = currentPrayer(entries)

    return PrayerResult(
        city       = cityName,
        country    = country,
        dateGreg   = dateGreg,
        dateHijri  = dateHij,
        entries    = entries,
        activeNow  = active,
        cacheJson  = timings.toString(),
    )
}

// Build result from cached timings JSON (no network)
private fun fromCache(city: String, cacheJson: String): PrayerResult? = runCatching {
    val timings  = JSONObject(cacheJson)
    val entries  = buildEntries(timings)
    val today    = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())
    PrayerResult(city.replaceFirstChar{it.uppercase()}, "", today, "", entries, currentPrayer(entries), cacheJson)
}.getOrNull()

// ── UI ────────────────────────────────────────────────────────────────────────
@Composable
fun PrayerTool(
    savedCities      : List<String>,
    savedCacheJson   : String,          // raw JSON map "city"→timingsJSON
    onAddCity        : (String) -> Unit,
    onRemoveCity     : (String) -> Unit,
    onUpdateCache    : (String, String) -> Unit,
) {
    val scope     = rememberCoroutineScope()
    val darkMode  = LocalAppTheme.current.darkMode

    var query     by remember { mutableStateOf("") }
    var result    by remember { mutableStateOf<PrayerResult?>(null) }
    var loading   by remember { mutableStateOf(false) }
    var error     by remember { mutableStateOf("") }
    var showSaved by remember { mutableStateOf(false) }

    // Parse cache map lazily
    val cacheMap = remember(savedCacheJson) {
        runCatching { JSONObject(savedCacheJson) }.getOrElse { JSONObject() }
    }

    // Auto-load kota pertama yg disave dari cache (nol network)
    LaunchedEffect(savedCities) {
        if (result == null && savedCities.isNotEmpty()) {
            val first = savedCities.first()
            val cached = cacheMap.optString(first, null)
            if (cached != null) result = fromCache(first, cached)
        }
    }

    fun fetch(city: String) {
        if (city.isBlank()) return
        scope.launch {
            loading = true; error = ""
            try {
                val pt = withContext(Dispatchers.IO) { fetchPrayer(city) }
                result = pt
                query  = city
                // Update cache jika kota ini disave
                onUpdateCache(city.lowercase(), pt.cacheJson)
            } catch (_: Exception) {
                error = "City not found. Try English name (e.g. Yogyakarta, Cairo, London)"
            } finally {
                loading = false
            }
        }
    }

    fun fetchSaved(city: String) {
        // Selalu request ulang ke API saat tap kota save
        scope.launch {
            loading = true; error = ""
            showSaved = false
            try {
                val pt = withContext(Dispatchers.IO) { fetchPrayer(city) }
                result = pt
                query  = city
                onUpdateCache(city.lowercase(), pt.cacheJson)
            } catch (_: Exception) {
                // Fallback ke cache jika ada
                val cached = cacheMap.optString(city.lowercase(), null)
                result = if (cached != null) fromCache(city, cached) else null
                if (result == null) error = "Failed to load. Check connection."
            } finally {
                loading = false
            }
        }
    }

    // Warna tema
    val cardBg     = SatriaColors.CardBg
    val accentCol  = SatriaColors.Accent
    val textPrim   = SatriaColors.TextPrimary
    val textSec    = SatriaColors.TextSecondary
    val textTert   = SatriaColors.TextTertiary
    val divider    = SatriaColors.Divider
    val screenBg   = SatriaColors.ScreenBackground

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {

        // ── Search bar ────────────────────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value         = query,
                onValueChange = { query = it },
                placeholder   = { Text("City name  (e.g. Yogyakarta)", color = textTert) },
                singleLine    = true,
                colors        = toolTextFieldColors(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction      = ImeAction.Search,
                ),
                keyboardActions = KeyboardActions(onSearch = { fetch(query.trim()) }),
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)),
            )
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentCol)
                    .clickable { fetch(query.trim()) },
                contentAlignment = Alignment.Center,
            ) { Text("🔍", fontSize = 18.sp) }
        }

        // ── Saved cities ──────────────────────────────────────────────────
        AnimatedVisibility(savedCities.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(cardBg),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSaved = !showSaved }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("🕌 Saved cities (${savedCities.size}/8)",
                        color = textPrim, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(if (showSaved) "▲" else "▼", color = textTert, fontSize = 11.sp)
                }
                AnimatedVisibility(showSaved, enter = expandVertically(), exit = shrinkVertically()) {
                    Column {
                        savedCities.forEachIndexed { idx, city ->
                            if (idx > 0)
                                Box(Modifier.fillMaxWidth().height(1.dp).padding(start = 16.dp).background(divider))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { fetchSaved(city) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(city.replaceFirstChar { it.uppercase() },
                                    color = textSec, fontSize = 14.sp)
                                Text("✕", color = textTert, fontSize = 13.sp,
                                    modifier = Modifier.clickable {
                                        onRemoveCity(city.lowercase())
                                    })
                            }
                        }
                    }
                }
            }
        }

        // ── Loading ───────────────────────────────────────────────────────
        if (loading) {
            Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accentCol, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
            }
        }

        // ── Error ─────────────────────────────────────────────────────────
        if (error.isNotEmpty()) {
            Text(error, color = SatriaColors.Danger, fontSize = 13.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }

        // ── Result ────────────────────────────────────────────────────────
        result?.let { pt ->

            // Header card — gradient halus
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                accentCol,
                                if (darkMode) accentCol.copy(alpha = 0.75f) else accentCol.copy(alpha = 0.85f)
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    // Kota + negara
                    Text(
                        buildString {
                            append(pt.city)
                            if (pt.country.isNotBlank()) append("  ·  ${pt.country}")
                        },
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    // Tanggal Gregorian
                    Text(pt.dateGreg, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    // Tanggal Hijriah (jika ada)
                    if (pt.dateHijri.isNotBlank())
                        Text(pt.dateHijri, color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp)
                    Spacer(Modifier.height(8.dp))
                    // Chip waktu aktif
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(Color.White.copy(alpha = 0.20f))
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(7.dp).clip(RoundedCornerShape(50.dp)).background(Color.White))
                        Text("Now: ${pt.activeNow}", color = Color.White,
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Prayer times card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg),
            ) {
                pt.entries.forEachIndexed { i, entry ->
                    val isActive = entry.name == pt.activeNow
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isActive) Modifier.background(accentCol.copy(alpha = 0.09f))
                                else Modifier
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Icon
                        Text(entry.icon, fontSize = 18.sp, modifier = Modifier.width(24.dp))

                        // Name
                        Text(
                            entry.name,
                            color      = if (isActive) accentCol else textPrim,
                            fontSize   = 14.sp,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                            modifier   = Modifier.weight(1f),
                        )

                        // Active dot
                        if (isActive)
                            Box(Modifier.size(6.dp).clip(RoundedCornerShape(50.dp)).background(accentCol))

                        // Time
                        Text(
                            entry.time,
                            color      = if (isActive) accentCol else textSec,
                            fontSize   = 14.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                    if (i < pt.entries.lastIndex)
                        Box(Modifier.fillMaxWidth().height(0.5.dp).padding(start = 50.dp).background(divider))
                }
            }

            // Save / saved button
            val cityKey      = query.trim().lowercase()
            val alreadySaved = savedCities.any { it.lowercase() == cityKey }
            val canSave      = !alreadySaved && savedCities.size < 8 && cityKey.isNotBlank()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (alreadySaved) SatriaColors.SurfaceMid
                        else accentCol
                    )
                    .clickable(enabled = canSave) { onAddCity(cityKey) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (alreadySaved) "✓ Saved" else "📌  Save City",
                    color      = if (alreadySaved) textTert else Color.White,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(4.dp))
        }
    }
}
