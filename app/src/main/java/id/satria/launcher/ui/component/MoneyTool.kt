package id.satria.launcher.ui.component

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import id.satria.launcher.data.MoneyTransaction
import id.satria.launcher.data.MoneyWallet
import id.satria.launcher.ui.theme.SatriaColors
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

private data class MoneyCategory(
        val key: String,
        val emoji: String,
        val label: String,
        val color: Color
)

private val EXPENSE_CATEGORIES =
        listOf(
                MoneyCategory("food", "🍔", "Food", Color(0xFFE74C3C)),
                MoneyCategory("transport", "🚗", "Transport", Color(0xFF3498DB)),
                MoneyCategory("shopping", "🛍️", "Shopping", Color(0xFF9B59B6)),
                MoneyCategory("health", "💊", "Health", Color(0xFFE67E22)),
                MoneyCategory("entertainment", "🎬", "Entertainment", Color(0xFFE91E63)),
                MoneyCategory("bills", "🏠", "Bills", Color(0xFF1ABC9C)),
                MoneyCategory("education", "📚", "Education", Color(0xFF2ECC71)),
                MoneyCategory("travel", "✈️", "Travel", Color(0xFF27AE60)),
                MoneyCategory("sport", "⚽", "Sport", Color(0xFFF39C12)),
                MoneyCategory("other", "💸", "Other", Color(0xFF95A5A6)),
        )
private val INCOME_CATEGORIES =
        listOf(
                MoneyCategory("salary", "💼", "Salary", Color(0xFF2ECC71)),
                MoneyCategory("freelance", "💻", "Freelance", Color(0xFF3498DB)),
                MoneyCategory("investment", "📈", "Investment", Color(0xFF9B59B6)),
                MoneyCategory("gift", "🎁", "Gift", Color(0xFFE91E63)),
                MoneyCategory("other_in", "💰", "Other", Color(0xFF95A5A6)),
        )
private val WALLET_COLORS =
        listOf(
                "#27AE60",
                "#3498DB",
                "#9B59B6",
                "#E74C3C",
                "#F39C12",
                "#1ABC9C",
                "#E91E63",
                "#2C3E50"
        )
private val WALLET_EMOJIS = listOf("💳", "🏦", "💵", "💰", "👝", "🪙", "📊", "💹")
private val CURRENCIES = listOf("IDR", "USD", "EUR", "SGD", "MYR", "JPY", "GBP", "AUD")

private fun String.toColorSafe(): Color =
        try {
                Color(android.graphics.Color.parseColor(this))
        } catch (_: Exception) {
                Color(0xFF27AE60)
        }

private fun formatAmount(amount: Double, currency: String = "IDR"): String {
        val df = DecimalFormat("#,###")
        return when (currency) {
                "IDR" -> "Rp ${df.format(amount.toLong())}"
                "USD" -> "\$ ${DecimalFormat("#,##0.00").format(amount)}"
                "EUR" -> "€ ${DecimalFormat("#,##0.00").format(amount)}"
                "JPY" -> "¥ ${df.format(amount.toLong())}"
                else -> "$currency ${DecimalFormat("#,##0.00").format(amount)}"
        }
}

private fun formatAmountShort(amount: Double, currency: String = "IDR"): String {
        val abs = kotlin.math.abs(amount)
        val num =
                when {
                        abs >= 1_000_000_000 -> "%.1fB".format(abs / 1_000_000_000)
                        abs >= 1_000_000 -> "%.1fM".format(abs / 1_000_000)
                        abs >= 1_000 -> "%.0fK".format(abs / 1_000)
                        else -> abs.toLong().toString()
                }
        val sign =
                when (currency) {
                        "IDR" -> "Rp "
                        "USD" -> "\$"
                        "EUR" -> "€"
                        "JPY" -> "¥"
                        else -> "$currency "
                }
        return sign + num
}

