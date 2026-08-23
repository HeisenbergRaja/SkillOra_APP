package com.simats.skillora.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.simats.skillora.data.Chat
import com.simats.skillora.data.ChatRepository
import com.simats.skillora.data.SkillRepository
import com.simats.skillora.data.UserProfile
import com.simats.skillora.ui.components.BottomNav
import com.simats.skillora.ui.theme.*

@Composable
fun PublisherChatListScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val chatRepository = remember { ChatRepository() }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    
    var chats by remember { mutableStateOf<List<Chat>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(currentUserId) {
        chatRepository.observePublisherChats(currentUserId).collect { chatList ->
            chats = chatList
            isLoading = false
        }
    }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            BottomNav(activeKey = "profile", onNavigate = onNavigate)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Primary)
                }
                Text(
                    text = "Learner Conversations",
                    color = Primary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AvatarBg)
                }
            } else if (chats.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Chat, contentDescription = null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "No learner conversations yet.", color = Color.White.copy(alpha = 0.5f))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(chats) { chat ->
                        PublisherChatItem(
                            chat = chat,
                            onChatClick = {
                                onNavigate("chat?skillId=${chat.skillId}&studentId=${chat.studentId}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PublisherChatItem(
    chat: Chat,
    onChatClick: () -> Unit
) {
    val repository = remember { SkillRepository() }
    var studentProfile by remember { mutableStateOf<UserProfile?>(null) }

    LaunchedEffect(chat.studentId) {
        val result = repository.getUserProfile(chat.studentId)
        if (result.isSuccess) {
            studentProfile = result.getOrNull()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GrayGreen.copy(alpha = 0.3f))
            .clickable { onChatClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(AvatarBg),
            contentAlignment = Alignment.Center
        ) {
            if (studentProfile?.profileImageUrl != null) {
                AsyncImage(
                    model = studentProfile?.profileImageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(text = (studentProfile?.name ?: "U").take(1).uppercase(), color = com.simats.skillora.ui.theme.Surface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = studentProfile?.name ?: "Loading...",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = chat.skillTitle,
                color = AvatarBg,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = chat.lastMessage.ifEmpty { "No messages yet" },
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        if (chat.publisherUnreadCount > 0) {
            Box(
                modifier = Modifier.size(20.dp).clip(CircleShape).background(AvatarBg),
                contentAlignment = Alignment.Center
            ) {
                Text(text = chat.publisherUnreadCount.toString(), color = Background, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
