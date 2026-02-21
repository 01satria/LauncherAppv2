package id.satria.launcher.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.satria.launcher.data.TodoItem
import id.satria.launcher.ui.theme.SatriaColors

@Composable
fun TodoTool(
    todos: List<TodoItem>,
    onAdd: (String) -> Unit,
    onToggle: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    var input by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("📝 To Do", color = SatriaColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)

        // Input row
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)),
                placeholder = { Text("Add a task...", color = SatriaColors.TextTertiary) },
                colors = toolTextFieldColors(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (input.isNotBlank()) { onAdd(input.trim()); input = "" } }),
            )
            Button(
                onClick = { if (input.isNotBlank()) { onAdd(input.trim()); input = "" } },
                colors = ButtonDefaults.buttonColors(containerColor = SatriaColors.SurfaceMid),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            ) { Text("+", fontSize = 20.sp, color = SatriaColors.TextPrimary) }
        }

        if (todos.isEmpty()) {
            Text("No tasks yet. Add one!", color = SatriaColors.TextTertiary, fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }

        todos.forEach { todo ->
            key(todo.id) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Checkbox
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .then(if (todo.done) Modifier.background(Color.White) else Modifier.border(1.5.dp, SatriaColors.TextTertiary, CircleShape))
                            .clickable { onToggle(todo.id) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (todo.done) Text("✓", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = todo.text,
                        color = if (todo.done) SatriaColors.TextTertiary else SatriaColors.TextPrimary,
                        fontSize = 15.sp,
                        textDecoration = if (todo.done) TextDecoration.LineThrough else TextDecoration.None,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                    )
                    Text(
                        text = "✕",
                        color = SatriaColors.TextTertiary,
                        fontSize = 16.sp,
                        modifier = Modifier.clickable { onRemove(todo.id) }.padding(4.dp),
                    )
                }
                HorizontalDivider(color = SatriaColors.Border)
            }
        }
    }
}

@Composable
fun toolTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor   = SatriaColors.Surface,
    unfocusedContainerColor = SatriaColors.Surface,
    focusedTextColor        = SatriaColors.TextPrimary,
    unfocusedTextColor      = SatriaColors.TextPrimary,
    focusedIndicatorColor   = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    cursorColor             = SatriaColors.Accent,
)