private fun todayDateStr(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

private fun formatDate(dateStr: String): String =
        try {
                val d =
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
                                ?: return dateStr
                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(d)
        } catch (_: Exception) {
                dateStr
        }

private fun sameMonth(d1: String, d2: String) = d1.take(7) == d2.take(7)

private fun walletBalance(wallet: MoneyWallet, txs: List<MoneyTransaction>): Double {
        return txs.filter { it.walletId == wallet.id || it.toWalletId == wallet.id }.sumOf { tx ->
                when {
                        tx.type == "income" && tx.walletId == wallet.id -> tx.amount
                        tx.type == "expense" && tx.walletId == wallet.id -> -tx.amount
                        tx.type == "transfer" && tx.walletId == wallet.id -> -tx.amount
                        tx.type == "transfer" && tx.toWalletId == wallet.id -> tx.amount
                        else -> 0.0
                }
        }
}

private enum class MoneyTab {
        OVERVIEW,
        TRANSACTIONS,
        ANALYTICS,
        WALLETS
}

@Composable
fun MoneyTool(
        wallets: List<MoneyWallet>,
        transactions: List<MoneyTransaction>,
        onAddWallet: (String, String, String, String) -> Unit,
        onDeleteWallet: (String) -> Unit,
        onAddTransaction:
                (
                        walletId: String,
                        type: String,
                        amount: Double,
                        category: String,
                        note: String,
                        date: String,
                        toWalletId: String) -> Unit,
        onDeleteTransaction: (String) -> Unit,
        onExportJson: () -> String,
        onImportJson: (String) -> Boolean,
) {
        val context = LocalContext.current
        var activeTab by remember { mutableStateOf(MoneyTab.OVERVIEW) }
        var showAddTx by remember { mutableStateOf(false) }
        var showAddWallet by remember { mutableStateOf(false) }

        val exportLauncher =
                rememberLauncherForActivityResult(
                        ActivityResultContracts.CreateDocument("application/json")
                ) { uri ->
                        uri ?: return@rememberLauncherForActivityResult
                        try {
                                context.contentResolver.openOutputStream(uri)?.use { os ->
                                        OutputStreamWriter(os).use { it.write(onExportJson()) }
                                }
                                Toast.makeText(context, "Data exported ✓", Toast.LENGTH_SHORT)
                                        .show()
                        } catch (e: Exception) {
                                Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                        }
                }
        val importLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                        uri ?: return@rememberLauncherForActivityResult
                        try {
                                val json =
                                        context.contentResolver.openInputStream(uri)?.use {
                                                BufferedReader(InputStreamReader(it)).readText()
                                        }
                                                ?: ""
                                if (onImportJson(json))
                                        Toast.makeText(
                                                        context,
                                                        "Data imported ✓",
                                                        Toast.LENGTH_SHORT
                                                )
                                                .show()
                                else
                                        Toast.makeText(
                                                        context,
                                                        "Invalid file format",
                                                        Toast.LENGTH_SHORT
                                                )
                                                .show()
                        } catch (e: Exception) {
                                Toast.makeText(context, "Import failed", Toast.LENGTH_SHORT).show()
                        }
                }

        val currentMonthKey = remember { todayDateStr().take(7) }
        val monthTxs =
                remember(transactions) {
                        transactions.filter { sameMonth(it.date, currentMonthKey) }
                }
        val totalIncome =
                remember(monthTxs) { monthTxs.filter { it.type == "income" }.sumOf { it.amount } }
        val totalExpense =
                remember(monthTxs) { monthTxs.filter { it.type == "expense" }.sumOf { it.amount } }
        val netBalance =
                remember(wallets, transactions) {
                        wallets.sumOf { walletBalance(it, transactions) }
                }
        val currency = wallets.firstOrNull()?.currency ?: "IDR"

        Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                        // Header
                        Column(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .background(SatriaColors.Surface)
                                                .padding(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                                Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        Text(
                                                "Money",
                                                color = SatriaColors.TextPrimary,
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.SemiBold
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                SmallPill("Export") {
                                                        exportLauncher.launch(
                                                                "money_backup_${todayDateStr()}.json"
                                                        )
                                                }
                                                SmallPill("Import") {
                                                        importLauncher.launch(
                                                                arrayOf(
                                                                        "application/json",
                                                                        "text/plain",
                                                                        "*/*"
                                                                )
                                                        )
                                                }
                                        }
                                }
                                Spacer(Modifier.height(12.dp))
                                Text(
                                        "Total Balance",
                                        color = SatriaColors.TextTertiary,
                                        fontSize = 12.sp
                                )
                                Text(
                                        formatAmount(netBalance, currency),
                                        color = SatriaColors.TextPrimary,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.5).sp
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                                Box(
                                                        Modifier.size(8.dp)
                                                                .clip(CircleShape)
                                                                .background(Color(0xFF2ECC71))
                                                )
                                                Text(
                                                        "Income: ${formatAmountShort(totalIncome, currency)}",
                                                        color = SatriaColors.TextSecondary,
                                                        fontSize = 12.sp
                                                )
                                        }
                                        Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                                Box(
                                                        Modifier.size(8.dp)
                                                                .clip(CircleShape)
                                                                .background(Color(0xFFE74C3C))
                                                )
                                                Text(
                                                        "Expense: ${formatAmountShort(totalExpense, currency)}",
                                                        color = SatriaColors.TextSecondary,
                                                        fontSize = 12.sp
                                                )
                                        }
                                }
                        }
                        // Tab bar
                        val tabs =
                                listOf(
                                        MoneyTab.OVERVIEW to "Overview",
                                        MoneyTab.TRANSACTIONS to "History",
                                        MoneyTab.ANALYTICS to "Analytics",
                                        MoneyTab.WALLETS to "Wallets"
                                )
                        Row(
                                Modifier.fillMaxWidth()
                                        .background(SatriaColors.Surface)
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                                tabs.forEach { (tab, label) ->
                                        val sel = tab == activeTab
                                        val acc = SatriaColors.Accent
                                        Box(
                                                contentAlignment = Alignment.Center,
                                                modifier =
                                                        Modifier.weight(1f)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(
                                                                        if (sel)
                                                                                acc.copy(
                                                                                        alpha =
                                                                                                0.15f
                                                                                )
                                                                        else Color.Transparent
                                                                )
                                                                .clickable(
                                                                        interactionSource =
                                                                                remember {
                                                                                        MutableInteractionSource()
                                                                                },
                                                                        indication = null
                                                                ) { activeTab = tab }
                                                                .padding(vertical = 7.dp)
                                        ) {
                                                Text(
                                                        label,
                                                        fontSize = 11.sp,
                                                        fontWeight =
                                                                if (sel) FontWeight.SemiBold
                                                                else FontWeight.Normal,
                                                        color =
                                                                if (sel) acc
                                                                else SatriaColors.TextTertiary
                                                )
                                        }
                                }
                        }
                        Box(
                                Modifier.fillMaxWidth()
                                        .height(0.5.dp)
                                        .background(SatriaColors.BorderLight)
                        )
                        // Content
                        Box(Modifier.weight(1f)) {
                                when (activeTab) {
                                        MoneyTab.OVERVIEW ->
                                                OverviewTab(wallets, transactions, monthTxs)
                                        MoneyTab.TRANSACTIONS ->
                                                TransactionsTab(
                                                        wallets,
                                                        transactions,
                                                        onDeleteTransaction
                                                )
                                        MoneyTab.ANALYTICS ->
                                                AnalyticsTab(monthTxs, totalExpense, totalIncome)
                                        MoneyTab.WALLETS ->
                                                WalletsTab(
                                                        wallets,
                                                        transactions,
                                                        { showAddWallet = true },
                                                        onDeleteWallet
                                                )
                                }
                        }
                }
                // FAB
                val acc = SatriaColors.Accent
                Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                                Modifier.align(Alignment.BottomEnd)
                                        .padding(bottom = 72.dp, end = 20.dp)
                                        .size(52.dp)
                                        .shadow(8.dp, CircleShape)
                                        .clip(CircleShape)
                                        .background(acc)
                                        .clickable(
                                                interactionSource =
                                                        remember { MutableInteractionSource() },
                                                indication = null
                                        ) { showAddTx = true }
                ) {
                        Text(
                                "+",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Light
                        )
                }
                // Sheets
                if (showAddTx)
                        AddTransactionSheet(
                                wallets,
                                wallets.firstOrNull()?.id,
                                { showAddTx = false }
                        ) { wId, type, amt, cat, note, date, toWId ->
                                onAddTransaction(wId, type, amt, cat, note, date, toWId)
                                showAddTx = false
                        }
                if (showAddWallet)
                        AddWalletSheet({ showAddWallet = false }) { name, emoji, color, curr ->
                                onAddWallet(name, emoji, color, curr)
                                showAddWallet = false
                        }
        }
}

@Composable
private fun SmallPill(label: String, onClick: () -> Unit) {
        Box(
                Modifier.clip(RoundedCornerShape(20.dp))
                        .background(SatriaColors.SurfaceMid)
                        .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onClick
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
        ) { Text(label, color = SatriaColors.TextSecondary, fontSize = 11.sp) }
}

// ─── OVERVIEW ─────────────────────────────────────────────────────────────────
@Composable
private fun OverviewTab(
        wallets: List<MoneyWallet>,
        allTxs: List<MoneyTransaction>,
        monthTxs: List<MoneyTransaction>
) {
        LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
        ) {
                if (wallets.isNotEmpty()) {
                        item {
                                Text(
                                        "Wallets",
                                        color = SatriaColors.TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        items(wallets) { w ->
                                                val balance =
                                                        remember(allTxs) {
                                                                walletBalance(w, allTxs)
                                                        }
                                                val clr = w.color.toColorSafe()
                                                Box(
                                                        Modifier.width(148.dp)
                                                                .clip(RoundedCornerShape(14.dp))
                                                                .background(clr.copy(alpha = 0.12f))
                                                                .border(
                                                                        1.dp,
                                                                        clr.copy(alpha = 0.25f),
                                                                        RoundedCornerShape(14.dp)
                                                                )
                                                                .padding(12.dp)
                                                ) {
                                                        Column {
                                                                Text(w.emoji, fontSize = 20.sp)
                                                                Spacer(Modifier.height(5.dp))
                                                                Text(
                                                                        w.name,
                                                                        color =
                                                                                SatriaColors
                                                                                        .TextPrimary,
                                                                        fontSize = 12.sp,
                                                                        fontWeight =
                                                                                FontWeight.SemiBold,
                                                                        maxLines = 1,
                                                                        overflow =
                                                                                TextOverflow
                                                                                        .Ellipsis
                                                                )
                                                                Text(
                                                                        w.currency,
                                                                        color =
                                                                                SatriaColors
                                                                                        .TextTertiary,
                                                                        fontSize = 10.sp
                                                                )
                                                                Spacer(Modifier.height(4.dp))
                                                                Text(
                                                                        formatAmountShort(
                                                                                balance,
                                                                                w.currency
                                                                        ),
                                                                        color =
                                                                                if (balance >= 0)
                                                                                        Color(
                                                                                                0xFF2ECC71
                                                                                        )
                                                                                else
                                                                                        Color(
                                                                                                0xFFE74C3C
                                                                                        ),
                                                                        fontSize = 14.sp,
                                                                        fontWeight = FontWeight.Bold
                                                                )
                                                        }
                                                }
                                        }
                                }
                        }
                } else {
                        item {
                                EmptyCard("💳", "No wallets yet", "Go to Wallets tab to create one")
                        }
                }
                if (monthTxs.any { it.type == "expense" }) {
                        item { SpendingByCatCard(monthTxs) }
                }
                val recent = allTxs.sortedByDescending { it.date }.take(5)
                if (recent.isNotEmpty()) {
                        item {
                                Text(
                                        "Recent Transactions",
                                        color = SatriaColors.TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.height(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        recent.forEach { tx ->
                                                TxRow(
                                                        tx,
                                                        wallets.find { it.id == tx.walletId },
                                                        null
                                                )
                                        }
                                }
                        }
                }
                item { Spacer(Modifier.height(80.dp)) }
        }
}

