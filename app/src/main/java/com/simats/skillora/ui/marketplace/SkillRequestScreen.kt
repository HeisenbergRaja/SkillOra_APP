package com.simats.skillora.ui.marketplace

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.skillora.data.SkillRequestRepository
import com.simats.skillora.ui.components.BottomNav
import com.simats.skillora.ui.theme.Background
import com.simats.skillora.ui.theme.Primary
import com.simats.skillora.ui.theme.AvatarBg
import kotlinx.coroutines.launch

@Composable
fun SkillRequestScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { SkillRequestRepository() }
    
    var skillName by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            BottomNav(activeKey = "home", onNavigate = onNavigate)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Primary)
                }
                Text(
                    text = "Request a Skill",
                    color = Primary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            Text(
                text = "What skill would you like to learn?",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 30.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = skillName,
                onValueChange = { if (it.length <= 100) skillName = it },
                placeholder = { Text(text = "Enter skill name...", color = Color.White.copy(alpha = 0.4f)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedBorderColor = AvatarBg,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                maxLines = 2
            )
            
            Text(
                text = "${skillName.length}/100",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                textAlign = TextAlign.End
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Tell us what skill you want to learn and we'll let the community know. Creators will see your request and might build a roadmap just for you!",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 14.sp,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(100.dp))

            Button(
                onClick = {
                    if (skillName.trim().isEmpty()) {
                        Toast.makeText(context, "Please enter a skill name.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    isSubmitting = true
                    scope.launch {
                        val result = repository.submitRequest(skillName)
                        isSubmitting = false
                        if (result.isSuccess) {
                            Toast.makeText(context, "Skill request submitted successfully!", Toast.LENGTH_SHORT).show()
                            onBack()
                        } else {
                            Toast.makeText(context, result.exceptionOrNull()?.message ?: "Submission failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AvatarBg),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "Send Request", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
