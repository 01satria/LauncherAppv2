package id.satria.launcher.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
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
        Text(
            "To Do",
            color = SatriaColors.TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
        )

        // Input row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)),
                placeholder = { Text("Add a task...", color = SatriaColors.TextTertiary) },
                colors = toolTextFieldColors(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (input.isNotBlank()) { onAdd(input.trim()); input = "" }
                }),
            )
            Button(
                onClick = { if (input.isNotBlank()) { onAdd(input.trim()); input = "" } },
                colors = ButtonDefaults.buttonColors(containerColor = SatriaColors.SurfaceMid),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text("+", fontSize = 20.sp, color = SatriaColors.TextPrimary)
            }
        }

        if (todos.isEmpty()) {
            Text(
                "No tasks yet. Add one!",
                color = SatriaColors.TextTertiary,
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                textAlign = TextAlign.Center,
            )
        }

        todos.forEach { todo ->
            key(todo.id) {
                TodoRow(
                    todo = todo,
                    onToggle = { onToggle(todo.id) },
                    onRemove = { onRemove(todo.id) },
                )
            }
        }
    }
}

@Composable
private fun TodoRow(
    todo: TodoItem,
    onToggle: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Modern animated checkbox ─────────────────────────────────────
        ModernCheckbox(checked = todo.done, onChecked = onToggle)

        Text(
            text = todo.text,
            color = if (todo.done) SatriaColors.TextTertiary else SatriaColors.TextPrimary,
            fontSize = 15.sp,
            textDecoration = if (todo.done) TextDecoration.LineThrough else TextDecoration.None,
            modifier = Modifier.weight(1f),
            maxLines = 2,
        )

        // Remove button — X tipis tanpa border
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable { onRemove() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "✕",
                color = SatriaColors.TextTertiary,
                fontSize = 13.sp,
            )
        }
    }
    HorizontalDivider(color = SatriaColors.Border, thickness = 0.5.dp)
}

@Composable
private fun ModernCheckbox(
    checked: Boolean,
    onChecked: () -> Unit,
) {
    // Animasi scale saat tap
    val scale by animateFloatAsState(
        targetValue = if (checked) 1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "checkScale",
    )

    // Animasi warna background
    val bgColor by animateColorAsState(
        targetValue = if (checked) SatriaColors.Accent else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "checkBg",
    )

    // Animasi warna border
    val borderColor by animateColorAsState(
        targetValue = if (checked) SatriaColors.Accent else SatriaColors.TextTertiary,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "checkBorder",
    )

    // Animasi progress centang (0f → 1f)
    val checkProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "checkProgress",
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .size(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(6.dp))
            .clickable { onChecked() },
        contentAlignment = Alignment.Center,
    ) {
        // Gambar centang dengan Canvas — smooth dan ringan
        if (checkProgress > 0f) {
            Canvas(modifier = Modifier.size(13.dp)) {
                val w = size.width
                val h = size.height

                // Path centang: dari kiri-bawah → tengah-bawah → kanan-atas
                val path = Path().apply {
                    moveTo(0f, h * 0.5f)
                    lineTo(w * 0.38f, h * 0.85f)
                    lineTo(w, h * 0.15f)
                }

                // Hitung panjang path untuk animasi draw progress
                val totalLen = (w * 0.38f) * 1.2f + (w * 0.62f) * 1.1f
                val drawn = totalLen * checkProgress

                drawPath(
                    path = path,
                    color = Color.White,
                    style = Stroke(
                        width = 2.2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                    alpha = checkProgress,
                )
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