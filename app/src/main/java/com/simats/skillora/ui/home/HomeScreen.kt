package com.simats.skillora.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.skillora.ui.components.BottomNav
import com.simats.skillora.ui.components.SectionHeader
import com.simats.skillora.ui.theme.Background
import com.simats.skillora.ui.theme.AvatarBg
import com.simats.skillora.ui.theme.Surface as ThemeSurface
import com.simats.skillora.ui.marketplace.CategoryChip
import com.simats.skillora.ui.learning.ContinueLearningCardLarge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.simats.skillora.data.SkillRepository
import com.simats.skillora.ui.theme.Primary
import com.google.firebase.auth.FirebaseAuth
import com.simats.skillora.data.CreditsManager
import com.simats.skillora.data.EnrolledSkill
import com.simats.skillora.data.UserProfile

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit
) {
    val opacity = remember { Animatable(0f) }
    val repository = remember { SkillRepository() }
    val creditsManager = remember { CreditsManager() }
    val requestRepository = remember { com.simats.skillora.data.SkillRequestRepository() }
    val leaderboardRepo = remember { com.simats.skillora.data.LeaderboardRepository() }

    val userCredits by repository.observeCredits().collectAsState(initial = 0)
    val user = FirebaseAuth.getInstance().currentUser

    val userProfile by remember(user?.uid) {
        if (user?.uid != null) {
            repository.observeUserProfile(user.uid)
        } else {
            kotlinx.coroutines.flow.flowOf(null)
        }
    }.collectAsState(initial = null)

    var enrolledSkills by remember { mutableStateOf<List<EnrolledSkill>>(emptyList()) }
    var recentRequests by remember { mutableStateOf<List<com.simats.skillora.ui.marketplace.SkillRequest>>(emptyList()) }
    var topLearners by remember { mutableStateOf<List<com.simats.skillora.data.LeaderboardUser>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        opacity.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 450, easing = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1f))
        )
        // Initialize credits if this is a first-time login
        user?.uid?.let { uid ->
            creditsManager.initializeCreditsIfNew(uid)
        }
    }

    LaunchedEffect(user?.uid) {
        if (user?.uid != null) {
            val enrolledResult = repository.getEnrolledSkillsWithProgress()
            if (enrolledResult.isSuccess) {
                enrolledSkills = enrolledResult.getOrDefault(emptyList())
            }
            
            val leaderboardResult = leaderboardRepo.getLeaderboard("credits", user.uid)
            if (leaderboardResult.isSuccess) {
                topLearners = leaderboardResult.getOrDefault(emptyList())
            }
            
            requestRepository.observePendingRequests(limit = 3).collect { requests ->
                recentRequests = requests
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

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
                .alpha(opacity.value)
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp)
        ) {
            HomeHeader(name = userProfile?.name ?: user?.displayName ?: "User", credits = userCredits)

            Spacer(modifier = Modifier.height(8.dp))

            // Categories
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                contentPadding = PaddingValues(end = 24.dp)
            ) {
                items(HomeDummyData.categories) { cat ->
                    CategoryChip(label = cat.label, selected = false, onPress = {
                        onNavigate("marketplace")
                    })
                }
            }

            // Continue Learning Section
            val inProgressSkills = enrolledSkills.filter { !it.enrollment.completed }
            if (inProgressSkills.isNotEmpty()) {
                Column(modifier = Modifier.padding(bottom = 32.dp)) {
                    SectionHeader(title = "Continue Learning", actionLabel = "View All", onActionClick = { onNavigate("learning") })
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(end = 24.dp)
                    ) {
                        items(inProgressSkills) { item ->
                            ContinueLearningCardLarge(
                                category = item.skill.category,
                                title = item.skill.title,
                                progress = item.enrollment.progress,
                                icon = Icons.Default.Book,
                                onContinue = { onNavigate("enrolled_skill?id=${item.skill.id}") }
                            )
                        }
                    }
                }
            } else if (!isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                        .background(com.simats.skillora.ui.theme.Surface.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Start learning your first skill", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onNavigate("marketplace") },
                        colors = ButtonDefaults.buttonColors(containerColor = AvatarBg),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Explore Skills", color = Color.White)
                    }
                }
            }

            // Top Learners Section
            if (topLearners.isNotEmpty()) {
                Column(modifier = Modifier.padding(bottom = 32.dp)) {
                    SectionHeader(title = "Top Learners")
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(end = 24.dp)
                    ) {
                        items(topLearners.take(5)) { learner ->
                            CreatorAvatar(
                                imageUrl = learner.profileImageUrl ?: "https://picsum.photos/seed/${learner.userId}/100",
                                name = learner.displayName,
                                credits = learner.totalCreditsEarned,
                                rank = learner.rank
                            )
                        }
                    }
                }
            }

            // Recent Requests Section
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                SectionHeader(
                    title = "Recent Requests",
                    actionLabel = "New Request",
                    onActionClick = { onNavigate("skill_request") }
                )
                if (recentRequests.isEmpty() && !isLoading) {
                    Text(
                        text = "No skill requests yet.",
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    recentRequests.forEach { req ->
                        RequestCard(
                            title = req.title,
                            requestedBy = req.userName,
                            bounty = 20,
                            upvotes = req.likes,
                            onClick = { onNavigate("marketplace") }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
