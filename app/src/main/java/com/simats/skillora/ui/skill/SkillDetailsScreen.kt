package com.simats.skillora.ui.skill

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.simats.skillora.data.Enrollment
import com.simats.skillora.data.SkillRepository
import com.simats.skillora.ui.components.SectionHeader
import com.simats.skillora.ui.home.*
import com.simats.skillora.ui.theme.*
import com.simats.skillora.ui.theme.Surface as ThemeSurface
import com.simats.skillora.ui.upload.RoadmapDay
import com.simats.skillora.ui.upload.Skill
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth

@Composable
fun SkillDetailsScreen(
    skillId: String,
    onBack: () -> Unit,
    onEnrollSuccess: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val repository = remember { SkillRepository() }
    var skill by remember { mutableStateOf<Skill?>(null) }
    var enrollment by remember { mutableStateOf<Enrollment?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isEnrolling by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val userCredits by repository.observeCredits().collectAsState(initial = 0)

    LaunchedEffect(skillId) {
        val result = repository.getSkillById(skillId)
        if (result.isSuccess) {
            skill = result.getOrNull()
        } else {
            error = result.exceptionOrNull()?.message ?: "Skill not found"
        }
        val enrollmentResult = repository.getEnrollment(skillId)
        if (enrollmentResult.isSuccess) {
            enrollment = enrollmentResult.getOrNull()
        }
        isLoading = false
    }

    var isWishlisted by remember { mutableStateOf(false) }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val isCreator = skill?.creatorId == currentUserId
    val isEnrolled = enrollment != null

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AvatarBg)
        }
        return
    }

    if (skill == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = error ?: "Skill not found", color = Color.White)
        }
        return
    }

    val currentSkill = skill!!

    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(
                color = Background,
                modifier = Modifier.fillMaxWidth().padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isWishlisted = !isWishlisted },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (isWishlisted) Color(0xBFAEC279) else Color(0x29E7E9E6))
                            .border(1.dp, Color(0x14F2F3F1), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Wishlist",
                            tint = if (isWishlisted) ThemeSurface else Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Button(
                        onClick = {
                            if (isCreator) return@Button
                            if (isEnrolled) {
                                onEnrollSuccess(true)
                                return@Button
                            }
                            if (isEnrolling) return@Button
                            isEnrolling = true
                            scope.launch {
                                val result = repository.enrollInSkill(currentSkill.id, currentSkill.creditsRequired)
                                isEnrolling = false
                                if (result.isSuccess) {
                                    onEnrollSuccess(false)
                                } else {
                                    snackbarHostState.showSnackbar(result.exceptionOrNull()?.message ?: "Enrollment failed")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCreator) Color(0x33E7E9E6) else Color(0x8CAEC279)
                        ),
                        shape = RoundedCornerShape(18.dp),
                        enabled = !isEnrolling
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (isEnrolling) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Primary, strokeWidth = 2.dp)
                            } else if (isCreator) {
                                Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                                Text(text = "Your Skill", color = Primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            } else if (isEnrolled) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                                Text(text = "Continue Learning", color = Primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            } else {
                                Icon(imageVector = Icons.Default.CreditCard, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                                Text(text = "Enroll Now \u2014 ${currentSkill.creditsRequired} Credits", color = Primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Primary, modifier = Modifier.size(20.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = Primary, modifier = Modifier.size(18.dp))
                    CreditBadge(credits = userCredits)
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                // Hero Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x29E7E9E6), RoundedCornerShape(18.dp))
                        .border(1.dp, Color(0x14F2F3F1), RoundedCornerShape(18.dp))
                        .padding(vertical = 32.dp, horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(74.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(AvatarBg.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = AvatarBg,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = currentSkill.title, color = Primary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AsyncImage(
                            model = "https://i.pravatar.cc/150?u=${currentSkill.creatorId}",
                            contentDescription = null,
                            modifier = Modifier.size(18.dp).clip(CircleShape).background(ThemeSurface)
                        )
                        Text(text = "by ${currentSkill.creatorName}", color = Color(0xB2F2F3F1), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.Center) {
                        DetailPill(label = currentSkill.category, selected = true)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(5) { i ->
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (i < 5) Primary else Color(0x40F2F3F1),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        Text(text = "4.8 (0 reviews)", color = Color(0x8CF2F3F1), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = Icons.Default.CreditCard, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                        Text(text = "${currentSkill.creditsRequired} Credits to Enroll", color = Color(0xBFF2F3F1), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Description
                SectionHeader(title = "Description")
                Text(
                    text = currentSkill.description,
                    color = Color(0xA6F2F3F1),
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // What You'll Learn
                SectionHeader(title = "What You'll Learn")
                currentSkill.whatYoullLearn.split("\n").filter { it.isNotBlank() }.forEach { pt ->
                    Row(modifier = Modifier.padding(bottom = 12.dp), verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(0x1FE7E9E6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Primary, modifier = Modifier.size(12.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = pt.trim().removePrefix("•").trim(), color = Color(0xB2F2F3F1), fontSize = 14.sp, lineHeight = 22.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Learning Roadmap
                SectionHeader(title = "Learning Roadmap")
                currentSkill.roadmap.forEach { day ->
                    RoadmapDayAccordion(
                        day = day,
                        onOpenUrl = { url ->
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                scope.launch { snackbarHostState.showSnackbar("Unable to open link") }
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun RoadmapDayAccordion(
    day: RoadmapDay,
    onOpenUrl: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .background(GrayGreen, RoundedCornerShape(12.dp))
            .clickable { isExpanded = !isExpanded }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Day ${day.dayNumber}",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = day.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                if (!isExpanded) {
                    Text(
                        text = "${day.fileResources.size} Resources • ${day.videoResources.size} Videos",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp
                    )
                }
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f)
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Overall Description", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                Text(text = day.description, color = Color.White, fontSize = 13.sp, lineHeight = 20.sp)

                if (day.fileResources.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "File / Notes Resources", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    day.fileResources.forEach { res ->
                        ResourceDetailItem(title = res.title, url = res.url, icon = Icons.Default.Description, onClick = onOpenUrl)
                    }
                }

                if (day.videoResources.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Video Resources", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    day.videoResources.forEach { video ->
                        ResourceDetailItem(title = video.title, url = video.url, icon = Icons.Default.PlayCircle, onClick = onOpenUrl)
                    }
                }
            }
        }
    }
}

@Composable
fun ResourceDetailItem(title: String, url: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(url) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = title, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
    }
}
