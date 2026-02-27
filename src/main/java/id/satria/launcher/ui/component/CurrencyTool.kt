package id.satria.launcher.ui.component

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.text.input.KeyboardType
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

private fun httpGet(urlStr: String, extraHeaders: Map<String, String> = emptyMap()): String {
    val conn = URL(urlStr).openConnection() as HttpURLConnection
    return try {
        conn.connectTimeout = 15_000
        conn.readTimeout    = 15_000
        conn.requestMethod  = "GET"
        conn.setRequestProperty("Accept", "text/html,application/json")
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36")
        extraHeaders.forEach { (k, v) -> conn.setRequestProperty(k, v) }
        conn.connect()
        if (conn.responseCode !in 200..299) throw Exception("HTTP ${conn.responseCode}")
        BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }
    } finally { conn.disconnect() }
}

/** Scrape kurs dari Google Finance — hanya untuk pasangan yang didukung Google.
 *  Jika gagal (pair tidak ada / diblokir), kembalikan null agar fallback ke API. */
private fun scrapeGoogleFinance(from: String, to: String): Double? {
    return try {
        val url  = "https://www.google.com/finance/quote/${from.uppercase()}-${to.uppercase()}"
        val html = httpGet(url, mapOf("Accept-Language" to "en-US,en;q=0.9"))
        // Google menyimpan harga di data-last-price="..." atau di class "YMlKec fxKbKc"
        val r1 = Regex("""data-last-price="([0-9.,]+)"""").find(html)?.groupValues?.get(1)
        if (r1 != null) return r1.replace(",", "").toDoubleOrNull()
        val r2 = Regex("""class="YMlKec fxKbKc">([0-9.,]+)<""").find(html)?.groupValues?.get(1)
        r2?.replace(",", "")?.toDoubleOrNull()
    } catch (_: Exception) { null }
}

private val CURRENCIES = listOf(
    "USD" to "🇺🇸", "EUR" to "🇪🇺", "IDR" to "🇮🇩", "GBP" to "🇬🇧",
    "JPY" to "🇯🇵", "CNY" to "🇨🇳", "SGD" to "🇸🇬", "AUD" to "🇦🇺",
    "KRW" to "🇰🇷", "MYR" to "🇲🇾", "THB" to "🇹🇭", "INR" to "🇮🇳",
    "SAR" to "🇸🇦", "AED" to "🇦🇪",
)

@Composable
fun CurrencyTool() {
    val scope       = rememberCoroutineScope()
    var from        by remember { mutableStateOf("USD") }
    var to          by remember { mutableStateOf("IDR") }
    var amount      by remember { mutableStateOf("1") }
    var result      by remember { mutableStateOf<Double?>(null) }
    var rate        by remember { mutableStateOf<Double?>(null) }
    var loading     by remember { mutableStateOf(false) }
    var error       by remember { mutableStateOf("") }
    var lastUpdated by remember { mutableStateOf("") }

    fun convert() {
        val amt = amount.replace(",", ".").toDoubleOrNull()
        if (amt == null || amt <= 0) { error = "Enter a valid amount"; return }
        scope.launch {
            loading = true; error = ""; result = null; rate = null
            try {
                val (r, ts) = withContext(Dispatchers.IO) {
                    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                        .format(java.util.Date())

                    // Strategi 1: Scrape Google Finance (lebih akurat, real-time)
                    val googleRate = scrapeGoogleFinance(from, to)
                    if (googleRate != null && googleRate > 0) {
                        return@withContext Pair(googleRate, "Google Finance (real-time)")
                    }

                    // Strategi 2: Fallback ke fawazahmed0/exchange-api
                    val fromLower   = from.lowercase()
                    val toLower     = to.lowercase()
                    val primaryUrl  = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@$today/v1/currencies/$fromLower.json"
                    val fallbackUrl = "https://$today.currency-api.pages.dev/v1/currencies/$fromLower.json"
                    val rawJson = try { httpGet(primaryUrl) } catch (_: Exception) { httpGet(fallbackUrl) }
                    val json  = org.json.JSONObject(rawJson)
                    val rates = json.getJSONObject(fromLower)
                    val r     = rates.getDouble(toLower)
                    val ts    = json.optString("date", today)
                    Pair(r, ts)
                }
                rate        = r
                result      = amt * r
                lastUpdated = ts
            } catch (e: Exception) {
                error = when {
                    e.message?.contains("HTTP", true) == true -> "⚠️ Server error. Try again."
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
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("💱 Currency", color = SatriaColors.TextPrimary,
            fontSize = 17.sp, fontWeight = FontWeight.SemiBold)

        CurrencySection("FROM", from) { from = it; result = null }
        CurrencySection("TO",   to)   { to   = it; result = null }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value         = amount,
                onValueChange = { amount = it; result = null },
                modifier      = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)),
                placeholder   = { Text("Amount", color = SatriaColors.TextTertiary) },
                colors        = toolTextFieldColors(),
                singleLine    = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { convert() }),
            )
            Button(
                onClick        = ::convert,
                enabled        = !loading,
                colors         = ButtonDefaults.buttonColors(containerColor = SatriaColors.Accent),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            ) {
                if (loading)
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                else
                    Text("Convert", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }

        AnimatedVisibility(error.isNotEmpty(),
            enter = fadeIn() + slideInVertically { -it / 2 }, exit = fadeOut()) {
            Text(error, color = SatriaColors.Danger, fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF2A1010))
                    .padding(12.dp))
        }

        AnimatedVisibility(result != null,
            enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 5 },
            exit  = fadeOut()) {
            result?.let { res ->
                Column(modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SatriaColors.Surface)
                    .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("$amount $from =", color = SatriaColors.TextSecondary, fontSize = 14.sp)
                    Text(
                        "${fmtCurrency(res)} $to",
                        color = SatriaColors.TextPrimary,
                        fontSize = 28.sp, fontWeight = FontWeight.SemiBold,
                    )
                    HorizontalDivider(color = SatriaColors.Border, thickness = 0.5.dp)
                    Text("1 $from = ${rate?.let { fmtRate(it) }} $to",
                        color = SatriaColors.TextTertiary, fontSize = 12.sp)
                    if (lastUpdated.isNotEmpty())
                        Text("Source: $lastUpdated",
                            color = SatriaColors.TextTertiary, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun CurrencySection(label: String, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = SatriaColors.TextSecondary, fontSize = 11.sp,
            fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CURRENCIES.forEach { (code, flag) ->
                val active = code == selected
                Box(modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) SatriaColors.Accent else SatriaColors.Surface)
                    .clickable { onSelect(code) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(flag, fontSize = 14.sp)
                        Text(code,
                            color      = if (active) Color.White else SatriaColors.TextSecondary,
                            fontSize   = 13.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

private fun fmtCurrency(v: Double): String =
    if (v >= 1000) "%,.0f".format(v) else "%.4f".format(v)

private fun fmtRate(v: Double): String =
    if (v >= 1000) "%,.0f".format(v) else if (v >= 1) "%.4f".format(v) else "%.6f".format(v)
