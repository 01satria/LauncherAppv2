package id.satria.launcher.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import java.net.URL
import java.util.*

private fun weatherDesc(c: Int) = when {
    c == 0   -> "Clear sky"; c <= 3  -> "Partly cloudy"; c <= 9  -> "Foggy"
    c <= 29  -> "Rain";      c <= 39 -> "Snow";           c <= 69 -> "Drizzle / Rain"
    c <= 79  -> "Snow showers"; c <= 84 -> "Rain showers"; else -> "Thunderstorm"
}
private fun weatherIcon(c: Int) = when {
    c == 0  -> "☀️"; c <= 3  -> "⛅"; c <= 9  -> "🌫️"; c <= 29 -> "🌧️"
    c <= 39 -> "❄️"; c <= 69 -> "🌦️"; c <= 79 -> "🌨️"; c <= 84 -> "🌩️"; else -> "⛈️"
}

@Composable
fun WeatherTool(
    savedLocations: List<String>,
    onAddLocation: (String) -> Unit,
    onRemoveLocation: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()

    var query    by remember { mutableStateOf("") }
    var weather  by remember { mutableStateOf<WeatherResult?>(null) }
    var loading  by remember { mutableStateOf(false) }
    var error    by remember { mutableStateOf("") }
    var showSaved by remember { mutableStateOf(false) }

    fun fetchWeather(q: String = query) {
        if (q.isBlank()) return
        scope.launch {
            loading = true; error = ""; weather = null; showSaved = false
            try {
                val geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=${q.trim().encodeUrl()}&count=1&language=en&format=json"
                val geoJson = JSONObject(withContext(Dispatchers.IO) { URL(geoUrl).readText() })
                val results = geoJson.optJSONArray("results")
                if (results == null || results.length() == 0) { error = "Location not found."; loading = false; return@launch }

                val r    = results.getJSONObject(0)
                val lat  = r.getDouble("latitude")
                val lon  = r.getDouble("longitude")
                val city = "${r.getString("name")}, ${r.getString("country")}"

                val wxUrl = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,weathercode,windspeed_10m,relativehumidity_2m" +
                    "&hourly=temperature_2m,weathercode,precipitation_probability&forecast_days=2&timezone=auto"
                val wxJson = JSONObject(withContext(Dispatchers.IO) { URL(wxUrl).readText() })

                val cur     = wxJson.getJSONObject("current")
                val hourly  = wxJson.getJSONObject("hourly")
                val times   = hourly.getJSONArray("time")
                val temps   = hourly.getJSONArray("temperature_2m")
                val codes   = hourly.getJSONArray("weathercode")
                val pops    = hourly.getJSONArray("precipitation_probability")

                val todayStr     = android.text.format.DateFormat.format("yyyy-MM-dd", Date()).toString()
                val currentHour  = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

                val forecast = mutableListOf<WeatherForecast>()
                for (i in 0 until times.length()) {
                    val t = times.getString(i)
                    if (!t.startsWith(todayStr)) continue
                    val h = t.substring(11, 13).toInt()
                    if (h < currentHour) continue
                    forecast.add(WeatherForecast(
                        label = "%02d:00".format(h),
                        temp  = Math.round(temps.getDouble(i)).toInt(),
                        icon  = weatherIcon(codes.getInt(i)),
                        pop   = pops.optInt(i, 0),
                        isNow = h == currentHour,
                    ))
                }

                val code = cur.getInt("weathercode")
                weather = WeatherResult(
                    city     = city,
                    temp     = Math.round(cur.getDouble("temperature_2m")).toInt(),
                    desc     = weatherDesc(code),
                    wind     = Math.round(cur.getDouble("windspeed_10m")).toInt(),
                    humidity = cur.getInt("relativehumidity_2m"),
                    icon     = weatherIcon(code),
                    rawQuery = q.trim(),
                    forecast = forecast,
                )
            } catch (e: Exception) { error = "Failed to fetch weather." }
            loading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("🌤️ Weather", color = SatriaColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)),
                placeholder = { Text("Enter city name...", color = SatriaColors.TextTertiary) },
                colors = toolTextFieldColors(), singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { fetchWeather() }),
            )
            Button(onClick = { fetchWeather() }, colors = ButtonDefaults.buttonColors(containerColor = SatriaColors.SurfaceMid),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
                Text("Go", color = SatriaColors.TextPrimary)
            }
        }

        // Saved locations toggle
        if (savedLocations.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SatriaColors.Surface)
                    .clickable { showSaved = !showSaved }.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("📌 Saved (${savedLocations.size}/8)", color = SatriaColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(if (showSaved) "▲" else "▼", color = SatriaColors.TextTertiary, fontSize = 11.sp)
            }

            if (showSaved) {
                Column(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(SatriaColors.Surface)) {
                    savedLocations.forEachIndexed { idx, loc ->
                        if (idx > 0) HorizontalDivider(color = SatriaColors.SurfaceMid)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "📍 $loc",
                                color = SatriaColors.TextPrimary, fontSize = 14.sp,
                                modifier = Modifier.weight(1f).clickable { fetchWeather(loc) }.padding(horizontal = 14.dp, vertical = 11.dp),
                                maxLines = 1,
                            )
                            Text("✕", color = SatriaColors.Danger, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable { onRemoveLocation(loc) }.padding(horizontal = 14.dp, vertical = 11.dp))
                        }
                    }
                }
            }
        }

        if (loading) Text("Fetching weather...", color = SatriaColors.TextTertiary, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        if (error.isNotEmpty()) Text(error, color = SatriaColors.Danger, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

        weather?.let { w ->
            // Main card
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF141414)).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(w.icon, fontSize = 52.sp)
                Text(w.city, color = SatriaColors.TextSecondary, fontSize = 13.sp)
                Text("${w.temp}°C", color = SatriaColors.TextPrimary, fontSize = 52.sp, fontWeight = FontWeight.Light)
                Text(w.desc, color = SatriaColors.TextSecondary, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Text("💨 ${w.wind} km/h", color = SatriaColors.TextSecondary, fontSize = 13.sp)
                    Text("💧 ${w.humidity}%", color = SatriaColors.TextSecondary, fontSize = 13.sp)
                }
                val canSave = savedLocations.size < 8 && !savedLocations.contains(w.rawQuery)
                val saved   = savedLocations.contains(w.rawQuery)
                if (canSave) {
                    TextButton(onClick = { onAddLocation(w.rawQuery) }) {
                        Text("+ Save Location", color = SatriaColors.TextSecondary, fontSize = 14.sp)
                    }
                } else if (saved) {
                    Text("✓ Saved", color = SatriaColors.TextSecondary, fontSize = 12.sp)
                }
            }

            // Forecast
            if (w.forecast.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF141414)).padding(vertical = 12.dp)) {
                    Text("TODAY'S FORECAST", color = SatriaColors.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp, modifier = Modifier.padding(horizontal = 14.dp, bottom = 10.dp))
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        w.forecast.forEach { f ->
                            Column(
                                modifier = Modifier.width(60.dp).clip(RoundedCornerShape(12.dp))
                                    .background(if (f.isNow) SatriaColors.SurfaceMid else SatriaColors.Surface).padding(vertical = 10.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(f.label, color = if (f.isNow) SatriaColors.TextPrimary else SatriaColors.TextTertiary, fontSize = 10.sp, fontWeight = if (f.isNow) FontWeight.Bold else FontWeight.Normal)
                                Text(f.icon, fontSize = 20.sp)
                                Text("${f.temp}°", color = if (f.isNow) SatriaColors.TextPrimary else SatriaColors.TextPrimary.copy(alpha = 0.8f), fontSize = 14.sp, fontWeight = if (f.isNow) FontWeight.Bold else FontWeight.Normal)
                                if (f.pop > 0) Text("💧${f.pop}%", color = SatriaColors.TextSecondary, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun String.encodeUrl() = java.net.URLEncoder.encode(this, "UTF-8")
