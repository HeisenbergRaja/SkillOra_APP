package com.simats.skillora.ui.marketplace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
fun SkillRequestDetailsScreen(
    request: SkillRequest,
    onBack: () -> Unit,
    onUploadSkill: (SkillRequest) -> Unit
) {
    Scaffold(
        containerColor = Background,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(text = "Skill Request", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = request.userAvatarUrl,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Surface),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = request.userName, color = Primary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Requested ${request.requestedAgo}", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(text = request.title, color = Primary, fontSize = 24.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.background(Color(0x29E7E9E6), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = Icons.Default.ThumbUp, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                Text(text = "${request.likes} likes", color = Primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { onUploadSkill(request) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AvatarBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Upload Skill", color = Background, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
