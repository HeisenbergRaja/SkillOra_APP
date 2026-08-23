package com.simats.skillora.ui.quiz

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.skillora.ui.theme.*

@Composable
fun QuizOptionCard(
    label: String,
    text: String,
    selected: Boolean,
    submitted: Boolean,
    isCorrect: Boolean,
    onClick: () -> Unit
) {
    val borderColor = when {
        submitted && isCorrect -> AvatarBg
        submitted && selected && !isCorrect -> LogoutColor
        selected && !submitted -> AvatarBg
        else -> Color.White.copy(alpha = 0.2f)
    }

    val bgColor = when {
        submitted && isCorrect -> AvatarBg.copy(alpha = 0.3f)
        submitted && selected && !isCorrect -> LogoutColor.copy(alpha = 0.1f)
        selected && !submitted -> AvatarBg.copy(alpha = 0.1f)
        else -> Color.Transparent
    }

    val labelBgColor = when {
        submitted && isCorrect -> AvatarBg
        submitted && selected && !isCorrect -> LogoutColor
        selected && !submitted -> AvatarBg
        else -> GrayGreen
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(enabled = !submitted) { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(labelBgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (selected || (submitted && isCorrect)) Background else Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}
