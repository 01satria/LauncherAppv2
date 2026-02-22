package id.satria.launcher.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import id.satria.launcher.data.NoteItem
import id.satria.launcher.ui.theme.SatriaColors
import java.text.SimpleDateFormat
import java.util.*

private val NOTE_CARD = Color(0xFF0D0D0D)

@Composable
fun NotesTool(
    notes    : List<NoteItem>,
    onAdd    : (String) -> Unit,
    onUpdate : (String, String) -> Unit,
    onDelete : (String) -> Unit,
) {
    var input     by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<String?>(null) }
    var editText  by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Input bar
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = input, onValueChange = { input = it },
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)),
                placeholder = { Text("New note...", color = SatriaColors.TextTertiary) },
                colors = toolTextFieldColors(), maxLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (input.isNotBlank()) { onAdd(input.trim()); input = "" } }),
            )
            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                .background(if (input.isNotBlank()) SatriaColors.Accent else SatriaColors.Surface)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    if (input.isNotBlank()) { onAdd(input.trim()); input = "" }
                }, contentAlignment = Alignment.Center) {
                Text("+", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Light)
            }
        }

        if (notes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No notes yet.\nTap above to add one.",
                    color = SatriaColors.TextTertiary, fontSize = 14.sp, textAlign = TextAlign.Center)
            }
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(notes, key = { it.id }) { note ->
                NoteCard(
                    note = note,
                    isEditing = editingId == note.id,
                    editText = editText,
                    onEditStart  = { editingId = note.id; editText = note.text },
                    onEditChange = { editText = it },
                    onEditSave   = { if (editText.isNotBlank()) onUpdate(note.id, editText.trim()); editingId = null },
                    onEditCancel = { editingId = null },
                    onDelete     = { onDelete(note.id) },
                )
            }
        }
    }
}

@Composable
private fun NoteCard(
    note        : NoteItem,
    isEditing   : Boolean,
    editText    : String,
    onEditStart : () -> Unit,
    onEditChange: (String) -> Unit,
    onEditSave  : () -> Unit,
    onEditCancel: () -> Unit,
    onDelete    : () -> Unit,
) {
    val dateFmt = remember { SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()) }
    val dateStr = remember(note.updatedAt) { dateFmt.format(Date(note.updatedAt)) }

    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(NOTE_CARD)
        .clickable(interactionSource = remember{MutableInteractionSource()}, indication = null, onClick = onEditStart)
        .padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (isEditing) {
            TextField(value = editText, onValueChange = onEditChange,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                colors = toolTextFieldColors(), maxLines = 8,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onEditSave() }))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onEditSave) {
                    Text("Save", color = SatriaColors.Accent, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                TextButton(onClick = onEditCancel) {
                    Text("Cancel", color = SatriaColors.TextTertiary, fontSize = 13.sp)
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDelete) {
                    Text("Delete", color = SatriaColors.Danger, fontSize = 13.sp)
                }
            }
        } else {
            Text(note.text, color = SatriaColors.TextPrimary, fontSize = 15.sp, lineHeight = 22.sp)
            Text(dateStr, color = SatriaColors.TextTertiary, fontSize = 11.sp)
        }
    }
}
