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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import id.satria.launcher.MainViewModel
import id.satria.launcher.ui.theme.SatriaColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private data class Message(
    val id: String,
    val text: String,
    val from: String, // "user" | "assistant"
    val time: String,
)

// Module-level chat cache — bertahan selama app hidup, mirip _chatCache di RN
private val _chatCache = mutableListOf<Message>()
private const val MAX_MESSAGES = 200

private fun getTimeStr(): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
}

private fun makeId() = "${System.currentTimeMillis()}-${(Math.random() * 100000).toInt()}"

private fun getReply(input: String, userName: String, assistantName: String): String {
    val t = input.lowercase().trim()
    return when {
        t.contains("hai") || t.contains("hi") || t.contains("hello") || t.contains("halo") ->
            "Hey $userName! 👋 What can I do for you?"
        t.contains("apa kabar") || t.contains("how are you") ->
            "I'm doing great, $userName! 😊 How about you?"
        t.contains("siapa kamu") || t.contains("who are you") || t.contains("nama kamu") ->
            "I'm $assistantName, your personal assistant! 🤖"
        t.contains("makasih") || t.contains("thanks") || t.contains("thank you") || t.contains("terima kasih") ->
            "You're welcome, $userName! 😊"
        t.contains("jam berapa") || t.contains("what time") || t.contains("pukul berapa") ->
            "It's ${getTimeStr()} right now, $userName! ⏰"
        t.contains("bosan") || t.contains("gabut") || t.contains("bored") ->
            "Try opening your favorite app! 📱 Or just keep chatting with me 😄"
        t.contains("capek") || t.contains("tired") || t.contains("lelah") ->
            "Take a break, $userName! 😴 Your health matters."
        t.contains("lapar") || t.contains("hungry") || t.contains("makan") ->
            "Hungry? Don't skip your meals, $userName! 🍔"
        t.contains("bye") || t.contains("dadah") || t.contains("sampai jumpa") ->
            "Bye $userName! 👋 See you later~"
        t.contains("good morning") || t.contains("selamat pagi") || t.contains("pagi") ->
            "Good morning, $userName! ☀️ Hope you have a great day!"
        t.contains("good night") || t.contains("selamat malam") || t.contains("malam") ->
            "Good night, $userName! 🌙 Sweet dreams!"
        t.contains("love") || t.contains("sayang") ->
            "Aww, I appreciate that, $userName! 🥰"
        t.contains("joke") || t.contains("lucu") ->
            "Why don't scientists trust atoms? Because they make up everything! 😄"
        else -> listOf(
            "Interesting! Tell me more, $userName. 😊",
            "I'm not sure about that, but I'm always here for you! 🤗",
            "Oh really? Go on! 👀",
            "Sounds fun! Anything else on your mind, $userName? 😄",
            "I still have a lot to learn! 😅",
            "Hmm, that's a new one for me! Care to explain? 🤔",
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
        if (_chatCache.size > MAX_MESSAGES) _chatCache.removeAt(0)
        messages = _chatCache.toList()
        input = ""
        isTyping = true
        scrollToEnd()

        scope.launch {
            delay((600 + Math.random() * 600).toLong())
            val reply = Message(makeId(), getReply(text, userName, assistantName), "assistant", getTimeStr())
            _chatCache.add(reply)
            if (_chatCache.size > MAX_MESSAGES) _chatCache.removeAt(0)
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

            Column(modifier = Modifier.weight(1f)) {
                Text(assistantName, color = SatriaColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("Online", color = Color(0xFF00D134), fontSize = 12.sp)
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
            Text(msg.text, color = Color.White, fontSize = 15.sp, lineHeight = 21.sp)
            Text(
                msg.time,
                color = Color.White.copy(alpha = 0.4f),
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