@Composable
private fun SpendingByCatCard(monthTxs: List<MoneyTransaction>) {
        val expenses = monthTxs.filter { it.type == "expense" }
        val total = expenses.sumOf { it.amount }
        val byCat =
                expenses
                        .groupBy { it.categoryKey }
                        .mapValues { (_, t) -> t.sumOf { it.amount } }
                        .entries
                        .sortedByDescending { it.value }
                        .take(5)
        Box(
                Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SatriaColors.Surface)
                        .padding(16.dp)
        ) {
                Column {
                        Text(
                                "This Month's Spending",
                                color = SatriaColors.TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(10.dp))
                        byCat.forEach { (key, amt) ->
                                val cat = EXPENSE_CATEGORIES.find { it.key == key }
                                val frac = if (total > 0) (amt / total).toFloat() else 0f
                                Row(
                                        Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        Text(
                                                cat?.emoji ?: "💸",
                                                fontSize = 14.sp,
                                                modifier = Modifier.width(24.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Column(Modifier.weight(1f)) {
                                                Row(
                                                        Modifier.fillMaxWidth(),
                                                        horizontalArrangement =
                                                                Arrangement.SpaceBetween
                                                ) {
                                                        Text(
                                                                cat?.label ?: key,
                                                                color = SatriaColors.TextPrimary,
                                                                fontSize = 12.sp
                                                        )
                                                        Text(
                                                                "${(frac * 100).toInt()}%",
                                                                color = SatriaColors.TextTertiary,
                                                                fontSize = 11.sp
                                                        )
                                                }
                                                Spacer(Modifier.height(2.dp))
                                                Box(
                                                        Modifier.fillMaxWidth()
                                                                .height(4.dp)
                                                                .clip(RoundedCornerShape(2.dp))
                                                                .background(
                                                                        SatriaColors.SurfaceHigh
                                                                )
                                                ) {
                                                        Box(
                                                                Modifier.fillMaxWidth(frac)
                                                                        .fillMaxHeight()
                                                                        .clip(
                                                                                RoundedCornerShape(
                                                                                        2.dp
                                                                                )
                                                                        )
                                                                        .background(
                                                                                cat?.color
                                                                                        ?: SatriaColors
                                                                                                .Accent
                                                                        )
                                                        )
                                                }
                                        }
                                }
                        }
                }
        }
}

// ─── TRANSACTIONS ─────────────────────────────────────────────────────────────
@Composable
private fun TransactionsTab(
        wallets: List<MoneyWallet>,
        allTxs: List<MoneyTransaction>,
        onDelete: (String) -> Unit
) {
        val sorted = remember(allTxs) { allTxs.sortedByDescending { it.date } }
        val grouped =
                remember(sorted) {
                        sorted.groupBy { it.date }.entries.sortedByDescending { it.key }
                }
        if (sorted.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📋", fontSize = 40.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                        "No transactions yet",
                                        color = SatriaColors.TextSecondary,
                                        fontSize = 14.sp
                                )
                                Text(
                                        "Tap + to add your first one",
                                        color = SatriaColors.TextTertiary,
                                        fontSize = 12.sp
                                )
                        }
                }
                return
        }
        LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                modifier = Modifier.fillMaxSize()
        ) {
                grouped.forEach { (date, txs) ->
                        item(key = "h_$date") {
                                Spacer(Modifier.height(10.dp))
                                Text(
                                        formatDate(date),
                                        color = SatriaColors.TextTertiary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                )
                        }
                        items(txs, key = { it.id }) { tx ->
                                TxRow(tx, wallets.find { it.id == tx.walletId }) { onDelete(tx.id) }
                                Spacer(Modifier.height(4.dp))
                        }
                }
                item { Spacer(Modifier.height(80.dp)) }
        }
}

@Composable
private fun TxRow(tx: MoneyTransaction, wallet: MoneyWallet?, onDelete: (() -> Unit)?) {
        val cat =
                when (tx.type) {
                        "income" -> INCOME_CATEGORIES.find { it.key == tx.categoryKey }
                        "expense" -> EXPENSE_CATEGORIES.find { it.key == tx.categoryKey }
                        else -> null
                }
        val amtColor =
                when (tx.type) {
                        "income" -> Color(0xFF2ECC71)
                        "expense" -> Color(0xFFE74C3C)
                        else -> Color(0xFF3498DB)
                }
        val sign =
                when (tx.type) {
                        "income" -> "+"
                        "expense" -> "-"
                        else -> "⇄"
                }
        var del by remember { mutableStateOf(false) }
        Box(
                Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SatriaColors.Surface)
                        .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                        ) { if (onDelete != null) del = !del }
        ) {
                Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                        Box(
                                contentAlignment = Alignment.Center,
                                modifier =
                                        Modifier.size(38.dp)
                                                .clip(CircleShape)
                                                .background(
                                                        (cat?.color ?: Color(0xFF95A5A6)).copy(
                                                                alpha = 0.15f
                                                        )
                                                )
                        ) {
                                Text(
                                        if (tx.type == "transfer") "↔️" else cat?.emoji ?: "💸",
                                        fontSize = 16.sp
                                )
                        }
                        Column(Modifier.weight(1f)) {
                                Text(
                                        cat?.label
                                                ?: if (tx.type == "transfer") "Transfer"
                                                else tx.categoryKey,
                                        color = SatriaColors.TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                )
                                Text(
                                        if (tx.note.isNotBlank()) tx.note else wallet?.name ?: "",
                                        color = SatriaColors.TextTertiary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                )
                        }
                        Text(
                                "$sign${formatAmountShort(tx.amount, wallet?.currency ?: "IDR")}",
                                color = amtColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                        )
                }
                AnimatedVisibility(
                        del && onDelete != null,
                        modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                        Box(
                                contentAlignment = Alignment.Center,
                                modifier =
                                        Modifier.fillMaxHeight()
                                                .width(64.dp)
                                                .background(Color(0xFFE74C3C).copy(alpha = 0.9f))
                                                .clickable(
                                                        interactionSource =
                                                                remember {
                                                                        MutableInteractionSource()
                                                                },
                                                        indication = null
                                                ) { onDelete?.invoke() }
                        ) { Text("🗑️", fontSize = 18.sp) }
                }
        }
}

