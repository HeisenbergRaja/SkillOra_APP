package com.simats.skillora.ui.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.simats.skillora.ui.theme.*

@Composable
fun PodiumItem(
    rank: Int,
    name: String,
    stats: String,
    imageUrl: String,
    isFirst: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier
                .size(if (isFirst) 70.dp else 50.dp)
                .clip(CircleShape)
                .border(if (isFirst) 3.dp else 2.dp, if (isFirst) AvatarBg else Color.White.copy(alpha = 0.4f), CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .width(if (isFirst) 70.dp else 60.dp)
                .height(if (isFirst) 100.dp else 80.dp)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(if (isFirst) AvatarBg else Color.White.copy(alpha = 0.1f))
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = rank.toString(),
                color = if (isFirst) Background else Color.White.copy(alpha = 0.5f),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Text(
            text = name,
            color = if (isFirst) AvatarBg else Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = stats,
            color = if (isFirst) AvatarBg.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.6f),
            fontSize = 10.sp
        )
    }
}

@Composable
fun RankingItemRow(
    rank: Int,
    name: String,
    dept: String,
    credits: Int,
    skills: Int,
    imageUrl: String,
    isCurrent: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .background(GrayGreen, RoundedCornerShape(12.dp))
            .border(if (isCurrent) 1.dp else 0.dp, if (isCurrent) AvatarBg else Color.Transparent, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = rank.toString(),
            color = if (isCurrent) AvatarBg else Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(24.dp),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.width(8.dp))
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier.size(40.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = name,
                    color = if (isCurrent) AvatarBg else Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AvatarBg.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = "CURRENT", color = AvatarBg, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text(text = dept, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$credits Cr",
                color = if (isCurrent) AvatarBg else Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(text = "$skills Skills", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
        }
    }
}
