package com.simats.skillora.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.simats.skillora.ui.components.BottomNav
import com.simats.skillora.ui.components.SectionHeader
import com.simats.skillora.ui.theme.Background
import com.simats.skillora.data.SkillRepository
import com.simats.skillora.data.UserProfile
import com.simats.skillora.data.LeaderboardRepository
import com.simats.skillora.ui.upload.Skill
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import com.simats.skillora.ui.theme.AvatarBg
import com.simats.skillora.ui.theme.ProfileCardBg
import com.simats.skillora.ui.theme.LogoutColor
import androidx.compose.foundation.horizontalScroll

@Composable
fun ProfileScreen(
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val repository = remember { SkillRepository() }
    val leaderboardRepo = remember { LeaderboardRepository() }
    
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val userCredits by repository.observeCredits().collectAsState(initial = 0)
    val totalCreditsEarned by repository.observeTotalCreditsEarned().collectAsState(initial = 0)
    
    val userProfile by remember(currentUserId) {
        if (currentUserId != null) {
            repository.observeUserProfile(currentUserId)
        } else {
            kotlinx.coroutines.flow.flowOf(null)
        }
    }.collectAsState(initial = null)
    
    var createdSkills by remember { mutableStateOf<List<Skill>>(emptyList()) }
    var enrolledSkills by remember { mutableStateOf<List<com.simats.skillora.data.EnrolledSkill>>(emptyList()) }
    var leaderboardRank by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(currentUserId) {
        currentUserId?.let { uid ->
            val createdResult = repository.getCreatedSkills(uid)
            if (createdResult.isSuccess) {
                createdSkills = createdResult.getOrDefault(emptyList())
            }
            
            val enrolledResult = repository.getEnrolledSkillsWithProgress()
            if (enrolledResult.isSuccess) {
                enrolledSkills = enrolledResult.getOrDefault(emptyList())
            }
            
            leaderboardRank = leaderboardRepo.getUserRank(uid, "credits")
        }
        isLoading = false
    }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            BottomNav(activeKey = "profile", onNavigate = onNavigate)
        }
    ) { padding ->
        if (isLoading && userProfile == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AvatarBg)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp)
            ) {
            ProfileHeader()

                Column(
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    val profile = userProfile
                    ProfileCard(
                        name = profile?.name ?: "User",
                        dept = profile?.dept ?: "Student",
                        college = profile?.college ?: "Skillora Academy",
                        credits = userCredits,
                        profileImageUrl = profile?.profileImageUrl
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val enrolled = enrolledSkills.size
                        val completed = enrolledSkills.count { it.enrollment.completed }
                        val inProgress = enrolled - completed
                        
                        StatBlock(number = createdSkills.size.toString(), label = "Skills\nCreated")
                        StatBlock(number = inProgress.coerceAtLeast(0).toString(), label = "Skills\nIn Progress")
                        StatBlock(number = completed.toString(), label = "Skills\nCompleted")
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Rank Display
                    if (leaderboardRank > 0) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = ProfileCardBg.copy(alpha = 0.3f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, tint = AvatarBg)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(text = "Global Rank", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                }
                                Text(text = "#$leaderboardRank", color = AvatarBg, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    SectionHeader(
                        title = "Skills I Created", 
                        actionLabel = "Learner Chats", 
                        onActionClick = { onNavigate("publisher_chats") }
                    )
                    if (createdSkills.isEmpty()) {
                        Text(text = "You haven't published any skills yet.", color = Color.White.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 16.dp))
                    } else {
                        createdSkills.forEach { skill ->
                            CreatedSkillCard(
                                title = skill.title,
                                learners = 0, 
                                credits = 20, 
                                rating = "4.8"
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    SectionHeader(title = "Skills I Learned", actionLabel = "See All", onActionClick = {})
                    if (enrolledSkills.isEmpty()) {
                        Text(text = "You haven't enrolled in any skills yet.", color = Color.White.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 16.dp))
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            enrolledSkills.forEach { item ->
                                LearnedSkillCard(
                                    title = item.skill.title,
                                    status = if (item.enrollment.completed) "Completed" else "In Progress",
                                    completed = item.enrollment.completed
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Extra Stat for Earned Credits
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                         Text(text = "Total Credits Earned", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                         Text(text = "$totalCreditsEarned Credits", color = AvatarBg, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ProfileCardBg),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Logout, contentDescription = null, tint = LogoutColor)
                            Text(text = "Logout", color = LogoutColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
