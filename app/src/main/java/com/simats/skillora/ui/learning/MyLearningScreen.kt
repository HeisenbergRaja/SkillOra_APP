package com.simats.skillora.ui.learning

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.skillora.data.EnrolledSkill
import com.simats.skillora.data.SkillRepository
import com.simats.skillora.ui.components.BottomNav
import com.simats.skillora.ui.theme.AvatarBg
import com.simats.skillora.ui.theme.Background
import com.simats.skillora.ui.theme.Primary
import com.simats.skillora.ui.theme.Surface as ThemeSurface
import com.simats.skillora.ui.upload.Skill

@Composable
fun MyLearningScreen(
    onNavigate: (String) -> Unit,
    onContinueLesson: (String) -> Unit
) {
    val repository = remember { SkillRepository() }
    var enrolledSkills by remember { mutableStateOf<List<EnrolledSkill>>(emptyList()) }
    val userCredits by repository.observeCredits().collectAsState(initial = 0)
    val totalCreditsEarned by repository.observeTotalCreditsEarned().collectAsState(initial = 0)
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val result = repository.getEnrolledSkillsWithProgress()
        if (result.isSuccess) {
            enrolledSkills = result.getOrDefault(emptyList())
        }
        isLoading = false
    }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            BottomNav(activeKey = "learning", onNavigate = onNavigate)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Learning",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .background(AvatarBg, androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "💰", fontSize = 14.sp)
                        Text(text = "$userCredits Credits", color = ThemeSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LearningStatBlock(number = enrolledSkills.size.toString(), label = "Skills\nEnrolled")
                LearningStatBlock(number = enrolledSkills.count { it.enrollment.completed }.toString(), label = "Skills\nCompleted")
                LearningStatBlock(number = totalCreditsEarned.toString(), label = "Credits\nEarned")
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AvatarBg)
                }
            } else if (enrolledSkills.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text(text = "No skills enrolled yet", color = Color.White.copy(alpha = 0.5f))
                }
            } else {
                Text(
                    text = "CONTINUE LEARNING",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    enrolledSkills.filter { !it.enrollment.completed }.forEach { item ->
                        ContinueLearningCardLarge(
                            category = item.skill.category,
                            title = item.skill.title,
                            progress = item.enrollment.progress,
                            icon = Icons.Default.Code,
                            onContinue = { onContinueLesson(item.skill.id) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "ENROLLED SKILLS",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                enrolledSkills.forEach { item ->
                    EnrolledSkillRow(
                        title = item.skill.title,
                        progress = item.enrollment.progress,
                        icon = Icons.Default.Code,
                        onPlay = { onContinueLesson(item.skill.id) }
                    )
                }

                if (enrolledSkills.any { it.enrollment.completed }) {
                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "COMPLETED SKILLS",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    enrolledSkills.filter { it.enrollment.completed }.forEach { item ->
                        CompletedSkillRow(
                            title = item.skill.title,
                            icon = Icons.Default.CheckCircle,
                            rating = 5
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