// ─── ANALYTICS ────────────────────────────────────────────────────────────────
@Composable
private fun AnalyticsTab(
        monthTxs: List<MoneyTransaction>,
        totalExpense: Double,
        totalIncome: Double
) {
        val byCatExp =
                remember(monthTxs) {
                        monthTxs
                                .filter { it.type == "expense" }
                                .groupBy { it.categoryKey }
                                .mapValues { (_, t) -> t.sumOf { it.amount } }
                                .entries
                                .sortedByDescending { it.value }
                }
        val byCatInc =
                remember(monthTxs) {
                        monthTxs
                                .filter { it.type == "income" }
                                .groupBy { it.categoryKey }
                                .mapValues { (_, t) -> t.sumOf { it.amount } }
                                .entries
                                .sortedByDescending { it.value }
                }
        LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
        ) {
                item {
                        val net = totalIncome - totalExpense
                        val total = totalIncome + totalExpense
                        Box(
                                Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(SatriaColors.Surface)
                                        .padding(16.dp)
                        ) {
                                Column {
                                        Text(
                                                "Monthly Balance",
                                                color = SatriaColors.TextSecondary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                                "${if (net >= 0) "+" else ""}${formatAmountShort(net)}",
                                                color =
                                                        if (net >= 0) Color(0xFF2ECC71)
                                                        else Color(0xFFE74C3C),
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.Bold
                                        )
                                        if (total > 0) {
                                                Spacer(Modifier.height(10.dp))
                                                val inFrac = (totalIncome / total).toFloat()
                                                Box(
                                                        Modifier.fillMaxWidth()
                                                                .height(10.dp)
                                                                .clip(RoundedCornerShape(5.dp))
                                                                .background(Color(0xFFE74C3C))
                                                ) {
                                                        Box(
                                                                Modifier.fillMaxWidth(inFrac)
                                                                        .fillMaxHeight()
                                                                        .clip(
                                                                                RoundedCornerShape(
                                                                                        5.dp
                                                                                )
                                                                        )
                                                                        .background(
                                                                                Color(0xFF2ECC71)
                                                                        )
                                                        )
                                                }
                                                Spacer(Modifier.height(6.dp))
                                                Row(
                                                        Modifier.fillMaxWidth(),
                                                        horizontalArrangement =
                                                                Arrangement.SpaceBetween
                                                ) {
                                                        Row(
                                                                verticalAlignment =
                                                                        Alignment.CenterVertically,
                                                                horizontalArrangement =
                                                                        Arrangement.spacedBy(4.dp)
                                                        ) {
                                                                Box(
                                                                        Modifier.size(8.dp)
                                                                                .clip(CircleShape)
                                                                                .background(
                                                                                        Color(
                                                                                                0xFF2ECC71
                                                                                        )
                                                                                )
                                                                )
                                                                Text(
                                                                        "Income",
                                                                        color =
                                                                                SatriaColors
                                                                                        .TextTertiary,
                                                                        fontSize = 11.sp
                                                                )
                                                        }
                                                        Row(
                                                                verticalAlignment =
                                                                        Alignment.CenterVertically,
                                                                horizontalArrangement =
                                                                        Arrangement.spacedBy(4.dp)
                                                        ) {
                                                                Box(
                                                                        Modifier.size(8.dp)
                                                                                .clip(CircleShape)
                                                                                .background(
                                                                                        Color(
                                                                                                0xFFE74C3C
                                                                                        )
                                                                                )
                                                                )
                                                                Text(
                                                                        "Expense",
                                                                        color =
                                                                                SatriaColors
                                                                                        .TextTertiary,
                                                                        fontSize = 11.sp
                                                                )
                                                        }
                                                }
                                        }
                                }
                        }
                }
                if (byCatExp.isNotEmpty()) {
                        item {
                                Box(
                                        Modifier.fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(SatriaColors.Surface)
                                                .padding(16.dp)
                                ) {
                                        Column {
                                                Text(
                                                        "Expenses by Category",
                                                        color = SatriaColors.TextSecondary,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium
                                                )
                                                Spacer(Modifier.height(12.dp))
                                                Row(
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        DonutChart(
                                                                byCatExp,
                                                                totalExpense,
                                                                Modifier.size(110.dp)
                                                        )
                                                        Spacer(Modifier.width(14.dp))
                                                        Column(
                                                                Modifier.weight(1f),
                                                                verticalArrangement =
                                                                        Arrangement.spacedBy(6.dp)
                                                        ) {
                                                                byCatExp.take(5).forEach { (k, amt)
                                                                        ->
                                                                        val cat =
                                                                                EXPENSE_CATEGORIES
                                                                                        .find {
                                                                                                it.key ==
                                                                                                        k
                                                                                        }
                                                                        val pct =
                                                                                if (totalExpense > 0
                                                                                )
                                                                                        (amt /
                                                                                                        totalExpense *
                                                                                                        100)
                                                                                                .toInt()
                                                                                else 0
                                                                        Row(
                                                                                verticalAlignment =
                                                                                        Alignment
                                                                                                .CenterVertically,
                                                                                horizontalArrangement =
                                                                                        Arrangement
                                                                                                .spacedBy(
                                                                                                        5.dp
                                                                                                )
                                                                        ) {
                                                                                Box(
                                                                                        Modifier.size(
                                                                                                        8.dp
                                                                                                )
                                                                                                .clip(
                                                                                                        CircleShape
                                                                                                )
                                                                                                .background(
                                                                                                        cat?.color
                                                                                                                ?: Color(
                                                                                                                        0xFF95A5A6
                                                                                                                )
                                                                                                )
                                                                                )
                                                                                Text(
                                                                                        "${cat?.emoji ?: ""} ${cat?.label ?: k}",
                                                                                        color =
                                                                                                SatriaColors
                                                                                                        .TextPrimary,
                                                                                        fontSize =
                                                                                                11.sp,
                                                                                        modifier =
                                                                                                Modifier.weight(
                                                                                                        1f
                                                                                                )
                                                                                )
                                                                                Text(
                                                                                        "$pct%",
                                                                                        color =
                                                                                                SatriaColors
                                                                                                        .TextTertiary,
                                                                                        fontSize =
                                                                                                10.sp
                                                                                )
                                                                        }
                                                                }
                                                        }
                                                }
                                        }
                                }
                        }
                }
                if (byCatInc.isNotEmpty()) {
                        item {
                                Box(
                                        Modifier.fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(SatriaColors.Surface)
                                                .padding(16.dp)
                                ) {
                                        Column {
                                                Text(
                                                        "Income by Category",
                                                        color = SatriaColors.TextSecondary,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium
                                                )
                                                Spacer(Modifier.height(8.dp))
                                                byCatInc.forEach { (k, amt) ->
                                                        val cat =
                                                                INCOME_CATEGORIES.find {
                                                                        it.key == k
                                                                }
                                                        val frac =
                                                                if (totalIncome > 0)
                                                                        (amt / totalIncome)
                                                                                .toFloat()
                                                                else 0f
                                                        Row(
                                                                Modifier.fillMaxWidth()
                                                                        .padding(vertical = 3.dp),
                                                                verticalAlignment =
                                                                        Alignment.CenterVertically
                                                        ) {
                                                                Text(
                                                                        cat?.emoji ?: "💰",
                                                                        fontSize = 14.sp,
                                                                        modifier =
                                                                                Modifier.width(
                                                                                        24.dp
                                                                                )
                                                                )
                                                                Spacer(Modifier.width(8.dp))
                                                                Column(Modifier.weight(1f)) {
                                                                        Row(
                                                                                Modifier.fillMaxWidth(),
                                                                                horizontalArrangement =
                                                                                        Arrangement
                                                                                                .SpaceBetween
                                                                        ) {
                                                                                Text(
                                                                                        cat?.label
                                                                                                ?: k,
                                                                                        color =
                                                                                                SatriaColors
                                                                                                        .TextPrimary,
                                                                                        fontSize =
                                                                                                12.sp
                                                                                )
                                                                                Text(
                                                                                        formatAmountShort(
                                                                                                amt
                                                                                        ),
                                                                                        color =
                                                                                                Color(
                                                                                                        0xFF2ECC71
                                                                                                ),
                                                                                        fontSize =
                                                                                                11.sp,
                                                                                        fontWeight =
                                                                                                FontWeight
                                                                                                        .SemiBold
                                                                                )
                                                                        }
                                                                        Spacer(
                                                                                Modifier.height(
                                                                                        2.dp
                                                                                )
                                                                        )
                                                                        Box(
                                                                                Modifier.fillMaxWidth()
                                                                                        .height(
                                                                                                4.dp
                                                                                        )
                                                                                        .clip(
                                                                                                RoundedCornerShape(
                                                                                                        2.dp
                                                                                                )
                                                                                        )
                                                                                        .background(
                                                                                                SatriaColors
                                                                                                        .SurfaceHigh
                                                                                        )
                                                                        ) {
                                                                                Box(
                                                                                        Modifier.fillMaxWidth(
                                                                                                        frac
                                                                                                )
                                                                                                .fillMaxHeight()
                                                                                                .clip(
                                                                                                        RoundedCornerShape(
                                                                                                                2.dp
                                                                                                        )
                                                                                                )
                                                                                                .background(
                                                                                                        cat?.color
                                                                                                                ?: Color(
                                                                                                                        0xFF2ECC71
                                                                                                                )
                                                                                                )
                                                                                )
                                                                        }
                                                                }
                                                        }
                                                }
                                        }
                                }
                        }
                }
                if (monthTxs.isEmpty())
                        item { EmptyCard("📊", "No data yet", "Add transactions to see analytics") }
                item { Spacer(Modifier.height(80.dp)) }
        }
}

