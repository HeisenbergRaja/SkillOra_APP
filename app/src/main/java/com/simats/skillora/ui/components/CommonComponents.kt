package com.simats.skillora.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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

data class NavItem(val key: String, val label: String, val icon: ImageVector)

@Composable
fun BottomNav(activeKey: String, onNavigate: (String) -> Unit) {
    val items = listOf(
        NavItem("home", "Home", Icons.Default.Home),
        NavItem("marketplace", "Marketplace", Icons.Default.GridView),
        NavItem("learning", "Learning", Icons.Default.MenuBook),
        NavItem("leaders", "Leaders", Icons.Default.BarChart),
        NavItem("profile", "Profile", Icons.Default.Person)
    )

    Surface(
        color = Surface,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, Color(0x14F2F3F1))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 16.dp)
                .height(72.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isActive = item.key == activeKey
                val color = if (isActive) Primary else Color(0x80F2F3F1)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable { onNavigate(item.key) }
                ) {
                    Icon(imageVector = item.icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                    Text(text = item.label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Primary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (actionLabel != null && onActionClick != null) {
            Text(
                text = actionLabel,
                color = Color(0x8CF2F3F1),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { onActionClick() }
            )
        }
    }
}
