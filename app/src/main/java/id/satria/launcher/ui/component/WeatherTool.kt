package id.satria.launcher.ui.component

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.satria.launcher.data.WeatherForecast
import id.satria.launcher.data.WeatherResult
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
import java.util.*

// HTTP GET dengan timeout proper — fix root cause API failure
private fun safeHttpGet(urlStr: String): String {
    val conn = URL(urlStr).openConnection() as HttpURLConnection
    return try {
        conn.apply {
            connectTimeout = 15_000
            readTimeout    = 15_000
            requestMethod  = "GET"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "SatriaLauncher/1.0 Android")
            doInput = true
        }
        conn.connect()
        val code = conn.responseCode
        if (code !in 200..299) throw Exception("HTTP $code")
        BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }
    } finally {
        conn.disconnect()
    }
}

private fun weatherDesc(c: Int) = when {
    c == 0  -> "Clear sky"
    c <= 3  -> "Partly cloudy"
    c <= 9  -> "Foggy"
    c <= 29 -> "Rainy"
    c <= 39 -> "Snow"
    c <= 69 -> "Drizzle"
    c <= 79 -> "Snow showers"
    c <= 84 -> "Rain showers"
    else    -> "Thunderstorm"
}

private fun weatherEmoji(c: Int) = when {
    c == 0  -> "☀️"; c <= 3 -> "⛅"; c <= 9 -> "🌫️"
    c <= 29 -> "🌧️"; c <= 39 -> "❄️"; c <= 69 -> "🌦️"
    c <= 79 -> "🌨️"; c <= 84 -> "🌩️"; else -> "⛈️"
}

