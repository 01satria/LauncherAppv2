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

// ── Data ─────────────────────────────────────────────────────────────────────
private data class PrayerEntry(val icon: String, val name: String, val time: String)

private data class PrayerResult(
    val city      : String,
    val country   : String,
    val dateGreg  : String,
    val dateHijri : String,
    val entries   : List<PrayerEntry>,
    val cacheJson : String,
)

// ── Network ───────────────────────────────────────────────────────────────────
private fun safeGet(url: String): String {
    val conn = URL(url).openConnection() as HttpURLConnection
    return try {
        conn.connectTimeout = 15_000
        conn.readTimeout    = 15_000
        conn.requestMethod  = "GET"
        conn.setRequestProperty("User-Agent", "CloudysLauncher/1.0 Android")
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

// Geocode: resolves city → (country_name, lat, lon) via Open-Meteo geocoding
private fun geocodeCity(city: String): Pair<String, String> {
    val enc = URLEncoder.encode(city.trim(), "UTF-8")
    val raw = safeGet("https://geocoding-api.open-meteo.com/v1/search?name=$enc&count=1&language=en&format=json")
    val results = JSONObject(raw).optJSONArray("results")
        ?: throw Exception("City not found")
    if (results.length() == 0) throw Exception("City not found")
    val r       = results.getJSONObject(0)
    val country = r.optString("country", "")
    val tzId    = r.optString("timezone", "")   // e.g. "Asia/Jakarta"
    return Pair(country, tzId)
}

private fun fetchPrayer(city: String): PrayerResult {
    // Step 1: geocode to get country name
    val (country, _) = geocodeCity(city)

    // Step 2: fetch prayer times with explicit today date
    val today    = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date())
    val cityEnc  = URLEncoder.encode(city.trim(), "UTF-8")
    val ctryEnc  = URLEncoder.encode(country, "UTF-8")
    val url      = "https://api.aladhan.com/v1/timingsByCity/$today?city=$cityEnc&country=$ctryEnc&method=11"
    val raw      = safeGet(url)
    val root     = JSONObject(raw)
    if (root.optInt("code", 0) != 200) throw Exception("City not found")

    val data    = root.getJSONObject("data")
    val timings = data.getJSONObject("timings")
    val dateObj = data.getJSONObject("date")
    val greg    = dateObj.getJSONObject("gregorian")
    val hijri   = dateObj.getJSONObject("hijri")

    val dateGreg = "${greg.getString("day")} ${greg.getJSONObject("month").getString("en")} ${greg.getString("year")}"
    val dateHij  = "${hijri.getString("day")} ${hijri.getJSONObject("month").getString("en")} ${hijri.getString("year")} H"

    return PrayerResult(
        city      = city.trim().replaceFirstChar { it.uppercase() },
        country   = country,
        dateGreg  = dateGreg,
        dateHijri = dateHij,
        entries   = buildEntries(timings),
        cacheJson = timings.toString(),
    )
}

private fun fromCache(city: String, country: String, cacheJson: String): PrayerResult? = runCatching {
    val timings = JSONObject(cacheJson)
    val today   = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())
    PrayerResult(city.replaceFirstChar { it.uppercase() }, country, today, "", buildEntries(timings), cacheJson)
}.getOrNull()