@Composable
private fun DonutChart(
        data: List<Map.Entry<String, Double>>,
        total: Double,
        modifier: Modifier = Modifier
) {
        androidx.compose.foundation.Canvas(modifier = modifier) {
                val stroke = size.minDimension * 0.18f
                val radius = (size.minDimension - stroke) / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                var start = -90f
                data.take(6).forEach { (k, amt) ->
                        val cat = EXPENSE_CATEGORIES.find { it.key == k }
                        val sweep = if (total > 0) (amt / total * 360f).toFloat() else 0f
                        drawArc(
                                cat?.color ?: Color(0xFF95A5A6),
                                start,
                                sweep - 1f,
                                false,
                                topLeft = Offset(center.x - radius, center.y - radius),
                                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                                style = Stroke(stroke, cap = StrokeCap.Round)
                        )
                        start += sweep
                }
        }
}

// ─── WALLETS ──────────────────────────────────────────────────────────────────
@Composable
private fun WalletsTab(
        wallets: List<MoneyWallet>,
        allTxs: List<MoneyTransaction>,
        onAdd: () -> Unit,
        onDelete: (String) -> Unit
) {
        LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
        ) {
                item {
                        Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Text(
                                        "My Wallets",
                                        color = SatriaColors.TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                )
                                val acc = SatriaColors.Accent
                                Box(
                                        Modifier.clip(RoundedCornerShape(20.dp))
                                                .background(acc)
                                                .clickable(
                                                        interactionSource =
                                                                remember {
                                                                        MutableInteractionSource()
                                                                },
                                                        indication = null,
                                                        onClick = onAdd
                                                )
                                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                        Text(
                                                "+ Add Wallet",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                        )
                                }
                        }
                }
                if (wallets.isEmpty())
                        item {
                                EmptyCard("💳", "No wallets yet", "Tap + Add Wallet to get started")
                        }
                items(wallets, key = { it.id }) { w -> WalletCard(w, allTxs) { onDelete(w.id) } }
                item { Spacer(Modifier.height(80.dp)) }
        }
}

@Composable
private fun WalletCard(wallet: MoneyWallet, allTxs: List<MoneyTransaction>, onDelete: () -> Unit) {
        val balance = remember(allTxs) { walletBalance(wallet, allTxs) }
        val txs =
                remember(allTxs) {
                        allTxs.filter { it.walletId == wallet.id || it.toWalletId == wallet.id }
                }
        val income =
                remember(txs) {
                        txs.filter { it.type == "income" && it.walletId == wallet.id }.sumOf {
                                it.amount
                        }
                }
        val expense =
                remember(txs) {
                        txs.filter { it.type == "expense" && it.walletId == wallet.id }.sumOf {
                                it.amount
                        }
                }
        val clr = wallet.color.toColorSafe()
        var confirm by remember { mutableStateOf(false) }
        Box(
                Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(clr.copy(alpha = 0.1f))
                        .border(1.dp, clr.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                        .padding(16.dp)
        ) {
                Column {
                        Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                        ) {
                                Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                        Box(
                                                contentAlignment = Alignment.Center,
                                                modifier =
                                                        Modifier.size(44.dp)
                                                                .clip(CircleShape)
                                                                .background(clr.copy(alpha = 0.2f))
                                        ) { Text(wallet.emoji, fontSize = 22.sp) }
                                        Column {
                                                Text(
                                                        wallet.name,
                                                        color = SatriaColors.TextPrimary,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                        wallet.currency,
                                                        color = SatriaColors.TextTertiary,
                                                        fontSize = 11.sp
                                                )
                                        }
                                }
                                Box(
                                        Modifier.clip(CircleShape)
                                                .clickable(
                                                        interactionSource =
                                                                remember {
                                                                        MutableInteractionSource()
                                                                },
                                                        indication = null
                                                ) { confirm = !confirm }
                                                .padding(4.dp)
                                ) { Text(if (confirm) "✕" else "🗑️", fontSize = 16.sp) }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                                formatAmount(balance, wallet.currency),
                                color = if (balance >= 0) Color(0xFF2ECC71) else Color(0xFFE74C3C),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column {
                                        Text(
                                                "Income",
                                                color = SatriaColors.TextTertiary,
                                                fontSize = 10.sp
                                        )
                                        Text(
                                                formatAmountShort(income, wallet.currency),
                                                color = Color(0xFF2ECC71),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                        )
                                }
                                Column {
                                        Text(
                                                "Expense",
                                                color = SatriaColors.TextTertiary,
                                                fontSize = 10.sp
                                        )
                                        Text(
                                                formatAmountShort(expense, wallet.currency),
                                                color = Color(0xFFE74C3C),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                        )
                                }
                                Column {
                                        Text(
                                                "Transactions",
                                                color = SatriaColors.TextTertiary,
                                                fontSize = 10.sp
                                        )
                                        Text(
                                                "${txs.size}",
                                                color = SatriaColors.TextSecondary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                        )
                                }
                        }
                        AnimatedVisibility(confirm) {
                                Box(
                                        contentAlignment = Alignment.Center,
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .padding(top = 12.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color(0xFFE74C3C))
                                                        .clickable(
                                                                interactionSource =
                                                                        remember {
                                                                                MutableInteractionSource()
                                                                        },
                                                                indication = null,
                                                                onClick = onDelete
                                                        )
                                                        .padding(vertical = 8.dp)
                                ) {
                                        Text(
                                                "Delete Wallet & Transactions",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                        )
                                }
                        }
                }
        }
}