@Composable
fun WeatherTool(
    savedLocations: List<String>,
    onAddLocation: (String) -> Unit,
    onRemoveLocation: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var query     by remember { mutableStateOf("") }
    var weather   by remember { mutableStateOf<WeatherResult?>(null) }
    var loading   by remember { mutableStateOf(false) }
    var error     by remember { mutableStateOf("") }
    var showSaved by remember { mutableStateOf(false) }

    fun fetchWeather(q: String = query.trim()) {
        if (q.isBlank()) return
        scope.launch {
            loading = true; error = ""; weather = null
            try {
                val result = withContext(Dispatchers.IO) {
                    val enc     = URLEncoder.encode(q, "UTF-8")
                    val geoRaw  = safeHttpGet(
                        "https://geocoding-api.open-meteo.com/v1/search?name=$enc&count=1&language=en&format=json"
                    )
                    val geoJson = JSONObject(geoRaw)
                    val results = geoJson.optJSONArray("results")
                        ?: throw Exception("Location not found")
                    if (results.length() == 0) throw Exception("Location not found")

                    val r    = results.getJSONObject(0)
                    val lat  = r.getDouble("latitude")
                    val lon  = r.getDouble("longitude")
                    val city = "${r.getString("name")}, ${r.optString("country_code", r.optString("country", ""))}"

                    val wxRaw  = safeHttpGet(
                        "https://api.open-meteo.com/v1/forecast" +
                        "?latitude=$lat&longitude=$lon" +
                        "&current=temperature_2m,weathercode,windspeed_10m,relativehumidity_2m" +
                        "&hourly=temperature_2m,weathercode,precipitation_probability" +
                        "&forecast_days=2&timezone=auto"
                    )
                    val wxJson = JSONObject(wxRaw)
                    val cur    = wxJson.getJSONObject("current")
                    val hourly = wxJson.getJSONObject("hourly")
                    val times  = hourly.getJSONArray("time")
                    val temps  = hourly.getJSONArray("temperature_2m")
                    val codes  = hourly.getJSONArray("weathercode")
                    val pops   = hourly.getJSONArray("precipitation_probability")

                    val cal      = Calendar.getInstance()
                    val todayStr = "%04d-%02d-%02d".format(
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH) + 1,
                        cal.get(Calendar.DAY_OF_MONTH)
                    )
                    val nowH = cal.get(Calendar.HOUR_OF_DAY)

                    val forecast = mutableListOf<WeatherForecast>()
                    for (i in 0 until times.length()) {
                        val t = times.getString(i)
                        if (!t.startsWith(todayStr)) continue
                        val h = t.substring(11, 13).toInt()
                        if (h < nowH) continue
                        if (forecast.size >= 8) break
                        forecast.add(WeatherForecast(
                            label = "%02d:00".format(h),
                            temp  = Math.round(temps.getDouble(i)).toInt(),
                            icon  = weatherEmoji(codes.getInt(i)),
                            pop   = pops.optInt(i, 0),
                            isNow = h == nowH,
                        ))
                    }
                    val code = cur.optInt("weathercode", 0)
                    WeatherResult(
                        city     = city,
                        temp     = Math.round(cur.getDouble("temperature_2m")).toInt(),
                        desc     = weatherDesc(code),
                        wind     = Math.round(cur.optDouble("windspeed_10m", 0.0)).toInt(),
                        humidity = cur.optInt("relativehumidity_2m", 0),
                        icon     = weatherEmoji(code),
                        rawQuery = q,
                        forecast = forecast,
                    )
                }
                weather = result
            } catch (e: Exception) {
                error = when {
                    e.message?.contains("not found", ignoreCase = true) == true ->
                        "📍 Location not found. Try a different city name."
                    e.message?.contains("HTTP", ignoreCase = true) == true ->
                        "⚠️ Server error. Try again later."
                    else -> "❌ No connection. Check your internet."
                }
            } finally {
                loading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("🌤️ Weather", color = SatriaColors.TextPrimary,
            fontSize = 17.sp, fontWeight = FontWeight.SemiBold)

        // Search row
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)),
                placeholder = { Text("City name...", color = SatriaColors.TextTertiary) },
                colors = toolTextFieldColors(), singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { fetchWeather() }),
            )
            Button(
                onClick = { fetchWeather() },
                enabled = !loading,
                colors  = ButtonDefaults.buttonColors(containerColor = SatriaColors.SurfaceMid),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            ) { Text("Go", color = SatriaColors.TextPrimary, fontWeight = FontWeight.SemiBold) }
        }

        // Saved locations
        AnimatedVisibility(savedLocations.isNotEmpty(),
            enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SatriaColors.Surface)
                        .clickable { showSaved = !showSaved }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("📌 Saved (${savedLocations.size}/8)", color = SatriaColors.TextPrimary,
                        fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(if (showSaved) "▲" else "▼", color = SatriaColors.TextTertiary, fontSize = 11.sp)
                }
                AnimatedVisibility(showSaved, enter = expandVertically(), exit = shrinkVertically()) {
                    Column(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(SatriaColors.Surface)) {
                        savedLocations.forEachIndexed { idx, loc ->
                            if (idx > 0) HorizontalDivider(color = SatriaColors.SurfaceMid, thickness = 0.5.dp)
                            Row(modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically) {
                                Text("📍 $loc", color = SatriaColors.TextPrimary, fontSize = 14.sp, maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                        .clickable { query = loc; fetchWeather(loc) }
                                        .padding(horizontal = 14.dp, vertical = 11.dp))
                                Text("✕", color = SatriaColors.Danger, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                                    modifier = Modifier.clickable { onRemoveLocation(loc) }
                                        .padding(horizontal = 14.dp, vertical = 11.dp))
                            }
                        }
                    }
                }
            }
        }

        // Loading
        AnimatedVisibility(loading) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp, color = SatriaColors.Accent)
                Text("Fetching weather...", color = SatriaColors.TextTertiary, fontSize = 13.sp)
            }
        }

        // Error
        AnimatedVisibility(error.isNotEmpty(),
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit  = fadeOut()) {
            Text(error, color = SatriaColors.Danger, fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF2A1010))
                    .padding(12.dp))
        }

        // Weather result
        AnimatedVisibility(weather != null,
            enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 5 },
            exit  = fadeOut()) {
            weather?.let { w ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Main card
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF111111))
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(w.icon, fontSize = 60.sp)
                        Text(w.city, color = SatriaColors.TextSecondary, fontSize = 13.sp)
                        Text("${w.temp}°", color = SatriaColors.TextPrimary,
                            fontSize = 64.sp, fontWeight = FontWeight.Thin)
                        Text(w.desc, color = SatriaColors.TextSecondary, fontSize = 15.sp)
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            WeatherStat("💨", "${w.wind} km/h")
                            WeatherStat("💧", "${w.humidity}%")
                        }
                        val alreadySaved = savedLocations.contains(w.rawQuery)
                        val canSave      = !alreadySaved && savedLocations.size < 8
                        Spacer(Modifier.height(2.dp))
                        if (canSave) {
                            OutlinedButton(
                                onClick = { onAddLocation(w.rawQuery) },
                                border  = BorderStroke(1.dp, SatriaColors.SurfaceHigh),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            ) {
                                Text("+ Save Location", color = SatriaColors.TextSecondary, fontSize = 13.sp)
                            }
                        } else if (alreadySaved) {
                            Text("✓ Saved", color = SatriaColors.TextSecondary, fontSize = 12.sp)
                        }
                    }

                    // Forecast
                    if (w.forecast.isNotEmpty()) {
                        Column(modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF111111))
                            .padding(vertical = 14.dp)) {
                            Text("TODAY'S FORECAST", color = SatriaColors.TextSecondary,
                                fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.8.sp,
                                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp))
                            Row(modifier = Modifier.horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                w.forecast.forEach { f ->
                                    Column(
                                        modifier = Modifier
                                            .width(62.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (f.isNow) SatriaColors.SurfaceMid else SatriaColors.Surface)
                                            .padding(vertical = 12.dp, horizontal = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(5.dp),
                                    ) {
                                        Text(f.label,
                                            color = if (f.isNow) SatriaColors.Accent else SatriaColors.TextTertiary,
                                            fontSize = 10.sp, fontWeight = FontWeight.Medium)
                                        Text(f.icon, fontSize = 22.sp)
                                        Text("${f.temp}°",
                                            color = if (f.isNow) SatriaColors.TextPrimary
                                                    else SatriaColors.TextPrimary.copy(alpha = 0.75f),
                                            fontSize = 14.sp,
                                            fontWeight = if (f.isNow) FontWeight.SemiBold else FontWeight.Normal)
                                        // ── FIX: selalu tampilkan baris ini agar tinggi card sama ──
                                        // Kalau pop == 0, tampilkan teks transparan sebagai placeholder
                                        Text(
                                            text = if (f.pop > 0) "${f.pop}%" else "",
                                            color = if (f.pop > 0) Color(0xFF5BA4F5) else Color.Transparent,
                                            fontSize = 9.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherStat(icon: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(icon, fontSize = 14.sp)
        Text(value, color = SatriaColors.TextSecondary, fontSize = 13.sp)
    }
}