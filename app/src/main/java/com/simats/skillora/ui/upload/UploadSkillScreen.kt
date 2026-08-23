package com.simats.skillora.ui.upload

import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.skillora.ui.theme.Background
import com.simats.skillora.ui.theme.Primary
import com.simats.skillora.ui.theme.AvatarBg
import com.simats.skillora.ui.theme.GrayGreen
import com.simats.skillora.data.SkillRepository
import com.simats.skillora.ui.components.BottomNav
import kotlinx.coroutines.launch
import com.simats.skillora.data.FinalQuiz
import com.simats.skillora.data.QuizQuestion
import org.json.JSONObject
import org.json.JSONArray
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalClipboardManager
import com.simats.skillora.data.llm.LocalLLMEngine
import com.simats.skillora.data.llm.LocalModelManager
import com.simats.skillora.data.llm.ModelState
import com.simats.skillora.data.llm.QuizGenerationManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadSkillScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    requestedTitle: String? = null,
    requestId: String? = null
) {
    val TAG = "UploadSkillScreen"
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val repository = remember { SkillRepository() }
    val context = LocalContext.current

    val modelManager = remember { LocalModelManager(context) }
    val llmEngine = remember { LocalLLMEngine() }
    val generationManager = remember { QuizGenerationManager(modelManager, llmEngine) }
    
    val modelStatus by modelManager.modelStatus.collectAsState()
    val isGenerating by generationManager.isGenerating.collectAsState()

    LaunchedEffect(modelStatus.state) {
        if (modelStatus.state == ModelState.INSTALLED && LocalLLMEngine.isAvailable() && modelStatus.modelPath != null) {
            llmEngine.initialize(modelStatus.modelPath!!, com.simats.skillora.data.llm.LocalModelConfig())
        }
    }

    var title by remember { mutableStateOf(requestedTitle ?: "") }
    var description by remember { mutableStateOf("") }
    var whatYouLearn by remember { mutableStateOf("") }
    var credits by remember { mutableStateOf("20") }
    var selectedCategory by remember { mutableStateOf("Programming") }
    var customCategoryName by remember { mutableStateOf("") }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }

    var isPublishing by remember { mutableStateOf(false) }
    var questionCount by remember { mutableStateOf("10") }
    var passingScore by remember { mutableStateOf("70") }
    var generatedPrompt by remember { mutableStateOf("") }
    var showPromptDialog by remember { mutableStateOf(false) }
    var pastedQuizPrompt by remember { mutableStateOf("") }
    var validatedQuiz by remember { mutableStateOf<FinalQuiz?>(null) }
    var quizStatus by remember { mutableStateOf("NOT_GENERATED") } 
    var showQuizReview by remember { mutableStateOf(false) }
    var generationPhase by remember { mutableStateOf("") }
    var rawGeneratedContent by remember { mutableStateOf<String?>(null) }
    var showRawOutputDialog by remember { mutableStateOf(false) }
    
    val clipboardManager = LocalClipboardManager.current
    val userCredits by repository.observeCredits().collectAsState(initial = 0)

    val roadmap = remember { mutableStateListOf<RoadmapDay>(
        RoadmapDay(dayNumber = 1, title = "", isExpanded = true)
    ) }

    var editingVideo by remember { mutableStateOf<Pair<String, ResourceLink?>?>(null) }
    var editingResource by remember { mutableStateOf<Pair<String, ResourceLink?>?>(null) }
    var dayToDelete by remember { mutableStateOf<RoadmapDay?>(null) }

    fun validate(): String? {
        if (title.isBlank()) return "Please enter a skill title"
        if (description.isBlank()) return "Please enter a description"
        val parsedCredits = credits.toIntOrNull()
        if (parsedCredits == null || parsedCredits <= 0) return "Credits to Buy must be a valid positive number"
        if (roadmap.isEmpty()) return "Please add at least one roadmap day"
        if (quizStatus != "APPROVED") return "Please approve the AI-generated quiz first."
        return null
    }

    fun generateQuizPrompt(): String {
        val days = roadmap.joinToString("\n") { "Day ${it.dayNumber}: ${it.title} - ${it.description}" }
        return "Skill: $title\nRoadmap:\n$days\n\nTask: Extract key facts and produce a prompt for a local AI to generate $questionCount MCQs. Format requirements: JSON with 'questions' key."
    }

    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { BottomNav(activeKey = "marketplace", onNavigate = onNavigate) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Primary) }
                Text("Upload Skill", color = Primary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                Surface(color = AvatarBg, shape = RoundedCornerShape(20.dp)) {
                    Text("$userCredits Credits", color = Color(0xFF20271E), modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(24.dp))
            UploadLabel("Skill Title")
            UploadTextField(title, { title = it }, "e.g. Kotlin Coroutines")
            
            Spacer(Modifier.height(16.dp))
            UploadLabel("Description")
            UploadTextField(description, { description = it }, "Describe this skill...", 100.dp)

            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f)) {
                    UploadLabel("Category")
                    Box {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            shape = RoundedCornerShape(14.dp),
                            trailingIcon = { IconButton(onClick = { isCategoryDropdownExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null, tint = Color.White) } },
                            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.White.copy(0.1f), focusedBorderColor = AvatarBg, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        DropdownMenu(
                            expanded = isCategoryDropdownExpanded,
                            onDismissRequest = { isCategoryDropdownExpanded = false },
                            modifier = Modifier.background(Background)
                        ) {
                            com.simats.skillora.ui.marketplace.MarketplaceDummyData.categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.label, color = Color.White) },
                                    onClick = { selectedCategory = cat.label; isCategoryDropdownExpanded = false }
                                )
                            }
                        }
                    }
                }
                Column(Modifier.weight(1f)) {
                    UploadLabel("Credits to Buy")
                    UploadTextField(credits, { credits = it }, "e.g. 50")
                }
            }

            Spacer(Modifier.height(32.dp))
            Text("Roadmap", color = Color.White, fontWeight = FontWeight.Bold)
            roadmap.forEachIndexed { index, day ->
                RoadmapCard(day, { roadmap[index] = day.copy(isExpanded = !day.isExpanded) }, { dayToDelete = day }, { roadmap[index] = day.copy(title = it) }, { roadmap[index] = day.copy(description = it) },
                    { editingResource = day.id to null }, { editingResource = day.id to it }, { id -> roadmap[index] = day.copy(fileResources = day.fileResources.filter { it.id != id }) },
                    { editingVideo = day.id to null }, { editingVideo = day.id to it }, { id -> roadmap[index] = day.copy(videoResources = day.videoResources.filter { it.id != id }) })
            }
            DashedButton("Add Day", Icons.Default.Add) { roadmap.add(RoadmapDay(dayNumber = roadmap.size + 1, isExpanded = true)) }

            Spacer(Modifier.height(32.dp))
            Text("AI QUIZ GENERATION", color = Color.White, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f)) { UploadLabel("Questions / Count"); UploadTextField(questionCount, { questionCount = it }, "10") }
                Column(Modifier.weight(1f)) { UploadLabel("Passing Score (%)"); UploadTextField(passingScore, { passingScore = it }, "70") }
            }

            Spacer(Modifier.height(16.dp))
            Button(onClick = { 
                val count = questionCount.toIntOrNull() ?: 10
                val promptText = com.simats.skillora.data.llm.QuizSystemPrompt.buildResourceAnalysisPrompt(title, count)
                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(promptText))
                scope.launch { snackbarHostState.showSnackbar("Resource analysis prompt copied.") }
            }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = GrayGreen)) {
                Text("1. Get Resource Analysis Prompt", color = Color.Black)
            }
            
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = pastedQuizPrompt, 
                onValueChange = { pastedQuizPrompt = it }, 
                modifier = Modifier.fillMaxWidth().height(120.dp), 
                placeholder = { Text("2. Paste self-contained quiz prompt here...") }
            )
            Text("Hint: Paste the self-contained quiz prompt generated by an external AI.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))

            if (isGenerating || generationPhase.isNotEmpty()) {
                Button(onClick = { generationManager.cancel(); generationPhase = "" }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                    Text(if (generationPhase.isNotEmpty()) generationPhase else "Generating quiz locally...")
                }
            } else {
                Button(onClick = {
                    if (pastedQuizPrompt.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar("Please paste a self-contained quiz prompt.") }
                        return@Button
                    }
                    val count = questionCount.toIntOrNull() ?: 0
                    if (count < 1 || count > 20) {
                        scope.launch { snackbarHostState.showSnackbar("Question count must be between 1 and 20.") }
                        return@Button
                    }
                    val passScore = passingScore.toIntOrNull() ?: -1
                    if (passScore < 0 || passScore > 100) {
                        scope.launch { snackbarHostState.showSnackbar("Passing score must be between 0 and 100.") }
                        return@Button
                    }

                    scope.launch {
                        generationPhase = "Generating quiz locally..."
                        val res = generationManager.generateQuiz(title, pastedQuizPrompt, count)
                        if (res.isSuccess) {
                            val quiz = res.getOrThrow().copy(passingScore = passScore)
                            validatedQuiz = quiz
                            quizStatus = "GENERATED"
                            showQuizReview = true
                            snackbarHostState.showSnackbar("Quiz Generated Successfully ✓")
                        } else {
                            val errMessage = res.exceptionOrNull()?.message ?: "JSON_INVALID"
                            val displayMsg = when (errMessage) {
                                "PROMPT_TOO_LARGE" -> "Quiz prompt is too long. Please generate a more concise resource analysis prompt."
                                "QUESTION_COUNT_MISMATCH" -> "The generated quiz does not contain the requested number of questions."
                                "JSON_INVALID" -> "The local AI returned an invalid quiz. Please try again."
                                "NATIVE_ENGINE_ERROR" -> "Local AI engine failed to generate the quiz."
                                else -> "Quiz generation stopped before the quiz was complete. Please try again."
                            }
                            snackbarHostState.showSnackbar(displayMsg)
                        }
                        generationPhase = ""
                    }
                }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = AvatarBg)) {
                    Text("3. Generate Quiz Locally", color = Color.Black)
                }
            }

            Spacer(Modifier.height(16.dp))
            val isEngineAvailable = LocalLLMEngine.isAvailable()
            val aiStatusText = if (!isEngineAvailable) "NATIVE ENGINE MISSING" else if (modelStatus.state == ModelState.INSTALLED && modelStatus.modelPath != null) "READY" else "MODEL ERROR"
            
            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Text("LOCAL AI", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Qwen3 1.7B", color = Color.White, fontSize = 14.sp)
                Text("Status: $aiStatusText", color = if (aiStatusText == "READY") Color.Green else Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            if (validatedQuiz != null) {
                Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), colors = CardDefaults.cardColors(containerColor = GrayGreen.copy(0.2f))) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Quiz Status: $quizStatus", color = Primary, fontWeight = FontWeight.Bold)
                        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { showQuizReview = true }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = AvatarBg)) { Text("Review/Edit", color = Color.Black) }
                            Button(onClick = { quizStatus = "APPROVED" }, Modifier.weight(1f)) { Text("Approve") }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
            Button(onClick = {
                val err = validate()
                if (err != null) { scope.launch { snackbarHostState.showSnackbar(err) }; return@Button }
                isPublishing = true
                scope.launch {
                    val skill = com.simats.skillora.ui.upload.Skill(
                        title = title, description = description, whatYoullLearn = whatYouLearn,
                        category = selectedCategory, creditsRequired = credits.toIntOrNull() ?: 20, roadmap = roadmap.toList(),
                        finalQuiz = validatedQuiz?.copy(status = "PUBLISHED"), status = "published"
                    )
                    if (repository.publishSkill(skill).isSuccess) onNavigate("upload_success")
                    else snackbarHostState.showSnackbar("Publishing failed.")
                    isPublishing = false
                }
            }, modifier = Modifier.fillMaxWidth().height(56.dp), enabled = !isPublishing) {
                Text(if (isPublishing) "Publishing..." else "Publish Skill", fontWeight = FontWeight.Bold)
            }
        }

        // Dialogs
        editingVideo?.let { pair ->
            ResourceDialog(pair.second, "Video", { editingVideo = null }, { t, u ->
                val idx = roadmap.indexOfFirst { it.id == pair.first }
                if (idx != -1) {
                    val day = roadmap[idx]
                    roadmap[idx] = if (pair.second == null) day.copy(videoResources = day.videoResources + ResourceLink(title = t, url = u))
                    else day.copy(videoResources = day.videoResources.map { if (it.id == pair.second?.id) it.copy(title = t, url = u) else it })
                }
                editingVideo = null
            })
        }
        editingResource?.let { pair ->
            ResourceDialog(pair.second, "Resource", { editingResource = null }, { t, u ->
                val idx = roadmap.indexOfFirst { it.id == pair.first }
                if (idx != -1) {
                    val day = roadmap[idx]
                    roadmap[idx] = if (pair.second == null) day.copy(fileResources = day.fileResources + ResourceLink(title = t, url = u))
                    else day.copy(fileResources = day.fileResources.map { if (it.id == pair.second?.id) it.copy(title = t, url = u) else it })
                }
                editingResource = null
            })
        }
        dayToDelete?.let { day -> DeleteConfirmationDialog(day.title, { dayToDelete = null }, { roadmap.remove(day); dayToDelete = null }) }

        if (showQuizReview) {
            validatedQuiz?.let { quiz ->
                QuizReviewDialog(quiz, title, { showQuizReview = false }, { validatedQuiz = it; quizStatus = "APPROVED" }, {
                    scope.launch {
                        generationPhase = "Regenerating..."
                        val res = generationManager.generateQuiz(title, pastedQuizPrompt, questionCount.toIntOrNull() ?: 10)
                        if (res.isSuccess) { validatedQuiz = res.getOrThrow().copy(passingScore = passingScore.toIntOrNull() ?: 70); quizStatus = "GENERATED" }
                        generationPhase = ""
                    }
                })
            }
        }
        if (showRawOutputDialog) {
            AlertDialog(onDismissRequest = { showRawOutputDialog = false }, title = { Text("AI Raw Output") }, text = { Text(rawGeneratedContent ?: "", fontSize = 10.sp, fontFamily = FontFamily.Monospace) }, confirmButton = { TextButton(onClick = { showRawOutputDialog = false }) { Text("Close") } })
        }
        if (showPromptDialog) {
            AlertDialog(onDismissRequest = { showPromptDialog = false }, title = { Text("Preparation Prompt") }, text = { Text(generatedPrompt) }, confirmButton = { TextButton(onClick = { clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(generatedPrompt)); showPromptDialog = false }) { Text("Copy") } })
        }
    }
}

