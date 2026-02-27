package id.satria.launcher.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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

// ── All conversion offline — zero network ─────────────────────────────────────
private data class UnitDef(val name: String, val symbol: String, val toBase: Double)
private data class Category(val icon: String, val name: String, val units: List<UnitDef>)

private val CATS = listOf(
    Category("📏","Length", listOf(
        UnitDef("Kilometer","km",1000.0), UnitDef("Meter","m",1.0),
        UnitDef("Centimeter","cm",0.01), UnitDef("Millimeter","mm",0.001),
        UnitDef("Mile","mi",1609.344), UnitDef("Yard","yd",0.9144),
        UnitDef("Foot","ft",0.3048), UnitDef("Inch","in",0.0254))),
    Category("⚖️","Weight", listOf(
        UnitDef("Kilogram","kg",1.0), UnitDef("Gram","g",0.001),
        UnitDef("Milligram","mg",1e-6), UnitDef("Pound","lb",0.453592),
        UnitDef("Ounce","oz",0.0283495), UnitDef("Ton","t",1000.0))),
    Category("🌡️","Temp", listOf(
        UnitDef("Celsius","°C",1.0), UnitDef("Fahrenheit","°F",1.0), UnitDef("Kelvin","K",1.0))),
    Category("💨","Speed", listOf(
        UnitDef("km/h","km/h",1.0), UnitDef("m/s","m/s",3.6),
        UnitDef("mph","mph",1.60934), UnitDef("Knot","kn",1.852))),
    Category("💧","Volume", listOf(
        UnitDef("Liter","L",1.0), UnitDef("Milliliter","mL",0.001),
        UnitDef("Gallon US","gal",3.78541), UnitDef("Fl oz","fl oz",0.0295735),
        UnitDef("Cup","cup",0.236588), UnitDef("Tablespoon","tbsp",0.0147868))),
    Category("📐","Area", listOf(
        UnitDef("sq meter","m²",1.0), UnitDef("sq km","km²",1e6),
        UnitDef("Hectare","ha",10000.0), UnitDef("sq foot","ft²",0.092903),
        UnitDef("Acre","ac",4046.86))),
)

private fun convertTemp(v: Double, from: String, to: String): Double {
    val c = when(from){ "Fahrenheit"-> (v-32)*5/9; "Kelvin"-> v-273.15; else-> v }
    return when(to){ "Fahrenheit"-> c*9/5+32; "Kelvin"-> c+273.15; else-> c }
}
private fun convert(v: Double, from: UnitDef, to: UnitDef, catName: String): Double {
    if (catName == "Temp") return convertTemp(v, from.name, to.name)
    return v * from.toBase / to.toBase
}
private fun fmtResult(v: Double): String {
    if (v.isInfinite() || v.isNaN()) return "–"
    if (v == 0.0) return "0"
    val abs = Math.abs(v)
    return when {
        abs >= 1e12 || abs < 1e-6 -> "%.4e".format(v)
        abs >= 1    -> "%.8f".format(v).trimEnd('0').trimEnd('.')
        else        -> "%.10f".format(v).trimEnd('0').trimEnd('.')
    }
}

@Composable
fun ConverterTool() {
    var catIdx  by remember { mutableIntStateOf(0) }
    var fromIdx by remember { mutableIntStateOf(0) }
    var toIdx   by remember { mutableIntStateOf(1) }
    var input   by remember { mutableStateOf("1") }

    val cat    = CATS[catIdx]
    val from   = cat.units[fromIdx.coerceIn(cat.units.indices)]
    val to     = cat.units[toIdx.coerceIn(cat.units.indices)]
    val result by remember(input, catIdx, fromIdx, toIdx) {
        derivedStateOf {
            val v = input.replace(",",".").toDoubleOrNull()
            if (v != null) fmtResult(convert(v, from, to, cat.name)) else "–"
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {

        // Category chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(CATS.indices.toList()) { idx ->
                val c = CATS[idx]; val active = catIdx == idx
                Box(modifier = Modifier.clip(RoundedCornerShape(50.dp))
                    .background(if (active) SatriaColors.Accent else SatriaColors.Surface)
                    .clickable(interactionSource = remember{MutableInteractionSource()}, indication = null) {
                        catIdx = idx; fromIdx = 0; toIdx = 1; input = "1"
                    }.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Text("${c.icon} ${c.name}",
                        color = if (active) Color.White else SatriaColors.TextSecondary,
                        fontSize = 13.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }

        // Input
        TextField(value = input, onValueChange = { input = it },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
            placeholder = { Text("Enter value...", color = SatriaColors.TextTertiary) },
            colors = toolTextFieldColors(), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {}))

        // FROM
        SectionLabel("FROM")
        UnitChips(cat.units, fromIdx) { fromIdx = it }

        // TO
        SectionLabel("TO")
        UnitChips(cat.units, toIdx) { toIdx = it }

        // Result
        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(SatriaColors.Surface).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("$input ${from.symbol}  =", color = SatriaColors.TextSecondary, fontSize = 13.sp)
            Text("$result ${to.symbol}",
                color = SatriaColors.TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = SatriaColors.TextTertiary, fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
}

@Composable
private fun UnitChips(units: List<UnitDef>, selected: Int, onSelect: (Int) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(units.indices.toList()) { idx ->
            val u = units[idx]; val active = selected == idx
            Box(modifier = Modifier.clip(RoundedCornerShape(10.dp))
                .background(if (active) SatriaColors.AccentDim else SatriaColors.SurfaceMid)
                .clickable(interactionSource = remember{MutableInteractionSource()}, indication = null) { onSelect(idx) }
                .padding(horizontal = 12.dp, vertical = 8.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(u.symbol, color = if (active) Color.White else SatriaColors.TextPrimary,
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(u.name, color = if (active) androidx.compose.ui.graphics.Color.White.copy(.65f) else SatriaColors.TextTertiary,
                        fontSize = 9.sp)
                }
            }
        }
    }
}
