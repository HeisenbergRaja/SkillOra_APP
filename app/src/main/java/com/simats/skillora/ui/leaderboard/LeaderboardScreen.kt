package com.simats.skillora.ui.leaderboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.simats.skillora.ui.components.BottomNav
import com.simats.skillora.ui.theme.*
import com.simats.skillora.data.SkillRepository
import com.simats.skillora.data.LeaderboardRepository
import com.simats.skillora.data.LeaderboardUser
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LeaderboardScreen(
    onNavigate: (String) -> Unit
) {
    var activeTab by remember { mutableStateOf("credits") } // "credits" or "skills"
    val repository = remember { SkillRepository() }
    val leaderboardRepo = remember { LeaderboardRepository() }
    
    val userCredits by repository.observeCredits().collectAsState(initial = 0)
    val currentUser = FirebaseAuth.getInstance().currentUser
    
    var leaderboardUsers by remember { mutableStateOf<List<LeaderboardUser>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(activeTab) {
        isLoading = true
        val result = leaderboardRepo.getLeaderboard(activeTab, currentUser?.uid)
        if (result.isSuccess) {
            leaderboardUsers = result.getOrDefault(emptyList())
        }
        isLoading = false
    }

    val top3 = leaderboardUsers.take(3)
    val remaining = leaderboardUsers.drop(3)
    val myRank = leaderboardUsers.find { it.isCurrent }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            BottomNav(activeKey = "leaders", onNavigate = onNavigate)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Leaderboard",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .background(AvatarBg, RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "💰", fontSize = 14.sp)
                            Text(text = "$userCredits Credits", color = Surface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AvatarBg)
                    }
                }
            } else if (leaderboardUsers.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text(text = "No leaderboard data yet.", color = Color.White.copy(alpha = 0.5f))
                    }
                }
            } else {
                // Podium Section
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .background(GrayGreen, RoundedCornerShape(24.dp))
                            .padding(top = 24.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Rank 2
                        if (top3.size >= 2) {
                            val user2 = top3[1]
                            PodiumItem(
                                rank = user2.rank,
                                name = user2.displayName,
                                stats = if (activeTab == "credits") "${user2.totalCreditsEarned} Cr" else "${user2.skillsCompleted} Skills",
                                imageUrl = user2.profileImageUrl ?: "https://picsum.photos/seed/${user2.userId}/100"
                            )
                        }
                        
                        // Rank 1
                        if (top3.isNotEmpty()) {
                            val user1 = top3[0]
                            PodiumItem(
                                rank = user1.rank,
                                name = user1.displayName,
                                stats = if (activeTab == "credits") "${user1.totalCreditsEarned} Cr" else "${user1.skillsCompleted} Skills",
                                imageUrl = user1.profileImageUrl ?: "https://picsum.photos/seed/${user1.userId}/100",
                                isFirst = true
                            )
                        }

                        // Rank 3
                        if (top3.size >= 3) {
                            val user3 = top3[2]
                            PodiumItem(
                                rank = user3.rank,
                                name = user3.displayName,
                                stats = if (activeTab == "credits") "${user3.totalCreditsEarned} Cr" else "${user3.skillsCompleted} Skills",
                                imageUrl = user3.profileImageUrl ?: "https://picsum.photos/seed/${user3.userId}/100"
                            )
                        }
                    }
                }

                // Current User Summary
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    myRank?.let { me ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GrayGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = me.profileImageUrl ?: "https://picsum.photos/seed/${me.userId}/100",
                                contentDescription = null,
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(GrayGreen),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "You're ranked #${me.rank}!", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (activeTab == "credits") "Keep earning to climb higher!" else "Complete more skills to reach the top!",
                                    color = Color.White.copy(alpha = 0.65f),
                                    fontSize = 11.sp
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "Keep\nGoing", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp)
                                Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // Tab Buttons
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GrayGreen.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                            .padding(4.dp)
                    ) {
                        TabButton(label = "By Credits", active = activeTab == "credits", onClick = { activeTab = "credits" }, modifier = Modifier.weight(1f))
                        TabButton(label = "By Skills Completed", active = activeTab == "skills", onClick = { activeTab = "skills" }, modifier = Modifier.weight(1f))
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(text = "All Rankings", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                items(remaining) { ranking ->
                    RankingItemRow(
                        rank = ranking.rank,
                        name = ranking.displayName,
                        dept = ranking.dept,
                        credits = ranking.totalCreditsEarned,
                        skills = ranking.skillsCompleted,
                        imageUrl = ranking.profileImageUrl ?: "https://picsum.photos/seed/${ranking.userId}/100",
                        isCurrent = ranking.isCurrent
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun TabButton(label: String, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) AvatarBg else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (active) Color.White else Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}
