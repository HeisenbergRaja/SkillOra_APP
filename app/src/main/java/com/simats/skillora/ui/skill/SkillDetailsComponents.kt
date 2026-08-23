package com.simats.skillora.ui.skill

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.simats.skillora.ui.theme.*

@Composable
fun DetailPill(label: String, selected: Boolean = false) {
    Box(
        modifier = Modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Color(0x52AEC279) else Color(0x29E7E9E6))
            .border(1.dp, Color(0x14F2F3F1), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = if (selected) Primary else Color(0xBFF2F3F1),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ResourceChip(label: String) {
    Box(
        modifier = Modifier
            .padding(end = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x29E7E9E6))
            .border(1.dp, Color(0x14F2F3F1), RoundedCornerShape(16.dp))
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = Primary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ReviewCard(avatarUrl: String, name: String, rating: Float, text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .background(Color(0x29E7E9E6), RoundedCornerShape(18.dp))
            .border(1.dp, Color(0x14F2F3F1), RoundedCornerShape(18.dp))
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Surface),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = name, color = Primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(5) { i ->
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (i < rating.toInt()) Primary else Color(0x40F2F3F1),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = text, color = Color(0xA6F2F3F1), fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
fun CurriculumItem(title: String, lessons: Int, duration: String, items: List<String>) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .background(Color(0x29E7E9E6), RoundedCornerShape(18.dp))
            .border(1.dp, Color(0x14F2F3F1), RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = Primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(text = "$lessons lessons \u00B7 $duration", color = Color(0x8CF2F3F1), fontSize = 12.sp)
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(18.dp)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)) {
                items.forEach { item ->
                    Row(modifier = Modifier.padding(top = 12.dp), verticalAlignment = Alignment.Top) {
                        Box(modifier = Modifier.padding(top = 6.dp).size(6.dp).clip(CircleShape).background(Color(0x59F2F3F1)))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = item, color = Color(0xB2F2F3F1), fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
