package id.satria.launcher.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import id.satria.launcher.MainViewModel
import id.satria.launcher.ui.theme.SatriaColors
import id.satria.launcher.ui.theme.LocalAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

private data class Message(
    val id: String,
    val text: String,
    val from: String, // "user" | "assistant"
    val time: String,
)

// Module-level chat cache — bertahan selama app hidup
private val _chatCache = mutableListOf<Message>()
private const val MAX_MESSAGES = 50

private fun getTimeStr(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

private fun makeId() = "${System.currentTimeMillis()}-${(Math.random() * 100000).toInt()}"

// Pollinations AI — free, no key, very lightweight
private suspend fun aiReply(
    userMessage   : String,
    history       : List<Message>,
    userName      : String,
    assistantName : String,
): String = withContext(Dispatchers.IO) {
    try {
        val messages = JSONArray()

        // System prompt singkat — hemat token
        messages.put(JSONObject().apply {
            put("role", "system")
            put("content", "You are $assistantName, a friendly personal assistant for $userName. " +
                "Reply concisely (1-3 sentences). Be warm and helpful.")
        })

        // Sertakan max 10 pesan terakhir sebagai context (hemat RAM)
        history.takeLast(10).forEach { msg ->
            messages.put(JSONObject().apply {
                put("role", if (msg.from == "user") "user" else "assistant")
                put("content", msg.text)
            })
        }

        // Pesan saat ini
        messages.put(JSONObject().apply {
            put("role", "user")
            put("content", userMessage)
        })

        val body = JSONObject().apply {
            put("model", "openai")   // alias model terbaru di Pollinations
            put("messages", messages)
            put("max_tokens", 150)
            put("private", true)     // tidak dicatat di log publik
        }

        val conn = URL("https://text.pollinations.ai/openai").openConnection() as HttpURLConnection
        conn.doOutput     = true
        conn.connectTimeout = 15_000
        conn.readTimeout    = 20_000
        conn.requestMethod  = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("User-Agent", "CloudysLauncher/1.0 Android")

        OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(body.toString()) }

        if (conn.responseCode !in 200..299) throw Exception("HTTP ${conn.responseCode}")
        val raw = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }
        conn.disconnect()

        val json = JSONObject(raw)
        json.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()
    } catch (_: Exception) {
        // Fallback ke reply lokal jika API gagal / offline
        localReply(userMessage, userName, assistantName)
    }
}

// Fallback ringan jika offline
private fun localReply(input: String, userName: String, assistantName: String): String {
    val t = input.lowercase().trim()
    return when {
        t.contains("hi") || t.contains("hello") || t.contains("hai") || t.contains("halo") ->
            "Hey $userName! 👋 How can I help you today?"
        t.contains("how are you") || t.contains("apa kabar") ->
            "I'm doing great, $userName! 😊 What about you?"
        t.contains("who are you") || t.contains("siapa kamu") ->
            "I'm $assistantName, your personal assistant! 🤖"
        t.contains("thank") || t.contains("makasih") || t.contains("terima kasih") ->
            "You're welcome, $userName! 😊"
        t.contains("bye") || t.contains("dadah") ->
            "Bye $userName! 👋 See you soon~"
        t.contains("good morning") || t.contains("pagi") ->
            "Good morning, $userName! ☀️ Have a great day!"
        t.contains("good night") || t.contains("malam") ->
            "Good night, $userName! 🌙 Sweet dreams!"
        else -> listOf(
            "Interesting! Tell me more, $userName. 😊",
            "I'm here for you! 🤗 Go on…",
            "Oh really? That's cool! 😄",
            "Hmm, let me think… 🤔 Can you elaborate?",
        ).random()
    }
}