// ─── ADD TRANSACTION SHEET ────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTransactionSheet(
        wallets: List<MoneyWallet>,
        defaultWalletId: String?,
        onDismiss: () -> Unit,
        onSave: (String, String, Double, String, String, String, String) -> Unit
) {
        var txType by remember { mutableStateOf("expense") }
        var wId by remember { mutableStateOf(defaultWalletId ?: wallets.firstOrNull()?.id ?: "") }
        var toWId by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }
        var cat by remember { mutableStateOf<String?>(null) }
        var note by remember { mutableStateOf("") }
        var date by remember { mutableStateOf(todayDateStr()) }
        var err by remember { mutableStateOf("") }
        val cats = if (txType == "expense") EXPENSE_CATEGORIES else INCOME_CATEGORIES
        var showDatePicker by remember { mutableStateOf(false) }

        // Parse initial date string → millis for the M3 picker
        val initialMillis = remember {
                runCatching {
                                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                                .parse(todayDateStr())!!
                                        .time
                        }
                        .getOrDefault(System.currentTimeMillis())
        }
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        // Sync picker selection → date string
        LaunchedEffect(pickerState.selectedDateMillis) {
                pickerState.selectedDateMillis?.let {
                        date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it))
                }
        }

        val displayDate =
                remember(pickerState.selectedDateMillis) {
                        pickerState.selectedDateMillis?.let {
                                SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault())
                                        .format(Date(it))
                        }
                                ?: "Tap to pick a date"
                }

        Box(
                Modifier.fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onDismiss
                        )
        ) {
                Box(
                        Modifier.align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                                .background(SatriaColors.Surface)
                                .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                ) {}
                ) {
                        Column {
                                Box(
                                        Modifier.padding(top = 10.dp)
                                                .align(Alignment.CenterHorizontally)
                                                .width(36.dp)
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(SatriaColors.SurfaceHigh)
                                )
                                Column(
                                        Modifier.fillMaxWidth()
                                                .verticalScroll(rememberScrollState())
                                                .padding(horizontal = 20.dp, vertical = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                        Text(
                                                "Add Transaction",
                                                color = SatriaColors.TextPrimary,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.SemiBold
                                        )
                                        // Type picker
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                listOf(
                                                                "expense" to "💸 Expense",
                                                                "income" to "💰 Income",
                                                                "transfer" to "↔️ Transfer"
                                                        )
                                                        .forEach { (type, label) ->
                                                                val sel = type == txType
                                                                val acc = SatriaColors.Accent
                                                                Box(
                                                                        contentAlignment =
                                                                                Alignment.Center,
                                                                        modifier =
                                                                                Modifier.weight(1f)
                                                                                        .clip(
                                                                                                RoundedCornerShape(
                                                                                                        8.dp
                                                                                                )
                                                                                        )
                                                                                        .background(
                                                                                                if (sel
                                                                                                )
                                                                                                        acc.copy(
                                                                                                                alpha =
                                                                                                                        0.15f
                                                                                                        )
                                                                                                else
                                                                                                        SatriaColors
                                                                                                                .SurfaceMid
                                                                                        )
                                                                                        .clickable(
                                                                                                interactionSource =
                                                                                                        remember {
                                                                                                                MutableInteractionSource()
                                                                                                        },
                                                                                                indication =
                                                                                                        null
                                                                                        ) {
                                                                                                txType =
                                                                                                        type
                                                                                                cat =
                                                                                                        null
                                                                                        }
                                                                                        .padding(
                                                                                                vertical =
                                                                                                        8.dp
                                                                                        )
                                                                ) {
                                                                        Text(
                                                                                label,
                                                                                fontSize = 11.sp,
                                                                                color =
                                                                                        if (sel) acc
                                                                                        else
                                                                                                SatriaColors
                                                                                                        .TextSecondary,
                                                                                fontWeight =
                                                                                        if (sel)
                                                                                                FontWeight
                                                                                                        .SemiBold
                                                                                        else
                                                                                                FontWeight
                                                                                                        .Normal
                                                                        )
                                                                }
                                                        }
                                        }
                                        // Wallet
                                        if (wallets.isNotEmpty()) {
                                                Text(
                                                        "From Wallet",
                                                        color = SatriaColors.TextTertiary,
                                                        fontSize = 11.sp
                                                )
                                                LazyRow(
                                                        horizontalArrangement =
                                                                Arrangement.spacedBy(8.dp)
                                                ) {
                                                        items(wallets) { w ->
                                                                val sel = w.id == wId
                                                                val clr = w.color.toColorSafe()
                                                                Box(
                                                                        contentAlignment =
                                                                                Alignment.Center,
                                                                        modifier =
                                                                                Modifier.clip(
                                                                                                RoundedCornerShape(
                                                                                                        8.dp
                                                                                                )
                                                                                        )
                                                                                        .background(
                                                                                                if (sel
                                                                                                )
                                                                                                        clr.copy(
                                                                                                                alpha =
                                                                                                                        0.2f
                                                                                                        )
                                                                                                else
                                                                                                        SatriaColors
                                                                                                                .SurfaceMid
                                                                                        )
                                                                                        .border(
                                                                                                if (sel
                                                                                                )
                                                                                                        1.dp
                                                                                                else
                                                                                                        0.dp,
                                                                                                clr.copy(
                                                                                                        alpha =
                                                                                                                if (sel
                                                                                                                )
                                                                                                                        0.5f
                                                                                                                else
                                                                                                                        0f
                                                                                                ),
                                                                                                RoundedCornerShape(
                                                                                                        8.dp
                                                                                                )
                                                                                        )
                                                                                        .clickable(
                                                                                                interactionSource =
                                                                                                        remember {
                                                                                                                MutableInteractionSource()
                                                                                                        },
                                                                                                indication =
                                                                                                        null
                                                                                        ) {
                                                                                                wId =
                                                                                                        w.id
                                                                                        }
                                                                                        .padding(
                                                                                                horizontal =
                                                                                                        12.dp,
                                                                                                vertical =
                                                                                                        8.dp
                                                                                        )
                                                                ) {
                                                                        Text(
                                                                                "${w.emoji} ${w.name}",
                                                                                fontSize = 12.sp,
                                                                                color =
                                                                                        if (sel)
                                                                                                SatriaColors
                                                                                                        .TextPrimary
                                                                                        else
                                                                                                SatriaColors
                                                                                                        .TextSecondary
                                                                        )
                                                                }
                                                        }
                                                }
                                        } else {
                                                Box(
                                                        Modifier.fillMaxWidth()
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(SatriaColors.SurfaceMid)
                                                                .padding(12.dp)
                                                ) {
                                                        Text(
                                                                "⚠️ No wallets — create one in Wallets tab first",
                                                                color = SatriaColors.TextTertiary,
                                                                fontSize = 12.sp
                                                        )
                                                }
                                        }
                                        // To wallet
                                        if (txType == "transfer" && wallets.size > 1) {
                                                Text(
                                                        "To Wallet",
                                                        color = SatriaColors.TextTertiary,
                                                        fontSize = 11.sp
                                                )
                                                LazyRow(
                                                        horizontalArrangement =
                                                                Arrangement.spacedBy(8.dp)
                                                ) {
                                                        items(wallets.filter { it.id != wId }) { w
                                                                ->
                                                                val sel = w.id == toWId
                                                                val clr = w.color.toColorSafe()
                                                                Box(
                                                                        contentAlignment =
                                                                                Alignment.Center,
                                                                        modifier =
                                                                                Modifier.clip(
                                                                                                RoundedCornerShape(
                                                                                                        8.dp
                                                                                                )
                                                                                        )
                                                                                        .background(
                                                                                                if (sel
                                                                                                )
                                                                                                        clr.copy(
                                                                                                                alpha =
                                                                                                                        0.2f
                                                                                                        )
                                                                                                else
                                                                                                        SatriaColors
                                                                                                                .SurfaceMid
                                                                                        )
                                                                                        .clickable(
                                                                                                interactionSource =
                                                                                                        remember {
                                                                                                                MutableInteractionSource()
                                                                                                        },
                                                                                                indication =
                                                                                                        null
                                                                                        ) {
                                                                                                toWId =
                                                                                                        w.id
                                                                                        }
                                                                                        .padding(
                                                                                                horizontal =
                                                                                                        12.dp,
                                                                                                vertical =
                                                                                                        8.dp
                                                                                        )
                                                                ) {
                                                                        Text(
                                                                                "${w.emoji} ${w.name}",
                                                                                fontSize = 12.sp,
                                                                                color =
                                                                                        if (sel)
                                                                                                SatriaColors
                                                                                                        .TextPrimary
                                                                                        else
                                                                                                SatriaColors
                                                                                                        .TextSecondary
                                                                        )
                                                                }
                                                        }
                                                }
                                        }
                                        // Amount
                                        val acc = SatriaColors.Accent
                                        OutlinedTextField(
                                                value = amount,
                                                onValueChange = { v ->
                                                        amount =
                                                                v.filter {
                                                                        it.isDigit() || it == '.'
                                                                }
                                                },
                                                label = { Text("Amount") },
                                                keyboardOptions =
                                                        KeyboardOptions(
                                                                keyboardType = KeyboardType.Decimal,
                                                                imeAction = ImeAction.Next
                                                        ),
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth(),
                                                colors =
                                                        OutlinedTextFieldDefaults.colors(
                                                                focusedBorderColor = acc,
                                                                unfocusedBorderColor =
                                                                        SatriaColors.BorderLight,
                                                                focusedLabelColor = acc,
                                                                unfocusedLabelColor =
                                                                        SatriaColors.TextTertiary,
                                                                focusedTextColor =
                                                                        SatriaColors.TextPrimary,
                                                                unfocusedTextColor =
                                                                        SatriaColors.TextPrimary,
                                                                cursorColor = acc
                                                        ),
                                                shape = RoundedCornerShape(10.dp)
                                        )
                                        // Category
                                        if (txType != "transfer") {
                                                Text(
                                                        "Category",
                                                        color = SatriaColors.TextTertiary,
                                                        fontSize = 11.sp
                                                )
                                                LazyRow(
                                                        horizontalArrangement =
                                                                Arrangement.spacedBy(8.dp)
                                                ) {
                                                        items(cats) { c ->
                                                                val sel = c.key == cat
                                                                Box(
                                                                        contentAlignment =
                                                                                Alignment.Center,
                                                                        modifier =
                                                                                Modifier.clip(
                                                                                                RoundedCornerShape(
                                                                                                        8.dp
                                                                                                )
                                                                                        )
                                                                                        .background(
                                                                                                if (sel
                                                                                                )
                                                                                                        c.color
                                                                                                                .copy(
                                                                                                                        alpha =
                                                                                                                                0.2f
                                                                                                                )
                                                                                                else
                                                                                                        SatriaColors
                                                                                                                .SurfaceMid
                                                                                        )
                                                                                        .border(
                                                                                                if (sel
                                                                                                )
                                                                                                        1.dp
                                                                                                else
                                                                                                        0.dp,
                                                                                                c.color
                                                                                                        .copy(
                                                                                                                alpha =
                                                                                                                        if (sel
                                                                                                                        )
                                                                                                                                0.5f
                                                                                                                        else
                                                                                                                                0f
                                                                                                        ),
                                                                                                RoundedCornerShape(
                                                                                                        8.dp
                                                                                                )
                                                                                        )
                                                                                        .clickable(
                                                                                                interactionSource =
                                                                                                        remember {
                                                                                                                MutableInteractionSource()
                                                                                                        },
                                                                                                indication =
                                                                                                        null
                                                                                        ) {
                                                                                                cat =
                                                                                                        c.key
                                                                                        }
                                                                                        .padding(
                                                                                                horizontal =
                                                                                                        10.dp,
                                                                                                vertical =
                                                                                                        8.dp
                                                                                        )
                                                                ) {
                                                                        Column(
                                                                                horizontalAlignment =
                                                                                        Alignment
                                                                                                .CenterHorizontally
                                                                        ) {
                                                                                Text(
                                                                                        c.emoji,
                                                                                        fontSize =
                                                                                                18.sp
                                                                                )
                                                                                Text(
                                                                                        c.label,
                                                                                        fontSize =
                                                                                                9.sp,
                                                                                        color =
                                                                                                if (sel
                                                                                                )
                                                                                                        SatriaColors
                                                                                                                .TextPrimary
                                                                                                else
                                                                                                        SatriaColors
                                                                                                                .TextTertiary
                                                                                )
                                                                        }
                                                                }
                                                        }
                                                }
                                        }
                                        // Note
                                        OutlinedTextField(
                                                value = note,
                                                onValueChange = { note = it },
                                                label = { Text("Note (optional)") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth(),
                                                colors =
                                                        OutlinedTextFieldDefaults.colors(
                                                                focusedBorderColor = acc,
                                                                unfocusedBorderColor =
                                                                        SatriaColors.BorderLight,
                                                                focusedLabelColor = acc,
                                                                unfocusedLabelColor =
                                                                        SatriaColors.TextTertiary,
                                                                focusedTextColor =
                                                                        SatriaColors.TextPrimary,
                                                                unfocusedTextColor =
                                                                        SatriaColors.TextPrimary,
                                                                cursorColor = acc
                                                        ),
                                                shape = RoundedCornerShape(10.dp)
                                        )
                                        // Date — tap to open M3 DatePickerDialog
                                        Row(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .clip(RoundedCornerShape(10.dp))
                                                                .background(SatriaColors.Surface)
                                                                .clickable { showDatePicker = true }
                                                                .padding(
                                                                        horizontal = 16.dp,
                                                                        vertical = 14.dp
                                                                ),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                                Text("📅", fontSize = 20.sp)
                                                Column {
                                                        Text(
                                                                "Date",
                                                                color = SatriaColors.TextSecondary,
                                                                fontSize = 11.sp
                                                        )
                                                        Text(
                                                                displayDate,
                                                                color =
                                                                        if (pickerState
                                                                                        .selectedDateMillis !=
                                                                                        null
                                                                        )
                                                                                SatriaColors
                                                                                        .TextPrimary
                                                                        else
                                                                                SatriaColors
                                                                                        .TextTertiary,
                                                                fontSize = 14.sp,
                                                                fontWeight = FontWeight.Medium
                                                        )
                                                }
                                        }
                                        if (err.isNotBlank())
                                                Text(
                                                        err,
                                                        color = Color(0xFFE74C3C),
                                                        fontSize = 12.sp
                                                )
                                        val accBtn = SatriaColors.Accent
                                        Button(
                                                onClick = {
                                                        val a = amount.toDoubleOrNull()
                                                        err =
                                                                when {
                                                                        wallets.isEmpty() ->
                                                                                "Add a wallet first"
                                                                        wId.isEmpty() ->
                                                                                "Select a wallet"
                                                                        a == null || a <= 0 ->
                                                                                "Enter a valid amount"
                                                                        txType != "transfer" &&
                                                                                cat == null ->
                                                                                "Select a category"
                                                                        txType == "transfer" &&
                                                                                toWId.isEmpty() ->
                                                                                "Select destination wallet"
                                                                        else -> {
                                                                                onSave(
                                                                                        wId,
                                                                                        txType,
                                                                                        a,
                                                                                        cat
                                                                                                ?: "transfer",
                                                                                        note.trim(),
                                                                                        date,
                                                                                        toWId
                                                                                )
                                                                                return@Button
                                                                        }
                                                                }
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp),
                                                colors =
                                                        ButtonDefaults.buttonColors(
                                                                containerColor = accBtn
                                                        )
                                        ) {
                                                Text(
                                                        "Save Transaction",
                                                        color = Color.White,
                                                        fontWeight = FontWeight.SemiBold
                                                )
                                        }
                                        Spacer(Modifier.height(8.dp))
                                }
                        }
                }
        }

        // ── Material3 DatePickerDialog — pure Compose, lightweight ─────────────
        if (showDatePicker) {
                DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                                TextButton(onClick = { showDatePicker = false }) {
                                        Text(
                                                "OK",
                                                color = SatriaColors.Accent,
                                                fontWeight = FontWeight.Bold
                                        )
                                }
                        },
                        dismissButton = {
                                TextButton(onClick = { showDatePicker = false }) {
                                        Text("Cancel", color = SatriaColors.TextSecondary)
                                }
                        },
                        colors =
                                DatePickerDefaults.colors(
                                        containerColor = SatriaColors.Surface,
                                        titleContentColor = SatriaColors.TextSecondary,
                                        headlineContentColor = SatriaColors.TextPrimary,
                                        weekdayContentColor = SatriaColors.TextSecondary,
                                        subheadContentColor = SatriaColors.TextSecondary,
                                        navigationContentColor = SatriaColors.TextPrimary,
                                        yearContentColor = SatriaColors.TextPrimary,
                                        currentYearContentColor = SatriaColors.Accent,
                                        selectedYearContentColor = Color.White,
                                        selectedYearContainerColor = SatriaColors.Accent,
                                        dayContentColor = SatriaColors.TextPrimary,
                                        selectedDayContentColor = Color.White,
                                        selectedDayContainerColor = SatriaColors.Accent,
                                        todayContentColor = SatriaColors.Accent,
                                        todayDateBorderColor = SatriaColors.Accent,
                                        disabledDayContentColor = SatriaColors.TextTertiary,
                                        disabledSelectedDayContentColor = SatriaColors.TextTertiary,
                                        disabledSelectedDayContainerColor = SatriaColors.SurfaceMid,
                                ),
                ) {
                        DatePicker(
                                state = pickerState,
                                colors =
                                        DatePickerDefaults.colors(
                                                containerColor = SatriaColors.Surface,
                                        )
                        )
                }
        }
}