@Composable
fun QuizReviewDialog(quiz: FinalQuiz, skillTitle: String, onDismiss: () -> Unit, onSave: (FinalQuiz) -> Unit, onRegenerate: () -> Unit) {
    var editedQuiz by remember { mutableStateOf(quiz) }
    var questionToEdit by remember { mutableStateOf<Pair<Int, QuizQuestion>?>(null) }
    var showAddQuestion by remember { mutableStateOf(false) }
    var questionToDelete by remember { mutableStateOf<Int?>(null) }
    var showConfirmClose by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { showConfirmClose = true },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxSize(),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Review Quiz", modifier = Modifier.weight(1f), color = Primary, fontWeight = FontWeight.Bold)
                IconButton(onClick = { showConfirmClose = true }) { Icon(Icons.Default.Close, null, tint = Color.White) }
            }
        },
        text = {
            Column {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = CardDefaults.cardColors(containerColor = GrayGreen.copy(0.1f))) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Skill: $skillTitle", color = Primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(editedQuiz.quizTitle, color = Color.White, fontWeight = FontWeight.Bold)
                        Text("${editedQuiz.questions.size} Questions • Passing: ${editedQuiz.passingScore}%", fontSize = 12.sp, color = Color.White.copy(0.6f))
                    }
                }
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(editedQuiz.questions) { index, q -> 
                        QuestionReviewCard(index, q, onEdit = { questionToEdit = index to q }, onDelete = { questionToDelete = index })
                    }
                    item { Button(onClick = { showAddQuestion = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = GrayGreen)) { Icon(Icons.Default.Add, null); Text("Add Question") } }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = { onRegenerate(); onDismiss() }) { Text("Regenerate", color = Color.Red) }
                Button(onClick = { onSave(editedQuiz); onDismiss() }, colors = ButtonDefaults.buttonColors(containerColor = AvatarBg)) { Text("Approve & Save", color = Color.Black) }
            }
        },
        containerColor = Background
    )

    if (showConfirmClose) {
        AlertDialog(onDismissRequest = { showConfirmClose = false }, title = { Text("Leave without saving?") }, text = { Text("Your generated quiz and edits will be lost.") },
            confirmButton = { TextButton(onClick = { showConfirmClose = false; onDismiss() }) { Text("Leave", color = Color.Red) } },
            dismissButton = { TextButton(onClick = { showConfirmClose = false }) { Text("Stay") } })
    }

    questionToEdit?.let { (idx, q) ->
        QuestionEditDialog(q, { questionToEdit = null }, { updated ->
            val nl = editedQuiz.questions.toMutableList(); nl[idx] = updated
            editedQuiz = editedQuiz.copy(questions = nl); questionToEdit = null
        })
    }
    if (showAddQuestion) {
        QuestionEditDialog(QuizQuestion(), { showAddQuestion = false }, { newQ ->
            val nl = editedQuiz.questions.toMutableList(); nl.add(newQ)
            editedQuiz = editedQuiz.copy(questions = nl, totalQuestions = nl.size); showAddQuestion = false
        })
    }
    questionToDelete?.let { idx ->
        AlertDialog(onDismissRequest = { questionToDelete = null }, title = { Text("Delete question?") },
            confirmButton = { TextButton(onClick = { val nl = editedQuiz.questions.toMutableList(); nl.removeAt(idx); editedQuiz = editedQuiz.copy(questions = nl, totalQuestions = nl.size); questionToDelete = null }) { Text("Delete", color = Color.Red) } },
            dismissButton = { TextButton(onClick = { questionToDelete = null }) { Text("Cancel") } })
    }
}

