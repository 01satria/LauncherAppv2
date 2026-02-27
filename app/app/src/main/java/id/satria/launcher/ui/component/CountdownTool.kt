package id.satria.launcher.ui.component

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import id.satria.launcher.data.CountdownItem
import id.satria.launcher.ui.theme.SatriaColors
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountdownTool(
    countdowns: List<CountdownItem>,
    onAdd: (name: String, isoDate: String) -> Unit,
    onRemove: (String) -> Unit,
) {
    var name       by remember { mutableStateOf("") }
    var showPicker by remember { mutableStateOf(false) }
    var nameError  by remember { mutableStateOf(false) }

    // Material3 DatePickerState — tidak crash karena pakai Compose sepenuhnya
    val today = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = today + TimeUnit.DAYS.toMillis(1),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis > today
        }
    )

    val displayDate = remember(pickerState.selectedDateMillis) {
        pickerState.selectedDateMillis?.let {
            SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(Date(it))
        } ?: "Tap to pick a date"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("⏳ Countdown", color = SatriaColors.TextPrimary,
            fontSize = 17.sp, fontWeight = FontWeight.SemiBold)

        // Event name
        TextField(
            value = name,
            onValueChange = { name = it; nameError = false },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)),
            placeholder = { Text("Event name...", color = SatriaColors.TextTertiary) },
            colors = toolTextFieldColors(),
            singleLine = true,
            isError = nameError,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        if (nameError)
            Text("Please enter event name", color = SatriaColors.Danger, fontSize = 12.sp)

        // Date picker button — opens M3 DatePickerDialog (no force close)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SatriaColors.Surface)
                .clickable { showPicker = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("📅", fontSize = 20.sp)
            Column {
                Text("Date", color = SatriaColors.TextSecondary, fontSize = 11.sp)
                Text(displayDate,
                    color = if (pickerState.selectedDateMillis != null)
                        SatriaColors.TextPrimary else SatriaColors.TextTertiary,
                    fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }

        // Add button
        Button(
            onClick = {
                if (name.isBlank()) { nameError = true; return@Button }
                val millis = pickerState.selectedDateMillis ?: return@Button
                val iso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(millis))
                onAdd(name.trim(), iso)
                name = ""
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = SatriaColors.Accent),
            shape  = RoundedCornerShape(10.dp),
        ) {
            Text("＋ Start Countdown", color = Color.White, fontWeight = FontWeight.SemiBold)
        }

        HorizontalDivider(color = SatriaColors.Border, modifier = Modifier.padding(vertical = 4.dp))

        if (countdowns.isEmpty()) {
            Text("No countdowns yet.\nAdd your first event above.",
                color = SatriaColors.TextTertiary, fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
        } else {
            countdowns.forEach { item ->
                key(item.id) {
                    CountdownRow(item = item, onRemove = { onRemove(item.id) })
                }
            }
        }
    }

    // ── Material3 DatePickerDialog — pure Compose, zero force close ─────────
    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("OK", color = SatriaColors.Accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancel", color = SatriaColors.TextSecondary)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor           = SatriaColors.Surface,
                titleContentColor        = SatriaColors.TextSecondary,
                headlineContentColor     = SatriaColors.TextPrimary,
                weekdayContentColor      = SatriaColors.TextSecondary,
                subheadContentColor      = SatriaColors.TextSecondary,
                navigationContentColor   = SatriaColors.TextPrimary,
                yearContentColor         = SatriaColors.TextPrimary,
                currentYearContentColor  = SatriaColors.Accent,
                selectedYearContentColor = Color.White,
                selectedYearContainerColor  = SatriaColors.Accent,
                dayContentColor             = SatriaColors.TextPrimary,
                selectedDayContentColor     = Color.White,
                selectedDayContainerColor   = SatriaColors.Accent,
                todayContentColor           = SatriaColors.Accent,
                todayDateBorderColor        = SatriaColors.Accent,
                disabledDayContentColor     = SatriaColors.TextTertiary,
                disabledSelectedDayContentColor    = SatriaColors.TextTertiary,
                disabledSelectedDayContainerColor  = SatriaColors.SurfaceMid,
            ),
        ) {
            DatePicker(
                state  = pickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = SatriaColors.Surface,
                )
            )
        }
    }
}

@Composable
private fun CountdownRow(item: CountdownItem, onRemove: () -> Unit) {
    val days = daysLeft(item.targetDate)
    val past = days < 0

    val bgColor = SatriaColors.CardBg
    val daysLabel = when {
        past      -> "${Math.abs(days)}d ago"
        days == 0 -> "Today! 🎉"
        days == 1 -> "Tomorrow"
        else      -> "${days} days"
    }
    val daysColor = when {
        days == 0 -> Color(0xFF4CAF50)
        past      -> SatriaColors.Danger
        days <= 7 -> Color(0xFFFFB74D)
        else      -> SatriaColors.TextPrimary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.name, color = SatriaColors.TextPrimary,
                fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(formatDate(item.targetDate), color = SatriaColors.TextTertiary, fontSize = 11.sp)
        }
        Text(daysLabel, color = daysColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text("✕", color = SatriaColors.TextTertiary, fontSize = 16.sp,
            modifier = Modifier.clickable { onRemove() }.padding(4.dp))
    }
    Spacer(Modifier.height(6.dp))
}

private fun daysLeft(iso: String): Int = runCatching {
    val target = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(iso.take(10))!!
    val now    = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.time
    TimeUnit.MILLISECONDS.toDays(target.time - now.time).toInt()
}.getOrDefault(0)

private fun formatDate(iso: String): String = runCatching {
    val d = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(iso.take(10))!!
    SimpleDateFormat("d MMM yyyy, EEEE", Locale.getDefault()).format(d)
}.getOrDefault(iso)