// ─── ADD WALLET SHEET ─────────────────────────────────────────────────────────
@Composable
private fun AddWalletSheet(
        onDismiss: () -> Unit,
        onSave: (String, String, String, String) -> Unit
) {
        var name by remember { mutableStateOf("") }
        var emoji by remember { mutableStateOf(WALLET_EMOJIS.first()) }
        var color by remember { mutableStateOf(WALLET_COLORS.first()) }
        var currency by remember { mutableStateOf(CURRENCIES.first()) }
        var err by remember { mutableStateOf("") }
        val acc = SatriaColors.Accent

        Box(
                Modifier.fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onDismiss
                        )
        ) {
                Box(
                        Modifier.align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                                .background(SatriaColors.Surface)
                                .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                ) {}
                ) {
                        Column {
                                Box(
                                        Modifier.padding(top = 10.dp)
                                                .align(Alignment.CenterHorizontally)
                                                .width(36.dp)
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(SatriaColors.SurfaceHigh)
                                )
                                Column(
                                        Modifier.fillMaxWidth()
                                                .padding(horizontal = 20.dp, vertical = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                        Text(
                                                "New Wallet",
                                                color = SatriaColors.TextPrimary,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                                "Icon",
                                                color = SatriaColors.TextTertiary,
                                                fontSize = 11.sp
                                        )
                                        LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                                items(WALLET_EMOJIS) { e ->
                                                        Box(
                                                                contentAlignment = Alignment.Center,
                                                                modifier =
                                                                        Modifier.size(40.dp)
                                                                                .clip(CircleShape)
                                                                                .background(
                                                                                        if (e ==
                                                                                                        emoji
                                                                                        )
                                                                                                acc.copy(
                                                                                                        alpha =
                                                                                                                0.2f
                                                                                                )
                                                                                        else
                                                                                                SatriaColors
                                                                                                        .SurfaceMid
                                                                                )
                                                                                .clickable(
                                                                                        interactionSource =
                                                                                                remember {
                                                                                                        MutableInteractionSource()
                                                                                                },
                                                                                        indication =
                                                                                                null
                                                                                ) { emoji = e }
                                                        ) { Text(e, fontSize = 20.sp) }
                                                }
                                        }
                                        Text(
                                                "Color",
                                                color = SatriaColors.TextTertiary,
                                                fontSize = 11.sp
                                        )
                                        LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                                items(WALLET_COLORS) { c ->
                                                        val clr = c.toColorSafe()
                                                        Box(
                                                                Modifier.size(32.dp)
                                                                        .clip(CircleShape)
                                                                        .background(clr)
                                                                        .then(
                                                                                if (c == color)
                                                                                        Modifier.border(
                                                                                                2.dp,
                                                                                                Color.White,
                                                                                                CircleShape
                                                                                        )
                                                                                else Modifier
                                                                        )
                                                                        .clickable(
                                                                                interactionSource =
                                                                                        remember {
                                                                                                MutableInteractionSource()
                                                                                        },
                                                                                indication = null
                                                                        ) { color = c }
                                                        )
                                                }
                                        }
                                        OutlinedTextField(
                                                value = name,
                                                onValueChange = { name = it },
                                                label = { Text("Wallet name") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth(),
                                                colors =
                                                        OutlinedTextFieldDefaults.colors(
                                                                focusedBorderColor = acc,
                                                                unfocusedBorderColor =
                                                                        SatriaColors.BorderLight,
                                                                focusedLabelColor = acc,
                                                                unfocusedLabelColor =
                                                                        SatriaColors.TextTertiary,
                                                                focusedTextColor =
                                                                        SatriaColors.TextPrimary,
                                                                unfocusedTextColor =
                                                                        SatriaColors.TextPrimary,
                                                                cursorColor = acc
                                                        ),
                                                shape = RoundedCornerShape(10.dp)
                                        )
                                        Text(
                                                "Currency",
                                                color = SatriaColors.TextTertiary,
                                                fontSize = 11.sp
                                        )
                                        LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                                items(CURRENCIES) { cur ->
                                                        val sel = cur == currency
                                                        Box(
                                                                contentAlignment = Alignment.Center,
                                                                modifier =
                                                                        Modifier.clip(
                                                                                        RoundedCornerShape(
                                                                                                8.dp
                                                                                        )
                                                                                )
                                                                                .background(
                                                                                        if (sel)
                                                                                                acc.copy(
                                                                                                        alpha =
                                                                                                                0.15f
                                                                                                )
                                                                                        else
                                                                                                SatriaColors
                                                                                                        .SurfaceMid
                                                                                )
                                                                                .border(
                                                                                        if (sel)
                                                                                                1.dp
                                                                                        else 0.dp,
                                                                                        acc.copy(
                                                                                                alpha =
                                                                                                        if (sel
                                                                                                        )
                                                                                                                0.4f
                                                                                                        else
                                                                                                                0f
                                                                                        ),
                                                                                        RoundedCornerShape(
                                                                                                8.dp
                                                                                        )
                                                                                )
                                                                                .clickable(
                                                                                        interactionSource =
                                                                                                remember {
                                                                                                        MutableInteractionSource()
                                                                                                },
                                                                                        indication =
                                                                                                null
                                                                                ) { currency = cur }
                                                                                .padding(
                                                                                        horizontal =
                                                                                                12.dp,
                                                                                        vertical =
                                                                                                8.dp
                                                                                )
                                                        ) {
                                                                Text(
                                                                        cur,
                                                                        fontSize = 12.sp,
                                                                        color =
                                                                                if (sel) acc
                                                                                else
                                                                                        SatriaColors
                                                                                                .TextSecondary,
                                                                        fontWeight =
                                                                                if (sel)
                                                                                        FontWeight
                                                                                                .SemiBold
                                                                                else
                                                                                        FontWeight
                                                                                                .Normal
                                                                )
                                                        }
                                                }
                                        }
                                        if (err.isNotBlank())
                                                Text(
                                                        err,
                                                        color = Color(0xFFE74C3C),
                                                        fontSize = 12.sp
                                                )
                                        val accBtn = SatriaColors.Accent
                                        Button(
                                                onClick = {
                                                        if (name.isBlank())
                                                                err = "Enter a wallet name"
                                                        else
                                                                onSave(
                                                                        name.trim(),
                                                                        emoji,
                                                                        color,
                                                                        currency
                                                                )
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp),
                                                colors =
                                                        ButtonDefaults.buttonColors(
                                                                containerColor = accBtn
                                                        )
                                        ) {
                                                Text(
                                                        "Create Wallet",
                                                        color = Color.White,
                                                        fontWeight = FontWeight.SemiBold
                                                )
                                        }
                                        Spacer(Modifier.height(8.dp))
                                }
                        }
                }
        }
}

@Composable
private fun EmptyCard(emoji: String, title: String, sub: String) {
        Box(
                contentAlignment = Alignment.Center,
                modifier =
                        Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(SatriaColors.Surface)
                                .padding(24.dp)
        ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(emoji, fontSize = 32.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                                title,
                                color = SatriaColors.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                        )
                        Text(
                                sub,
                                color = SatriaColors.TextTertiary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                        )
                }
        }
}
