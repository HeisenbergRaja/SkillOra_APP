package com.simats.skillora.ui.enrolled

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.skillora.data.Enrollment
import com.simats.skillora.data.SkillRepository
import com.simats.skillora.ui.components.BottomNav
import com.simats.skillora.ui.theme.*
import com.simats.skillora.ui.theme.Primary
import com.simats.skillora.ui.upload.Skill
import kotlinx.coroutines.launch

@Composable
fun EnrolledSkillScreen(
    skillId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val repository = remember { SkillRepository() }
    var skill by remember { mutableStateOf<Skill?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    
    val userCredits by repository.observeCredits().collectAsState(initial = 0)
    val enrollmentState by repository.observeEnrollment(skillId).collectAsState(initial = null)
    
    LaunchedEffect(skillId) {
        val result = repository.getSkillById(skillId)
        if (result.isSuccess) {
            skill = result.getOrNull()
        }
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AvatarBg)
        }
        return
    }

    if (skill == null || enrollmentState == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = if (skill == null) "Skill not found" else "Enrollment not found", color = Color.White)
        }
        return
    }

    val currentSkill = skill!!
    val currentEnrollment = enrollmentState!!
    val completedDays = currentEnrollment.completedDays
    val totalTopics = currentSkill.roadmap.size
    val progressPercent = currentEnrollment.progress

    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        text = currentSkill.title, 
                        color = Color.White, 
                        fontSize = 18.sp, 
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.widthIn(max = 200.dp)
                    )
                }
                Box(
                    modifier = Modifier.background(AvatarBg, RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(text = "$userCredits Credits", color = Background, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            EnrolledProgressCard(percent = progressPercent, completed = completedDays, total = totalTopics)

            Spacer(modifier = Modifier.height(32.dp))
            Text(text = "Learning Roadmap", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(20.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                currentSkill.roadmap.forEachIndexed { index, day ->
                    val dayProgress = currentEnrollment.dayProgress.find { it.dayId == day.id }
                    val status = when {
                        dayProgress?.completed == true -> "done"
                        index == completedDays -> "active"
                        else -> "locked"
                    }
                    RoadmapNodeSmall(
                        dayNumber = day.dayNumber,
                        title = day.title,
                        status = status,
                        isLast = index == currentSkill.roadmap.size - 1
                    )
                }
            }

            if (completedDays < totalTopics) {
                val currentDay = currentSkill.roadmap[completedDays]
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Day ${currentDay.dayNumber} Resources", 
                    color = Color.White, 
                    fontSize = 16.sp, 
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                currentDay.fileResources.forEach { res ->
                    EnrolledResourceCard(title = res.title, icon = Icons.Default.Description, onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(res.url))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("Progress", "Error opening link", e)
                        }
                    })
                }
                currentDay.videoResources.forEach { video ->
                    EnrolledResourceCard(title = video.title, icon = Icons.Default.PlayCircle, onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(video.url))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("Progress", "Error opening link", e)
                        }
                    })
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { 
                        if (isSaving) return@Button
                        isSaving = true
                        scope.launch {
                            val result = repository.updateProgress(skillId, currentDay.id)
                            if (!result.isSuccess) {
                                snackbarHostState.showSnackbar(result.exceptionOrNull()?.message ?: "Failed to save progress")
                            }
                            isSaving = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AvatarBg),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isSaving
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Background, strokeWidth = 2.dp)
                            Text(text = "Saving...", color = Background, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Background)
                            Text(text = "Mark as Complete", color = Background, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (currentEnrollment.completed) {
                Spacer(modifier = Modifier.height(32.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AvatarBg.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, tint = AvatarBg, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Skill Completed!", color = AvatarBg, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Final Score: ${currentEnrollment.finalQuizScore}%", color = Color.White, fontSize = 16.sp)
                        Text(text = "Credits Earned: +${currentSkill.completionCredits}", color = Primary, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(text = "Final Assessment", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            val roadmapCompleted = completedDays == totalTopics
            val quizLocked = !roadmapCompleted
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !quizLocked) { onNavigate("quiz_view?skillId=$skillId") },
                colors = CardDefaults.cardColors(
                    containerColor = if (quizLocked) GrayGreen.copy(alpha = 0.2f) else GrayGreen.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (currentEnrollment.completed) Primary else if (quizLocked) Color.Transparent else AvatarBg)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (currentEnrollment.completed) Primary else if (quizLocked) Color.White.copy(alpha = 0.05f) else AvatarBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (currentEnrollment.completed) Icons.Default.CheckCircle else if (quizLocked) Icons.Default.Lock else Icons.Default.Assignment,
                            contentDescription = null,
                            tint = if (quizLocked) Color.White.copy(alpha = 0.3f) else if (currentEnrollment.completed) Color.White else Background
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Final Quiz",
                            color = if (quizLocked) Color.White.copy(alpha = 0.4f) else Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (currentEnrollment.completed) "Passed with ${currentEnrollment.finalQuizScore}%" else if (quizLocked) "Complete all days to unlock" else "Test your knowledge to complete skill",
                            color = if (quizLocked) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                    if (!quizLocked && !currentEnrollment.completed) {
                        Icon(imageVector = Icons.Default.ArrowForwardIos, contentDescription = null, tint = AvatarBg, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(GrayGreen.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        .clickable { onNavigate("chat?skillId=$skillId") }
                        .padding(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Message, contentDescription = null, tint = Color.White.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Chat Creator", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Ask ${currentSkill.creatorName}", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                }
                // Placeholder or other action
                Box(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
