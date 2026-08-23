package com.simats.skillora.ui.learning

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.skillora.ui.theme.*

@Composable
fun LearningStatBlock(number: String, label: String) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(GrayGreen)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = number,
            color = AvatarBg,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = TextColor.copy(alpha = 0.8f),
            fontSize = 10.sp,
            lineHeight = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun ContinueLearningCardLarge(
    category: String,
    title: String,
    progress: Int,
    icon: ImageVector,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(260.dp)
            .padding(end = 16.dp)
            .background(GrayGreen, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(DarkGreen, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = EditBtnBg, modifier = Modifier.size(24.dp))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = category, color = TextColor.copy(alpha = 0.8f), fontSize = 10.sp)
                Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Progress", color = TextColor.copy(alpha = 0.6f), fontSize = 10.sp)
            Text(text = "$progress%", color = AvatarBg, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }

        LinearProgressIndicator(
            progress = { progress / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = AvatarBg,
            trackColor = DarkGreen
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = EditBtnBg),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            Text(text = "Continue", color = Surface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EnrolledSkillRow(
    title: String,
    progress: Int,
    icon: ImageVector,
    onPlay: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .background(GrayGreen, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(DarkGreen, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = EditBtnBg, modifier = Modifier.size(20.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(text = "$progress%", color = AvatarBg, fontSize = 10.sp)
            }
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = AvatarBg,
                trackColor = DarkGreen
            )
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .border(1.dp, TextColor.copy(alpha = 0.2f), CircleShape)
                .clickable { onPlay() },
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = TextColor.copy(alpha = 0.8f), modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
fun CompletedSkillRow(
    title: String,
    icon: ImageVector,
    rating: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .background(GrayGreen, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.padding(end = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(DarkGreen, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
                    .size(16.dp)
                    .background(AvatarBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Surface, modifier = Modifier.size(10.dp))
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(5) { i ->
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (i < rating) AvatarBg else TextColor.copy(alpha = 0.2f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        Text(text = "Completed", color = AvatarBg, fontSize = 10.sp)
    }
}

@Composable
fun SkillPathNode(
    title: String,
    subtitle: String,
    status: String, // "completed", "active", "locked"
    onPress: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        when (status) {
            "completed" -> {
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .background(AvatarBg, CircleShape)
                        .padding(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(GrayGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Surface, modifier = Modifier.size(20.dp))
                    }
                }
            }
            "active" -> {
                Box(
                    modifier = Modifier
                        .clickable { onPress() }
                        .padding(6.dp)
                        .background(GrayGreen, CircleShape)
                        .padding(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(GrayGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 12.dp, y = (-4).dp)
                            .background(Color(0xFFFF6B6B), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = "HOT", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .border(2.dp, TextColor.copy(alpha = 0.2f), CircleShape)
                        .background(Color.Transparent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = TextColor.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(text = subtitle, color = TextColor.copy(alpha = 0.5f), fontSize = 10.sp)
    }
}
