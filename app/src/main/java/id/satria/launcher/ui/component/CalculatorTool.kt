package id.satria.launcher.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.satria.launcher.ui.theme.SatriaColors

// ── Pure Kotlin evaluator — zero dependencies ─────────────────────────────────
private fun calculate(src: String): String {
    return try {
        val s = src.replace(" ", "").replace("×", "*").replace("÷", "/")
        var i = 0
        fun peek() = if (i < s.length) s[i] else '\u0000'
        fun parseDouble(): Double {
            val start = i; if (peek() == '-') i++
            while (peek().isDigit() || peek() == '.') i++
            return s.substring(start, i).toDouble()
        }
        var parseExprRef: () -> Double = { 0.0 }
        fun parseFactor(): Double = if (peek() == '(') { i++; val v = parseExprRef(); if (peek() == ')') i++; v } else parseDouble()
        fun parseTerm(): Double { var v = parseFactor(); while (peek() == '*' || peek() == '/') { val op = s[i++]; val r = parseFactor(); v = if (op == '*') v * r else v / r }; return v }
        fun parseExpr(): Double { var v = if (peek() == '-') { i++; -parseTerm() } else parseTerm(); while (peek() == '+' || peek() == '-') { val op = s[i++]; val r = parseTerm(); v = if (op == '+') v + r else v - r }; return v }
        parseExprRef = ::parseExpr
        val r = parseExpr()
        if (r.isNaN() || r.isInfinite()) "Error"
        else if (r == r.toLong().toDouble() && r in -1e15..1e15) r.toLong().toString()
        else "%.10f".format(r).trimEnd('0').trimEnd('.')
    } catch (_: Exception) { "Error" }
}

private enum class BtnType { NUMBER, OP, EQUAL, ACTION }
private data class CalcBtn(val label: String, val type: BtnType)
private val ROWS = listOf(
    listOf(CalcBtn("AC",BtnType.ACTION),CalcBtn("±",BtnType.ACTION),CalcBtn("%",BtnType.OP),CalcBtn("÷",BtnType.OP)),
    listOf(CalcBtn("7",BtnType.NUMBER),CalcBtn("8",BtnType.NUMBER),CalcBtn("9",BtnType.NUMBER),CalcBtn("×",BtnType.OP)),
    listOf(CalcBtn("4",BtnType.NUMBER),CalcBtn("5",BtnType.NUMBER),CalcBtn("6",BtnType.NUMBER),CalcBtn("-",BtnType.OP)),
    listOf(CalcBtn("1",BtnType.NUMBER),CalcBtn("2",BtnType.NUMBER),CalcBtn("3",BtnType.NUMBER),CalcBtn("+",BtnType.OP)),
    listOf(CalcBtn("0",BtnType.NUMBER),CalcBtn(".",BtnType.NUMBER),CalcBtn("⌫",BtnType.ACTION),CalcBtn("=",BtnType.EQUAL)),
)
private val C_NUM=Color(0xFF1C1C1E); private val C_OP=Color(0xFF2C2C2E)
private val C_EQ=Color(0xFF27AE60); private val C_AC=Color(0xFF3A3A3C)

@Composable
fun CalculatorTool() {
    var expr      by remember { mutableStateOf("") }
    var display   by remember { mutableStateOf("0") }
    var justEqual by remember { mutableStateOf(false) }

    fun press(btn: CalcBtn) {
        when (btn.label) {
            "AC" -> { expr=""; display="0"; justEqual=false }
            "⌫"  -> { if(justEqual){expr="";display="0";justEqual=false;return}; expr=expr.dropLast(1); display=expr.ifEmpty{"0"} }
            "±"  -> { val v=display.toDoubleOrNull()?:return; val n=-v; val s=if(n==n.toLong().toDouble())n.toLong().toString() else n.toString(); display=s;expr=s;justEqual=false }
            "%"  -> { val v=display.toDoubleOrNull()?:return; val p=v/100.0; val s=if(p==p.toLong().toDouble())p.toLong().toString() else "%.8f".format(p).trimEnd('0').trimEnd('.'); display=s;expr=s;justEqual=false }
            "="  -> { val r=calculate(expr); display=r; expr=if(r=="Error") "" else r; justEqual=true }
            else -> {
                val isOp=btn.type==BtnType.OP
                if(justEqual&&!isOp){expr=btn.label;display=btn.label;justEqual=false;return}
                justEqual=false
                val ops=setOf('+','-','×','÷')
                if(isOp && expr.lastOrNull() in ops) expr=expr.dropLast(1)+btn.label else expr+=btn.label
                display=expr
            }
        }
    }

    Column(modifier=Modifier.fillMaxSize().background(Color.Black), verticalArrangement=Arrangement.Bottom) {
        Box(modifier=Modifier.fillMaxWidth().weight(1f).padding(horizontal=24.dp,vertical=8.dp), contentAlignment=Alignment.BottomEnd) {
            Column(horizontalAlignment=Alignment.End) {
                if(expr.isNotEmpty()&&!justEqual&&expr!=display)
                    Text(expr, color=SatriaColors.TextTertiary, fontSize=15.sp, maxLines=1, overflow=TextOverflow.Ellipsis)
                Text(display, color=Color.White,
                    fontSize=when{display.length>14->32.sp;display.length>9->44.sp;else->60.sp},
                    fontWeight=FontWeight.Thin, textAlign=TextAlign.End, maxLines=1, overflow=TextOverflow.Ellipsis)
            }
        }
        Column(modifier=Modifier.fillMaxWidth().padding(horizontal=12.dp).padding(bottom=16.dp), verticalArrangement=Arrangement.spacedBy(10.dp)) {
            ROWS.forEach { row ->
                Row(modifier=Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    row.forEach { btn -> CalcButton(btn=btn, modifier=Modifier.weight(1f), onClick={press(btn)}) }
                }
            }
        }
    }
}

@Composable
private fun CalcButton(btn: CalcBtn, modifier: Modifier, onClick: () -> Unit) {
    val src=remember{MutableInteractionSource()}; val dn by src.collectIsPressedAsState()
    val bg=when(btn.type){BtnType.ACTION->if(dn)C_AC.copy(.55f)else C_AC; BtnType.OP->if(dn)C_OP.copy(.55f)else C_OP; BtnType.EQUAL->if(dn)C_EQ.copy(.65f)else C_EQ; BtnType.NUMBER->if(dn)C_NUM.copy(.55f)else C_NUM}
    val fg=if(btn.type==BtnType.OP) Color(0xFFFF9F0A) else Color.White
    Box(modifier=modifier.height(72.dp).clip(RoundedCornerShape(50.dp)).background(bg).clickable(interactionSource=src,indication=null,onClick=onClick), contentAlignment=Alignment.Center) {
        Text(btn.label, color=fg, fontSize=22.sp, fontWeight=FontWeight.Medium)
    }
}
