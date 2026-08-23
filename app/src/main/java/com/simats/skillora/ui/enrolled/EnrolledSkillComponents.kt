package com.simats.skillora.ui.enrolled

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.skillora.ui.theme.*

@Composable
fun EnrolledProgressCard(
    percent: Int,
    completed: Int,
    total: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GrayGreen, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "YOUR PROGRESS",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(text = "$percent%", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$completed of $total topics completed",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "${total - completed} remaining", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
            }
        }
        LinearProgressIndicator(
            progress = { percent / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = AvatarBg,
            trackColor = EditBtnBg
        )
    }
}

@Composable
fun RoadmapNodeSmall(
    dayNumber: Int,
    title: String,
    status: String, // "done", "active", "locked"
    isLast: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min), // Important for vertical line
        verticalAlignment = Alignment.Top
    ) {
        // Timeline Column
        Column(
            modifier = Modifier
                .width(48.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (status == "done") AvatarBg.copy(alpha = 0.2f)
                        else if (status == "active") EditBtnBg
                        else Color.Transparent
                    )
                    .border(
                        1.dp,
                        if (status == "locked") Color.White.copy(alpha = 0.2f) else AvatarBg,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (status) {
                    "done" -> Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(AvatarBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Background,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    "active" -> Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Background,
                        modifier = Modifier.size(14.dp)
                    )
                    else -> Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(if (status == "done") AvatarBg else Color.White.copy(alpha = 0.1f))
                )
            }
        }

        // Card Column
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 24.dp) // Consistent spacing between cards
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (status == "active") Color.Black.copy(alpha = 0.3f) else GrayGreen.copy(alpha = 0.3f))
                    .border(
                        1.dp,
                        if (status == "active") AvatarBg.copy(alpha = 0.3f) else Color.Transparent,
                        RoundedCornerShape(18.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Day $dayNumber",
                            color = if (status == "locked") Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = title,
                            color = if (status == "locked") Color.White.copy(alpha = 0.4f) else Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 20.sp
                        )
                    }
                    
                    // Status Badge
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (status == "done") AvatarBg
                                else if (status == "active") AvatarBg.copy(alpha = 0.15f)
                                else Color.Transparent
                            )
                            .then(
                                if (status == "locked") Modifier.border(
                                    1.dp,
                                    Color.White.copy(alpha = 0.15f),
                                    RoundedCornerShape(8.dp)
                                ) else Modifier
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (status == "done") "Completed" else if (status == "active") "In Progress" else "Locked",
                            color = if (status == "done") Background else if (status == "active") AvatarBg else Color.White.copy(alpha = 0.4f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EnrolledResourceCard(title: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .background(GrayGreen.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            maxLines = 2
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.3f),
            modifier = Modifier.size(20.dp)
        )
    }
}
