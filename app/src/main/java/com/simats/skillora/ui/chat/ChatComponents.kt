package com.simats.skillora.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.skillora.ui.theme.*

@Composable
fun MessageBubbleDynamic(
    text: String,
    isMe: Boolean,
    timestamp: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(
            modifier = Modifier.weight(1f, fill = false),
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMe) 16.dp else 4.dp,
                            bottomEnd = if (isMe) 4.dp else 16.dp
                        )
                    )
                    .background(if (isMe) AvatarBg else GrayGreen)
                    .padding(12.dp)
            ) {
                Text(
                    text = text,
                    color = if (isMe) Background else Color.White,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
            Text(
                text = timestamp,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 9.sp,
                modifier = Modifier.padding(top = 4.dp, start = if (isMe) 0.dp else 4.dp, end = if (isMe) 4.dp else 0.dp)
            )
        }
    }
}
