package id.satria.launcher.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.satria.launcher.data.CountdownItem
import id.satria.launcher.ui.theme.SatriaColors
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun CountdownTool(
    countdowns: List<CountdownItem>,
    onAdd: (name: String, isoDate: String) -> Unit,
    onRemove: (String) -> Unit,
) {
    var name        by remember { mutableStateOf("") }
    var pickedDate  by remember { mutableStateOf<Date?>(null) }
    var showPicker  by remember { mutableStateOf(false) }

    val dateFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val isoFmt  = SimpleDateFormat("yyyy-MM-dd'T'00:00:00.000'Z'", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("⏳ Countdown", color = SatriaColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)

        // Name input
        TextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)),
            placeholder = { Text("Event name...", color = SatriaColors.TextTertiary) },
            colors = toolTextFieldColors(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )

        // Date picker button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SatriaColors.Surface)
                .clickable { showPicker = true }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("📅", fontSize = 18.sp)
            Text(
                text  = pickedDate?.let { dateFmt.format(it) } ?: "Pick a date",
                color = if (pickedDate != null) SatriaColors.TextPrimary else SatriaColors.TextTertiary,
                fontSize = 15.sp,
            )
        }

        Button(
            onClick = {
                if (name.isBlank() || pickedDate == null) return@Button
                onAdd(name.trim(), isoFmt.format(pickedDate!!))
                name = ""; pickedDate = null
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = SatriaColors.SurfaceMid),
        ) {
            Text("Start Countdown", color = SatriaColors.TextPrimary)
        }

        if (countdowns.isEmpty()) {
            Text("No countdowns yet.", color = SatriaColors.TextTertiary, fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }

        countdowns.forEach { item ->
            key(item.id) {
                val days = daysLeft(item.targetDate)
                val past = days < 0
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.name, color = SatriaColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text(
                            SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(
                                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(item.targetDate.take(10)) ?: Date()
                            ),
                            color = SatriaColors.TextTertiary, fontSize = 11.sp,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (past) Color(0xFF2A1A1A) else SatriaColors.SurfaceMid)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = when {
                                past      -> "${Math.abs(days)}d ago"
                                days == 0 -> "Today!"
                                else      -> "${days}d"
                            },
                            color = if (past) SatriaColors.Danger else SatriaColors.TextPrimary,
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text("✕", color = SatriaColors.TextTertiary, fontSize = 16.sp,
                        modifier = Modifier.clickable { onRemove(item.id) }.padding(4.dp))
                }
                HorizontalDivider(color = SatriaColors.Border)
            }
        }
    }

    // Simple date picker dialog menggunakan DatePickerDialog Android native
    if (showPicker) {
        val calendar = Calendar.getInstance()
        pickedDate?.let { calendar.time = it }
        NativeDatePickerDialog(
            initialYear  = calendar.get(Calendar.YEAR),
            initialMonth = calendar.get(Calendar.MONTH),
            initialDay   = calendar.get(Calendar.DAY_OF_MONTH),
            onConfirm    = { y, m, d -> pickedDate = Calendar.getInstance().apply { set(y, m, d) }.time; showPicker = false },
            onDismiss    = { showPicker = false },
        )
    }
}

private fun daysLeft(iso: String): Int {
    return try {
        val sdf  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val target = sdf.parse(iso.take(10)) ?: return 0
        val now    = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.time
        TimeUnit.MILLISECONDS.toDays(target.time - now.time).toInt()
    } catch (e: Exception) { 0 }
}

@Composable
private fun NativeDatePickerDialog(
    initialYear: Int,
    initialMonth: Int,
    initialDay: Int,
    onConfirm: (year: Int, month: Int, day: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    DisposableEffect(Unit) {
        val dialog = android.app.DatePickerDialog(
            context,
            { _, y, m, d -> onConfirm(y, m, d) },
            initialYear, initialMonth, initialDay,
        )
        dialog.datePicker.minDate = System.currentTimeMillis()
        dialog.setOnDismissListener { onDismiss() }
        dialog.show()
        onDispose { if (dialog.isShowing) dialog.dismiss() }
    }
}
