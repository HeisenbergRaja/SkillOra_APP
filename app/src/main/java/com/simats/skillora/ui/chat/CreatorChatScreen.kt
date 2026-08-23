package com.simats.skillora.ui.chat

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.simats.skillora.data.Chat
import com.simats.skillora.data.ChatMessage
import com.simats.skillora.data.ChatRepository
import com.simats.skillora.data.SkillRepository
import com.simats.skillora.data.UserProfile
import com.simats.skillora.ui.components.BottomNav
import com.simats.skillora.ui.theme.*
import com.simats.skillora.ui.upload.Skill
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CreatorChatScreen(
    skillId: String,
    studentId: String? = null, 
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val TAG = "CHAT_DEBUG"
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { SkillRepository() }
    val chatRepository = remember { ChatRepository() }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var skill by remember { mutableStateOf<Skill?>(null) }
    var chat by remember { mutableStateOf<Chat?>(null) }
    var otherUser by remember { mutableStateOf<UserProfile?>(null) }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isPublisherView = studentId != null && studentId != "null" && studentId.isNotBlank()

    LaunchedEffect(skillId, currentUserId) {
        if (currentUserId.isEmpty()) {
            Log.e(TAG, "User not authenticated")
            errorMessage = "Please sign in to continue"
            isLoading = false
            return@LaunchedEffect
        }

        if (skillId.isEmpty() || skillId == "null") {
            Log.e(TAG, "Invalid skillId: $skillId")
            errorMessage = "Unable to open this conversation (Invalid ID)"
            isLoading = false
            return@LaunchedEffect
        }

        Log.d(TAG, "Opening conversation for skill: $skillId. Publisher view: $isPublisherView")
        
        try {
            // 1. Load Skill
            val skillResult = repository.getSkillById(skillId)
            if (skillResult.isFailure) {
                errorMessage = "Skill not found"
                isLoading = false
                return@LaunchedEffect
            }
            val s = skillResult.getOrNull()
            skill = s
            
            if (s == null) {
                errorMessage = "Skill data is empty"
                isLoading = false
                return@LaunchedEffect
            }

            // 2. Determine target participants
            val targetStudentId = if (isPublisherView) studentId!! else currentUserId
            val targetPublisherId = s.creatorId
            
            Log.d(TAG, "Student: $targetStudentId, Publisher: $targetPublisherId")

            // 3. Get or Create Chat
            val chatResult = chatRepository.getOrCreateChat(s.id, s.title, targetPublisherId, targetStudentId)
            if (chatResult.isFailure) {
                errorMessage = "Unable to initialize chat"
                isLoading = false
                return@LaunchedEffect
            }
            val c = chatResult.getOrNull()
            chat = c
            
            if (c == null) {
                errorMessage = "Chat creation returned null"
                isLoading = false
                return@LaunchedEffect
            }

            // 4. Load Other User Profile
            val otherUid = if (isPublisherView) studentId!! else targetPublisherId
            val userResult = repository.getUserProfile(otherUid)
            if (userResult.isSuccess) {
                otherUser = userResult.getOrNull()
            }

            // 5. Start observing messages in a separate coroutine to avoid blocking terminal loading state
            launch {
                chatRepository.observeMessages(c.chatId).collectLatest { msgs ->
                    Log.d(TAG, "Snapshot received: ${msgs.size} messages")
                    messages.clear()
                    messages.addAll(msgs)
                    isLoading = false // First snapshot turns off loading
                    
                    // Mark as read
                    chatRepository.markAsRead(c.chatId, !isPublisherView)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in ChatScreen", e)
            errorMessage = "An error occurred: ${e.message}"
            isLoading = false
        }
    }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            BottomNav(activeKey = "learning", onNavigate = onNavigate)
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AvatarBg)
            }
        } else if (errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = errorMessage!!, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = AvatarBg)) {
                        Text("Go Back")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(AvatarBg),
                            contentAlignment = Alignment.Center
                        ) {
                            if (otherUser?.profileImageUrl != null) {
                                coil.compose.AsyncImage(
                                    model = otherUser?.profileImageUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Text(text = (otherUser?.name ?: "U").take(1).uppercase(), color = Surface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Column {
                            Text(text = otherUser?.name ?: "User", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(text = if (isPublisherView) "Student \u2022 ${skill?.title}" else "Creator \u2022 ${skill?.title}", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        }
                    }
                }

                // Course Info Card
                Row(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                        .background(GrayGreen, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(AvatarBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Dashboard, contentDescription = null, tint = Background, modifier = Modifier.size(16.dp))
                    }
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(text = skill?.title ?: "", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(text = if (isPublisherView) "Chatting with learner" else "Ask ${otherUser?.name ?: "the creator"} anything about this skill", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                    }
                }

                // Messages
                Box(modifier = Modifier.weight(1f)) {
                    if (messages.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (isPublisherView) "No messages from student yet." else "Ask the creator your first question!",
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(24.dp),
                        reverseLayout = false
                    ) {
                        items(messages) { msg ->
                            MessageBubbleDynamic(
                                text = msg.text,
                                isMe = msg.senderId == currentUserId,
                                timestamp = formatTime(msg.sentAt?.toDate())
                            )
                        }
                    }
                }

                // Input Area
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .background(GrayGreen, RoundedCornerShape(24.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (inputText.isEmpty()) {
                            Text(text = "Type a message...", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
                        }
                        BasicTextField(
                            value = inputText,
                            onValueChange = { if (it.length <= 2000) inputText = it },
                            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                            cursorBrush = SolidColor(AvatarBg),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                val text = inputText
                                inputText = ""
                                scope.launch {
                                    val receiverId = if (isPublisherView) studentId!! else skill!!.creatorId
                                    chatRepository.sendMessage(chat!!.chatId, receiverId, text)
                                }
                            }
                        },
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(if (inputText.isBlank()) AvatarBg.copy(alpha = 0.3f) else AvatarBg)
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = if (inputText.isBlank()) Color.White.copy(alpha = 0.5f) else Background, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

private fun formatTime(date: Date?): String {
    if (date == null) return ""
    return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date)
}