@Composable
fun ChatScreen(vm: MainViewModel, onClose: () -> Unit) {
    val userName      by vm.userName.collectAsState()
    val assistantName by vm.assistantName.collectAsState()
    val avatarPath    by vm.avatarPath.collectAsState()

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var input     by remember { mutableStateOf("") }
    var isTyping  by remember { mutableStateOf(false) }
    var messages  by remember { mutableStateOf(_chatCache.toList()) }

    val hasInput = input.trim().isNotEmpty()

    // Scroll ke pesan terakhir saat chat dibuka
    LaunchedEffect(Unit) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(messages.size - 1)
        }
    }

    BackHandler { onClose() }

    fun scrollToEnd() {
        scope.launch {
            if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun sendMessage() {
        val text = input.trim()
        if (text.isEmpty()) return

        val userMsg = Message(makeId(), text, "user", getTimeStr())
        _chatCache.add(userMsg)
        while (_chatCache.size > MAX_MESSAGES) _chatCache.removeAt(0)
        messages  = _chatCache.toList()
        input     = ""
        isTyping  = true
        scrollToEnd()

        scope.launch {
            val historySnapshot = messages.toList()  // snapshot ringan, tidak copy data besar
            val replyText = aiReply(text, historySnapshot, userName, assistantName)
            val reply = Message(makeId(), replyText, "assistant", getTimeStr())
            _chatCache.add(reply)
            while (_chatCache.size > MAX_MESSAGES) _chatCache.removeAt(0)
            messages = _chatCache.toList()
            isTyping = false
            scrollToEnd()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SatriaColors.ScreenBackground)
            .systemBarsPadding()
            .imePadding()  // naik otomatis saat keyboard muncul
    ) {
        // ── Header ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(SatriaColors.Surface),
                contentAlignment = Alignment.Center,
            ) {
                if (avatarPath != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(avatarPath)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(androidx.compose.foundation.shape.CircleShape),
                    )
                } else {
                    Text("🤖", fontSize = 20.sp)
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Text(assistantName, color = SatriaColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 17.sp)
                Text("● Online", color = Color(0xFF2ECC71), fontSize = 11.sp, lineHeight = 13.sp)
            }

            // Hapus chat
            TextButton(onClick = {
                _chatCache.clear()
                messages = emptyList()
            }) {
                Text("Clear", color = SatriaColors.TextSecondary, fontSize = 13.sp)
            }

            // Tutup
            IconButton(onClick = onClose) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SatriaColors.Surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✕", color = SatriaColors.TextSecondary, fontSize = 14.sp)
                }
            }
        }

        HorizontalDivider(color = SatriaColors.Border)

        // ── Messages ───────────────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            items(messages, key = { it.id }) { msg ->
                MessageBubble(msg = msg)
            }
            if (isTyping) {
                item { TypingBubble() }
            }
        }

        HorizontalDivider(color = SatriaColors.Border)

        // ── Input ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(21.dp)),
                placeholder = { Text("Message $assistantName...", color = SatriaColors.TextTertiary) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor   = SatriaColors.Surface,
                    unfocusedContainerColor = SatriaColors.Surface,
                    focusedTextColor        = SatriaColors.TextPrimary,
                    unfocusedTextColor      = SatriaColors.TextPrimary,
                    focusedIndicatorColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor             = SatriaColors.Accent,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { sendMessage() }),
                singleLine = true,
            )

            Spacer(Modifier.width(8.dp))

            IconButton(
                onClick = ::sendMessage,
                enabled = hasInput,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (hasInput) SatriaColors.AccentDim else SatriaColors.Surface),
            ) {
                Text("↑", color = if (hasInput) Color.White else SatriaColors.TextTertiary, fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: Message) {
    val isUser = msg.from == "user"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp, topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp,
                    )
                )
                .background(if (isUser) SatriaColors.Accent else SatriaColors.Surface)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            val textColor  = if (isUser) Color.White else SatriaColors.TextPrimary
            val timeColor  = if (isUser) Color.White.copy(alpha = 0.55f) else SatriaColors.TextTertiary
            Text(msg.text, color = textColor, fontSize = 15.sp, lineHeight = 21.sp)
            Text(
                msg.time,
                color = timeColor,
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun TypingBubble() {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 4.dp))
                .background(SatriaColors.Surface)
                .padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(SatriaColors.TextTertiary)
                    )
                }
            }
        }
    }
}