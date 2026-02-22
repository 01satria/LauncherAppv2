package id.satria.launcher.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.satria.launcher.data.HabitItem
import id.satria.launcher.ui.theme.SatriaColors
import java.text.SimpleDateFormat
import java.util.*

private val DONE_GREEN   = Color(0xFF30D158)
private val HABIT_CARD   = Color(0xFF0D0D0D)
private val EMOJIS = listOf("💪","🏃","📚","💧","🥗","😴","🧘","🎯","✍️","🎵","🌿","🚴","🏋️","🧹","☕")

@Composable
fun HabitTool(
    habits   : List<HabitItem>,
    onAdd    : (name: String, emoji: String) -> Unit,
    onToggle : (String) -> Unit,
    onDelete : (String) -> Unit,
) {
    val todayKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val doneCount = habits.count { it.doneToday(todayKey) }
    var showAdd  by remember { mutableStateOf(false) }
    var newName  by remember { mutableStateOf("") }
    var newEmoji by remember { mutableStateOf(EMOJIS.first()) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Habits", color = SatriaColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text("$doneCount / ${habits.size} done today", color = SatriaColors.TextTertiary, fontSize = 12.sp)
                }
                Box(modifier = Modifier.clip(RoundedCornerShape(50.dp))
                    .background(if (showAdd) SatriaColors.Surface else SatriaColors.Accent)
                    .clickable(interactionSource = remember{MutableInteractionSource()}, indication = null) { showAdd = !showAdd }
                    .padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(if (showAdd) "Cancel" else "+ Add", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (habits.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                val progress = if (habits.isEmpty()) 0f else doneCount.toFloat() / habits.size
                Box(modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF1A1A1A))) {
                    Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(DONE_GREEN))
                }
            }
        }

        // Add form
        if (showAdd) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(14.dp)).background(HABIT_CARD).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TextField(value = newName, onValueChange = { newName = it },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)),
                    placeholder = { Text("Habit name...", color = SatriaColors.TextTertiary) },
                    colors = toolTextFieldColors(), singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (newName.isNotBlank()) { onAdd(newName.trim(), newEmoji); newName = ""; showAdd = false }
                    }))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(EMOJIS) { emoji ->
                        Box(modifier = Modifier.size(38.dp).clip(CircleShape)
                            .background(if (newEmoji == emoji) SatriaColors.Accent else SatriaColors.Surface)
                            .clickable(interactionSource = remember{MutableInteractionSource()}, indication = null) { newEmoji = emoji },
                            contentAlignment = Alignment.Center) {
                            Text(emoji, fontSize = 18.sp)
                        }
                    }
                }
                Button(onClick = {
                    if (newName.isNotBlank()) { onAdd(newName.trim(), newEmoji); newName = ""; showAdd = false }
                }, enabled = newName.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SatriaColors.Accent),
                    shape = RoundedCornerShape(10.dp)) {
                    Text("Add Habit", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (habits.isEmpty() && !showAdd) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No habits yet.\nTap + Add to start.", color = SatriaColors.TextTertiary,
                    fontSize = 14.sp, textAlign = TextAlign.Center)
            }
            return@Column
        }

        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(habits, key = { it.id }) { habit ->
                HabitRow(
                    habit    = habit,
                    todayKey = todayKey,
                    onToggle = { onToggle(habit.id) },
                    onDelete = { onDelete(habit.id) },
                )
            }
        }
    }
}

@Composable
private fun HabitRow(habit: HabitItem, todayKey: String, onToggle: () -> Unit, onDelete: () -> Unit) {
    val done = habit.doneToday(todayKey)
    val checkBg by animateColorAsState(
        targetValue = if (done) DONE_GREEN else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMedium), label = "habitBg")

    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(HABIT_CARD)
        .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(habit.emoji, fontSize = 24.sp, modifier = Modifier.width(32.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(habit.name,
                color = if (done) SatriaColors.TextSecondary else SatriaColors.TextPrimary,
                fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (habit.streak > 0)
                Text("🔥 ${habit.streak} day streak", color = Color(0xFFFF9F0A), fontSize = 11.sp)
        }
        // Delete
        Box(modifier = Modifier.size(28.dp)
            .clickable(interactionSource = remember{MutableInteractionSource()}, indication = null) { onDelete() },
            contentAlignment = Alignment.Center) {
            Text("✕", color = SatriaColors.TextTertiary, fontSize = 13.sp)
        }
        // Check
        Box(modifier = Modifier.size(28.dp).clip(CircleShape)
            .background(checkBg)
            .clickable(interactionSource = remember{MutableInteractionSource()}, indication = null) { onToggle() }
            .then(if (!done) Modifier.background(SatriaColors.SurfaceMid, CircleShape) else Modifier),
            contentAlignment = Alignment.Center) {
            if (done) Text("✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}