// ── UI ────────────────────────────────────────────────────────────────────────
@Composable
fun PrayerTool(
    savedCities    : List<String>,
    savedCacheJson : String,
    onAddCity      : (String) -> Unit,
    onRemoveCity   : (String) -> Unit,
    onUpdateCache  : (String, String) -> Unit,
) {
    val scope    = rememberCoroutineScope()
    val darkMode = LocalAppTheme.current.darkMode

    var query     by remember { mutableStateOf("") }
    var result    by remember { mutableStateOf<PrayerResult?>(null) }
    var loading   by remember { mutableStateOf(false) }
    var error     by remember { mutableStateOf("") }
    var showSaved by remember { mutableStateOf(false) }

    val cacheMap = remember(savedCacheJson) {
        runCatching { JSONObject(savedCacheJson) }.getOrElse { JSONObject() }
    }

    // Auto-load dari cache saat pertama buka
    LaunchedEffect(savedCities) {
        if (result == null && savedCities.isNotEmpty()) {
            val first = savedCities.first()
            val cached = cacheMap.optString(first, null)
            // Ambil country dari cache map (disimpan dengan key "city__country")
            val parts   = first.split("||")
            val cityKey = parts.getOrElse(0) { first }
            val ctry    = parts.getOrElse(1) { "" }
            if (cached != null) {
                result = fromCache(cityKey, ctry, cached)
                query  = cityKey
            }
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
                // Update cache hanya jika kota disave
                val savedKey = savedCities.find { it.split("||").getOrElse(0){""}.equals(city.trim(), true) }
                if (savedKey != null) onUpdateCache(savedKey, pt.cacheJson)
            } catch (_: Exception) {
                error = "City not found. Try English name (e.g. Jakarta, London, Cairo)"
            } finally {
                loading = false
            }
        }
    }

    fun fetchSaved(key: String) {
        val parts   = key.split("||")
        val city    = parts.getOrElse(0) { key }
        showSaved   = false
        scope.launch {
            loading = true; error = ""
            try {
                val pt = withContext(Dispatchers.IO) { fetchPrayer(city) }
                result = pt
                query  = city
                onUpdateCache(key, pt.cacheJson)
            } catch (_: Exception) {
                val cached = cacheMap.optString(key, null)
                val ctry   = parts.getOrElse(1) { "" }
                result = if (cached != null) fromCache(city, ctry, cached) else null
                if (result == null) error = "Failed to load. Check connection."
            } finally {
                loading = false
            }
        }
    }

    val accentCol = SatriaColors.Accent
    val cardBg    = SatriaColors.CardBg
    val textPrim  = SatriaColors.TextPrimary
    val textSec   = SatriaColors.TextSecondary
    val textTert  = SatriaColors.TextTertiary
    val divider   = SatriaColors.Divider

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SatriaColors.ScreenBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {

        // ── Search ────────────────────────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value         = query,
                onValueChange = { query = it },
                placeholder   = { Text("City name  (e.g. Jakarta, Cairo)", color = textTert) },
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
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text("🕌 Saved  (${savedCities.size}/8)",
                        color = textPrim, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(if (showSaved) "▲" else "▼", color = textTert, fontSize = 11.sp)
                }
                AnimatedVisibility(showSaved, enter = expandVertically(), exit = shrinkVertically()) {
                    Column {
                        savedCities.forEachIndexed { idx, key ->
                            val parts  = key.split("||")
                            val city   = parts.getOrElse(0) { key }.replaceFirstChar { it.uppercase() }
                            val ctry   = parts.getOrElse(1) { "" }
                            if (idx > 0)
                                Box(Modifier.fillMaxWidth().height(1.dp)
                                    .padding(start = 16.dp).background(divider))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { fetchSaved(key) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(city, color = textSec, fontSize = 14.sp)
                                    if (ctry.isNotBlank())
                                        Text(ctry, color = textTert, fontSize = 11.sp)
                                }
                                Text("✕", color = textTert, fontSize = 13.sp,
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

            // Header card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(accentCol, accentCol.copy(alpha = if (darkMode) 0.7f else 0.85f))
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    // Kota
                    Text(pt.city, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    // Negara (dari geocoding, bukan timezone/benua)
                    if (pt.country.isNotBlank())
                        Text(pt.country, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                    Spacer(Modifier.height(6.dp))
                    // Tanggal Gregorian
                    Text(pt.dateGreg, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                    // Tanggal Hijriah
                    if (pt.dateHijri.isNotBlank())
                        Text(pt.dateHijri, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                }
            }

            // Prayer times list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg),
            ) {
                pt.entries.forEachIndexed { i, entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(entry.icon, fontSize = 18.sp, modifier = Modifier.width(24.dp))
                        Text(
                            entry.name,
                            color    = textPrim,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            entry.time,
                            color      = textSec,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    if (i < pt.entries.lastIndex)
                        Box(Modifier.fillMaxWidth().height(0.5.dp)
                            .padding(start = 50.dp).background(divider))
                }
            }

            // Save button
            val cityKey      = query.trim().lowercase()
            val savedKey     = "${cityKey}||${pt.country}"
            val alreadySaved = savedCities.any { it.split("||").getOrElse(0){""}.equals(cityKey, true) }
            val canSave      = !alreadySaved && savedCities.size < 8 && cityKey.isNotBlank()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (alreadySaved) SatriaColors.SurfaceMid else accentCol)
                    .clickable(enabled = canSave) { onAddCity(savedKey) }
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