@Composable
fun QuestionReviewCard(index: Int, question: QuizQuestion, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = GrayGreen.copy(0.15f)), border = BorderStroke(1.dp, Color.White.copy(0.05f))) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Question ${index + 1}", color = Primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                IconButton(onEdit, Modifier.size(32.dp)) { Icon(Icons.Default.Edit, null, tint = AvatarBg, modifier = Modifier.size(18.dp)) }
                IconButton(onDelete, Modifier.size(32.dp)) { Icon(Icons.Default.Delete, null, tint = Color.Red.copy(0.7f), modifier = Modifier.size(18.dp)) }
            }
            Text(question.question, color = Color.White, modifier = Modifier.padding(vertical = 8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                question.options.forEachIndexed { i, opt ->
                    val isCorrect = question.correctAnswer == i
                    Row(modifier = Modifier.fillMaxWidth().background(if (isCorrect) AvatarBg.copy(0.1f) else Color.Transparent, RoundedCornerShape(4.dp)).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (isCorrect) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, tint = if (isCorrect) AvatarBg else Color.White.copy(0.3f), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("${'A' + i}. $opt", color = if (isCorrect) AvatarBg else Color.White.copy(0.7f), fontSize = 13.sp)
                    }
                }
            }
            if (question.explanation.isNotBlank()) {
                Text("Explanation:", color = Primary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                Text(question.explanation, color = Color.White.copy(0.6f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun QuestionEditDialog(question: QuizQuestion, onDismiss: () -> Unit, onSave: (QuizQuestion) -> Unit) {
    var text by remember { mutableStateOf(question.question) }
    var options by remember { mutableStateOf(question.options.ifEmpty { listOf("", "", "", "") }.toMutableList()) }
    var correct by remember { mutableIntStateOf(question.correctAnswer) }
    var explanation by remember { mutableStateOf(question.explanation) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Edit Question", color = Primary) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(text, { text = it }, label = { Text("Question") }, modifier = Modifier.fillMaxWidth())
                options.forEachIndexed { i, opt ->
                    OutlinedTextField(opt, { val nl = options.toMutableList(); nl[i] = it; options = nl }, label = { Text("Option ${'A' + i}") }, modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { RadioButton(correct == i, { correct = i }, colors = RadioButtonDefaults.colors(selectedColor = AvatarBg)) })
                }
                OutlinedTextField(explanation, { explanation = it }, label = { Text("Explanation") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = { if (text.isNotBlank() && options.all { it.isNotBlank() }) onSave(question.copy(question = text, options = options, correctAnswer = correct, explanation = explanation)) }, colors = ButtonDefaults.buttonColors(containerColor = AvatarBg)) { Text("Save", color = Color.Black) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White) } }, containerColor = Background)
}

@Composable
fun UploadLabel(text: String) { Text(text, color = Color.White.copy(0.65f), fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp)) }

@Composable
fun UploadTextField(value: String, onValueChange: (String) -> Unit, placeholder: String, minHeight: androidx.compose.ui.unit.Dp = 56.dp) {
    OutlinedTextField(value, onValueChange, placeholder = { Text(placeholder, color = Color.White.copy(0.4f), fontSize = 14.sp) }, modifier = Modifier.fillMaxWidth().heightIn(min = minHeight), shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.White.copy(0.1f), focusedBorderColor = AvatarBg, focusedTextColor = Color.White, unfocusedTextColor = Color.White))
}
