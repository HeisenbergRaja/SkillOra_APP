package com.simats.skillora.ui.marketplace

import androidx.compose.animation.core.*
import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.simats.skillora.data.Enrollment
import com.simats.skillora.data.SkillRepository
import com.simats.skillora.ui.components.BottomNav
import com.simats.skillora.ui.components.SectionHeader
import com.simats.skillora.ui.home.*
import com.simats.skillora.ui.theme.*
import com.simats.skillora.ui.upload.Skill
import com.google.firebase.auth.FirebaseAuth

@Composable
fun MarketplaceScreen(
    onNavigate: (String) -> Unit,
    onNavigateToSkillDetails: (String) -> Unit,
    onNavigateToUploadSkill: (SkillRequest?) -> Unit,
    onNavigateToSkillRequestDetails: (SkillRequest) -> Unit
) {
    val opacity = remember { Animatable(0f) }
    var searchQuery by remember { mutableStateOf("") }
    
    val categories = remember { MarketplaceDummyData.categories }
    var activeCategoryId by remember { mutableStateOf(categories[0].id) }
    
    val repository = remember { SkillRepository() }
    val requestRepository = remember { com.simats.skillora.data.SkillRequestRepository() }
    var publishedSkills by remember { mutableStateOf<List<Skill>>(emptyList()) }
    var userEnrollments by remember { mutableStateOf<List<Enrollment>>(emptyList()) }
    var dynamicSkillRequests by remember { mutableStateOf<List<SkillRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val userCredits by repository.observeCredits().collectAsState(initial = 0)
    val user = FirebaseAuth.getInstance().currentUser

    LaunchedEffect(Unit) {
        opacity.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 450, easing = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1f))
        )
        try {
            val result = repository.getPublishedSkills()
            if (result.isSuccess) {
                publishedSkills = result.getOrDefault(emptyList())
            }
            val enrollmentsResult = repository.getEnrolledSkillsWithProgress()
            if (enrollmentsResult.isSuccess) {
                userEnrollments = enrollmentsResult.getOrNull()?.map { it.enrollment } ?: emptyList()
            }
        } catch (e: Exception) {
            Log.e("Marketplace", "Error loading data", e)
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        requestRepository.observePendingRequests().collect { requests ->
            dynamicSkillRequests = requests
        }
    }

    val filteredSkills = remember(activeCategoryId, searchQuery, publishedSkills) {
        val q = searchQuery.trim().lowercase()
        publishedSkills.filter { s ->
            val categoryItem = categories.find { it.id == activeCategoryId }
            val categoryLabel = categoryItem?.label?.lowercase() ?: ""
            val matchesCategory = activeCategoryId == "all" || 
                                 s.category.lowercase() == activeCategoryId.lowercase() || 
                                 s.category.lowercase() == categoryLabel
            
            val matchesQuery = if (q.isNotEmpty()) {
                "${s.title} ${s.creatorName} ${s.category}".lowercase().contains(q)
            } else true
            matchesCategory && matchesQuery
        }
    }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            BottomNav(activeKey = "marketplace", onNavigate = onNavigate)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToUploadSkill(null) },
                containerColor = Color(0x8CAEC279),
                contentColor = Color(0xE620271E),
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 80.dp)
                    .size(54.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Upload skill", modifier = Modifier.size(20.dp))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .alpha(opacity.value),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        AsyncImage(
                            model = user?.photoUrl ?: MarketplaceDummyData.user.avatarUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(com.simats.skillora.ui.theme.Surface),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = "Marketplace",
                            color = Primary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    CreditBadge(credits = userCredits)
                }
            }

            item {
                SearchBar(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Search skills..."
                )
            }

            item {
                Column(modifier = Modifier.padding(bottom = 32.dp)) {
                    SectionHeader(title = "Categories")
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(end = 24.dp)
                    ) {
                        items(categories) { cat ->
                            CategoryChip(
                                label = cat.label,
                                selected = cat.id == activeCategoryId,
                                onPress = { activeCategoryId = cat.id }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    SectionHeader(title = "Published Skills")
                }
            }

            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AvatarBg)
                    }
                }
            } else if (filteredSkills.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text(text = "No skills found", color = Color.White.copy(alpha = 0.5f))
                    }
                }
            }

            items(filteredSkills) { skill ->
                val enrollment = userEnrollments.find { it.skillId == skill.id }
                val currentUserId = user?.uid
                
                MarketplaceSkillCard(
                    imageUrl = null, // Remove random picsum URL
                    title = skill.title,
                    instructor = skill.creatorName,
                    category = skill.category,
                    rating = "4.8",
                    credits = skill.creditsRequired,
                    enrollmentStatus = when {
                        skill.creatorId == currentUserId -> "Your Skill"
                        enrollment?.completed == true -> "Completed"
                        enrollment != null -> "Continue"
                        else -> null
                    },
                    onPress = { onNavigateToSkillDetails(skill.id) },
                    onPressEnroll = { onNavigateToSkillDetails(skill.id) }
                )
            }

            item {
                Column(modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)) {
                    SectionHeader(title = "Skill Requests")
                    if (dynamicSkillRequests.isEmpty()) {
                        Text(
                            text = "No skill requests yet.",
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    } else {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(end = 24.dp)
                        ) {
                            items(dynamicSkillRequests) { req ->
                                SkillRequestCard(
                                    userAvatarUrl = req.userAvatarUrl,
                                    userName = req.userName,
                                    title = req.title,
                                    likes = req.likes,
                                    requestedAgo = req.requestedAgo,
                                    onPress = { onNavigateToSkillRequestDetails(req) },
                                    onPressUpload = { onNavigateToUploadSkill(req) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
