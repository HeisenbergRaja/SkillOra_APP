package com.simats.skillora.ui.quiz

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.skillora.ui.components.BottomNav
import com.simats.skillora.ui.theme.*
import com.simats.skillora.data.*
import kotlinx.coroutines.launch

@Composable
fun QuizScreen(
    skillId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val repository = remember { SkillRepository() }
    val scope = rememberCoroutineScope()
    
    var quiz by remember { mutableStateOf<FinalQuiz?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var currentIndex by remember { mutableStateOf(0) }
    val userAnswers = remember { mutableStateMapOf<String, Int>() }
    var isSubmitting by remember { mutableStateOf(false) }
    var quizResult by remember { mutableStateOf<QuizAttempt?>(null) }

    LaunchedEffect(skillId) {
        val result = repository.getSkillById(skillId)
        if (result.isSuccess) {
            val skill = result.getOrNull()
            if (skill?.finalQuiz != null) {
                quiz = skill.finalQuiz
            } else {
                errorMessage = "No quiz found for this skill."
            }
        } else {
            errorMessage = result.exceptionOrNull()?.message ?: "Failed to load skill."
        }
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Primary)
        }
        return
    }

    if (errorMessage != null) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = errorMessage!!, color = Color.White, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBack) { Text("Go Back") }
            }
        }
        return
    }

    if (quizResult != null) {
        QuizResultScreen(
            result = quizResult!!,
            onBack = onBack,
            onRetry = {
                quizResult = null
                currentIndex = 0
                userAnswers.clear()
            }
        )
        return
    }

    val currentQuiz = quiz ?: return
    val currentQuestion = currentQuiz.questions[currentIndex]
    val progress = ((currentIndex + 1).toFloat() / currentQuiz.questions.size)

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
                    Text(text = "Final Quiz", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier.background(AvatarBg.copy(alpha = 0.5f), RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(text = "${currentIndex + 1}/${currentQuiz.questions.size}", color = Primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Progress Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GrayGreen, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Question Progress", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(text = "${currentIndex + 1} of ${currentQuiz.questions.size}", color = AvatarBg, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = AvatarBg,
                    trackColor = EditBtnBg
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Question Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GrayGreen, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "QUESTION ${currentIndex + 1}", color = AvatarBg, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = currentQuestion.question,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Options
            currentQuestion.options.forEachIndexed { index, option ->
                QuizOptionCard(
                    label = ('A' + index).toString(),
                    text = option,
                    selected = userAnswers[currentQuestion.questionId] == index,
                    onClick = { userAnswers[currentQuestion.questionId] = index }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (currentIndex > 0) {
                    OutlinedButton(
                        onClick = { currentIndex-- },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, AvatarBg)
                    ) {
                        Text("Previous", color = AvatarBg)
                    }
                }

                if (currentIndex < currentQuiz.questions.size - 1) {
                    Button(
                        onClick = { currentIndex++ },
                        enabled = userAnswers.containsKey(currentQuestion.questionId),
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AvatarBg),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Next", color = Background)
                    }
                } else {
                    Button(
                        onClick = {
                            isSubmitting = true
                            scope.launch {
                                var correctCount = 0
                                currentQuiz.questions.forEach { q ->
                                    if (userAnswers[q.questionId] == q.correctAnswer) {
                                        correctCount++
                                    }
                                }
                                val scorePercentage = (correctCount * 100) / currentQuiz.questions.size
                                val passed = scorePercentage >= currentQuiz.passingScore
                                
                                val attempt = QuizAttempt(
                                    quizVersion = currentQuiz.quizVersion,
                                    score = correctCount,
                                    scorePercentage = scorePercentage,
                                    correctAnswers = correctCount,
                                    totalQuestions = currentQuiz.questions.size,
                                    passed = passed,
                                    answers = userAnswers.toMap()
                                )
                                
                                val result = repository.submitQuizAttempt(skillId, attempt)
                                if (result.isSuccess) {
                                    quizResult = result.getOrNull()
                                } else {
                                    errorMessage = result.exceptionOrNull()?.message
                                }
                                isSubmitting = false
                            }
                        },
                        enabled = userAnswers.size == currentQuiz.questions.size && !isSubmitting,
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Text("Submit Quiz", color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun QuizOptionCard(
    label: String,
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) AvatarBg.copy(alpha = 0.2f) else GrayGreen
        ),
        border = BorderStroke(
            1.dp,
            if (selected) AvatarBg else Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) AvatarBg else Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = label, color = if (selected) Background else Color.White, fontWeight = FontWeight.Bold)
            }
            Text(text = text, color = Color.White, fontSize = 16.sp)
        }
    }
}

@Composable
fun QuizResultScreen(
    result: QuizAttempt,
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (result.passed) {
            Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, tint = Primary, modifier = Modifier.size(100.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text("Congratulations!", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Skill Completed Successfully", color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp)
        } else {
            Icon(imageVector = Icons.Default.Cancel, contentDescription = null, tint = Color.Red, modifier = Modifier.size(100.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text("Quiz Failed", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Keep learning and try again!", color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(48.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GrayGreen),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Your Score", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                Text(text = "${result.scorePercentage}%", color = if (result.passed) Primary else Color.Red, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                Text(text = "${result.correctAnswers} / ${result.totalQuestions} Correct", color = Color.White, fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        if (result.passed) {
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text("Continue", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                ) {
                    Text("Exit", color = Color.White)
                }
                Button(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AvatarBg),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text("Retry Quiz", color = Background)
                }
            }
        }
    }
}
