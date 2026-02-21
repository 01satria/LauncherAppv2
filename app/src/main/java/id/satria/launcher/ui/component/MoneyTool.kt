package id.satria.launcher.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.satria.launcher.ui.theme.SatriaColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

private val CURRENCIES = listOf("USD","EUR","IDR","GBP","JPY","CNY","SGD","AUD","KRW","MYR","THB","INR")

@Composable
fun MoneyTool() {
    val scope = rememberCoroutineScope()

    var from    by remember { mutableStateOf("USD") }
    var to      by remember { mutableStateOf("IDR") }
    var amount  by remember { mutableStateOf("1") }
    var result  by remember { mutableStateOf<String?>(null) }
    var rate    by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error   by remember { mutableStateOf("") }

    fun convert() {
        scope.launch {
            loading = true; error = ""; result = null; rate = null
            try {
                val json = JSONObject(withContext(Dispatchers.IO) {
                    URL("https://open.er-api.com/v6/latest/$from").readText()
                })
                if (json.getString("result") != "success") throw Exception()
                val r   = json.getJSONObject("rates").getDouble(to)
                val val_ = (amount.toDoubleOrNull() ?: 1.0) * r
                rate   = "%.4f".format(r)
                result = "%,.2f".format(val_)
            } catch (e: Exception) { error = "Failed to fetch rate." }
            loading = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("💱 Money Exchange", color = SatriaColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)

        Text("FROM", color = SatriaColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp)
        CurrencyRow(selected = from, onSelect = { from = it })

        Text("TO", color = SatriaColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp)
        CurrencyRow(selected = to, onSelect = { to = it })

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = amount, onValueChange = { amount = it },
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)),
                placeholder = { Text("Amount", color = SatriaColors.TextTertiary) },
                colors = toolTextFieldColors(), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            Button(onClick = ::convert, colors = ButtonDefaults.buttonColors(containerColor = SatriaColors.SurfaceMid),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
                Text("Convert", color = SatriaColors.TextPrimary)
            }
        }

        if (loading) CircularProgressIndicator(color = SatriaColors.Accent, modifier = Modifier.align(Alignment.CenterHorizontally).size(28.dp))
        if (error.isNotEmpty()) Text(error, color = SatriaColors.Danger, fontSize = 13.sp)

        if (result != null) {
            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SatriaColors.Surface).padding(16.dp)) {
                Text("$amount $from = ", color = SatriaColors.TextSecondary, fontSize = 15.sp)
                Text("$result $to", color = SatriaColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("1 $from = $rate $to", color = SatriaColors.TextTertiary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun CurrencyRow(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CURRENCIES.forEach { c ->
            val active = c == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (active) SatriaColors.SurfaceHigh else SatriaColors.Surface)
                    .clickable { onSelect(c) }
                    .padding(horizontal = 13.dp, vertical = 7.dp),
            ) {
                Text(c, color = if (active) SatriaColors.TextPrimary else SatriaColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
