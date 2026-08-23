package com.simats.skillora.ui.home

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
import androidx.compose.runtime.remember
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
fun CreditBadge(credits: Int, label: String = "Credits") {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(Color(0x47AEC279), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CreditCard,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "$credits $label",
            color = Primary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun HomeHeader(name: String, credits: Int) {
    val greeting = remember {
        val calendar = java.util.Calendar.getInstance()
        when (calendar.get(java.util.Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..20 -> "Good Evening"
            else -> "Good Night"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$greeting, $name \uD83D\uDC4B",
                color = Primary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = "What are you learning today?",
                color = Color(0x8CF2F3F1),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        CreditBadge(credits = credits)
    }
}

@Composable
fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "Search skills, creators...",
    modifier: Modifier = Modifier
) {
    // Basic implementation from Home Page
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
            .background(Surface, RoundedCornerShape(18.dp))
            .border(1.dp, Color(0x14F2F3F1), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = Color(0x8CF2F3F1),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = if(value.isEmpty()) placeholder else value, color = Color(0x73F2F3F1), fontSize = 14.sp)
        }
    }
}

@Composable
fun SkillCard(
    imageUrl: String,
    level: String,
    title: String,
    author: String,
    rating: String,
    credits: Int,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(190.dp)
            .padding(end = 16.dp)
            .background(Surface, RoundedCornerShape(18.dp))
            .border(1.dp, Color(0x14F2F3F1), RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            contentScale = ContentScale.Crop
        )
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(Color(0x29E7E9E6), RoundedCornerShape(14.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = level, color = Primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
                    Text(text = rating, color = Primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, color = Primary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
            Text(text = "by $author", color = Color(0x8CF2F3F1), fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(vertical = 12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(imageVector = Icons.Default.CreditCard, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                    Text(text = credits.toString(), color = Primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Box(
                    modifier = Modifier
                        .background(Color(0x3DE7E9E6), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(text = "Enroll", color = Primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun CreatorAvatar(imageUrl: String, name: String, credits: Int, rank: Int) {
    Column(
        modifier = Modifier
            .width(88.dp)
            .padding(end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color(0x47E7E9E6), CircleShape),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
                    .size(22.dp)
                    .background(Primary, CircleShape)
                    .border(2.dp, Background, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = rank.toString(), color = Surface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = name, color = Primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(text = "$credits cr", color = Color(0x8CF2F3F1), fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
fun RequestCard(title: String, requestedBy: String, bounty: Int, upvotes: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .background(Surface, RoundedCornerShape(18.dp))
            .border(1.dp, Color(0x14F2F3F1), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(
                text = "requested by $requestedBy \u00B7 Bounty: $bounty cr",
                color = Color(0x8CF2F3F1),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "$upvotes Upvotes", color = Color(0x8CF2F3F1), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Color(0x24E7E9E6), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.ThumbUp, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun HomeFloatingActionButton(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = Primary,
        shape = CircleShape,
        modifier = Modifier.size(54.dp)
    ) {
        Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Surface, modifier = Modifier.size(20.dp))
    }
}
